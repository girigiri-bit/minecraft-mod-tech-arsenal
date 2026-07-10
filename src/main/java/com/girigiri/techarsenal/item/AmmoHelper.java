package com.girigiri.techarsenal.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Inventory ammo consumption shared by the guns. Creative mode never consumes. */
public final class AmmoHelper
{
    private AmmoHelper()
    {
    }

    /** Non-consuming check, e.g. to gate starting a hold-to-fire weapon. */
    public static boolean has(Player player, Item ammo)
    {
        if (player.getAbilities().instabuild)
            return true;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            if (player.getInventory().getItem(i).is(ammo))
                return true;
        player.displayClientMessage(
                Component.translatable("message.techarsenal.no_ammo_item", ammo.getDescription()), true);
        return false;
    }

    public static boolean tryConsume(Player player, Item ammo)
    {
        if (player.getAbilities().instabuild)
            return true;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
        {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot.is(ammo))
            {
                slot.shrink(1);
                return true;
            }
        }
        player.displayClientMessage(
                Component.translatable("message.techarsenal.no_ammo_item", ammo.getDescription()), true);
        return false;
    }
}
