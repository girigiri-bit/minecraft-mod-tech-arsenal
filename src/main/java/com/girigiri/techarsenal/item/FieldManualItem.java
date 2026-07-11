package com.girigiri.techarsenal.item;

import com.girigiri.techarsenal.client.gui.GuideBookOpener;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Opens the in-game player guide (a vanilla BookViewScreen) built from
 * GuideBookContent. No NBT, no server-side behavior — the screen only ever
 * opens client-side, reached via DistExecutor so this class never touches
 * client-only classes on a dedicated server.
 */
public class FieldManualItem extends Item
{
    public FieldManualItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        if (level.isClientSide())
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> GuideBookOpener::open);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable("tooltip.techarsenal.field_manual.hint")
                .withStyle(ChatFormatting.GRAY));
    }
}
