package com.leclowndu93150.libertyvillagers.mixin;

import com.leclowndu93150.libertyvillagers.goal.ReturnToShoreGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(IronGolem.class)
public abstract class IronGolemEntityMixin extends PathfinderMob {

    public IronGolemEntityMixin(EntityType<? extends AbstractGolem> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V")
    public void avoidCactus(EntityType<? extends AbstractGolem> entityType, Level world, CallbackInfo ci) {
        if (CONFIG.golemsConfig.golemsAvoidCactus) {
            this.setPathfindingMalus(PathType.DANGER_OTHER, -1);
        }
        if (CONFIG.golemsConfig.golemsAvoidWater) {
            this.setPathfindingMalus(PathType.WATER, 16);
            this.setPathfindingMalus(PathType.WATER_BORDER, 8);
        }
        if (CONFIG.golemsConfig.golemsAvoidRail) {
            this.setPathfindingMalus(PathType.RAIL, -1);
        }
        if (CONFIG.golemsConfig.golemsAvoidTrapdoor) {
            this.setPathfindingMalus(PathType.TRAPDOOR, -1);
        }
        if (CONFIG.golemsConfig.golemsAvoidPowderedSnow) {
            this.setPathfindingMalus(PathType.POWDER_SNOW, -1);
            this.setPathfindingMalus(PathType.DANGER_POWDER_SNOW, 16);
        }
    }

    @Inject(at = @At("HEAD"), method = "canAttackType", cancellable = true)
    public void replaceCanTarget(EntityType<?> type, CallbackInfoReturnable<Boolean> cir) {
        if (CONFIG.golemsConfig.golemsDontAttackPlayer && type == EntityType.PLAYER) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "registerGoals",
        at = @At("HEAD")
    )
    protected void initGoalsAddReturnToShore(CallbackInfo ci) {
        this.goalSelector.addGoal(1, new ReturnToShoreGoal(this, 1.0));
    }
}