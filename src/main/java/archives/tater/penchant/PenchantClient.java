package archives.tater.penchant;

import archives.tater.penchant.client.PenchantKeys;
import archives.tater.penchant.client.PenchantTooltips;
import archives.tater.penchant.client.gui.screen.PenchantmentScreen;
import archives.tater.penchant.network.PenchantNetworking;
import archives.tater.penchant.registry.PenchantMenus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.gui.screens.MenuScreens;

@Environment(EnvType.CLIENT)
public class PenchantClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(PenchantMenus.PENCHANTMENT_MENU, PenchantmentScreen::new);
        ItemTooltipCallback.EVENT.register(PenchantTooltips::onTooltip);
        PenchantNetworking.registerClient();
    }
}
