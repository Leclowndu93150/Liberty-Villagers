package com.leclowndu93150.libertyvillagers.mixin;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StrollToPoi;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.mutable.MutableLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(StrollToPoi.class)
public class GoToNearbyPositionTaskMixin {

    @Overwrite
    public static BehaviorControl<PathfinderMob> create(
        MemoryModuleType<GlobalPos> memoryType, float speedModifier, int closeEnoughDist, int maxDistanceFromPoi
    ) {
        MutableLong nextOkStartTime = new MutableLong(0L);
        return BehaviorBuilder.create(
            i -> i.group(i.registered(MemoryModuleType.WALK_TARGET), i.present(memoryType)).apply(i, (walkTarget, memory) -> (level, body, timestamp) -> {
                // Custom: Check if the entity is a fisherman villager holding a fishing rod
                if (body.getType() == EntityType.VILLAGER) {
                    Villager villager = (Villager) body;
                    if (villager.getVillagerData().profession().is(VillagerProfession.FISHERMAN) &&
                            villager.getMainHandItem().is(Items.FISHING_ROD)) {
                        return false; // Prevent strolling while fishing
                    }
                }

                GlobalPos pos = i.get(memory);
                if (level.dimension() != pos.dimension() || !pos.pos().closerToCenterThan(body.position(), maxDistanceFromPoi)) {
                    return false;
                } else if (timestamp <= nextOkStartTime.longValue()) {
                    return true;
                } else {
                    walkTarget.set(new WalkTarget(pos.pos(), speedModifier, closeEnoughDist));
                    nextOkStartTime.setValue(timestamp + 80L);
                    return true;
                }
            })
        );
    }
}
