package archives.tater.penchant.registry;

import archives.tater.penchant.Penchant;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;

public final class PenchantEnchantmentTags {
    private PenchantEnchantmentTags() {}

    public static final TagKey<Enchantment> DISABLED = TagKey.create(Registries.ENCHANTMENT, Penchant.id("disabled"));
    public static final TagKey<Enchantment> NO_LEVELING = TagKey.create(Registries.ENCHANTMENT, Penchant.id("no_leveling"));
    public static final TagKey<Enchantment> UNIQUE = TagKey.create(Registries.ENCHANTMENT, Penchant.id("unique"));
    public static final TagKey<Enchantment> RARE = TagKey.create(Registries.ENCHANTMENT, Penchant.id("rare"));
    public static final TagKey<Enchantment> UNCOMMON = TagKey.create(Registries.ENCHANTMENT, Penchant.id("uncommon"));
    public static final TagKey<Enchantment> COMMON = TagKey.create(Registries.ENCHANTMENT, Penchant.id("common"));
    public static final TagKey<Enchantment> ON_RANDOM_LOOT_BOOKS = TagKey.create(Registries.ENCHANTMENT, Penchant.id("on_random_loot_books"));

    public static boolean isNoLeveling(Enchantment enchantment) {
        return isIn(enchantment, NO_LEVELING);
    }

    public static boolean isDisabled(Enchantment enchantment) {
        return isIn(enchantment, DISABLED);
    }

    public static boolean isUnique(Enchantment enchantment) {
        return isIn(enchantment, UNIQUE);
    }

    public static boolean isCommon(Enchantment enchantment) {
        return isIn(enchantment, COMMON);
    }

    public static boolean isUncommon(Enchantment enchantment) {
        return isIn(enchantment, UNCOMMON);
    }

    public static boolean isRare(Enchantment enchantment) {
        return isIn(enchantment, RARE);
    }

    /** Equipment loot pool under loot rework: common + uncommon + rare. */
    public static boolean isEquipmentLoot(Enchantment enchantment) {
        return isCommon(enchantment) || isUncommon(enchantment) || isRare(enchantment);
    }

    /** Book loot pool under loot rework: uncommon + rare (no commons / curses). */
    public static boolean isBookLoot(Enchantment enchantment) {
        if (enchantment.isCurse() || isCommon(enchantment)) return false;
        return isUncommon(enchantment) || isRare(enchantment);
    }

    private static boolean isIn(Enchantment enchantment, TagKey<Enchantment> tag) {
        return BuiltInRegistries.ENCHANTMENT.getResourceKey(enchantment)
                .flatMap(BuiltInRegistries.ENCHANTMENT::getHolder)
                .map(holder -> holder.is(tag))
                .orElse(false);
    }
}
