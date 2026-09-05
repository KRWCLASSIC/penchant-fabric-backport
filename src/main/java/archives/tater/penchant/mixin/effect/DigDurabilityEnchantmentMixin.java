package archives.tater.penchant.mixin.effect;

import archives.tater.penchant.enchantment.UnbreakingRework;
import archives.tater.penchant.registry.PenchantFlag;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DigDurabilityEnchantment.class)
public class DigDurabilityEnchantmentMixin {
    @Inject(method = "getMaxLevel", at = @At("HEAD"), cancellable = true)
    private void penchant$fiveLevels(CallbackInfoReturnable<Integer> cir) {
        if (PenchantFlag.DURABILITY_REWORK.isEnabled()) {
            cir.setReturnValue(UnbreakingRework.MAX_LEVEL);
        }
    }

    @Inject(method = "shouldIgnoreDurabilityDrop", at = @At("HEAD"), cancellable = true)
    private static void penchant$reworkCurve(ItemStack stack, int level, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
        if (!PenchantFlag.DURABILITY_REWORK.isEnabled()) return;
        cir.setReturnValue(UnbreakingRework.shouldIgnoreDurabilityDrop(level, random));
    }
}
