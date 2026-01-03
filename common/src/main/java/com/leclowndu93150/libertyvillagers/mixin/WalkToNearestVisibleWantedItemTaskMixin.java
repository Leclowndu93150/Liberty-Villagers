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
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GoToWantedItem.class)
public abstract class WalkToNearestVisibleWantedItemTaskMixin {

    @Overwrite
    public static <E extends LivingEntity> BehaviorControl<E> create(
            Predicate<E> predicate, float speedModifier, boolean interruptOngoingWalk, int maxDistToWalk) {
        return BehaviorBuilder.create(
            i -> {
                BehaviorBuilder<E, ? extends MemoryAccessor<? extends K1, WalkTarget>> walkCondition = interruptOngoingWalk
                    ? i.registered(MemoryModuleType.WALK_TARGET)
                    : i.absent(MemoryModuleType.WALK_TARGET);
                return i.group(
                        i.registered(MemoryModuleType.LOOK_TARGET),
                        walkCondition,
                        i.present(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM),
                        i.registered(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS)
                    )
                    .apply(
                        i,
                        (lookTarget, walkTarget, wantedItem, cooldown) -> (level, body, timestamp) -> {
                            ItemEntity item = i.get(wantedItem);

                            // Custom villager inventory check
                            if (body.getType() == EntityType.VILLAGER) {
                                Villager villager = (Villager) body;

                                if (item.closerThan(body, 0)) {
                                    return false;
                                }

                                ItemStack stack = item.getItem();
                                if (!villager.getInventory().canAddItem(stack)) {
                                    return false;
                                }
                            }

                            if (i.tryGet(cooldown).isEmpty()
                                && predicate.test(body)
                                && item.closerThan(body, maxDistToWalk)
                                && body.level().getWorldBorder().isWithinBounds(item.blockPosition())
                                && body.canPickUpLoot()) {
                                WalkTarget target = new WalkTarget(new EntityTracker(item, false), speedModifier, 0);
                                lookTarget.set(new EntityTracker(item, true));
                                walkTarget.set(target);
                                return true;
                            } else {
                                return false;
                            }
                        }
                    );
            }
        );
    }
}
