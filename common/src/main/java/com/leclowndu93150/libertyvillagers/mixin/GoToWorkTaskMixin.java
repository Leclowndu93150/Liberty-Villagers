package com.leclowndu93150.libertyvillagers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;

@Mixin(AssignProfessionFromJobSite.class)
public class GoToWorkTaskMixin {

    // Inject into the lambda called by Task.trigger.
    @SuppressWarnings({"target", "descriptor"})
    @ModifyConstant(
            method = "lambda$create$4(Lnet/minecraft/world/entity/ai/behavior/declarative/BehaviorBuilder$Instance;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/Villager;J)Z",
            constant = @Constant(doubleValue = 2.0))
    static private double modifyDistanceInShouldRun(double distance) {
        return Math.max(distance, CONFIG.villagerPathfindingConfig.minimumPOISearchDistance + 1);
    }
}
