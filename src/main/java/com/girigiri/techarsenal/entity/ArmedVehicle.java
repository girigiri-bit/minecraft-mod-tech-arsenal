package com.girigiri.techarsenal.entity;

import net.minecraft.server.level.ServerPlayer;

/** Vehicles with a built-in weapon fired via the vehicle-fire key. */
public interface ArmedVehicle
{
    /** Called on the server when the rider presses the fire key. Implementations enforce their own cooldown. */
    void fireWeapon(ServerPlayer rider);
}
