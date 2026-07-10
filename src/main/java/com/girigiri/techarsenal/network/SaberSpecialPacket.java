package com.girigiri.techarsenal.network;

import com.girigiri.techarsenal.event.SaberComboManager;
import com.girigiri.techarsenal.item.BeamSaberItem;
import com.girigiri.techarsenal.registry.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: the player pressed the action key while holding the beam
 * saber. Starts the three-strike combo (kesa-giri, horizontal sweep, spin
 * slash — see SaberComboManager) with a 5s cooldown.
 */
public class SaberSpecialPacket
{
    private static final int COOLDOWN_TICKS = 100;

    public static void encode(SaberSpecialPacket msg, FriendlyByteBuf buf)
    {
    }

    public static SaberSpecialPacket decode(FriendlyByteBuf buf)
    {
        return new SaberSpecialPacket();
    }

    public static void handle(SaberSpecialPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null
                    || !(player.getMainHandItem().getItem() instanceof BeamSaberItem)
                    || player.getCooldowns().isOnCooldown(ModItems.BEAM_SABER.get()))
                return;
            SaberComboManager.startCombo(player);
            player.getCooldowns().addCooldown(ModItems.BEAM_SABER.get(), COOLDOWN_TICKS);
        });
        ctx.get().setPacketHandled(true);
    }
}
