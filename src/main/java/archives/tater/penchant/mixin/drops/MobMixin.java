package archives.tater.penchant.mixin.drops;

import archives.tater.penchant.registry.PenchantFlag;
import archives.tater.penchant.util.PenchantmentHelper;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity {
    @Shadow
    public abstract void setDropChance(EquipmentSlot slot, float dropChance);

    @Shadow
    public abstract ItemStack getItemBySlot(EquipmentSlot slot);

    protected MobMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "getEquipmentDropChance", at = @At("HEAD"), cancellable = true)
    private void penchant$guaranteeDropChance(EquipmentSlot slot, CallbackInfoReturnable<Float> cir) {
        ItemStack stack = this.getItemBySlot(slot);
        if (stack.isEmpty()) return;

        if (PenchantFlag.GUARANTEED_TRIDENT_DROP.isEnabled() && stack.is(Items.TRIDENT)) {
            cir.setReturnValue(1.0f);
            return;
        }

        if (PenchantFlag.GUARANTEED_ENCHANTED_DROP.isEnabled() && (stack.isEnchanted() || !PenchantmentHelper.getEnchantments(stack).isEmpty())) {
            cir.setReturnValue(1.0f);
        }
    }

    @Inject(
            method = "enchantSpawnedWeapon",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;enchantItem(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/item/ItemStack;IZ)Lnet/minecraft/world/item/ItemStack;")
    )
    private void penchant$guaranteeWeaponDrop(RandomSource random, float chance, CallbackInfo ci) {
        if (PenchantFlag.GUARANTEED_ENCHANTED_DROP.isEnabled()) {
            setDropChance(EquipmentSlot.MAINHAND, 1f);
        }
    }

    @Inject(
            method = "enchantSpawnedArmor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;enchantItem(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/item/ItemStack;IZ)Lnet/minecraft/world/item/ItemStack;")
    )
    private void penchant$guaranteeArmorDrop(RandomSource random, float chance, EquipmentSlot slot, CallbackInfo ci) {
        if (PenchantFlag.GUARANTEED_ENCHANTED_DROP.isEnabled()) {
            setDropChance(slot, 1f);
        }
    }
}
