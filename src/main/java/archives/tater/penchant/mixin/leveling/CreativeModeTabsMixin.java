package archives.tater.penchant.mixin.leveling;

import archives.tater.penchant.registry.PenchantEnchantmentTags;
import archives.tater.penchant.util.EnchantmentMaxLevelAccess;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.enchantment.Enchantment;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Creative inventory: one book per levelable enchantment (min level), matching Penchant leveling.
 * Targets every getMaxLevel() in this class (only book-generation lambdas use it).
 */
@Mixin(CreativeModeTabs.class)
public class CreativeModeTabsMixin {
    @Redirect(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"
            )
    )
    private static int penchant$onlyLevelOne(Enchantment enchantment) {
        if (PenchantEnchantmentTags.isNoLeveling(enchantment)) {
            return EnchantmentMaxLevelAccess.get(enchantment);
        }
        return enchantment.getMinLevel();
    }
}
