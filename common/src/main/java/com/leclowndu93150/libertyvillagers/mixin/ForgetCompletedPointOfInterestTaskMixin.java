package com.leclowndu93150.libertyvillagers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.ValidateNearbyPoi;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;

@Mixin(ValidateNearbyPoi.class)
public abstract class ForgetCompletedPointOfInterestTaskMixin  {

    @SuppressWarnings("target")
    @Inject(method = "lambda$create$0(Lnet/minecraft/world/entity/ai/behavior/declarative/BehaviorBuilder$Instance;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;J)Z",
            at = @At("HEAD"),
            cancellable = true)
    private static void shouldRun(BehaviorBuilder.Instance context, MemoryAccessor poiPos, Predicate predicate,
                             ServerLevel world,
                             LivingEntity entity,
                             long time,
                             CallbackInfoReturnable<Boolean> cir) {
        @SuppressWarnings("unchecked")
        GlobalPos globalPos = (GlobalPos)context.get(poiPos);
        BlockPos blockPos = globalPos.pos();
        // Replace isWithinDistance with the Manhattan distance to avoid being confused by beds placed near stairs.
        if (world.dimension() != globalPos.dimension() ||  (blockPos.distManhattan(entity.blockPosition()) >= 4))  {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}

