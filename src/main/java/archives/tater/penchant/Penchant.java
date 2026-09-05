package archives.tater.penchant;

import archives.tater.penchant.network.PenchantNetworking;
import archives.tater.penchant.registry.PenchantItems;
import archives.tater.penchant.registry.PenchantMenus;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Penchant implements ModInitializer {
    public static final String MOD_ID = "penchant";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Penchant initializing for Minecraft 1.20.1");

        PenchantItems.register();
        PenchantMenus.register();
        PenchantNetworking.registerServer();

        ModContainer container = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
        registerBuiltinResourcePack("table_rework", container);
        registerBuiltinResourcePack("bookshelf_placement", container);
        registerBuiltinResourcePack("no_anvil_books", container);
        registerBuiltinResourcePack("durability_rework", container);
        registerBuiltinResourcePack("loot_rework", container);
        registerBuiltinResourcePack("guaranteed_drops", container);
    }

    private static void registerBuiltinResourcePack(String path, ModContainer container) {
        ResourceManagerHelper.registerBuiltinResourcePack(
                id(path),
                container,
                Component.translatable("dataPack.penchant." + path + ".name"),
                ResourcePackActivationType.DEFAULT_ENABLED
        );
    }
}
