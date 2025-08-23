package com.leclowndu93150.libertyvillagers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.GoToWantedItem;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;

@Mixin(GoToWantedItem.class)
public abstract class WalkToNearestVisibleWantedItemTaskMixin {

    // Injecting into the lambda of the TaskTriggerer.
    @SuppressWarnings({"target", "descriptor"})
    @Inject(method = "lambda$create$1(Lnet/minecraft/world/entity/ai/behavior/declarative/BehaviorBuilder$Instance;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;Ljava/util/function/Predicate;IFLnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;J)Z",
            at = @At("HEAD"),
            cancellable = true)
    static private void dontMoveIfOnTopOfItem(BehaviorBuilder.Instance context,
                                              MemoryAccessor nearestVisibleWantedItem,
                                              MemoryAccessor itemPickupCooldownTicks,
                                              Predicate startCondition,
                                              int radius,
                                              float speed,
                                              MemoryAccessor walkTarget,
                                              MemoryAccessor lookTarget,
                                              ServerLevel world,
                                              LivingEntity entity,
                                              long time,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (entity.getType() != EntityType.VILLAGER) {
            return;
        }
        @SuppressWarnings("unchecked")
        ItemEntity itemEntity = (ItemEntity)context.get(nearestVisibleWantedItem);
        if (itemEntity.closerThan(entity, 0)) {
            // Already on top of the nearest visible item.
            cir.setReturnValue(false);
            cir.cancel();
        }
        // If our inventory is full, don't move towards the item.
        ItemStack stack = itemEntity.getItem();
        if (!((Villager)entity).getInventory().canAddItem(stack)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
