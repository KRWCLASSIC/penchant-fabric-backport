package archives.tater.penchant.mixin.anvil;

import archives.tater.penchant.registry.PenchantFlag;

import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Matches no_anvil_books: enchanted books cannot apply enchantments on the anvil.
 */
@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
    @Shadow @Final
    private DataSlot cost;

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void penchant$blockBookEnchanting(CallbackInfo ci) {
        if (!PenchantFlag.NO_ANVIL_BOOKS.isEnabled()) return;
        AnvilMenu self = (AnvilMenu) (Object) this;
        ItemStack left = self.getSlot(0).getItem();
        ItemStack right = self.getSlot(1).getItem();
        if (left.isEmpty() || right.isEmpty()) return;

        if (left.is(Items.ENCHANTED_BOOK) || right.is(Items.ENCHANTED_BOOK)) {
            self.getSlot(2).set(ItemStack.EMPTY);
            cost.set(0);
            ci.cancel();
        }
    }
}
