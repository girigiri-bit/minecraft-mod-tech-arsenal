package com.girigiri.techarsenal.network;

import com.girigiri.techarsenal.TechArsenal;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork
{
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TechArsenal.MODID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private ModNetwork()
    {
    }

    public static void register()
    {
        int id = 0;
        CHANNEL.registerMessage(id++, FireVehicleWeaponPacket.class,
                FireVehicleWeaponPacket::encode, FireVehicleWeaponPacket::decode, FireVehicleWeaponPacket::handle);
        CHANNEL.registerMessage(id++, SaberSpecialPacket.class,
                SaberSpecialPacket::encode, SaberSpecialPacket::decode, SaberSpecialPacket::handle);
        CHANNEL.registerMessage(id++, OpenCameraViewPacket.class,
                OpenCameraViewPacket::encode, OpenCameraViewPacket::decode, OpenCameraViewPacket::handle);
        CHANNEL.registerMessage(id++, CycleCameraViewPacket.class,
                CycleCameraViewPacket::encode, CycleCameraViewPacket::decode, CycleCameraViewPacket::handle);
        CHANNEL.registerMessage(id++, CloseCameraViewPacket.class,
                CloseCameraViewPacket::encode, CloseCameraViewPacket::decode, CloseCameraViewPacket::handle);
        CHANNEL.registerMessage(id++, PanCameraViewPacket.class,
                PanCameraViewPacket::encode, PanCameraViewPacket::decode, PanCameraViewPacket::handle);
    }
}
