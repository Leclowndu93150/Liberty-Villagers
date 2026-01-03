package com.leclowndu93150.libertyvillagers.mixin;

import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.StrollAroundPoi;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(StrollAroundPoi.class)
public class GoToIfNearbyTaskMixin {

    @Overwrite
    public static OneShot<PathfinderMob> create(MemoryModuleType<GlobalPos> memoryType, float speedModifier, int maxDistanceFromPoi) {
        MutableLong nextOkStartTime = new MutableLong(0L);
        return BehaviorBuilder.create(
            i -> i.group(i.registered(MemoryModuleType.WALK_TARGET), i.present(memoryType)).apply(i, (walkTarget, memory) -> (level, body, timestamp) -> {
                // Custom: Check if the entity is a fisherman villager holding a fishing rod
                if (body.getType() == EntityType.VILLAGER) {
                    Villager villager = (Villager) body;
                    if (villager.getVillagerData().profession().is(VillagerProfession.FISHERMAN) &&
                            villager.getMainHandItem().is(Items.FISHING_ROD)) {
                        return false; // Prevent wandering while fishing
                    }
                }

                GlobalPos pos = i.get(memory);
                if (level.dimension() != pos.dimension() || !pos.pos().closerToCenterThan(body.position(), maxDistanceFromPoi)) {
                    return false;
                } else if (timestamp <= nextOkStartTime.longValue()) {
                    return true;
                } else {
                    Optional<Vec3> landPos = Optional.ofNullable(LandRandomPos.getPos(body, 8, 6));
                    walkTarget.setOrErase(landPos.map(p -> new WalkTarget(p, speedModifier, 1)));
                    nextOkStartTime.setValue(timestamp + 180L);
                    return true;
                }
            })
        );
    }
}
