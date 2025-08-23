package com.leclowndu93150.libertyvillagers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.GolemRandomStrollInVillageGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.phys.Vec3;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(GolemRandomStrollInVillageGoal.class)
public abstract class IronGolemWanderAroundGoalMixin extends RandomStrollGoal {

    public IronGolemWanderAroundGoalMixin() {
        super(null, 0, 240, false);
    }

    @Inject(method = "getPosition", at = @At("RETURN"), cancellable = true)
    private void getWanderTargetDoesNotExceedRange(CallbackInfoReturnable<Vec3> cir) {
        Vec3 dest = cir.getReturnValue();
        if (dest == null) {
            return;
        }
        if (CONFIG.golemsConfig.golemStayNearBell) {
            ServerLevel serverWorld = (ServerLevel) this.mob.level();
            PoiManager pointOfInterestStorage = serverWorld.getPoiManager();

            Optional<BlockPos> nearestBell = pointOfInterestStorage.findClosest(poiType -> poiType.is(PoiTypes.MEETING),
                    this.mob.blockPosition(), 2 * CONFIG.golemsConfig.golemMaxBellRange, PoiManager.Occupancy.ANY);

            if (nearestBell.isPresent()) {
                BlockPos nearestBellPos = nearestBell.get();
                if (!nearestBellPos.closerToCenterThan(dest, CONFIG.golemsConfig.golemMaxBellRange)) {
                    // Wander back towards the bell.
                    dest = AirRandomPos.getPosTowards(this.mob, 10, 7, 0, Vec3.atBottomCenterOf(nearestBellPos),
                            0.3141592741012573);
                    cir.setReturnValue(dest);
                    cir.cancel();
                    return;
                }
            }
        }
        if (CONFIG.golemsConfig.golemsAvoidWater) {
            if (!this.mob.level().getFluidState(BlockPos.containing(dest.x, dest.y, dest.z)).isEmpty()) {
                dest = AirRandomPos.getPosTowards(this.mob, 10, 7, 0, dest, 0.3141592741012573);
                cir.setReturnValue(dest);
                cir.cancel();
            }
        }
    }

    @ModifyConstant(
            method = "getPositionTowardsVillagerWhoWantsGolem",
            constant = @Constant(doubleValue = 32.0))
    private double replaceFindVillagerRange(double value) {
        return CONFIG.villagerPathfindingConfig.findPOIRange;
    }
}
