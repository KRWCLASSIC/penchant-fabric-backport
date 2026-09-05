package archives.tater.penchant.network;

import archives.tater.penchant.Penchant;
import archives.tater.penchant.menu.PenchantmentMenu;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashSet;
import java.util.Set;

public final class PenchantNetworking {
    public static final ResourceLocation SELECT_ENCHANTMENT = Penchant.id("select_enchantment");
    public static final ResourceLocation UNLOCKED_ENCHANTMENTS = Penchant.id("unlocked_enchantments");

    private PenchantNetworking() {}

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(SELECT_ENCHANTMENT, (server, player, handler, buf, responseSender) -> {
            int enchantmentId = buf.readVarInt();
            server.execute(() -> {
                if (!(player.containerMenu instanceof PenchantmentMenu menu)) return;
                Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.byId(enchantmentId);
                if (enchantment != null) menu.handleEnchant(enchantment);
            });
        });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(UNLOCKED_ENCHANTMENTS, (client, handler, buf, responseSender) -> {
            int size = buf.readVarInt();
            Set<Enchantment> unlocked = new HashSet<>(size);
            for (int i = 0; i < size; i++) {
                Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.byId(buf.readVarInt());
                if (enchantment != null) unlocked.add(enchantment);
            }
            client.execute(() -> {
                if (client.player != null && client.player.containerMenu instanceof PenchantmentMenu menu) {
                    menu.setUnlockedFromClient(unlocked);
                }
            });
        });
    }

    public static void sendSelectEnchantment(Enchantment enchantment) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(BuiltInRegistries.ENCHANTMENT.getId(enchantment));
        ClientPlayNetworking.send(SELECT_ENCHANTMENT, buf);
    }

    public static void sendUnlocked(ServerPlayer player, Set<Enchantment> unlocked) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(unlocked.size());
        for (Enchantment enchantment : unlocked) {
            buf.writeVarInt(BuiltInRegistries.ENCHANTMENT.getId(enchantment));
        }
        ServerPlayNetworking.send(player, UNLOCKED_ENCHANTMENTS, buf);
    }
}
