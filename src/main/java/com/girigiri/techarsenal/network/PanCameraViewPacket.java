package com.girigiri.techarsenal.network;

import com.girigiri.techarsenal.event.RemoteCameraView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: report the desired camera yaw/pitch while mouse-panning
 * the active remote camera view. Throttled and deadbanded client-side; the
 * server clamps against the session's pan range regardless.
 */
public class PanCameraViewPacket
{
    private final float yaw;
    private final float pitch;

    public PanCameraViewPacket(float yaw, float pitch)
    {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static void encode(PanCameraViewPacket msg, FriendlyByteBuf buf)
    {
        buf.writeFloat(msg.yaw);
        buf.writeFloat(msg.pitch);
    }

    public static PanCameraViewPacket decode(FriendlyByteBuf buf)
    {
        return new PanCameraViewPacket(buf.readFloat(), buf.readFloat());
    }

    public static void handle(PanCameraViewPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null)
                RemoteCameraView.pan(sender, msg.yaw, msg.pitch);
        });
        ctx.get().setPacketHandled(true);
    }
}
