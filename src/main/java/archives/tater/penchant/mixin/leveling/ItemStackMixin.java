package archives.tater.penchant.mixin.leveling;

import archives.tater.penchant.component.EnchantmentProgress;
import archives.tater.penchant.enchantment.UnbreakingRework;
import archives.tater.penchant.registry.PenchantItemTags;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "isDamageableItem", at = @At("HEAD"), cancellable = true)
    private void penchant$unbreakingV(CallbackInfoReturnable<Boolean> cir) {
        if (UnbreakingRework.isUnbreakable((ItemStack) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurtAndBreak", at = @At("HEAD"))
    private <T extends LivingEntity> void penchant$updateProgress(int amount, T entity, Consumer<T> onBroken, CallbackInfo ci) {
        if (!entity.level().isClientSide && entity instanceof ServerPlayer player) {
            EnchantmentProgress.onDurabilityDamage((ItemStack) (Object) this, player);
        }
    }

    @ModifyVariable(method = "enchant(Lnet/minecraft/world/item/enchantment/Enchantment;I)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int penchant$forceLevel(int level, Enchantment enchantment) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.is(Items.ENCHANTED_BOOK) || self.is(Items.BOOK)) {
            return 1;
        }
        if (self.is(PenchantItemTags.MAX_LEVEL_ENCHANTMENTS)) {
            return enchantment.getMaxLevel();
        }
        return 1;
    }
}
