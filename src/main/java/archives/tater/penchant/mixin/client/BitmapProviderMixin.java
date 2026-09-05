package archives.tater.penchant.mixin.client;

import archives.tater.penchant.client.FontUtils;

import net.minecraft.client.gui.font.providers.BitmapProvider;
import net.minecraft.resources.ResourceLocation;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Removes the +1 glyph advance on the progress-bar font so segments sit flush. */
@Mixin(BitmapProvider.Definition.class)
public class BitmapProviderMixin {
    @Shadow
    @Final
    private ResourceLocation file;

    @ModifyArg(
            method = "load",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/providers/BitmapProvider$Glyph;<init>(FLcom/mojang/blaze3d/platform/NativeImage;IIIIII)V"
            ),
            index = 6
    )
    private int penchant$disableSpacing(int advance) {
        return file.equals(FontUtils.FONT_TEXTURE) ? advance - 1 : advance;
    }
}
