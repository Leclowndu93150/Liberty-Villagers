package com.leclowndu93150.libertyvillagers.tasks;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class FeedTargetTask extends HealTargetTask {
    private static final int COMPLETION_RANGE = 3;

    private final Class<? extends LivingEntity> entityClass;
    private final ImmutableSet<Item> foodTypes;

    private final double range;

    private final int maxEntities;

    public FeedTargetTask(Class<? extends LivingEntity> entityClass, ImmutableSet<Item> foodTypes, double range,
                          int maxEntities) {
        super(COMPLETION_RANGE);
        this.entityClass = entityClass;
        this.foodTypes = foodTypes;
        this.range = range;
        this.maxEntities = maxEntities;
    }

    @SuppressWarnings("unchecked")
    protected List<LivingEntity> getPossiblePatients(ServerLevel serverWorld, Villager villagerEntity) {
        if (!villagerEntity.getInventory().hasAnyOf(foodTypes)) {
            return Lists.newArrayList();
        }

        List<? extends LivingEntity> possiblePatients =
                villagerEntity.level().getEntitiesOfClass(entityClass,
                villagerEntity.getBoundingBox().inflate(range));

        if (possiblePatients.size() >= maxEntities) {
            return Lists.newArrayList();
        }

        return (List<LivingEntity>) possiblePatients;
    }

    protected void healTarget(ServerLevel serverWorld, Villager villagerEntity, LivingEntity currentPatient) {
        if (!(currentPatient instanceof Animal)) {
            return;
        }
        Animal animal = (Animal)currentPatient;
        if (!animal.canFallInLove()) {
            return;
        }
        SimpleContainer simpleInventory = villagerEntity.getInventory();
        if (!simpleInventory.hasAnyOf(foodTypes)) {
            return;
        }

        for (int i = 0; i < simpleInventory.getContainerSize(); ++i) {
            ItemStack itemStack = simpleInventory.getItem(i);
            if (itemStack.isEmpty() || !foodTypes.contains(itemStack.getItem())) continue;
            animal.setInLove(null);
            itemStack.shrink(1);
            break;
        }
    }

    protected boolean isValidPatient(LivingEntity entity) {
        if (!(entity instanceof Animal)) {
            return false;
        }
        Animal animal = (Animal)entity;
        return entity != null && entity.isAlive() &&
                !entity.isInvisible() && !entity.isInvulnerable() && animal.canFallInLove() && !animal.isBaby();
    }

}
