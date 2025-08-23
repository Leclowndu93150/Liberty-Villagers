package com.leclowndu93150.libertyvillagers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.phys.Vec3;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(MoveTowardsTargetGoal.class)
public abstract class WanderNearTargetGoalMixin {

    @Shadow
    private PathfinderMob mob;

    void checkForValidTarget(CallbackInfoReturnable<Boolean> cir) {
        if (this.mob.getType() != EntityType.IRON_GOLEM) {
            return;
        }
        if (this.mob.getTarget() == null) {
            return;
        }
        if (CONFIG.golemsConfig.golemStayNearBell) {
            Vec3 targetPos = this.mob.getTarget().position();
            ServerLevel serverWorld = (ServerLevel) this.mob.level();
            PoiManager pointOfInterestStorage = serverWorld.getPoiManager();

            Optional<BlockPos> nearestBell = pointOfInterestStorage.findClosest(
                    poiType -> poiType.is(PoiTypes.MEETING), this.mob.blockPosition(),
                    2 * CONFIG.golemsConfig.golemMaxBellRange, PoiManager.Occupancy.ANY);

            if (nearestBell.isPresent()) {
                BlockPos nearestBellPos = nearestBell.get();
                if (!nearestBellPos.closerToCenterThan(targetPos, CONFIG.golemsConfig.golemMaxBellRange)) {
                    cir.setReturnValue(false);
                    cir.cancel();
                }
            }
        }
        if (CONFIG.golemsConfig.golemsAvoidWater) {
            if (this.mob.getTarget().isInWater()) {
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void canStartIfNotTooFarFromBell(CallbackInfoReturnable<Boolean> cir) {
        checkForValidTarget(cir);
    }

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    public void shouldContinueIfNotTooFarFromBell(CallbackInfoReturnable<Boolean> cir) {
        checkForValidTarget(cir);
    }
}
