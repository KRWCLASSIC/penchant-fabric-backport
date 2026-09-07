package archives.tater.penchant;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import static java.lang.Math.max;

/**
 * Per-enchantment tuning for the table + leveling curve.
 * Fallbacks mirror Penchant design:
 * experience = anvil cost, books = max(2 * minCost(1) - 5, 0).
 */
public record PenchantmentDefinition(int experienceCost, int bookRequirement, Cost progressCostFactor) {

    public record Cost(int base, int perLevel) {
        public int calculate(int targetLevel) {
            return max(base + max(targetLevel - 1, 0) * perLevel, 1);
        }
    }

    public enum Property {
        PROGRESS_COST_FACTOR("progress_cost_factor"),
        EXPERIENCE_COST("experience_cost"),
        BOOK_REQUIREMENT("book_requirement");

        private final String serializedName;

        Property(String serializedName) {
            this.serializedName = serializedName;
        }

        public String getSerializedName() {
            return serializedName;
        }
    }

    public static class Partial {
        private @Nullable Integer experienceCost;
        private @Nullable Integer bookRequirement;
        private @Nullable Cost progressCostFactor;

        public Partial() {}

        public Partial(@Nullable Integer experienceCost, @Nullable Integer bookRequirement, @Nullable Cost progressCostFactor) {
            this.experienceCost = experienceCost;
            this.bookRequirement = bookRequirement;
            this.progressCostFactor = progressCostFactor;
        }

        public @Nullable Integer getExperienceCost() {
            return experienceCost;
        }

        public void setExperienceCost(@Nullable Integer experienceCost) {
            this.experienceCost = experienceCost;
        }

        public @Nullable Integer getBookRequirement() {
            return bookRequirement;
        }

        public void setBookRequirement(@Nullable Integer bookRequirement) {
            this.bookRequirement = bookRequirement;
        }

        public @Nullable Cost getProgressCostFactor() {
            return progressCostFactor;
        }

        public void setProgressCostFactor(@Nullable Cost progressCostFactor) {
            this.progressCostFactor = progressCostFactor;
        }

        public boolean hasExperienceCost() {
            return experienceCost != null;
        }

        public boolean hasBookRequirement() {
            return bookRequirement != null;
        }

        public boolean hasProgressCostFactor() {
            return progressCostFactor != null;
        }

        public boolean isEmpty() {
            return experienceCost == null && bookRequirement == null && progressCostFactor == null;
        }
    }

    private static final Map<ResourceLocation, Partial> DATAPACK_DEFINITIONS = new HashMap<>();
    private static final Map<ResourceLocation, Partial> CONFIG_DEFINITIONS = new HashMap<>();
    private static final Map<Enchantment, PenchantmentDefinition> RESOLVED_CACHE = new WeakHashMap<>();
    private static final Map<ResourceLocation, PenchantmentDefinition> CLIENT_SYNCED_DEFINITIONS = new HashMap<>();

    public int getProgressCostFactor(int targetLevel) {
        return progressCostFactor != null ? progressCostFactor.calculate(targetLevel) : 1;
    }

    public int baseProgressCost() {
        return progressCostFactor != null ? progressCostFactor.base() : 1;
    }

    /** Same mapping vanilla uses for anvil cost from rarity. */
    public static int anvilCostFromRarity(Enchantment enchantment) {
        return switch (enchantment.getRarity()) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 4;
            case VERY_RARE -> 8;
        };
    }

    public static PenchantmentDefinition createFallback(Enchantment enchantment) {
        int min = enchantment.getMinCost(1);
        int base = max(enchantment.getMaxCost(1) / 2, 3);
        int perLevel = max(base / 2, 1);
        return new PenchantmentDefinition(
                anvilCostFromRarity(enchantment),
                max(2 * min - 5, 0),
                new Cost(base, perLevel)
        );
    }

    public static synchronized PenchantmentDefinition getDefinition(Enchantment enchantment) {
        if (!CLIENT_SYNCED_DEFINITIONS.isEmpty()) {
            ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
            if (id != null && CLIENT_SYNCED_DEFINITIONS.containsKey(id)) {
                return CLIENT_SYNCED_DEFINITIONS.get(id);
            }
        }
        return RESOLVED_CACHE.computeIfAbsent(enchantment, PenchantmentDefinition::resolve);
    }

    public static PenchantmentDefinition resolve(Enchantment enchantment) {
        PenchantmentDefinition fallback = createFallback(enchantment);
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        if (id == null) return fallback;

        Partial datapack = DATAPACK_DEFINITIONS.get(id);
        Partial config = CONFIG_DEFINITIONS.get(id);

        int xpCost = (datapack != null && datapack.hasExperienceCost())
                ? datapack.getExperienceCost()
                : (config != null && config.hasExperienceCost() ? config.getExperienceCost() : fallback.experienceCost());

        int bookReq = (datapack != null && datapack.hasBookRequirement())
                ? datapack.getBookRequirement()
                : (config != null && config.hasBookRequirement() ? config.getBookRequirement() : fallback.bookRequirement());

        Cost cost = (datapack != null && datapack.hasProgressCostFactor())
                ? datapack.getProgressCostFactor()
                : (config != null && config.hasProgressCostFactor() ? config.getProgressCostFactor() : fallback.progressCostFactor());

        return new PenchantmentDefinition(xpCost, bookReq, cost);
    }

    public static synchronized void setDatapackDefinitions(Map<ResourceLocation, Partial> definitions) {
        DATAPACK_DEFINITIONS.clear();
        DATAPACK_DEFINITIONS.putAll(definitions);
        RESOLVED_CACHE.clear();
        CLIENT_SYNCED_DEFINITIONS.clear();
    }

    public static synchronized void setConfigDefinitions(Map<ResourceLocation, Partial> definitions) {
        CONFIG_DEFINITIONS.clear();
        CONFIG_DEFINITIONS.putAll(definitions);
        RESOLVED_CACHE.clear();
        CLIENT_SYNCED_DEFINITIONS.clear();
    }

    public static synchronized Map<ResourceLocation, Partial> getConfigDefinitions() {
        return Collections.unmodifiableMap(CONFIG_DEFINITIONS);
    }

    public static synchronized void setClientDefinitions(Map<ResourceLocation, PenchantmentDefinition> definitions) {
        CLIENT_SYNCED_DEFINITIONS.clear();
        CLIENT_SYNCED_DEFINITIONS.putAll(definitions);
        RESOLVED_CACHE.clear();
    }

    public static synchronized void clearClientDefinitions() {
        CLIENT_SYNCED_DEFINITIONS.clear();
        RESOLVED_CACHE.clear();
    }

    public static synchronized Map<ResourceLocation, PenchantmentDefinition> getAllResolvedDefinitions() {
        Map<ResourceLocation, PenchantmentDefinition> map = new HashMap<>();
        for (Enchantment enchantment : BuiltInRegistries.ENCHANTMENT) {
            ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
            if (id != null) {
                map.put(id, resolve(enchantment));
            }
        }
        return map;
    }

    public static synchronized boolean isDatapackForced(Enchantment enchantment, Property property) {
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        if (id == null) return false;
        Partial datapack = DATAPACK_DEFINITIONS.get(id);
        if (datapack == null) return false;
        return switch (property) {
            case PROGRESS_COST_FACTOR -> datapack.hasProgressCostFactor();
            case EXPERIENCE_COST -> datapack.hasExperienceCost();
            case BOOK_REQUIREMENT -> datapack.hasBookRequirement();
        };
    }

    public static synchronized String getSource(Enchantment enchantment, Property property) {
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        if (id != null) {
            Partial datapack = DATAPACK_DEFINITIONS.get(id);
            if (datapack != null) {
                boolean has = switch (property) {
                    case PROGRESS_COST_FACTOR -> datapack.hasProgressCostFactor();
                    case EXPERIENCE_COST -> datapack.hasExperienceCost();
                    case BOOK_REQUIREMENT -> datapack.hasBookRequirement();
                };
                if (has) return "Datapack";
            }

            Partial config = CONFIG_DEFINITIONS.get(id);
            if (config != null) {
                boolean has = switch (property) {
                    case PROGRESS_COST_FACTOR -> config.hasProgressCostFactor();
                    case EXPERIENCE_COST -> config.hasExperienceCost();
                    case BOOK_REQUIREMENT -> config.hasBookRequirement();
                };
                if (has) return "Server Config";
            }
        }
        return "Default (Fallback)";
    }

    public static synchronized Partial getOrCreateConfigPartial(ResourceLocation id) {
        return CONFIG_DEFINITIONS.computeIfAbsent(id, k -> new Partial());
    }

    public static synchronized boolean removeConfigOverride(ResourceLocation id) {
        Partial removed = CONFIG_DEFINITIONS.remove(id);
        RESOLVED_CACHE.clear();
        CLIENT_SYNCED_DEFINITIONS.clear();
        return removed != null;
    }

    public static synchronized void invalidateCache() {
        RESOLVED_CACHE.clear();
        CLIENT_SYNCED_DEFINITIONS.clear();
    }
}
