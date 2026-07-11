package com.girigiri.techarsenal.network;

import com.girigiri.techarsenal.event.RemoteCameraView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: end the active remote camera view and teleport the player
 * back to where they opened it. No payload.
 */
public class CloseCameraViewPacket
{
    public CloseCameraViewPacket()
    {
    }

    public static void encode(CloseCameraViewPacket msg, FriendlyByteBuf buf)
    {
    }

    public static CloseCameraViewPacket decode(FriendlyByteBuf buf)
    {
        return new CloseCameraViewPacket();
    }

    public static void handle(CloseCameraViewPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null)
                return;
            RemoteCameraView.close(sender, true);
        });
        ctx.get().setPacketHandled(true);
    }
}
