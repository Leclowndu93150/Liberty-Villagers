package com.leclowndu93150.libertyvillagers.mixin;

import com.mojang.datafixers.kinds.K1;
import java.util.function.Predicate;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.GoToWantedItem;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GoToWantedItem.class)
public abstract class WalkToNearestVisibleWantedItemTaskMixin {

    @Overwrite
    public static <E extends LivingEntity> BehaviorControl<E> create(Predicate<E> pCanWalkToItem, float pSpeedModifier, boolean pHasTarget, int pMaxDistToWalk) {
        return BehaviorBuilder.create((p_258371_) -> {
            BehaviorBuilder<E, ? extends MemoryAccessor<? extends K1, WalkTarget>> behaviorbuilder = pHasTarget ? p_258371_.registered(MemoryModuleType.WALK_TARGET) : p_258371_.absent(MemoryModuleType.WALK_TARGET);
            return p_258371_.group(p_258371_.registered(MemoryModuleType.LOOK_TARGET), behaviorbuilder, p_258371_.present(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM), p_258371_.registered(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS)).apply(p_258371_, (p_258387_, p_258388_, p_258389_, p_258390_) -> (p_258380_, p_258381_, p_258382_) -> {
                ItemEntity itementity = (ItemEntity)p_258371_.get(p_258389_);

                if (p_258381_.getType() == EntityType.VILLAGER) {
                    Villager villager = (Villager) p_258381_;

                    if (itementity.closerThan(p_258381_, 0)) {
                        return false;
                    }

                    ItemStack stack = itementity.getItem();
                    if (!villager.getInventory().canAddItem(stack)) {
                        return false;
                    }
                }
                
                if (p_258371_.tryGet(p_258390_).isEmpty() && pCanWalkToItem.test(p_258381_) && itementity.closerThan(p_258381_, (double)pMaxDistToWalk) && p_258381_.level().getWorldBorder().isWithinBounds(itementity.blockPosition())) {
                    WalkTarget walktarget = new WalkTarget(new EntityTracker(itementity, false), pSpeedModifier, 0);
                    p_258387_.set(new EntityTracker(itementity, true));
                    p_258388_.set(walktarget);
                    return true;
                } else {
                    return false;
                }
            });
        });
    }
}
