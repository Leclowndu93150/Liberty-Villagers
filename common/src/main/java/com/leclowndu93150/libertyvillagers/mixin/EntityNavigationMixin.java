package com.leclowndu93150.libertyvillagers.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PathNavigation.class)
public abstract class EntityNavigationMixin {

    @Final
    @Shadow
    protected Mob mob;

    @Shadow
    protected Path path;

    @Inject(method = "followThePath", at = @At("HEAD"), cancellable = true)
    protected void continueFollowingPath(CallbackInfo ci) {
        if (this.mob.getType() == EntityType.VILLAGER || this.mob.getType() == EntityType.IRON_GOLEM) {
            float tempNodeReachProximity = this.mob.getBbWidth() > 0.75f ? this.mob.getBbWidth() / 2.0f :
                    0.75f - this.mob.getBbWidth() / 2.0f;
            BlockPos vec3i = this.path.getNextNodePos();
            double d = Math.abs(this.mob.getX() - ((double) vec3i.getX() + 0.5));
            double f = Math.abs(this.mob.getZ() - ((double) vec3i.getZ() + 0.5));
            double g = d * d + f * f;
            // Prevent the case where the villager needs to make an u turn to get up a set of
            // steep stairs from a slab, but decides they are "close enough" and attempts to jump
            // to a stair that is too high. Using the pythagorean theorem to determine distance from
            // next node.
            if (g >= (double) (tempNodeReachProximity * tempNodeReachProximity)) {
                ci.cancel();
            }
        }
    }
}