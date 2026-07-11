package com.girigiri.techarsenal.event;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.block.SecurityCameraBlock;
import com.girigiri.techarsenal.entity.CameraEntity;
import com.girigiri.techarsenal.item.CameraMonitorItem;
import com.girigiri.techarsenal.registry.ModEntities;
import com.girigiri.techarsenal.world.CameraRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-driven remote camera viewing for the handheld {@code camera_monitor}
 * (the V-key CAM view). Unlike the old client-only hack (capped at 64m), this
 * uses vanilla's spectate mechanism: {@link ServerPlayer#setCamera(Entity)}
 * teleports the player onto a spawned {@link CameraEntity} and, every tick,
 * {@code ServerPlayer.tick()} keeps the player's tracked chunks following that
 * viewpoint — the only vanilla-supported way to stream chunks around an
 * arbitrary point to a specific client. Result: unlimited-distance CAM viewing
 * within the same dimension, with no forced-chunk tickets (no leak risk).
 * <p>
 * The player's body physically moves to the camera while viewing and is
 * teleported back on exit (sneak / V-key / camera destroyed / attacked /
 * logout / server stop / dimension change). Death or removal drops the session
 * without teleporting. State is server-thread only, so no synchronization.
 */
@Mod.EventBusSubscriber(modid = TechArsenal.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RemoteCameraView
{
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private RemoteCameraView()
    {
    }

    /** One active remote-view session. Server-thread only. */
    private static final class Session
    {
        CameraEntity entity;
        int cameraId;
        BlockPos cameraPos;
        InteractionHand hand;
        ResourceKey<Level> returnDim;
        Vec3 returnPos;
        float returnYRot;
        float returnXRot;
    }

    /** View position/yaw for a camera block, matching the old client viewPosFor. */
    private static Vec3 viewPosFor(BlockPos cameraPos, float yaw)
    {
        return Vec3.atCenterOf(cameraPos).add(Vec3.directionFromRotation(0.0F, yaw).scale(0.6D));
    }

    /**
     * Cheap validation (no chunk load): is the camera link's block still the
     * one registered under that id? A re-registered position means a stale
     * entry we must skip.
     */
    private static boolean registeredMatches(ServerLevel level, CameraMonitorItem.CameraLink link)
    {
        Integer regId = CameraRegistry.get(level).idAt(link.pos());
        return regId != null && regId == link.id();
    }

    /**
     * Full validation for a candidate. First the cheap registry check; only if
     * that passes does it call getBlockState (which loads at most one chunk).
     * Returns the camera block state when valid, else null.
     */
    @Nullable
    private static BlockState cameraStateIfValid(ServerLevel level, CameraMonitorItem.CameraLink link)
    {
        if (!registeredMatches(level, link))
            return null;
        // This is the single intentional synchronous chunk load per candidate.
        BlockState state = level.getBlockState(link.pos());
        return state.getBlock() instanceof SecurityCameraBlock ? state : null;
    }

    /**
     * Candidate order: the preferred id (requested, else current selection)
     * first, then the remaining ids ascending. {@code cameras} is already sorted
     * ascending; ids are unique so a single dedup of the preferred entry is enough.
     */
    private static List<CameraMonitorItem.CameraLink> orderCandidates(
            List<CameraMonitorItem.CameraLink> cameras, int requestedId, int selectedId)
    {
        int preferred = requestedId >= 0 ? requestedId : selectedId;
        List<CameraMonitorItem.CameraLink> order = new ArrayList<>();
        if (preferred >= 0)
        {
            for (CameraMonitorItem.CameraLink link : cameras)
            {
                if (link.id() == preferred)
                {
                    order.add(link);
                    break;
                }
            }
        }
        for (CameraMonitorItem.CameraLink link : cameras)
        {
            if (!order.isEmpty() && order.get(0).id() == link.id())
                continue;
            order.add(link);
        }
        return order;
    }

    private static void switchTo(ServerPlayer player, Session session, CameraMonitorItem.CameraLink link,
            BlockState state, ItemStack stack)
    {
        ServerLevel level = player.serverLevel();
        float yaw = state.getValue(HorizontalDirectionalBlock.FACING).toYRot();
        Vec3 viewPos = viewPosFor(link.pos(), yaw);

        CameraEntity newCam = new CameraEntity(ModEntities.CAMERA.get(), level);
        newCam.moveTo(viewPos.x, viewPos.y, viewPos.z, yaw, 15.0F);
        level.addFreshEntity(newCam);
        // setCamera BEFORE discarding the old entity so the player is never
        // briefly cameraless.
        player.setCamera(newCam);
        CameraEntity old = session.entity;
        session.entity = newCam;
        session.cameraId = link.id();
        session.cameraPos = link.pos();
        if (old != null)
            old.discard();

        stack.getOrCreateTag().putInt(CameraMonitorItem.TAG_SELECTED_ID, link.id());
    }

    // --- public API (called from packet handlers, server thread) ---

    public static void open(ServerPlayer player, InteractionHand hand, int requestedId)
    {
        if (SESSIONS.containsKey(player.getUUID()))
            return;

        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof CameraMonitorItem))
            return;

        CameraMonitorItem.migrateLegacyTag(stack);
        List<CameraMonitorItem.CameraLink> cameras = CameraMonitorItem.readCameras(stack);
        if (cameras.isEmpty())
        {
            player.displayClientMessage(Component.translatable("message.techarsenal.camera_not_linked"), true);
            return;
        }

        ServerLevel level = player.serverLevel();
        List<CameraMonitorItem.CameraLink> order =
                orderCandidates(cameras, requestedId, CameraMonitorItem.getSelectedId(stack));

        CameraMonitorItem.CameraLink chosen = null;
        BlockState chosenState = null;
        for (CameraMonitorItem.CameraLink link : order)
        {
            BlockState state = cameraStateIfValid(level, link);
            if (state != null)
            {
                chosen = link;
                chosenState = state;
                break;
            }
        }
        if (chosen == null)
        {
            player.displayClientMessage(Component.translatable("message.techarsenal.camera_none_valid"), true);
            return;
        }

        float yaw = chosenState.getValue(HorizontalDirectionalBlock.FACING).toYRot();
        Vec3 viewPos = viewPosFor(chosen.pos(), yaw);

        // Snapshot the return state BEFORE teleporting onto the camera.
        Session session = new Session();
        session.hand = hand;
        session.returnDim = level.dimension();
        session.returnPos = player.position();
        session.returnYRot = player.getYRot();
        session.returnXRot = player.getXRot();
        session.cameraId = chosen.id();
        session.cameraPos = chosen.pos();

        CameraEntity cam = new CameraEntity(ModEntities.CAMERA.get(), level);
        cam.moveTo(viewPos.x, viewPos.y, viewPos.z, yaw, 15.0F);
        level.addFreshEntity(cam);
        session.entity = cam;
        player.setCamera(cam);

        stack.getOrCreateTag().putInt(CameraMonitorItem.TAG_SELECTED_ID, chosen.id());
        SESSIONS.put(player.getUUID(), session);
    }

    public static void cycle(ServerPlayer player, int direction)
    {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null)
            return;

        ItemStack stack = player.getItemInHand(session.hand);
        if (!(stack.getItem() instanceof CameraMonitorItem))
        {
            close(player, true);
            return;
        }

        List<CameraMonitorItem.CameraLink> cameras = CameraMonitorItem.readCameras(stack);
        if (cameras.isEmpty())
        {
            close(player, true);
            return;
        }

        ServerLevel level = player.serverLevel();
        int size = cameras.size();
        int currentIndex = -1;
        for (int i = 0; i < size; i++)
        {
            if (cameras.get(i).id() == session.cameraId)
            {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex == -1)
            currentIndex = direction > 0 ? size - 1 : 0;

        CameraMonitorItem.CameraLink chosen = null;
        BlockState chosenState = null;
        int index = currentIndex;
        for (int attempt = 0; attempt < size; attempt++)
        {
            index = Math.floorMod(index + direction, size);
            CameraMonitorItem.CameraLink candidate = cameras.get(index);
            if (candidate.id() == session.cameraId && size > 1)
                continue;
            BlockState state = cameraStateIfValid(level, candidate);
            if (state != null)
            {
                chosen = candidate;
                chosenState = state;
                break;
            }
        }

        if (chosen == null)
        {
            player.displayClientMessage(Component.translatable("message.techarsenal.camera_no_switch_target"), true);
            return;
        }

        switchTo(player, session, chosen, chosenState, stack);
    }

    public static void close(ServerPlayer player, boolean restorePosition)
    {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null)
            return;

        if (player.getCamera() != player)
            player.setCamera(player);
        if (session.entity != null)
            session.entity.discard();

        if (restorePosition && player.isAlive())
        {
            ServerLevel target = player.server.getLevel(session.returnDim);
            if (target != null)
                player.teleportTo(target, session.returnPos.x, session.returnPos.y, session.returnPos.z,
                        session.returnYRot, session.returnXRot);
        }
    }

    // --- event handlers ---

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player.level().isClientSide)
            return;

        Session session = SESSIONS.get(player.getUUID());
        if (session == null)
            return;

        // Never teleport a dying/removed player — just drop the session and
        // discard the anchor entity directly.
        if (player.isRemoved() || player.isDeadOrDying())
        {
            SESSIONS.remove(player.getUUID());
            if (session.entity != null)
                session.entity.discard();
            return;
        }

        // Something outside us reverted the camera (e.g. vanilla's own
        // sneak-to-stop-spectating), or the anchor is gone: clean up and restore.
        if (player.getCamera() == player || session.entity == null || session.entity.isRemoved())
        {
            close(player, true);
            return;
        }

        // Periodically re-validate the camera the player is currently viewing.
        // Its chunk is loaded (the player is standing there), so this is cheap.
        if (player.tickCount % 20 == 0
                && !(player.serverLevel().getBlockState(session.cameraPos).getBlock() instanceof SecurityCameraBlock))
        {
            player.displayClientMessage(Component.translatable("message.techarsenal.camera_view_lost"), true);
            close(player, true);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player && SESSIONS.containsKey(player.getUUID()))
        {
            event.setCanceled(true);
            close(player, true);
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
            close(player, true);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player && SESSIONS.containsKey(player.getUUID()))
            close(player, true);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event)
    {
        for (UUID uuid : new ArrayList<>(SESSIONS.keySet()))
        {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
            if (player != null)
                close(player, true);
            else
                SESSIONS.remove(uuid);
        }
    }
}
