package archives.tater.penchant.menu;

import archives.tater.penchant.network.PenchantNetworking;
import archives.tater.penchant.registry.PenchantEnchantmentTags;
import archives.tater.penchant.registry.PenchantFlag;
import archives.tater.penchant.registry.PenchantMenus;
import archives.tater.penchant.util.PenchantmentHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PenchantmentMenu extends AbstractContainerMenu {
    private final Container enchantSlots = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };

    private final DataSlot bookCount = addDataSlot(DataSlot.standalone());
    private final DataSlot hasDisenchanter = addDataSlot(DataSlot.standalone());
    private final ContainerLevelAccess access;
    private final Player player;
    private Set<Enchantment> availableEnchantments = Set.of();
    private List<Enchantment> displayedEnchantments = List.of();
    private Runnable onSlotsChange = () -> {};
    private int unlockRefreshTicker;

    public static PenchantmentMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Set<Enchantment> unlocked = readEnchantmentSet(buf);
        int books = buf.readVarInt();
        boolean disenchanter = buf.readBoolean();
        PenchantmentMenu menu = new PenchantmentMenu(id, inv, ContainerLevelAccess.create(inv.player.level(), pos), false);
        menu.applyClientOpenState(unlocked, books, disenchanter);
        return menu;
    }

    public static void writeOpenData(FriendlyByteBuf buf, Level level, BlockPos pos, Player player) {
        buf.writeBlockPos(pos);
        Set<Enchantment> unlocked = player.getAbilities().instabuild
                ? new HashSet<>(BuiltInRegistries.ENCHANTMENT.stream().toList())
                : getUnlockedEnchantments(level, pos);
        writeEnchantmentSet(buf, unlocked);
        buf.writeVarInt(countBooks(level, pos));
        buf.writeBoolean(player.getAbilities().instabuild || hasDisenchanter(level, pos));
    }

    private static void writeEnchantmentSet(FriendlyByteBuf buf, Set<Enchantment> set) {
        buf.writeVarInt(set.size());
        for (Enchantment enchantment : set) {
            buf.writeVarInt(BuiltInRegistries.ENCHANTMENT.getId(enchantment));
        }
    }

    private static Set<Enchantment> readEnchantmentSet(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Set<Enchantment> set = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.byId(buf.readVarInt());
            if (enchantment != null) set.add(enchantment);
        }
        return set;
    }

    public PenchantmentMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL, true);
    }

    public PenchantmentMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        this(containerId, playerInventory, access, true);
    }

    private PenchantmentMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, boolean refresh) {
        super(PenchantMenus.PENCHANTMENT_MENU, containerId);
        this.player = playerInventory.player;
        this.access = access;

        addSlot(new Slot(enchantSlots, 0, 15, 58) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(enchantSlots, 1, 35, 58) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isEnchantingIngredient(stack) || (canDisenchant() && isDisenchantingIngredient(stack));
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return isDisenchantingIngredient(stack) ? 1 : super.getMaxStackSize(stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 23 + col * 18, 90 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 23 + col * 18, 148));
        }

        if (refresh) {
            refreshTableState(false);
        }
    }

    /** Applied on the client from the open-screen buffer (authoritative initial unlocks). */
    public void applyClientOpenState(Set<Enchantment> unlocked, int books, boolean disenchanter) {
        this.availableEnchantments = unlocked;
        this.bookCount.set(books);
        this.hasDisenchanter.set(disenchanter ? 1 : 0);
        rebuildDisplayed();
    }

    /** Force-sync unlocks after the container is open. */
    public void sendEnchantments() {
        access.execute((level, pos) -> {
            if (level.isClientSide) return;
            bookCount.set(countBooks(level, pos));
            hasDisenchanter.set(player.getAbilities().instabuild || hasDisenchanter(level, pos) ? 1 : 0);
            Set<Enchantment> unlocked = getUnlockedEnchantments(level, pos);
            availableEnchantments = player.getAbilities().instabuild
                    ? new HashSet<>(BuiltInRegistries.ENCHANTMENT.stream().toList())
                    : unlocked;
            if (player instanceof ServerPlayer serverPlayer) {
                PenchantNetworking.sendUnlocked(serverPlayer, availableEnchantments);
            }
        });
    }

    public void setSlotChangeListener(Runnable listener) {
        this.onSlotsChange = listener;
    }

    public ItemStack getEnchantingStack() {
        return enchantSlots.getItem(0);
    }

    public ItemStack getIngredientStack() {
        return enchantSlots.getItem(1);
    }

    public int getBookCount() {
        return bookCount.get();
    }

    public boolean hasDisenchanter() {
        return hasDisenchanter.get() != 0;
    }

    public int getPlayerXp() {
        return player.experienceLevel;
    }

    public List<Enchantment> getDisplayedEnchantments() {
        return displayedEnchantments;
    }

    public boolean isAvailable(Enchantment enchantment) {
        return player.getAbilities().instabuild || availableEnchantments.contains(enchantment);
    }

    public boolean isEnchanting() {
        ItemStack ingredient = getIngredientStack();
        return isEnchantingIngredient(ingredient) || ingredient.isEmpty();
    }

    public boolean isDisenchanting() {
        return canDisenchant() && isDisenchantingIngredient(getIngredientStack());
    }

    public boolean canDisenchant() {
        if (!hasDisenchanter()) return false;
        ItemStack stack = getEnchantingStack();
        return stack.isEmpty() || !PenchantmentHelper.getEnchantments(stack).isEmpty();
    }

    public static boolean isEnchantingIngredient(ItemStack stack) {
        return stack.is(Items.LAPIS_LAZULI);
    }

    public static boolean isDisenchantingIngredient(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
    }

    private void refreshTableState(boolean sendPacket) {
        access.execute((level, pos) -> {
            if (level.isClientSide) return;

            bookCount.set(countBooks(level, pos));
            hasDisenchanter.set(player.getAbilities().instabuild || hasDisenchanter(level, pos) ? 1 : 0);

            Set<Enchantment> unlocked = getUnlockedEnchantments(level, pos);
            Set<Enchantment> next = player.getAbilities().instabuild
                    ? new HashSet<>(BuiltInRegistries.ENCHANTMENT.stream().toList())
                    : unlocked;

            if (next.equals(availableEnchantments)) return;
            availableEnchantments = next;

            if (sendPacket && player instanceof ServerPlayer serverPlayer) {
                PenchantNetworking.sendUnlocked(serverPlayer, availableEnchantments);
            }
        });
        if (access == ContainerLevelAccess.NULL && player.getAbilities().instabuild) {
            availableEnchantments = new HashSet<>(BuiltInRegistries.ENCHANTMENT.stream().toList());
        }
    }

    public void setUnlockedFromClient(Set<Enchantment> unlocked) {
        if (unlocked.equals(availableEnchantments)) return;
        this.availableEnchantments = unlocked;
        rebuildDisplayed();
        onSlotsChange.run();
    }

    private static boolean isValidShelfOffset(Level level, BlockPos tablePos, BlockPos offset) {
        if (PenchantFlag.LENIENT_BOOKSHELF_PLACEMENT.isEnabled()) {
            return true;
        }
        return EnchantmentTableBlock.isValidBookShelf(level, tablePos, offset);
    }

    private static int countBooks(Level level, BlockPos tablePos) {
        int count = 0;
        for (BlockPos offset : PenchantmentHelper.bookshelfOffsets()) {
            if (!isValidShelfOffset(level, tablePos, offset)) continue;
            BlockPos pos = tablePos.offset(offset);
            var state = level.getBlockState(pos);
            if (state.is(Blocks.BOOKSHELF)) {
                count += 3;
            } else if (state.is(Blocks.CHISELED_BOOKSHELF) && level.getBlockEntity(pos) instanceof ChiseledBookShelfBlockEntity shelf) {
                for (int i = 0; i < shelf.getContainerSize(); i++) {
                    if (!shelf.getItem(i).isEmpty()) count++;
                }
            } else if (PenchantmentHelper.isBookshelfPowerProvider(state)) {
                count += 1;
            }
        }
        return count;
    }

    public static boolean hasDisenchanter(Level level, BlockPos tablePos) {
        for (BlockPos offset : PenchantmentHelper.bookshelfOffsets()) {
            if (level.getBlockState(tablePos.offset(offset)).is(Blocks.GRINDSTONE)) {
                return true;
            }
        }
        return false;
    }

    public static Set<Enchantment> getUnlockedEnchantments(Level level, BlockPos tablePos) {
        Set<Enchantment> unlocked = new HashSet<>();

        BuiltInRegistries.ENCHANTMENT.forEach(enchantment -> {
            if (PenchantEnchantmentTags.isDisabled(enchantment)) return;
            if (PenchantFlag.LOOT_REWORK.isEnabled()) {
                if (PenchantEnchantmentTags.isCommon(enchantment)) unlocked.add(enchantment);
            } else if (!enchantment.isTreasureOnly() && !enchantment.isCurse() && enchantment.isDiscoverable()) {
                unlocked.add(enchantment);
            }
        });

        for (BlockPos offset : PenchantmentHelper.bookshelfOffsets()) {
            if (!isValidShelfOffset(level, tablePos, offset)) continue;
            BlockPos pos = tablePos.offset(offset);
            if (!(level.getBlockEntity(pos) instanceof ChiseledBookShelfBlockEntity shelf)) continue;
            for (int i = 0; i < shelf.getContainerSize(); i++) {
                ItemStack book = shelf.getItem(i);
                if (book.isEmpty()) continue;
                for (Enchantment enchantment : EnchantmentHelper.getEnchantments(book).keySet()) {
                    if (!PenchantEnchantmentTags.isDisabled(enchantment)) {
                        unlocked.add(enchantment);
                    }
                }
            }
        }
        return unlocked;
    }

    public void clientSelect(Enchantment enchantment) {
        PenchantNetworking.sendSelectEnchantment(enchantment);
    }

    public void handleEnchant(Enchantment enchantment) {
        ItemStack stack = getEnchantingStack();
        if (stack.isEmpty()) return;

        refreshTableState(false);

        if (isDisenchanting()) {
            if (!PenchantmentHelper.hasEnchantment(stack, enchantment)) return;
            boolean creative = player.getAbilities().instabuild;
            int booksNeeded = PenchantmentHelper.getBookRequirement(enchantment);
            if (!creative && getBookCount() < booksNeeded) return;

            Map<Enchantment, Integer> map = new java.util.HashMap<>(PenchantmentHelper.getEnchantments(stack));
            int enchantLevel = map.getOrDefault(enchantment, 1);
            map.remove(enchantment);
            EnchantmentHelper.setEnchantments(map, stack);

            ItemStack ingredient = getIngredientStack();
            ItemStack out = PenchantmentHelper.enchant(ingredient.isEmpty() ? new ItemStack(Items.BOOK) : ingredient.copyWithCount(1), enchantment, enchantLevel);
            if (ingredient.getCount() <= 1) enchantSlots.setItem(1, out);
            else {
                ingredient.shrink(1);
                if (!player.getInventory().add(out)) player.drop(out, false);
            }
            enchantSlots.setItem(0, PenchantmentHelper.fixBookType(stack));
            access.execute((world, pos) -> world.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1f, 1f));
            enchantSlots.setChanged();
            return;
        }

        if (!isEnchanting()) return;
        if (getIngredientStack().isEmpty() && !player.getAbilities().instabuild) return;
        if (!player.getAbilities().instabuild && !isEnchantingIngredient(getIngredientStack())) return;
        if (!isAvailable(enchantment)) return;
        if (!PenchantmentHelper.canEnchant(stack, enchantment)) return;

        boolean creative = player.getAbilities().instabuild;
        int xpCost = PenchantmentHelper.getXpLevelCost(enchantment);
        int booksNeeded = PenchantmentHelper.getBookRequirement(enchantment);
        if (!creative) {
            if (player.experienceLevel < xpCost) return;
            if (getBookCount() < booksNeeded) return;
        }

        ItemStack result = PenchantmentHelper.enchant(stack, enchantment, 1);
        enchantSlots.setItem(0, result);

        if (!creative) {
            player.giveExperienceLevels(-xpCost);
            getIngredientStack().shrink(1);
        }

        access.execute((world, pos) -> world.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1f, 1f));
        enchantSlots.setChanged();
    }

    private void rebuildDisplayed() {
        ItemStack stack = getEnchantingStack();
        if (stack.isEmpty()) {
            displayedEnchantments = List.of();
            return;
        }

        if (isDisenchanting()) {
            displayedEnchantments = PenchantmentHelper.getEnchantments(stack).keySet().stream()
                    .sorted(Comparator.comparing(e -> String.valueOf(BuiltInRegistries.ENCHANTMENT.getKey(e))))
                    .toList();
        } else if (isEnchanting() && (stack.isEnchantable() || stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK) || !PenchantmentHelper.getEnchantments(stack).isEmpty())) {
            boolean creative = player.getAbilities().instabuild;
            List<Enchantment> list = new ArrayList<>();
            for (Enchantment enchantment : BuiltInRegistries.ENCHANTMENT) {
                if (PenchantEnchantmentTags.isDisabled(enchantment)) continue;
                if (!PenchantmentHelper.canEnchantItem(stack, enchantment) && !PenchantmentHelper.hasEnchantment(stack, enchantment)) continue;
                if (enchantment.isCurse() && !availableEnchantments.contains(enchantment) && !PenchantmentHelper.hasEnchantment(stack, enchantment)) continue;
                list.add(enchantment);
            }
            list.sort(Comparator
                    .comparingInt((Enchantment e) -> creative || availableEnchantments.contains(e) || PenchantmentHelper.hasEnchantment(stack, e) ? 0 : 2)
                    .thenComparingInt(e -> e.isCurse() ? 1 : 0)
                    .thenComparing(e -> String.valueOf(BuiltInRegistries.ENCHANTMENT.getKey(e))));
            displayedEnchantments = list;
        } else {
            displayedEnchantments = List.of();
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container != enchantSlots) return;
        refreshTableState(true);
        rebuildDisplayed();
        onSlotsChange.run();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < 2) {
                if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
            } else if (isEnchantingIngredient(stack) || (canDisenchant() && isDisenchantingIngredient(stack))) {
                if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else {
                if (!getEnchantingStack().isEmpty()) return ItemStack.EMPTY;
                ItemStack one = stack.copyWithCount(1);
                stack.shrink(1);
                slots.get(0).setByPlayer(one);
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!player.level().isClientSide && (++unlockRefreshTicker % 20) == 0) {
            refreshTableState(true);
        }
        return stillValid(access, player, Blocks.ENCHANTING_TABLE);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, enchantSlots));
    }
}
