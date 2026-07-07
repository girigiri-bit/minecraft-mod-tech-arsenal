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
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Per-dimension registry assigning stable numeric IDs (CAM-n) to placed
 * security cameras.
 */
public class CameraRegistry extends SavedData
{
    private static final String DATA_NAME = "techarsenal_cameras";

    private final SortedMap<Integer, BlockPos> byId = new TreeMap<>();
    private final Map<BlockPos, Integer> byPos = new HashMap<>();
    private int nextId = 1;

    public static CameraRegistry get(ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(CameraRegistry::load, CameraRegistry::new, DATA_NAME);
    }

    public int getOrAssign(BlockPos pos)
    {
        Integer existing = byPos.get(pos);
        if (existing != null)
            return existing;
        int id = nextId++;
        byId.put(id, pos.immutable());
        byPos.put(pos.immutable(), id);
        setDirty();
        return id;
    }

    public void unregister(BlockPos pos)
    {
        Integer id = byPos.remove(pos);
        if (id != null)
        {
            byId.remove(id);
            setDirty();
        }
    }

    @Nullable
    public Integer idAt(BlockPos pos)
    {
        return byPos.get(pos);
    }

    /** Snapshot of all registered cameras, sorted by ID. */
    public SortedMap<Integer, BlockPos> all()
    {
        return new TreeMap<>(byId);
    }

    public static CameraRegistry load(CompoundTag tag)
    {
        CameraRegistry registry = new CameraRegistry();
        registry.nextId = tag.getInt("NextId");
        ListTag list = tag.getList("Cameras", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag entry = list.getCompound(i);
            int id = entry.getInt("Id");
            BlockPos pos = BlockPos.of(entry.getLong("Pos"));
            registry.byId.put(id, pos);
            registry.byPos.put(pos, id);
            registry.nextId = Math.max(registry.nextId, id + 1);
        }
        if (registry.nextId < 1)
            registry.nextId = 1;
        return registry;
    }

    @Override
    public CompoundTag save(CompoundTag tag)
    {
        tag.putInt("NextId", nextId);
        ListTag list = new ListTag();
        byId.forEach((id, pos) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Id", id);
            entry.putLong("Pos", pos.asLong());
            list.add(entry);
        });
        tag.put("Cameras", list);
        return tag;
    }
}
