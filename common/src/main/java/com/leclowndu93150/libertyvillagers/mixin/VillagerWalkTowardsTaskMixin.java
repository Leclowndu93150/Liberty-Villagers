package com.leclowndu93150.libertyvillagers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromBlockMemory;

@Mixin(SetWalkTargetFromBlockMemory.class)
public abstract class VillagerWalkTowardsTaskMixin {
    @ModifyVariable(method = "create(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;FIII)Lnet/minecraft/world/entity/ai/behavior/OneShot;",
            at = @At("HEAD"),
            ordinal = 0)
    private static int increaseCompletionRange(int completionRange) {
        return CONFIG.villagerPathfindingConfig.minimumPOISearchDistance;
    }

    @ModifyVariable(method = "create(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;FIII)Lnet/minecraft/world/entity/ai/behavior/OneShot;",
            at = @At("HEAD"),
            ordinal = 1)
    private static int increaseMaxDistance(int maxDistance) {
        return CONFIG.villagerPathfindingConfig.pathfindingMaxRange;
    }

    @ModifyVariable(method = "create(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;FIII)Lnet/minecraft/world/entity/ai/behavior/OneShot;",
            at = @At("HEAD"),
            ordinal = 2)
    private static int increaseMaxRunTime(int maxRunTime) {
        return CONFIG.villagerPathfindingConfig.walkTowardsTaskMaxRunTime;
    }
}