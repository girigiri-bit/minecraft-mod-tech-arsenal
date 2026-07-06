package com.girigiri.techarsenal.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Invisible camera anchor. Only ever instantiated client-side and handed to
 * Minecraft#setCameraEntity — it is never added to the world.
 */
public class CameraEntity extends Entity
{
    public CameraEntity(EntityType<?> type, Level level)
    {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData()
    {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
    }
}
