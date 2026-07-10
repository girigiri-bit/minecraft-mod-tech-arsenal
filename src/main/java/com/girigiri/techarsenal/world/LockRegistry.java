package com.girigiri.techarsenal.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-dimension map of locked security doors: door position (lower half) -> key id. */
public class LockRegistry extends SavedData
{
    private static final String DATA_NAME = "techarsenal_locks";

    private final Map<BlockPos, UUID> locks = new HashMap<>();

    public static LockRegistry get(ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(LockRegistry::load, LockRegistry::new, DATA_NAME);
    }

    @Nullable
    public UUID getLock(BlockPos pos)
    {
        return locks.get(pos);
    }

    public void setLock(BlockPos pos, UUID keyId)
    {
        locks.put(pos.immutable(), keyId);
        setDirty();
    }

    public void clearLock(BlockPos pos)
    {
        if (locks.remove(pos) != null)
            setDirty();
    }

    public static LockRegistry load(CompoundTag tag)
    {
        LockRegistry registry = new LockRegistry();
        ListTag list = tag.getList("Locks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag entry = list.getCompound(i);
            registry.locks.put(BlockPos.of(entry.getLong("Pos")), entry.getUUID("Key"));
        }
        return registry;
    }

    @Override
    public CompoundTag save(CompoundTag tag)
    {
        ListTag list = new ListTag();
        locks.forEach((pos, key) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", pos.asLong());
            entry.putUUID("Key", key);
            list.add(entry);
        });
        tag.put("Locks", list);
        return tag;
    }
}
