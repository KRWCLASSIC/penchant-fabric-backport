package archives.tater.penchant;

import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Map;
import java.util.WeakHashMap;

import static java.lang.Math.max;

/**
 * Per-enchantment tuning for the table + leveling curve.
 * Fallbacks mirror Penchant design:
 * experience = anvil cost, books = max(2 * minCost(1) - 5, 0).
 */
public record PenchantmentDefinition(int experienceCost, int bookRequirement, int baseProgressCost) {
    private static final Map<Enchantment, PenchantmentDefinition> CACHE = new WeakHashMap<>();

    public int getProgressCostFactor(int targetLevel) {
        return max(baseProgressCost + (targetLevel - 1) * max(baseProgressCost / 2, 1), 1);
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
        return new PenchantmentDefinition(
                anvilCostFromRarity(enchantment),
                max(2 * min - 5, 0),
                max(enchantment.getMaxCost(1) / 2, 3)
        );
    }

    public static PenchantmentDefinition getDefinition(Enchantment enchantment) {
        return CACHE.computeIfAbsent(enchantment, PenchantmentDefinition::createFallback);
    }
}
