package archives.tater.penchant.mixin.drops;

import archives.tater.penchant.registry.PenchantFlag;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Drowned.class)
public class DrownedMixin {
    @Redirect(
            method = "populateDefaultEquipmentSlots",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/monster/Drowned;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"
            )
    )
    private void penchant$guaranteeTrident(Drowned instance, EquipmentSlot slot, ItemStack stack) {
        instance.setItemSlot(slot, stack);
        if (PenchantFlag.GUARANTEED_TRIDENT_DROP.isEnabled() && stack.is(Items.TRIDENT)) {
            instance.setDropChance(slot, 1f);
        }
    }
}
