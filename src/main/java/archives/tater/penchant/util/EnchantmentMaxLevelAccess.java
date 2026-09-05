package archives.tater.penchant.util;

import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Call {@link Enchantment#getMaxLevel()} from outside {@code CreativeModeTabs} mixin
 * handlers so a {@code @Redirect} on that class cannot recurse into itself.
 */
public final class EnchantmentMaxLevelAccess {
    private EnchantmentMaxLevelAccess() {}

    public static int get(Enchantment enchantment) {
        return enchantment.getMaxLevel();
    }
}
