package archives.tater.penchant.util;

import archives.tater.penchant.registry.PenchantEnchantmentTags;
import archives.tater.penchant.registry.PenchantFlag;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * 1.20.1 loot filtering (no #minecraft:on_random_loot tag like 1.21+).
 */
public final class PenchantLoot {
    private PenchantLoot() {}

    public static boolean isAllowed(ItemStack stack, Enchantment enchantment) {
        if (!PenchantFlag.LOOT_REWORK.isEnabled()) return true;
        if (PenchantEnchantmentTags.isDisabled(enchantment)) return false;
        if (stack.is(Items.BOOK)) {
            return PenchantEnchantmentTags.isBookLoot(enchantment);
        }
        return PenchantEnchantmentTags.isEquipmentLoot(enchantment);
    }
}
