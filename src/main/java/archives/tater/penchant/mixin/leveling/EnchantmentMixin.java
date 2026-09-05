package archives.tater.penchant.mixin.leveling;

import archives.tater.penchant.util.PenchantmentHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.enchantment.Enchantment;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hide Roman numerals only in Penchant UI (table slots), matching NO_LEVEL_NAME_CONTEXT.
 */
@Mixin(Enchantment.class)
public class EnchantmentMixin {
    @Inject(method = "getFullname(I)Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true)
    private void penchant$hideRomanNumeralInUi(int level, CallbackInfoReturnable<Component> cir) {
        if (!PenchantmentHelper.isNoLevelNameContext()) return;

        Enchantment self = (Enchantment) (Object) this;
        MutableComponent name = Component.translatable(self.getDescriptionId());
        if (self.isCurse()) {
            name.withStyle(ChatFormatting.RED);
        } else {
            name.withStyle(ChatFormatting.GRAY);
        }
        cir.setReturnValue(name);
    }
}
