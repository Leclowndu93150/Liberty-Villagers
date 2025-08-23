package com.leclowndu93150.libertyvillagers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

@Mixin(MeleeAttackGoal.class)
public class MeleeAttackGoalMixin {

    @Shadow
    protected PathfinderMob mob;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    public void canStart(CallbackInfoReturnable<Boolean> cir) {
        if (this.mob.getType() == EntityType.IRON_GOLEM && CONFIG.golemsConfig.golemsAvoidWater) {
            if (this.mob.getTarget() != null && this.mob.getTarget().isInWater()) {
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }
}
