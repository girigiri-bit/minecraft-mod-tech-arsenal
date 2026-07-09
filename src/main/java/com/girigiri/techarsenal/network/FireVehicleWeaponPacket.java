package com.girigiri.techarsenal.network;

import com.girigiri.techarsenal.entity.ArmedVehicle;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: the rider pressed the vehicle-fire key. No payload; the
 * server derives the weapon from the sender's current vehicle. */
public class FireVehicleWeaponPacket
{
    public static void encode(FireVehicleWeaponPacket msg, FriendlyByteBuf buf)
    {
    }

    public static FireVehicleWeaponPacket decode(FriendlyByteBuf buf)
    {
        return new FireVehicleWeaponPacket();
    }

    public static void handle(FireVehicleWeaponPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.getVehicle() instanceof ArmedVehicle armed)
                armed.fireWeapon(sender);
        });
        ctx.get().setPacketHandled(true);
    }
}
