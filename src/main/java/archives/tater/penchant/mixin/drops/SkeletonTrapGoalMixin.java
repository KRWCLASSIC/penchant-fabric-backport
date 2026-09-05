package archives.tater.penchant.mixin.drops;

import archives.tater.penchant.registry.PenchantFlag;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.SkeletonTrapGoal;
import net.minecraft.world.entity.monster.Skeleton;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkeletonTrapGoal.class)
public class SkeletonTrapGoalMixin {
    @Inject(method = "createSkeleton", at = @At("RETURN"))
    private void penchant$guaranteeDrop(DifficultyInstance difficulty, AbstractHorse horse, CallbackInfoReturnable<Skeleton> cir) {
        if (!PenchantFlag.GUARANTEED_ENCHANTED_DROP.isEnabled()) return;
        Skeleton skeleton = cir.getReturnValue();
        if (skeleton != null) {
            skeleton.setDropChance(EquipmentSlot.MAINHAND, 1f);
        }
    }
}
