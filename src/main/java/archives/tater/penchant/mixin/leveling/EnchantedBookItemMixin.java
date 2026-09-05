package archives.tater.penchant.mixin.leveling;

import archives.tater.penchant.registry.PenchantEnchantmentTags;

import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Creative / generated enchanted books for levelable enchants are always level 1.
 */
@Mixin(EnchantedBookItem.class)
public class EnchantedBookItemMixin {
    @Redirect(
            method = "createForEnchantment",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/EnchantedBookItem;addEnchantment(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/EnchantmentInstance;)V"
            )
    )
    private static void penchant$levelOne(ItemStack stack, EnchantmentInstance instance) {
        if (!PenchantEnchantmentTags.isNoLeveling(instance.enchantment) && instance.level != 1) {
            instance = new EnchantmentInstance(instance.enchantment, 1);
        }
        EnchantedBookItem.addEnchantment(stack, instance);
    }
}
