package com.leclowndu93150.libertyvillagers.mixin;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.SetClosestHomeAsWalkTarget;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(value = SetClosestHomeAsWalkTarget.class)
public abstract class WalkHomeTaskMixin {

    @Overwrite
    public static BehaviorControl<PathfinderMob> create(float speedModifier) {
        Long2LongMap batchCache = new Long2LongOpenHashMap();
        MutableLong lastUpdate = new MutableLong(0L);
        return BehaviorBuilder.create(
            instance -> instance.group(instance.absent(MemoryModuleType.WALK_TARGET), instance.absent(MemoryModuleType.HOME))
                    .apply(
                        instance,
                        (walkTarget, home) -> (level, body, timestamp) -> {
                                if (level.getGameTime() - lastUpdate.longValue() < 20L) {
                                    return false;
                                } else {
                                    PoiManager poiManager = level.getPoiManager();
                                    Optional<BlockPos> closest = poiManager.findClosest(holder -> holder.is(PoiTypes.HOME), body.blockPosition(), CONFIG.villagerPathfindingConfig.findPOIRange, PoiManager.Occupancy.ANY);
                                    // Use Manhattan distance instead of squared distance
                                    if (!closest.isEmpty() && !(closest.get().distManhattan(body.blockPosition()) <= 2)) {
                                        MutableInt triedCount = new MutableInt(0);
                                        lastUpdate.setValue(level.getGameTime() + level.getRandom().nextInt(20));
                                        Predicate<BlockPos> cacheTest = pos -> {
                                            long key = pos.asLong();
                                            if (batchCache.containsKey(key)) {
                                                return false;
                                            } else if (triedCount.incrementAndGet() >= 5) {
                                                return false;
                                            } else {
                                                batchCache.put(key, lastUpdate.longValue() + 40L);
                                                return true;
                                            }
                                        };

                                        // Modified predicate to filter occupied beds
                                        Predicate<BlockPos> bedFilterPredicate = pos -> {
                                            if (isBedOccupied(level, pos)) {
                                                return false;
                                            }
                                            return cacheTest.test(pos);
                                        };

                                        Set<Pair<Holder<PoiType>, BlockPos>> pois = poiManager.findAllWithType(
                                                holder -> holder.is(PoiTypes.HOME), bedFilterPredicate, body.blockPosition(),
                                                CONFIG.villagerPathfindingConfig.findPOIRange, PoiManager.Occupancy.HAS_SPACE
                                            )
                                            .collect(Collectors.toSet());

                                        // If all beds are occupied, fall back to default behavior
                                        if (pois.isEmpty()) {
                                            pois = poiManager.findAllWithType(
                                                    holder -> holder.is(PoiTypes.HOME), cacheTest, body.blockPosition(),
                                                    CONFIG.villagerPathfindingConfig.findPOIRange, PoiManager.Occupancy.ANY
                                                )
                                                .collect(Collectors.toSet());
                                        }

                                        Path path = AcquirePoi.findPathToPois(body, pois);
                                        if (path != null && path.canReach()) {
                                            BlockPos targetPos = path.getTarget();
                                            Optional<Holder<PoiType>> type = poiManager.getType(targetPos);
                                            if (type.isPresent()) {
                                                walkTarget.set(new WalkTarget(targetPos, speedModifier, 1));
                                                level.debugSynchronizers().updatePoi(targetPos);
                                            }
                                        } else if (triedCount.intValue() < 5) {
                                            batchCache.long2LongEntrySet().removeIf(entry -> entry.getLongValue() < lastUpdate.longValue());
                                        }

                                        return true;
                                    } else {
                                        return false;
                                    }
                                }
                            }
                    )
        );
    }

    private static boolean isBedOccupied(ServerLevel level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        return blockState.is(BlockTags.BEDS) && blockState.getValue(BedBlock.OCCUPIED);
    }
}