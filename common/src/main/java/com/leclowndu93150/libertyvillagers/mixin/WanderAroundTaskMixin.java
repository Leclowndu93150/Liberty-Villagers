package com.leclowndu93150.libertyvillagers.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

@Mixin(MoveToTargetSink.class)
public abstract class WanderAroundTaskMixin {

    static private final int MAX_RUN_TIME = 20 * 60; // One minute.
    static private final long STUCK_TIME = 20 * 3; // Three seconds.

    @Nullable
    @Shadow
    private Path path;

    private WalkTarget walkTarget = null;

    @Nullable
    private BlockPos previousEntityPos = null;

    private long previousEntityPosTime;

    private int fuzzyTries;

    @Shadow
    abstract boolean reachedTarget(Mob entity, WalkTarget walkTarget);

    @ModifyArg(
            method = "<init>(II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/Behavior;<init>(Ljava/util/Map;II)V"), index = 1)
    static private int replaceMinTimeForTask(int maxTime) {
        return Math.max(maxTime, 20 * 60 - 100);
    }

    @ModifyArg(
            method = "<init>(II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/Behavior;<init>(Ljava/util/Map;II)V"), index = 2)
    static private int replaceMaxTimeForTask(int maxTime) {
        return Math.max(maxTime, 20 * 60);
    }

    @Inject(method = "tryComputePath(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/entity/ai/memory/WalkTarget;J)Z",
            at = @At("HEAD"))
    private void storeWalkTargetFromHasFinishedPath(Mob entity, WalkTarget walkTarget, long time,
                                                    CallbackInfoReturnable<Boolean> cir) {
        this.walkTarget = walkTarget;
    }

    private void checkToSeeIfVillagerHasMoved(ServerLevel serverWorld, Mob entity, long time) {
        BlockPos entityPos =
                new BlockPos(entity.getBlockX(), (int)WalkNodeEvaluator.getFloorLevel(serverWorld, entity.blockPosition()),
                        entity.getBlockZ());
        if (previousEntityPos == null || !previousEntityPos.closerThan(entityPos, 1)) {
            previousEntityPos = entityPos;
            previousEntityPosTime = time;
            fuzzyTries = 0;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void keepRunningCheckToSeeIfVillagerHasMoved(ServerLevel serverWorld, Mob entity, long time,
                                                         CallbackInfo ci) {
        checkToSeeIfVillagerHasMoved(serverWorld, entity, time);
    }

    @Inject(method = "checkExtraStartConditions", at = @At("HEAD"))
    protected void shouldRun(ServerLevel serverWorld, Mob entity, CallbackInfoReturnable<Boolean> cir) {
        long time = serverWorld.getGameTime();
        checkToSeeIfVillagerHasMoved(serverWorld, entity, time);
    }

    private boolean lastDitchAttemptToFindPath(Mob entity, long time) {
        if (CONFIG.villagerPathfindingConfig.villagerWanderingFix && entity.getType() == EntityType.VILLAGER &&
                previousEntityPosTime > 0 && (time - previousEntityPosTime > STUCK_TIME)) {
            // Fuzzy pathing has failed, teleport.
            if (fuzzyTries > 3) {
                Vec3 desiredPos;
                boolean shouldRun = false;
                if (this.path != null) {
                    BlockPos blockPos = this.path.getNextNodePos();
                    desiredPos = new Vec3(blockPos.getX() + 0.5f, blockPos.getY(), blockPos.getZ() + 0.5f);
                    shouldRun = true;
                } else {
                    BlockPos blockPos = this.walkTarget.getTarget().currentBlockPosition();
                    desiredPos = new Vec3(blockPos.getX() + 0.5f, blockPos.getY(), blockPos.getZ() + 0.5f);
                }
                entity.randomTeleport(desiredPos.x, desiredPos.y, desiredPos.z, true);
                this.previousEntityPosTime = 0;
                return shouldRun;
            } else {
                // Fix for really difficult pathing situations such as the armorer's house in the SkyVillage mod using
                // fuzzy pathing to wiggle out of the area.
                BlockPos blockPos = this.walkTarget.getTarget().currentBlockPosition();
                Vec3 vec3d = DefaultRandomPos.getPosTowards((PathfinderMob) entity, 10, 7, Vec3.atBottomCenterOf(blockPos),
                        1.5707963705062866);
                if (vec3d != null) {
                    this.path = entity.getNavigation().createPath(vec3d.x, vec3d.y, vec3d.z, 0);
                }
                fuzzyTries++;
                previousEntityPosTime = time;
                return this.path != null;
            }
        }
        return false;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void keepRunninglastDitchAttemptToFindPath(ServerLevel serverWorld, Mob entity, long time,
                                                       CallbackInfo ci) {
        if (this.walkTarget == null) {
            if (entity.getBrain().getMemoryInternal(MemoryModuleType.WALK_TARGET).isEmpty()) {
                return;
            }
            this.walkTarget = entity.getBrain().getMemoryInternal(MemoryModuleType.WALK_TARGET).get();
        }
        lastDitchAttemptToFindPath(entity, time);
    }

    @Inject(method = "tryComputePath(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/entity/ai/memory/WalkTarget;J)Z",
            at = @At("RETURN"), cancellable = true)
    private void hasFinishedPathLastDitchAttemptToFindPath(Mob entity, WalkTarget walkTarget, long time,
                                                           CallbackInfoReturnable<Boolean> cir) {
        if (lastDitchAttemptToFindPath(entity, time)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @ModifyArg(
            method = "tryComputePath(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/entity/ai/memory/WalkTarget;J)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;createPath(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/pathfinder/Path;"), index = 1)
    int replaceDistanceInFindPathToInHasFinishedPath(int distance) {
        // Fix for villagers being unable to path to POIs where that are surrounded by blocks except for one side.
        // VillagerWalkTowardsTask uses manhattan distance, FindPathTo uses crow-flies distance. Setting the
        // distance to 1 means that positions all around the POI are valid, but still within the manhattan distance
        // of 3 (assuming VillagerWalkTowardsTask uses 3).
        if (walkTarget.getTarget() instanceof BlockPosTracker) {
            return Math.max(1, distance);
        }
        return distance;
    }
}
