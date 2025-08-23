package com.leclowndu93150.libertyvillagers.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(NearestAttackableTargetGoal.class)
public abstract class ActiveTargetGoalMixin extends TargetGoal {

    @Shadow
    protected TargetingConditions targetConditions;

    public ActiveTargetGoalMixin(Mob mob) {
        super(mob, false);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/Mob;Ljava/lang/Class;IZZLjava/util/function/Predicate;)V",
            at = @At("RETURN"))
    void changeAngerDistanceForIronGolems(Mob mob, Class<?> targetClass, int reciprocalChance,
                                          boolean checkVisibility, boolean checkCanNavigate,
                                          @Nullable Predicate<LivingEntity> targetPredicate, CallbackInfo ci) {
        if (mob.getType() == EntityType.IRON_GOLEM) {
            this.targetConditions.range(CONFIG.golemsConfig.golemAggroRange);
        }
    }

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    public void canStartIfNotTooFarFromBell(CallbackInfoReturnable<Boolean> cir) {
        if (mob.getType() == EntityType.IRON_GOLEM && CONFIG.golemsConfig.golemStayNearBell) {
            ServerLevel serverWorld = (ServerLevel) this.mob.level();
            PoiManager pointOfInterestStorage = serverWorld.getPoiManager();

            Optional<BlockPos> nearestBell = pointOfInterestStorage.findClosest(
                    poiType -> poiType.is(PoiTypes.MEETING), this.mob.blockPosition(),
                    2 * CONFIG.golemsConfig.golemMaxBellRange, PoiManager.Occupancy.ANY);

            if (nearestBell.isPresent()) {
                BlockPos nearestBellPos = nearestBell.get();
                if (!nearestBellPos.closerThan(this.mob.blockPosition(), CONFIG.golemsConfig.golemMaxBellRange)) {
                    cir.setReturnValue(false);
                    cir.cancel();
                }
            }
        }
    }

    @ModifyVariable(method = "getTargetSearchArea(D)Lnet/minecraft/world/phys/AABB;", at = @At("HEAD"), ordinal = 0)
    private double replaceSearchBoxForIronGolems(double distance) {
        if (mob.getType() == EntityType.IRON_GOLEM) {
            return CONFIG.golemsConfig.golemAggroRange;
        }
        return distance;
    }
}
