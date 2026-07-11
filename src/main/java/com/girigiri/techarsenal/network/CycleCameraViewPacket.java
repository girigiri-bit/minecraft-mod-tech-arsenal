package com.girigiri.techarsenal.network;

import com.girigiri.techarsenal.event.RemoteCameraView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: step the active remote camera view to the next (+1) or
 * previous (-1) camera. The direction is clamped defensively server-side.
 */
public class CycleCameraViewPacket
{
    private final int direction;

    public CycleCameraViewPacket(int direction)
    {
        this.direction = direction;
    }

    public static void encode(CycleCameraViewPacket msg, FriendlyByteBuf buf)
    {
        buf.writeVarInt(msg.direction);
    }

    public static CycleCameraViewPacket decode(FriendlyByteBuf buf)
    {
        return new CycleCameraViewPacket(buf.readVarInt());
    }

    public static void handle(CycleCameraViewPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null)
                return;
            int clamped = msg.direction >= 0 ? 1 : -1;
            RemoteCameraView.cycle(sender, clamped);
        });
        ctx.get().setPacketHandled(true);
    }
}
