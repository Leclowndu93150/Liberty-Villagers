package com.leclowndu93150.libertyvillagers.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.StrollToPoi;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.mutable.MutableLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StrollToPoi.class)
public class GoToNearbyPositionTaskMixin {

    @Inject(method = "lambda$create$0(Lnet/minecraft/world/entity/ai/behavior/declarative/BehaviorBuilder$Instance;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;ILorg/apache/commons/lang3/mutable/MutableLong;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;FILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)Z",
            at = @At("HEAD"),
            cancellable = true)
    static private void dontRunIfFishing(BehaviorBuilder.Instance<PathfinderMob> context, MemoryAccessor result,
                                         int maxDistance,
            MutableLong mutableLong, MemoryAccessor result2, float walkSpeed,
            int completionRange,
            ServerLevel serverWorld, PathfinderMob pathAwareEntity, long time,
            CallbackInfoReturnable<Boolean> cir) {
        if (pathAwareEntity.getType() == EntityType.VILLAGER) {
            Villager villager = (Villager) pathAwareEntity;
            if (villager.getVillagerData().getProfession() == VillagerProfession.FISHERMAN &&
                    villager.getMainHandItem().is(Items.FISHING_ROD)) {
                    cir.setReturnValue(false);
                    cir.cancel();
            }
        }
    }
}
