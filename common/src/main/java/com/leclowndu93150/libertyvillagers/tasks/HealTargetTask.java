package com.leclowndu93150.libertyvillagers.tasks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;

public abstract class HealTargetTask extends Behavior<Villager> {
    public static final float WALK_SPEED = 0.7F;
    private static final int MAX_RUN_TIME = 1000;

    @Nullable
    private LivingEntity currentPatient;
    private long nextResponseTime;
    private int ticksRan;
    private int completionRange;

    public HealTargetTask(int completionRange) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.JOB_SITE,
                MemoryStatus.VALUE_PRESENT), MAX_RUN_TIME);
        this.completionRange = completionRange;
    }

    protected abstract List<LivingEntity> getPossiblePatients(ServerLevel serverWorld, Villager villagerEntity);

    @Override
    protected boolean checkExtraStartConditions(ServerLevel serverWorld, Villager villagerEntity) {
        List<LivingEntity> possiblePatients = getPossiblePatients(serverWorld, villagerEntity);
        List<LivingEntity> patients = Lists.newArrayList();
        for (LivingEntity possiblePatient : possiblePatients) {
            if (isValidPatient(possiblePatient)) {
                patients.add(possiblePatient);
            }
        }

        if (patients.size() == 0) {
            return false;
        }

        this.currentPatient = patients.get(serverWorld.getRandom().nextInt(patients.size()));
        return this.currentPatient != null;
    }

    @Override
    protected void start(ServerLevel serverWorld, Villager villagerEntity, long l) {
        if (l > this.nextResponseTime && this.currentPatient != null) {
            villagerEntity.getBrain()
                    .setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.currentPatient, true));
            villagerEntity.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(this.currentPatient, WALK_SPEED, completionRange));
        }
    }

    @Override
    protected void stop(ServerLevel serverWorld, Villager villagerEntity, long l) {
        villagerEntity.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        villagerEntity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.ticksRan = 0;
        this.nextResponseTime = l + 40L;
    }

    @Override
    protected void tick(ServerLevel serverWorld, Villager villagerEntity, long l) {
        // Check to see if the patient was healed or died before the villager reached it.
        if (!isValidPatient(currentPatient)) {
            return;
        }

        if (this.currentPatient.distanceTo(villagerEntity) <= completionRange) {
            healTarget(serverWorld, villagerEntity, currentPatient);
            currentPatient = null;
            ++this.ticksRan;
        }
    }

    protected abstract void healTarget(ServerLevel serverWorld, Villager villagerEntity,
                                       LivingEntity currentPatient);

    @Override
    protected boolean canStillUse(ServerLevel serverWorld, Villager villagerEntity, long l) {
        // Check to see if the patient was healed or died before the villager reached it.
        if (!isValidPatient(currentPatient)) {
            return false;
        }
        return this.ticksRan < MAX_RUN_TIME;
    }

    protected boolean isValidPatient(LivingEntity entity) {
        return entity != null && !(entity.getHealth() >= entity.getMaxHealth()) && entity.isAlive() &&
                !entity.isInvisible() && !entity.isInvulnerable() &&
                !entity.hasEffect(MobEffects.REGENERATION);
    }
}
