package com.leclowndu93150.libertyvillagers.mixin;

import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.GoToPotentialJobSite;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

@Mixin(GoToPotentialJobSite.class)
public class WalkTowardJobSiteTaskMixin {

    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;J)V",
            at = @At("HEAD"),
            cancellable = true)
    private void dontSetWalkTargetIfAlreadySet(ServerLevel serverWorld, Villager villagerEntity, long l,
                                               CallbackInfo ci) {
        // Prevent the villager from spamming the brain over and over with the same walk target.
        if (villagerEntity.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
            ci.cancel();
        }
    }

    @ModifyArg(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;J)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/behavior/BehaviorUtils;setWalkAndLookTargetMemories(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;FI)V"), index = 3)
    private int replaceCompletionRangeInClaimSite(int completionRange) {
        return Math.max(completionRange, CONFIG.villagerPathfindingConfig.minimumPOISearchDistance);
    }
}
