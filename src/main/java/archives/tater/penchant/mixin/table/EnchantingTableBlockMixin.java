package archives.tater.penchant.mixin.table;

import archives.tater.penchant.menu.PenchantmentMenu;
import archives.tater.penchant.registry.PenchantFlag;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentTableBlock.class)
public class EnchantingTableBlockMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void penchant$openReworkedMenu(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!PenchantFlag.REWORKED_TABLE_MENU.isEnabled()) return;
        if (level.isClientSide) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new ExtendedScreenHandlerFactory() {
                @Override
                public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
                    PenchantmentMenu.writeOpenData(buf, level, pos, player);
                }

                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.enchant");
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                    return new PenchantmentMenu(id, inventory, ContainerLevelAccess.create(level, pos));
                }
            });

            if (serverPlayer.containerMenu instanceof PenchantmentMenu menu) {
                menu.sendEnchantments();
            }
        }
        cir.setReturnValue(InteractionResult.CONSUME);
    }
}
