package com.girigiri.techarsenal.blockentity;

import com.girigiri.techarsenal.block.AuthMonitorBlock;
import com.girigiri.techarsenal.block.FaceScannerBlock;
import com.girigiri.techarsenal.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Detection, enrollment and door control for the face scanner. */
public class FaceScannerBlockEntity extends BlockEntity
{
    private static final int SCAN_INTERVAL_TICKS = 5;
    private static final int ENROLL_DURATION_TICKS = 20 * 30;
    private static final int DOOR_HOLD_TICKS = 30;
    private static final int DENY_EFFECT_COOLDOWN_TICKS = 40;
    private static final double ZONE_DEPTH = 3.0D;
    private static final int DOOR_RADIUS = 4;
    private static final DustParticleOptions GREEN_DUST =
            new DustParticleOptions(new Vector3f(0.2F, 1.0F, 0.3F), 1.2F);
    private static final DustParticleOptions RED_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.2F);

    private UUID owner;
    private final Set<UUID> authorized = new HashSet<>();
    private final List<BlockPos> openedDoors = new ArrayList<>();
    private long enrollUntil;
    private long grantedUntil;
    private long nextDenyEffectTime;
    private boolean monitorsSynced;
    private AuthMonitorBlock.Status status = AuthMonitorBlock.Status.IDLE;

    public FaceScannerBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.FACE_SCANNER.get(), pos, state);
    }

    public void setOwner(Player player)
    {
        this.owner = player.getUUID();
        this.authorized.add(player.getUUID());
        setChanged();
        player.displayClientMessage(Component.translatable("message.techarsenal.scanner_placed"), true);
    }

    public void toggleEnrollMode(Player player)
    {
        if (owner != null && !player.getUUID().equals(owner))
        {
            player.displayClientMessage(Component.translatable("message.techarsenal.scanner_not_owner"), true);
            return;
        }
        long now = this.level.getGameTime();
        if (now < enrollUntil)
        {
            enrollUntil = 0L;
            player.displayClientMessage(Component.translatable("message.techarsenal.scanner_enroll_off"), true);
        }
        else
        {
            enrollUntil = now + ENROLL_DURATION_TICKS;
            player.displayClientMessage(Component.translatable("message.techarsenal.scanner_enroll_on"), true);
            this.level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.6F);
        }
        setChanged();
    }

    public void clearEnrollment(Player player)
    {
        if (owner != null && !player.getUUID().equals(owner))
        {
            player.displayClientMessage(Component.translatable("message.techarsenal.scanner_not_owner"), true);
            return;
        }
        authorized.clear();
        enrollUntil = 0L;
        setChanged();
        player.displayClientMessage(Component.translatable("message.techarsenal.scanner_cleared"), true);
        this.level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.6F, 1.2F);
    }

    public void serverTick()
    {
        ServerLevel level = (ServerLevel) this.level;
        long now = level.getGameTime();
        if (now % SCAN_INTERVAL_TICKS != 0)
            return;

        if (!monitorsSynced)
        {
            monitorsSynced = true;
            setStatus(level, status);
        }

        Direction facing = getBlockState().getValue(FaceScannerBlock.FACING);
        AABB zone = scanZone(facing);
        List<Player> inZone = level.getEntitiesOfClass(Player.class, zone,
                p -> p.isAlive() && !p.isSpectator());

        boolean enrolling = now < enrollUntil;
        if (enrolling)
        {
            // Scanning animation while enrollment mode is armed
            Vec3 front = scannerEye(facing);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, front.x, front.y, front.z, 2, 0.3D, 0.3D, 0.3D, 0.02D);
            for (Player player : inZone)
            {
                if (authorized.add(player.getUUID()))
                {
                    setChanged();
                    enrollEffects(level, player);
                }
            }
        }

        boolean anyGranted = false;
        Player deniedPlayer = null;
        for (Player player : inZone)
        {
            if (authorized.contains(player.getUUID()))
                anyGranted = true;
            else
                deniedPlayer = player;
        }

        if (anyGranted)
        {
            grantedUntil = now + DOOR_HOLD_TICKS;
            if (status != AuthMonitorBlock.Status.GRANTED)
            {
                setStatus(level, AuthMonitorBlock.Status.GRANTED);
                grantEffects(level, facing, inZone);
                openDoors(level);
            }
            return;
        }

        if (now < grantedUntil)
            return; // hold the doors briefly after the player walks through

        if (status == AuthMonitorBlock.Status.GRANTED)
            closeDoors(level);

        if (deniedPlayer != null && !enrolling)
        {
            if (status != AuthMonitorBlock.Status.DENIED)
                setStatus(level, AuthMonitorBlock.Status.DENIED);
            if (now >= nextDenyEffectTime)
            {
                nextDenyEffectTime = now + DENY_EFFECT_COOLDOWN_TICKS;
                denyEffects(level, facing, deniedPlayer);
            }
        }
        else if (status != AuthMonitorBlock.Status.IDLE)
        {
            setStatus(level, AuthMonitorBlock.Status.IDLE);
        }
    }

    private AABB scanZone(Direction facing)
    {
        BlockPos near = worldPosition.relative(facing);
        BlockPos far = worldPosition.relative(facing, (int) ZONE_DEPTH);
        return new AABB(near).minmax(new AABB(far)).inflate(0.5D, 0.0D, 0.5D).expandTowards(0.0D, 1.0D, 0.0D);
    }

    private Vec3 scannerEye(Direction facing)
    {
        return Vec3.atCenterOf(worldPosition).add(0.0D, 0.2D, 0.0D)
                .add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.6D));
    }

    private void setStatus(ServerLevel level, AuthMonitorBlock.Status newStatus)
    {
        status = newStatus;
        // Mirror the status on every auth monitor nearby
        for (BlockPos pos : BlockPos.betweenClosed(
                worldPosition.offset(-DOOR_RADIUS, -DOOR_RADIUS, -DOOR_RADIUS),
                worldPosition.offset(DOOR_RADIUS, DOOR_RADIUS, DOOR_RADIUS)))
        {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof AuthMonitorBlock && state.getValue(AuthMonitorBlock.STATUS) != newStatus)
                level.setBlock(pos, state.setValue(AuthMonitorBlock.STATUS, newStatus), 3);
        }
    }

    private void enrollEffects(ServerLevel level, Player player)
    {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                player.getX(), player.getEyeY(), player.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.0D);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.5F, 1.6F);
        player.displayClientMessage(Component.translatable("message.techarsenal.scanner_enrolled"), true);
    }

    private void grantEffects(ServerLevel level, Direction facing, List<Player> inZone)
    {
        Vec3 front = scannerEye(facing);
        level.sendParticles(GREEN_DUST, front.x, front.y, front.z, 15, 0.3D, 0.4D, 0.3D, 0.0D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, front.x, front.y, front.z, 6, 0.3D, 0.4D, 0.3D, 0.0D);
        level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.8F, 1.6F);
        for (Player player : inZone)
            if (authorized.contains(player.getUUID()))
                player.displayClientMessage(Component.translatable("message.techarsenal.scanner_granted"), true);
    }

    private void denyEffects(ServerLevel level, Direction facing, Player player)
    {
        Vec3 front = scannerEye(facing);
        level.sendParticles(RED_DUST, front.x, front.y, front.z, 15, 0.3D, 0.4D, 0.3D, 0.0D);
        level.sendParticles(ParticleTypes.SMOKE, front.x, front.y, front.z, 6, 0.2D, 0.3D, 0.2D, 0.01D);
        level.playSound(null, worldPosition, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.9F, 0.8F);
        player.displayClientMessage(Component.translatable("message.techarsenal.scanner_denied"), true);
    }

    private void openDoors(ServerLevel level)
    {
        openedDoors.clear();
        for (BlockPos pos : BlockPos.betweenClosed(
                worldPosition.offset(-DOOR_RADIUS, -DOOR_RADIUS, -DOOR_RADIUS),
                worldPosition.offset(DOOR_RADIUS, DOOR_RADIUS, DOOR_RADIUS)))
        {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock door
                    && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                    && !state.getValue(DoorBlock.OPEN))
            {
                door.setOpen(null, level, state, pos, true);
                openedDoors.add(pos.immutable());
            }
        }
        setChanged();
    }

    private void closeDoors(ServerLevel level)
    {
        for (BlockPos pos : openedDoors)
        {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock door && state.getValue(DoorBlock.OPEN))
                door.setOpen(null, level, state, pos, false);
        }
        openedDoors.clear();
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        if (owner != null)
            tag.putUUID("Owner", owner);
        ListTag list = new ListTag();
        for (UUID uuid : authorized)
        {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", uuid);
            list.add(entry);
        }
        tag.put("Authorized", list);
        ListTag doors = new ListTag();
        for (BlockPos pos : openedDoors)
        {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", pos.asLong());
            doors.add(entry);
        }
        tag.put("OpenedDoors", doors);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        if (tag.hasUUID("Owner"))
            owner = tag.getUUID("Owner");
        authorized.clear();
        ListTag list = tag.getList("Authorized", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
            authorized.add(list.getCompound(i).getUUID("Id"));
        openedDoors.clear();
        ListTag doors = tag.getList("OpenedDoors", Tag.TAG_COMPOUND);
        for (int i = 0; i < doors.size(); i++)
            openedDoors.add(BlockPos.of(doors.getCompound(i).getLong("Pos")));
    }
}
