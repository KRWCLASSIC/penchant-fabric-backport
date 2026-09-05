package archives.tater.penchant.mixin.loot;

import archives.tater.penchant.util.PenchantLoot;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(EnchantRandomlyFunction.class)
public class EnchantRandomlyFunctionMixin {
    @Redirect(
            method = "run",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/stream/Stream;collect(Ljava/util/stream/Collector;)Ljava/lang/Object;"
            )
    )
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object penchant$filterCollected(
            Stream stream,
            Collector collector,
            ItemStack stack,
            LootContext context
    ) {
        Object collected = stream.collect(collector);
        if (!(collected instanceof List<?> list)) {
            return collected;
        }
        List<Enchantment> enchantments = (List<Enchantment>) list;
        return enchantments.stream()
                .filter(enchantment -> PenchantLoot.isAllowed(stack, enchantment))
                .collect(Collectors.toList());
    }
}
