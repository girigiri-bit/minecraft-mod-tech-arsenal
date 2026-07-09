package com.girigiri.techarsenal.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/** Spawns a vehicle/machine entity when used on the ground (turret, helicopter, tank). */
public class DeployableItem extends Item
{
    private final Supplier<? extends EntityType<? extends Mob>> entityType;

    public DeployableItem(Supplier<? extends EntityType<? extends Mob>> entityType, Properties properties)
    {
        super(properties);
        this.entityType = entityType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null)
            return InteractionResult.PASS;

        if (!level.isClientSide)
        {
            BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
            Mob mob = entityType.get().create(level);
            if (mob == null)
                return InteractionResult.FAIL;
            mob.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                    player.getYRot(), 0.0F);
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);

            level.playSound(null, spawnPos, SoundEvents.IRON_GOLEM_STEP, SoundSource.NEUTRAL, 1.0F, 0.8F);
            if (!player.getAbilities().instabuild)
                context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
