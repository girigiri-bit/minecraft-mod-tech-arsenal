package com.girigiri.techarsenal.network;

import com.girigiri.techarsenal.event.RemoteCameraView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: request opening a remote camera view (V-key CAM).
 * {@code cameraId} of -1 means "use the item's current selection / first valid".
 * The server re-validates hand, item, list membership, registry and block
 * existence before spectating — the client's request is never trusted.
 */
public class OpenCameraViewPacket
{
    private final int hand;
    private final int cameraId;

    public OpenCameraViewPacket(int hand, int cameraId)
    {
        this.hand = hand;
        this.cameraId = cameraId;
    }

    public static void encode(OpenCameraViewPacket msg, FriendlyByteBuf buf)
    {
        buf.writeVarInt(msg.hand);
        buf.writeVarInt(msg.cameraId);
    }

    public static OpenCameraViewPacket decode(FriendlyByteBuf buf)
    {
        return new OpenCameraViewPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(OpenCameraViewPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || msg.hand < 0 || msg.hand >= InteractionHand.values().length)
                return;
            RemoteCameraView.open(sender, InteractionHand.values()[msg.hand], msg.cameraId);
        });
        ctx.get().setPacketHandled(true);
    }
}
