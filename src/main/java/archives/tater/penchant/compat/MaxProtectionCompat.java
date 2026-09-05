package archives.tater.penchant.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;

import java.util.Locale;

/**
 * Max Protection (and similarly named mods) lets Protection / Fire / Blast / Projectile
 * coexist on one armor piece. Vanilla and anvil-only patches still report them as
 * incompatible; the Penchant table must honor the stacking mod when it is present.
 */
public final class MaxProtectionCompat {
    private static Boolean installed;

    private MaxProtectionCompat() {}

    public static boolean isInstalled() {
        if (installed == null) installed = detect();
        return installed;
    }

    /**
     * @return true when both enchantments are armor-protection types that Max Protection
     *         is meant to combine. Feather Falling is already vanilla-compatible.
     */
    public static boolean allowsTogether(Enchantment first, Enchantment second) {
        if (!isInstalled() || first.equals(second)) return false;
        return isStackableProtection(first) && isStackableProtection(second);
    }

    private static boolean isStackableProtection(Enchantment enchantment) {
        return enchantment instanceof ProtectionEnchantment protection
                && protection.type != ProtectionEnchantment.Type.FALL;
    }

    private static boolean detect() {
        return FabricLoader.getInstance().getAllMods().stream().anyMatch(mod -> {
            String id = mod.getMetadata().getId().replace("-", "").replace("_", "");
            if (id.equals("maxprotection") || id.equals("maximumprotection")) return true;
            String name = mod.getMetadata().getName().toLowerCase(Locale.ROOT).trim();
            if (name.equals("max protection") || name.equals("maximum protection")) return true;
            return (name.startsWith("max protection") || name.startsWith("maximum protection"))
                    && !name.contains("iii") && !name.contains(" 3");
        });
    }
}
