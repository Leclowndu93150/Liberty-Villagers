package com.leclowndu93150.libertyvillagers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.phys.Vec3;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(WaterAvoidingRandomStrollGoal.class)
public abstract class WanderAroundFarGoalMixin extends RandomStrollGoal  {

    public WanderAroundFarGoalMixin() {
        super(null, 0, 240, false);
    }

    @Inject(method = "getPosition", at = @At("RETURN"), cancellable = true)
    private void getWanderTargetDoesNotExceedRange(CallbackInfoReturnable<Vec3> cir) {
        if (!CONFIG.catsConfig.catsStayNearBell) {
            return;
        }
        if (this.mob.getType() != EntityType.CAT) {
            return;
        }
        Vec3 dest = cir.getReturnValue();
        if (dest == null) {
            return;
        }
        ServerLevel serverWorld = (ServerLevel) this.mob.level();
        PoiManager pointOfInterestStorage = serverWorld.getPoiManager();

        Optional<BlockPos> nearestBell =
                pointOfInterestStorage.findClosest(poiType -> poiType.is(PoiTypes.MEETING),
                        this.mob.blockPosition(), 2 * CONFIG.catsConfig.catsMaxBellRange,
                        PoiManager.Occupancy.ANY);

        if (nearestBell.isPresent()) {
            BlockPos nearestBellPos = nearestBell.get();
            if (!nearestBellPos.closerToCenterThan(dest, CONFIG.catsConfig.catsMaxBellRange)) {
                // Wander back towards the bell.
                dest = LandRandomPos.getPosTowards(this.mob, 10, 7, Vec3.atLowerCornerOf(nearestBellPos));
                cir.setReturnValue(dest);
                cir.cancel();
            }
        }
    }
}
