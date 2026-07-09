package com.girigiri.techarsenal.entity;

import com.girigiri.techarsenal.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

public class DroneEntity extends PathfinderMob implements RangedAttackMob
{
    public static final int MAX_UPGRADE_LEVEL = 2;
    public static final String TAG_DAMAGE_LEVEL = "DamageLevel";
    public static final String TAG_ARMOR_LEVEL = "ArmorLevel";

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private int damageLevel;
    private int armorLevel;

    public DroneEntity(EntityType<? extends DroneEntity> type, Level level)
    {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.FLYING_SPEED, 0.9D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
    }

    @Override
    protected PathNavigation createNavigation(Level level)
    {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0D, 20, 16.0F));
        this.goalSelector.addGoal(2, new FollowOwnerGoal());
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    public void setOwner(@Nullable UUID uuid)
    {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    @Nullable
    public Player getOwner()
    {
        return this.entityData.get(OWNER_UUID).map(uuid -> this.level().getPlayerByUUID(uuid)).orElse(null);
    }

    public int getDamageLevel()
    {
        return damageLevel;
    }

    public int getArmorLevel()
    {
        return armorLevel;
    }

    public float getBoltDamage()
    {
        return 5.0F + 3.0F * damageLevel;
    }

    /** Applied when deployed from an upgraded item or loaded from NBT. */
    public void setUpgradeLevels(int damage, int armor)
    {
        this.damageLevel = Math.min(damage, MAX_UPGRADE_LEVEL);
        this.armorLevel = Math.min(armor, MAX_UPGRADE_LEVEL);
        applyArmorLevel();
        this.setHealth(this.getMaxHealth());
    }

    private void applyArmorLevel()
    {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0D + 10.0D * armorLevel);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power)
    {
        DroneBoltEntity bolt = new DroneBoltEntity(this.level(), this, getBoltDamage());
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.5D) - bolt.getY();
        double dz = target.getZ() - this.getZ();
        bolt.shoot(dx, dy, dz, 1.6F, 2.0F);
        this.playSound(SoundEvents.DISPENSER_LAUNCH, 0.8F, 1.4F);
        this.level().addFreshEntity(bolt);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        ItemStack held = player.getItemInHand(hand);

        // Upgrade modules: right-click the drone to install (max Lv2 each)
        boolean isDamageModule = held.is(ModItems.DRONE_UPGRADE_DAMAGE.get());
        if (isDamageModule || held.is(ModItems.DRONE_UPGRADE_ARMOR.get()))
        {
            if (!this.level().isClientSide)
            {
                int current = isDamageModule ? damageLevel : armorLevel;
                if (current >= MAX_UPGRADE_LEVEL)
                {
                    player.displayClientMessage(
                            Component.translatable("message.techarsenal.drone_upgrade_max"), true);
                }
                else
                {
                    if (isDamageModule)
                    {
                        damageLevel++;
                    }
                    else
                    {
                        armorLevel++;
                        applyArmorLevel();
                        this.setHealth(this.getMaxHealth());
                    }
                    if (!player.getAbilities().instabuild)
                        held.shrink(1);
                    this.playSound(SoundEvents.ANVIL_USE, 0.5F, 1.8F);
                    ((ServerLevel) this.level()).sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            this.getX(), this.getY() + 0.3D, this.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.1D);
                    player.displayClientMessage(Component.translatable(
                            isDamageModule ? "message.techarsenal.drone_upgraded_damage"
                                    : "message.techarsenal.drone_upgraded_armor",
                            isDamageModule ? damageLevel : armorLevel), true);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Owner sneaking + empty hand: pick the drone back up (upgrades carry over)
        if (player.isShiftKeyDown() && held.isEmpty() && player == getOwner())
        {
            if (!this.level().isClientSide)
            {
                ItemStack stack = new ItemStack(ModItems.DRONE.get());
                if (damageLevel > 0 || armorLevel > 0)
                {
                    stack.getOrCreateTag().putInt(TAG_DAMAGE_LEVEL, damageLevel);
                    stack.getOrCreateTag().putInt(TAG_ARMOR_LEVEL, armorLevel);
                }
                if (!player.addItem(stack))
                    player.drop(stack, false);
                this.playSound(SoundEvents.ITEM_PICKUP, 1.0F, 1.0F);
                this.discard();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source)
    {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos)
    {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        this.entityData.get(OWNER_UUID).ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.putInt(TAG_DAMAGE_LEVEL, damageLevel);
        tag.putInt(TAG_ARMOR_LEVEL, armorLevel);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner"))
            setOwner(tag.getUUID("Owner"));
        this.damageLevel = Math.min(tag.getInt(TAG_DAMAGE_LEVEL), MAX_UPGRADE_LEVEL);
        this.armorLevel = Math.min(tag.getInt(TAG_ARMOR_LEVEL), MAX_UPGRADE_LEVEL);
        applyArmorLevel();
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state)
    {
    }

    // Fly back to the owner when idle and too far away
    private class FollowOwnerGoal extends Goal
    {
        private static final double START_FOLLOW_DIST_SQ = 10.0D * 10.0D;
        private static final double TELEPORT_DIST_SQ = 32.0D * 32.0D;

        FollowOwnerGoal()
        {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse()
        {
            Player owner = getOwner();
            return owner != null && owner.isAlive()
                    && DroneEntity.this.getTarget() == null
                    && DroneEntity.this.distanceToSqr(owner) > START_FOLLOW_DIST_SQ;
        }

        @Override
        public void tick()
        {
            Player owner = getOwner();
            if (owner == null)
                return;

            if (DroneEntity.this.distanceToSqr(owner) > TELEPORT_DIST_SQ)
            {
                Vec3 pos = owner.position();
                DroneEntity.this.moveTo(pos.x, pos.y + 2.0D, pos.z, DroneEntity.this.getYRot(), 0.0F);
                DroneEntity.this.getNavigation().stop();
            }
            else
            {
                DroneEntity.this.getNavigation().moveTo(owner.getX(), owner.getEyeY() + 1.5D, owner.getZ(), 1.2D);
            }
        }

        @Override
        public void stop()
        {
            DroneEntity.this.getNavigation().stop();
        }
    }
}
