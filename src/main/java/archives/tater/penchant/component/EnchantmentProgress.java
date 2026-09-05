package archives.tater.penchant.component;

import archives.tater.penchant.registry.PenchantEnchantmentTags;
import archives.tater.penchant.util.PenchantmentHelper;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static net.minecraft.util.Mth.clamp;

/**
 * Enchantment leveling progress stored in ItemStack NBT under {@code penchant.progress}.
 */
public class EnchantmentProgress {
    public static final String ROOT_KEY = "penchant";
    public static final String PROGRESS_KEY = "progress";

    private final Object2IntOpenHashMap<Enchantment> progress;

    public EnchantmentProgress(Object2IntOpenHashMap<Enchantment> progress) {
        this.progress = progress;
    }

    public int getProgress(Enchantment enchantment) {
        return progress.getInt(enchantment);
    }

    public Mutable toMutable() {
        return new Mutable(this);
    }

    public static final EnchantmentProgress EMPTY = new EnchantmentProgress(new Object2IntOpenHashMap<>());

    public static class Mutable {
        private final Object2IntOpenHashMap<Enchantment> progress;

        public Mutable(EnchantmentProgress progress) {
            this.progress = progress.progress.clone();
        }

        public int getProgress(Enchantment enchantment) {
            return progress.getInt(enchantment);
        }

        public void setProgress(Enchantment enchantment, int value) {
            if (value <= 0) progress.removeInt(enchantment);
            else progress.put(enchantment, value);
        }

        public void removeProgress(Enchantment enchantment) {
            progress.removeInt(enchantment);
        }

        public void addProgress(Enchantment enchantment, int amount) {
            setProgress(enchantment, getProgress(enchantment) + amount);
        }

        public EnchantmentProgress toImmutable() {
            return new EnchantmentProgress(progress);
        }

        public boolean isEmpty() {
            return progress.isEmpty();
        }
    }

    public static boolean shouldShowTooltip(Enchantment enchantment) {
        return enchantment.getMaxLevel() != 1 && !PenchantEnchantmentTags.isNoLeveling(enchantment);
    }

    public static int getMaxProgress(Enchantment enchantment, int currentLevel, int maxDurability) {
        int durabilityFactor = maxDurability > 0 ? clamp(maxDurability / 100, 1, 8) : 1;
        return durabilityFactor * PenchantmentHelper.getProgressCostFactor(enchantment, currentLevel + 1);
    }

    public static EnchantmentProgress getProgress(ItemStack stack) {
        CompoundTag root = stack.getTagElement(ROOT_KEY);
        if (root == null || !root.contains(PROGRESS_KEY, CompoundTag.TAG_COMPOUND)) return EMPTY;

        CompoundTag tag = root.getCompound(PROGRESS_KEY);
        Object2IntOpenHashMap<Enchantment> map = new Object2IntOpenHashMap<>();
        for (String key : tag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null) continue;
            Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(id);
            if (enchantment != null) map.put(enchantment, tag.getInt(key));
        }
        return map.isEmpty() ? EMPTY : new EnchantmentProgress(map);
    }

    public static void setProgress(ItemStack stack, EnchantmentProgress value) {
        if (value.progress.isEmpty()) {
            CompoundTag root = stack.getTagElement(ROOT_KEY);
            if (root != null) {
                root.remove(PROGRESS_KEY);
                if (root.isEmpty()) stack.removeTagKey(ROOT_KEY);
            }
            return;
        }

        CompoundTag progressTag = new CompoundTag();
        value.progress.forEach((enchantment, amount) -> {
            ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
            if (id != null) progressTag.putInt(id.toString(), amount.intValue());
        });
        stack.getOrCreateTagElement(ROOT_KEY).put(PROGRESS_KEY, progressTag);
    }

    public static void onDurabilityDamage(ItemStack stack, @Nullable LivingEntity user) {
        addToProgress(stack, 1, user);
    }

    public static void addToProgress(ItemStack stack, int increase, @Nullable LivingEntity user) {
        Map<Enchantment, Integer> enchantments = getItemEnchantments(stack);
        if (enchantments.isEmpty()) return;

        Mutable mutable = getProgress(stack).toMutable();
        for (Enchantment enchantment : enchantments.keySet()) {
            if (!PenchantEnchantmentTags.isNoLeveling(enchantment)) {
                mutable.addProgress(enchantment, increase);
            }
        }

        updateEnchantmentsForStack(mutable, enchantments, stack, user);
        setProgress(stack, mutable.toImmutable());
    }

    public static void addRandomProgress(ItemStack stack, RandomSource random) {
        Map<Enchantment, Integer> enchantments = getItemEnchantments(stack);
        if (enchantments.isEmpty()) return;

        Mutable mutable = new Mutable(EMPTY);
        for (var entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();
            if (!PenchantEnchantmentTags.isNoLeveling(enchantment) && level < enchantment.getMaxLevel()) {
                mutable.setProgress(enchantment, (int) (random.nextFloat() * getMaxProgress(enchantment, level, stack.getMaxDamage())));
            }
        }
        if (!mutable.isEmpty()) setProgress(stack, mutable.toImmutable());
    }

    public static boolean updateEnchantments(Mutable progress, Map<Enchantment, Integer> enchantments, int maxDamage) {
        boolean changed = false;
        for (Enchantment enchantment : enchantments.keySet().toArray(Enchantment[]::new)) {
            if (PenchantEnchantmentTags.isNoLeveling(enchantment)) continue;

            int level = enchantments.get(enchantment);
            while (true) {
                if (level >= enchantment.getMaxLevel()) {
                    progress.removeProgress(enchantment);
                    break;
                }
                int maxProgress = getMaxProgress(enchantment, level, maxDamage);
                int value = progress.getProgress(enchantment);
                if (value < maxProgress) break;
                level++;
                progress.setProgress(enchantment, value - maxProgress);
            }

            if (level != enchantments.get(enchantment)) {
                enchantments.put(enchantment, level);
                changed = true;
            }
        }
        return changed;
    }

    public static void updateEnchantmentsForStack(Mutable progress, Map<Enchantment, Integer> enchantments, ItemStack stack, @Nullable LivingEntity user) {
        if (!updateEnchantments(progress, enchantments, stack.getMaxDamage())) return;
        setItemEnchantments(stack, enchantments);

        if (user == null) return;
        user.level().playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, user.getRandom().nextFloat() * 0.1F + 0.9F);
        if (user.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, user.getX(), user.getEyeY(), user.getZ(), 16, 0, 0, 0, 1.0);
        }
    }

    public static Map<Enchantment, Integer> getItemEnchantments(ItemStack stack) {
        if (stack.is(Items.ENCHANTED_BOOK) || stack.is(Items.BOOK)) {
            return EnchantmentHelper.getEnchantments(stack);
        }
        return EnchantmentHelper.getEnchantments(stack);
    }

    public static void setItemEnchantments(ItemStack stack, Map<Enchantment, Integer> enchantments) {
        if (stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK)) {
            ItemStack book = stack.is(Items.BOOK) ? new ItemStack(Items.ENCHANTED_BOOK) : stack.copy();
            book.setTag(stack.getTag() == null ? null : stack.getTag().copy());
            CompoundTag tag = book.getOrCreateTag();
            tag.remove("StoredEnchantments");
            for (var entry : enchantments.entrySet()) {
                EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(entry.getKey(), entry.getValue()));
            }
            stack.setTag(book.getTag());
            EnchantmentHelper.setEnchantments(enchantments, stack);
        } else {
            EnchantmentHelper.setEnchantments(enchantments, stack);
        }
    }
}
