package com.girigiri.techarsenal.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** Shared behavior for rideable vehicles: mount, recover with sneak, drop item on death. */
public abstract class VehicleEntityBase extends PathfinderMob
{
    private final Supplier<? extends Item> dropItem;

    protected VehicleEntityBase(EntityType<? extends VehicleEntityBase> type, Level level,
                                Supplier<? extends Item> dropItem)
    {
        super(type, level);
        this.dropItem = dropItem;
    }

    @Override
    protected void registerGoals()
    {
        // Vehicles have no AI — they only move under rider control
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger()
    {
        return this.getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty())
        {
            if (!this.level().isClientSide)
            {
                ItemStack stack = new ItemStack(dropItem.get());
                if (!player.addItem(stack))
                    player.drop(stack, false);
                this.playSound(SoundEvents.ITEM_PICKUP, 1.0F, 1.0F);
                this.discard();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (!this.isVehicle())
        {
            if (!this.level().isClientSide)
            {
                player.startRiding(this);
                // Chat, not action bar: vanilla's "Press Shift to Dismount"
                // overlay owns the action-bar slot while mounted
                if (this instanceof ArmedVehicle)
                    player.displayClientMessage(Component.translatable(
                            "message.techarsenal.vehicle_fire_hint",
                            Component.keybind("key.techarsenal.vehicle_fire")), false);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    protected void pushEntities()
    {
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source)
    {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater()
    {
        // Machines don't drown
        return true;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit)
    {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(dropItem.get()));
    }

    @Override
    public boolean shouldRiderSit()
    {
        return true;
    }
}
