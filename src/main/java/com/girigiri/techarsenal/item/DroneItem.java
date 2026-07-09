package com.girigiri.techarsenal.item;

import com.girigiri.techarsenal.entity.DroneEntity;
import com.girigiri.techarsenal.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class DroneItem extends Item
{
    public DroneItem(Properties properties)
    {
        super(properties);
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
            DroneEntity drone = new DroneEntity(ModEntities.DRONE.get(), level);
            drone.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY() + 0.5D, spawnPos.getZ() + 0.5D,
                    player.getYRot(), 0.0F);
            drone.setOwner(player.getUUID());
            CompoundTag tag = context.getItemInHand().getTag();
            if (tag != null)
                drone.setUpgradeLevels(tag.getInt(DroneEntity.TAG_DAMAGE_LEVEL),
                        tag.getInt(DroneEntity.TAG_ARMOR_LEVEL));
            drone.setPersistenceRequired();
            level.addFreshEntity(drone);

            level.playSound(null, spawnPos, SoundEvents.BEEHIVE_EXIT, SoundSource.NEUTRAL, 1.0F, 1.2F);
            if (!player.getAbilities().instabuild)
                context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        CompoundTag tag = stack.getTag();
        if (tag != null && (tag.getInt(DroneEntity.TAG_DAMAGE_LEVEL) > 0
                || tag.getInt(DroneEntity.TAG_ARMOR_LEVEL) > 0))
            tooltip.add(Component.translatable("tooltip.techarsenal.drone.upgrades",
                    tag.getInt(DroneEntity.TAG_DAMAGE_LEVEL), tag.getInt(DroneEntity.TAG_ARMOR_LEVEL))
                    .withStyle(ChatFormatting.AQUA));
    }
}
