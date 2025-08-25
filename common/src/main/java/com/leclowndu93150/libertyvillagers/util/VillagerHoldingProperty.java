package com.leclowndu93150.libertyvillagers.util;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public record VillagerHoldingProperty() implements ConditionalItemModelProperty {
    public static final MapCodec<VillagerHoldingProperty> MAP_CODEC = MapCodec.unit(new VillagerHoldingProperty());

    @Override
    public boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext context) {
        return entity != null && entity.getType() == EntityType.VILLAGER;
    }

    @Override
    public MapCodec<VillagerHoldingProperty> type() {
        return MAP_CODEC;
    }
}