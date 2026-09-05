package archives.tater.penchant.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class PenchantKeys {
    private PenchantKeys() {}

    /**
     * Unobtrusive Ctrl detection that works anywhere (in-game, inventory screens, tooltips)
     * without registering any KeyMapping or interfering with sprint / keybindings.
     */
    public static boolean isShowProgressDown() {
        if (Screen.hasControlDown()) return true;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return false;
        long window = mc.getWindow().getWindow();
        if (window == 0) return false;

        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }
}
