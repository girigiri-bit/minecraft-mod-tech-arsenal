package com.girigiri.techarsenal.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Invisible camera anchor (rendered with NoopRenderer). Used two ways:
 * <ul>
 *   <li>Client-only: instantiated locally and handed to
 *       {@code Minecraft#setCameraEntity} for the SAT view and monitor feed
 *       captures — never added to the world.</li>
 *   <li>Server-side (v0.9): spawned as a real entity ({@code noSave}, untracked
 *       by save) and passed to {@code ServerPlayer#setCamera} as the spectate
 *       anchor for the unlimited-distance handheld CAM view. It syncs to the
 *       viewing client like any tracked entity and is discarded when the view
 *       ends.</li>
 * </ul>
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
