package com.girigiri.techarsenal.client;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.entity.ArmedVehicle;
import com.girigiri.techarsenal.item.BeamSaberItem;
import com.girigiri.techarsenal.network.FireVehicleWeaponPacket;
import com.girigiri.techarsenal.network.ModNetwork;
import com.girigiri.techarsenal.network.SaberSpecialPacket;
import com.girigiri.techarsenal.registry.ModItems;
import com.girigiri.techarsenal.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The R key (weapon action): fires the mounted weapon while riding an armed
 * vehicle, or triggers the beam saber spin slash while holding the saber.
 * Also plays the saber "fwon" whoosh on every swing.
 */
@Mod.EventBusSubscriber(modid = TechArsenal.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientVehicleWeapons
{
    private static final int HOLD_FIRE_INTERVAL_TICKS = 4;

    private static boolean wasSwinging;

    private ClientVehicleWeapons()
    {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;
        Minecraft mc = Minecraft.getInstance();

        // Drain queued presses even when they don't apply so they don't fire later
        boolean pressed = false;
        while (ClientSetup.VEHICLE_FIRE_KEY.consumeClick())
            pressed = true;

        if (mc.player == null)
            return;

        // Saber swing whoosh ("fwon")
        boolean swinging = mc.player.swinging;
        if (swinging && !wasSwinging
                && mc.player.getMainHandItem().getItem() instanceof BeamSaberItem
                && mc.level != null)
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    ModSounds.SABER_SWING.get(), SoundSource.PLAYERS, 0.8F,
                    0.9F + mc.level.random.nextFloat() * 0.2F, false);
        wasSwinging = swinging;

        if (mc.player.getVehicle() instanceof ArmedVehicle)
        {
            if (pressed || (ClientSetup.VEHICLE_FIRE_KEY.isDown()
                    && mc.player.tickCount % HOLD_FIRE_INTERVAL_TICKS == 0))
                ModNetwork.CHANNEL.sendToServer(new FireVehicleWeaponPacket());
        }
        else if (pressed
                && mc.player.getMainHandItem().getItem() instanceof BeamSaberItem
                && !mc.player.getCooldowns().isOnCooldown(ModItems.BEAM_SABER.get()))
        {
            ModNetwork.CHANNEL.sendToServer(new SaberSpecialPacket());
        }
    }
}
