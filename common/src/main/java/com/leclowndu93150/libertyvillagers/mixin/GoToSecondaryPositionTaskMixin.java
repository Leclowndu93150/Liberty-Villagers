package com.leclowndu93150.libertyvillagers.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.StrollToPoiList;
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

@Mixin(StrollToPoiList.class)
public class GoToSecondaryPositionTaskMixin {

    @Inject(method = "lambda$create$0(Lnet/minecraft/world/entity/ai/behavior/declarative/BehaviorBuilder$Instance;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;ILorg/apache/commons/lang3/mutable/MutableLong;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;FILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/Villager;J)Z", at = @At("HEAD"), cancellable = true)
    static private void dontRunIfFishing(BehaviorBuilder.Instance<PathfinderMob> context,
                                         MemoryAccessor walkTarget,
                                         MemoryAccessor secondaryPositions, int completionRange,
                                         MutableLong mutableLong, MemoryAccessor primaryPositions, float walkSpeed,
                                         int primaryPositionActivationDistance,
                                         ServerLevel serverWorld, Villager villagerEntity, long time,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (villagerEntity.getVillagerData().getProfession() == VillagerProfession.FISHERMAN &&
                villagerEntity.getMainHandItem().is(Items.FISHING_ROD)) {
                cir.setReturnValue(false);
                cir.cancel();
        }
    }
}
