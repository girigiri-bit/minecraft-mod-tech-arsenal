package com.girigiri.techarsenal.network;

import com.girigiri.techarsenal.item.CameraMonitorItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: persists which linked camera id is currently selected on
 * a camera monitor item, so the choice survives a re-log. The server never
 * trusts the id blindly — it's re-validated against the item's own list.
 */
public class SelectCameraPacket
{
    private final int hand;
    private final int cameraId;

    public SelectCameraPacket(int hand, int cameraId)
    {
        this.hand = hand;
        this.cameraId = cameraId;
    }

    public static void encode(SelectCameraPacket msg, FriendlyByteBuf buf)
    {
        buf.writeVarInt(msg.hand);
        buf.writeVarInt(msg.cameraId);
    }

    public static SelectCameraPacket decode(FriendlyByteBuf buf)
    {
        return new SelectCameraPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(SelectCameraPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || msg.hand < 0 || msg.hand >= InteractionHand.values().length)
                return;

            ItemStack stack = sender.getItemInHand(InteractionHand.values()[msg.hand]);
            if (!(stack.getItem() instanceof CameraMonitorItem))
                return;

            CameraMonitorItem.migrateLegacyTag(stack);

            boolean valid = false;
            for (CameraMonitorItem.CameraLink link : CameraMonitorItem.readCameras(stack))
            {
                if (link.id() == msg.cameraId)
                {
                    valid = true;
                    break;
                }
            }
            if (valid)
                stack.getOrCreateTag().putInt(CameraMonitorItem.TAG_SELECTED_ID, msg.cameraId);
        });
        ctx.get().setPacketHandled(true);
    }
}
