package archives.tater.penchant.registry;

import archives.tater.penchant.Penchant;
import archives.tater.penchant.menu.PenchantmentMenu;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public final class PenchantMenus {
    public static final MenuType<PenchantmentMenu> PENCHANTMENT_MENU = Registry.register(
            BuiltInRegistries.MENU,
            Penchant.id("penchantment"),
            new ExtendedScreenHandlerType<>(PenchantmentMenu::fromNetwork)
    );

    private PenchantMenus() {}

    public static void init() {}

    public static void register() {
        init();
    }
}
