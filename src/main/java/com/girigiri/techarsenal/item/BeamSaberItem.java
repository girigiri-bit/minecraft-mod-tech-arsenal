package com.girigiri.techarsenal.item;

import com.girigiri.techarsenal.client.renderer.BeamSaberRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * Energy melee blade: 12 total attack damage, 1.6 attacks/sec (DPS 19.2).
 * Rendered as a glowing 3D beam (BeamSaberRenderer); while mid-swing it
 * deflects incoming projectiles (see SaberDeflection).
 */
public class BeamSaberItem extends SwordItem
{
    // 12 total = ENERGY bonus 8 + sword base 3 + player base 1
    private static final Tier ENERGY_TIER = new Tier()
    {
        @Override
        public int getUses()
        {
            return 2000;
        }

        @Override
        public float getSpeed()
        {
            return 8.0F;
        }

        @Override
        public float getAttackDamageBonus()
        {
            return 8.0F;
        }

        @Override
        public int getLevel()
        {
            return 4;
        }

        @Override
        public int getEnchantmentValue()
        {
            return 15;
        }

        @Override
        public Ingredient getRepairIngredient()
        {
            return Ingredient.of(Items.DIAMOND);
        }
    };

    public BeamSaberItem(Properties properties)
    {
        super(ENERGY_TIER, 3, -1.9F, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer)
    {
        consumer.accept(new IClientItemExtensions()
        {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return BeamSaberRenderer.instance();
            }
        });
    }
}
