package com.leclowndu93150.libertyvillagers.mixin;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.YieldJobSite;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(YieldJobSite.class)
public class TakeJobSiteTaskMixin {

    @Shadow
    private static boolean nearbyWantsJobsite(Holder<PoiType> type, Villager nearbyVillager, BlockPos poiPos) {
        throw new UnsupportedOperationException();
    }

    @Shadow
    private static boolean canReachPos(PathfinderMob nearbyVillager, BlockPos poiPos, PoiType type) {
        throw new UnsupportedOperationException();
    }

    @Overwrite
    public static BehaviorControl<Villager> create(float speedModifier) {
        return BehaviorBuilder.create(
            i -> i.group(
                    i.present(MemoryModuleType.POTENTIAL_JOB_SITE),
                    i.absent(MemoryModuleType.JOB_SITE),
                    i.present(MemoryModuleType.NEAREST_LIVING_ENTITIES),
                    i.registered(MemoryModuleType.WALK_TARGET),
                    i.registered(MemoryModuleType.LOOK_TARGET)
                )
                .apply(
                    i,
                    (potentialJob, jobSite, nearestEntities, walkTarget, lookTarget) -> (level, body, timestamp) -> {
                        if (body.isBaby()) {
                            return false;
                        } else if (!body.getVillagerData().profession().is(VillagerProfession.NONE)) {
                            return false;
                        } else {
                            BlockPos poiPos = i.<GlobalPos>get(potentialJob).pos();
                            Optional<Holder<PoiType>> poiType = level.getPoiManager().getType(poiPos);
                            if (poiType.isEmpty()) {
                                return true;
                            } else {
                                i.<List<LivingEntity>>get(nearestEntities)
                                    .stream()
                                    .filter(v -> v instanceof Villager && v != body)
                                    .map(v -> (Villager)v)
                                    .filter(LivingEntity::isAlive)
                                    .filter(v -> nearbyWantsJobsite(poiType.get(), v, poiPos))
                                    .findFirst()
                                    .ifPresent(nearbyVillager -> {
                                        walkTarget.erase();
                                        lookTarget.erase();
                                        potentialJob.erase();
                                        if (nearbyVillager.getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty()) {
                                            // Custom: Use configurable completion range
                                            int completionRange = Math.max(1, CONFIG.villagerPathfindingConfig.minimumPOISearchDistance + 1);
                                            BehaviorUtils.setWalkAndLookTargetMemories(nearbyVillager, poiPos, speedModifier, completionRange);
                                            nearbyVillager.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.of(level.dimension(), poiPos));
                                            level.debugSynchronizers().updatePoi(poiPos);
                                        }
                                    });
                                return true;
                            }
                        }
                    }
                )
        );
    }
}
