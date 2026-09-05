package archives.tater.penchant.registry;

import archives.tater.penchant.Penchant;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Collection;

/**
 * Feature flags tied to built-in datapack modules. Prefer the server's selected
 * pack list (what the player toggles); fall back to the module's marker item tag
 * on the client when no server is available.
 */
public enum PenchantFlag {
    REWORKED_TABLE_MENU("table_rework"),
    LENIENT_BOOKSHELF_PLACEMENT("bookshelf_placement"),
    NO_ANVIL_BOOKS("no_anvil_books"),
    DURABILITY_REWORK("durability_rework"),
    LOOT_REWORK("loot_rework"),
    GUARANTEED_ENCHANTED_DROP("guaranteed_drops"),
    GUARANTEED_TRIDENT_DROP("guaranteed_drops");

    private static MinecraftServer currentServer;

    static {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> currentServer = null);
    }

    private final String packPath;
    private final ResourceLocation packId;
    private final TagKey<Item> enabledTag;

    PenchantFlag(String packPath) {
        this.packPath = packPath;
        this.packId = Penchant.id(packPath);
        this.enabledTag = TagKey.create(Registries.ITEM, Penchant.id("flags/" + packPath));
    }

    public boolean isEnabled() {
        if (currentServer != null) {
            return isSelected(currentServer.getPackRepository().getSelectedIds());
        }
        return BuiltInRegistries.ITEM.getTag(enabledTag)
                .map(tag -> tag.size() > 0)
                .orElse(false);
    }

    private boolean isSelected(Collection<String> selected) {
        String exact = packId.toString();
        for (String id : selected) {
            if (id.equals(exact) || id.equals(packPath)
                    || id.endsWith(":" + packPath) || id.endsWith("/" + packPath)) {
                return true;
            }
        }
        return false;
    }
}
