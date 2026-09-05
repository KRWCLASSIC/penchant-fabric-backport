package archives.tater.penchant.util;

import archives.tater.penchant.PenchantmentDefinition;
import archives.tater.penchant.compat.MaxProtectionCompat;
import archives.tater.penchant.registry.PenchantFlag;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

import static java.lang.Math.abs;

public final class PenchantmentHelper {
    public static final boolean ITEM_DESCRIPTIONS_INSTALLED = FabricLoader.getInstance().isModLoaded("item_descriptions");

    private PenchantmentHelper() {}

    public static final List<BlockPos> LENIENT_BOOKSHELF_OFFSETS = BlockPos.betweenClosedStream(-3, -2, -3, 3, 2, 3)
            .filter(blockPos -> abs(blockPos.getX()) >= 2 || abs(blockPos.getZ()) >= 2 || blockPos.getY() >= 2 || blockPos.getY() <= -1)
            .map(BlockPos::immutable)
            .toList();

    private static final ThreadLocal<Boolean> NO_LEVEL_NAME_CONTEXT = ThreadLocal.withInitial(() -> false);

    public static boolean isNoLevelNameContext() {
        return NO_LEVEL_NAME_CONTEXT.get();
    }

    public static Component getName(Enchantment enchantment) {
        NO_LEVEL_NAME_CONTEXT.set(true);
        try {
            return enchantment.getFullname(1);
        } finally {
            NO_LEVEL_NAME_CONTEXT.remove();
        }
    }

    public static int getProgressCostFactor(Enchantment enchantment, int targetLevel) {
        return PenchantmentDefinition.getDefinition(enchantment).getProgressCostFactor(targetLevel);
    }

    public static int getBookRequirement(Enchantment enchantment) {
        return PenchantmentDefinition.getDefinition(enchantment).bookRequirement();
    }

    public static int getXpLevelCost(Enchantment enchantment) {
        return PenchantmentDefinition.getDefinition(enchantment).experienceCost();
    }

    public static boolean canEnchantItem(ItemStack stack, Enchantment enchantment) {
        if (stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK)) return true;
        return enchantment.canEnchant(stack);
    }

    public static Map<Enchantment, Integer> getEnchantments(ItemStack stack) {
        return EnchantmentHelper.getEnchantments(stack);
    }

    public static boolean hasEnchantment(ItemStack stack, Enchantment enchantment) {
        return getEnchantments(stack).getOrDefault(enchantment, 0) > 0;
    }

    public static boolean canEnchant(ItemStack stack, Enchantment enchantment) {
        if (hasEnchantment(stack, enchantment)) return false;
        if (!canEnchantItem(stack, enchantment)) return false;
        for (Enchantment other : getEnchantments(stack).keySet()) {
            if (!areCompatible(enchantment, other)) return false;
        }
        return true;
    }

    public static boolean areCompatible(Enchantment first, Enchantment second) {
        if (MaxProtectionCompat.allowsTogether(first, second)) return true;
        return first.isCompatibleWith(second);
    }

    /** Add an enchantment at the given level (always 1 for levelable enchants at the table). */
    public static ItemStack enchant(ItemStack stack, Enchantment enchantment, int level) {
        ItemStack result = stack.is(Items.BOOK) ? fixBookType(stack.copy()) : stack.copy();
        if (result.is(Items.BOOK)) {
            result = new ItemStack(Items.ENCHANTED_BOOK);
            if (stack.hasTag()) result.setTag(stack.getTag().copy());
        }
        Map<Enchantment, Integer> map = new java.util.HashMap<>(getEnchantments(result));
        map.put(enchantment, Math.max(level, 1));
        EnchantmentHelper.setEnchantments(map, result);
        return fixBookType(result);
    }

    public static ItemStack enchant(ItemStack stack, Enchantment enchantment) {
        return enchant(stack, enchantment, 1);
    }

    public static ItemStack fixBookType(ItemStack stack) {
        Map<Enchantment, Integer> enchantments = getEnchantments(stack);
        if (enchantments.isEmpty()) {
            if (stack.is(Items.ENCHANTED_BOOK)) {
                ItemStack book = new ItemStack(Items.BOOK, stack.getCount());
                if (stack.hasTag()) book.setTag(stack.getTag().copy());
                return book;
            }
        } else if (stack.is(Items.BOOK)) {
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK, stack.getCount());
            if (stack.hasTag()) book.setTag(stack.getTag().copy());
            EnchantmentHelper.setEnchantments(enchantments, book);
            return book;
        }
        return stack;
    }

    public static boolean isBookshelfPowerProvider(BlockState state) {
        return state.is(Blocks.BOOKSHELF)
                || state.is(Blocks.CHISELED_BOOKSHELF)
                || (state.getBlock() instanceof LecternBlock && state.hasProperty(LecternBlock.HAS_BOOK) && state.getValue(LecternBlock.HAS_BOOK));
    }

    public static List<BlockPos> bookshelfOffsets() {
        return PenchantFlag.LENIENT_BOOKSHELF_PLACEMENT.isEnabled()
                ? LENIENT_BOOKSHELF_OFFSETS
                : EnchantmentTableBlock.BOOKSHELF_OFFSETS;
    }
}
