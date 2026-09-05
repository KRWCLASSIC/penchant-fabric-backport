package archives.tater.penchant.registry;

import archives.tater.penchant.Penchant;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class PenchantItemTags {
    private PenchantItemTags() {}

    public static final TagKey<Item> MAX_LEVEL_ENCHANTMENTS =
            TagKey.create(Registries.ITEM, Penchant.id("max_level_enchantments"));
}
