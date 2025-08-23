package com.leclowndu93150.libertyvillagers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

import net.minecraft.world.entity.ai.behavior.YieldJobSite;

@Mixin(YieldJobSite.class)
public class TakeJobSiteTaskMixin {

    // Injecting into a lambda in create().
    @SuppressWarnings("target")
    @ModifyArg(method = "lambda$create$3(Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;Lnet/minecraft/core/BlockPos;FLnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/Villager;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/BehaviorUtils;setWalkAndLookTargetMemories(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;FI)V"), index = 3)
    static private int replaceCompletionRangeInClaimSite(int completionRange) {
        return Math.max(completionRange, CONFIG.villagerPathfindingConfig.minimumPOISearchDistance + 1);
    }

}
