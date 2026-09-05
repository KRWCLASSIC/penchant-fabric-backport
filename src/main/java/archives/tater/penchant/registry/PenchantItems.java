package archives.tater.penchant.registry;

import archives.tater.penchant.Penchant;
import archives.tater.penchant.item.TomeOfPenchantItem;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class PenchantItems {
    public static final Item TOME_OF_PENCHANT = Registry.register(
            BuiltInRegistries.ITEM,
            Penchant.id("tome_of_penchant"),
            new TomeOfPenchantItem(new Item.Properties().stacksTo(1))
    );

    private PenchantItems() {}

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(TOME_OF_PENCHANT);
        });
    }

    public static void register() {
        init();
    }
}
