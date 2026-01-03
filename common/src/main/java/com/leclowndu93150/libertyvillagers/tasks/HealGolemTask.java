package com.leclowndu93150.libertyvillagers.tasks;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

public class HealGolemTask extends HealTargetTask {
    private static final int COMPLETION_RANGE = 3;

    public HealGolemTask() {
        super(COMPLETION_RANGE);
    }

    protected List<LivingEntity> getPossiblePatients(ServerLevel serverWorld, Villager villagerEntity) {
        List<LivingEntity> possiblePatients = Lists.newArrayList();
        if (!CONFIG.villagersProfessionConfig.armorerHealsGolems) {
            return possiblePatients;
        }

        List<IronGolem> golems = villagerEntity.level().getEntitiesOfClass(IronGolem.class,
                villagerEntity.getBoundingBox().inflate(CONFIG.villagersProfessionConfig.armorerHealsGolemsRange));
        possiblePatients.addAll(golems);
        return possiblePatients;
    }

    protected void healTarget(ServerLevel serverWorld, Villager villagerEntity, LivingEntity currentPatient) {
        float g = 1.0f + (currentPatient.getRandom().nextFloat() - currentPatient.getRandom().nextFloat()) * 0.2f;
        currentPatient.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.F, g);
        currentPatient.heal(currentPatient.getMaxHealth());
    }
}
