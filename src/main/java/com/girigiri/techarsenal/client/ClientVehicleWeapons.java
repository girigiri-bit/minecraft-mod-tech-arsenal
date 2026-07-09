package com.girigiri.techarsenal.client;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.entity.ArmedVehicle;
import com.girigiri.techarsenal.network.FireVehicleWeaponPacket;
import com.girigiri.techarsenal.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Sends fire packets while the vehicle-fire key is pressed/held and the player rides an armed vehicle. */
@Mod.EventBusSubscriber(modid = TechArsenal.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientVehicleWeapons
{
    private static final int HOLD_FIRE_INTERVAL_TICKS = 4;

    private ClientVehicleWeapons()
    {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;
        Minecraft mc = Minecraft.getInstance();

        // Drain queued presses even when not riding so they don't fire later
        boolean pressed = false;
        while (ClientSetup.VEHICLE_FIRE_KEY.consumeClick())
            pressed = true;

        if (mc.player == null || !(mc.player.getVehicle() instanceof ArmedVehicle))
            return;
        if (pressed || (ClientSetup.VEHICLE_FIRE_KEY.isDown()
                && mc.player.tickCount % HOLD_FIRE_INTERVAL_TICKS == 0))
            ModNetwork.CHANNEL.sendToServer(new FireVehicleWeaponPacket());
    }
}
