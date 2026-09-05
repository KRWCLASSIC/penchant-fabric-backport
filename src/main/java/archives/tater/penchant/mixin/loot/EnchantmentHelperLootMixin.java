package archives.tater.penchant.mixin.loot;

import archives.tater.penchant.util.PenchantLoot;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Filters enchant_with_levels / table-style random enchant pools.
 */
@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperLootMixin {
    @Inject(method = "getAvailableEnchantmentResults", at = @At("RETURN"))
    private static void penchant$filterLootPool(int level, ItemStack stack, boolean allowTreasure, CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        List<EnchantmentInstance> list = cir.getReturnValue();
        if (list == null || list.isEmpty()) return;
        list.removeIf(instance -> !PenchantLoot.isAllowed(stack, instance.enchantment));
    }
}
