package com.leclowndu93150.libertyvillagers.mixin;

import com.leclowndu93150.libertyvillagers.util.JitteredLinearRetry;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.commons.lang3.mutable.MutableLong;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(AcquirePoi.class)
public abstract class FindPointOfInterestTaskMixin {

    private static final long TICKS_PER_DAY = 24000;
    private static final long TIME_NIGHT = 13000;

    @Shadow
    public static BehaviorControl<PathfinderMob> create(
        Predicate<Holder<PoiType>> poiType, MemoryModuleType<GlobalPos> memoryToAcquire, boolean onlyIfAdult, Optional<Byte> onPoiAcquisitionEvent
    ) {
        throw new UnsupportedOperationException();
    }

    @Overwrite
    public static BehaviorControl<PathfinderMob> create(
        Predicate<Holder<PoiType>> poiType,
        MemoryModuleType<GlobalPos> memoryToValidate,
        MemoryModuleType<GlobalPos> memoryToAcquire,
        boolean onlyIfAdult,
        Optional<Byte> onPoiAcquisitionEvent,
        BiPredicate<ServerLevel, BlockPos> validPoi
    ) {
        int batchSize = 5;
        int rate = 20;
        MutableLong nextScheduledStart = new MutableLong(0L);
        Long2ObjectMap<JitteredLinearRetry> batchCache = new Long2ObjectOpenHashMap<>();
        OneShot<PathfinderMob> acquirePoi = BehaviorBuilder.create(
            i -> i.group(i.absent(memoryToAcquire))
                .apply(
                    i,
                    toAcquire -> (level, body, timestamp) -> {
                        // Custom: Don't find workstations at night for villagers (except beds)
                        if (CONFIG.villagersGeneralConfig.villagersDontLookForWorkstationsAtNight &&
                                body.getType() == EntityType.VILLAGER &&
                                memoryToAcquire != MemoryModuleType.HOME) {
                            long timeOfDay = level.getDayTime() % TICKS_PER_DAY;
                            if (timeOfDay > TIME_NIGHT) {
                                return false;
                            }
                        }

                        if (onlyIfAdult && body.isBaby()) {
                            return false;
                        } else if (nextScheduledStart.longValue() == 0L) {
                            nextScheduledStart.setValue(level.getGameTime() + level.random.nextInt(20));
                            return false;
                        } else if (level.getGameTime() < nextScheduledStart.longValue()) {
                            return false;
                        } else {
                            nextScheduledStart.setValue(timestamp + 20L + level.getRandom().nextInt(20));
                            PoiManager poiManager = level.getPoiManager();
                            batchCache.long2ObjectEntrySet().removeIf(entry -> !entry.getValue().isStillValid(timestamp));
                            Predicate<BlockPos> cacheTest = pos -> {
                                // Custom: Filter out occupied beds
                                if (isBedOccupied(level, pos)) {
                                    return false;
                                }

                                JitteredLinearRetry retryMarker = batchCache.get(pos.asLong());
                                if (retryMarker == null) {
                                    return true;
                                } else if (!retryMarker.shouldRetry(timestamp)) {
                                    return false;
                                } else {
                                    retryMarker.markAttempt(timestamp);
                                    return true;
                                }
                            };

                            // Custom: Use configurable POI range
                            int poiRange = Math.max(48, CONFIG.villagerPathfindingConfig.findPOIRange);

                            Set<Pair<Holder<PoiType>, BlockPos>> poiPositions = poiManager.findAllClosestFirstWithType(
                                    poiType, cacheTest, body.blockPosition(), poiRange, PoiManager.Occupancy.HAS_SPACE
                                )
                                .limit(5L)
                                .filter(px -> validPoi.test(level, px.getSecond()))
                                .collect(Collectors.toSet());
                            Path path = findPathToPois(body, poiPositions);
                            if (path != null && path.canReach()) {
                                BlockPos targetPos = path.getTarget();
                                poiManager.getType(targetPos).ifPresent(type -> {
                                    poiManager.take(poiType, (t, poiPos) -> poiPos.equals(targetPos), targetPos, 1);
                                    toAcquire.set(GlobalPos.of(level.dimension(), targetPos));
                                    onPoiAcquisitionEvent.ifPresent(event -> level.broadcastEntityEvent(body, event));
                                    batchCache.clear();
                                    level.debugSynchronizers().updatePoi(targetPos);
                                });
                            } else {
                                for (Pair<Holder<PoiType>, BlockPos> p : poiPositions) {
                                    batchCache.computeIfAbsent(
                                        p.getSecond().asLong(),
                                        (Long2ObjectFunction<? extends JitteredLinearRetry>)(key -> new JitteredLinearRetry(level.random, timestamp))
                                    );
                                }
                            }

                            return true;
                        }
                    }
                )
        );
        return memoryToAcquire == memoryToValidate
            ? acquirePoi
            : BehaviorBuilder.create(i -> i.group(i.absent(memoryToValidate)).apply(i, toValidate -> acquirePoi));
    }

    @Overwrite
    public static @Nullable Path findPathToPois(Mob body, Set<Pair<Holder<PoiType>, BlockPos>> pois) {
        if (pois.isEmpty()) {
            return null;
        } else {
            Set<BlockPos> targets = new HashSet<>();
            int maxRange = 1;

            for (Pair<Holder<PoiType>, BlockPos> p : pois) {
                maxRange = Math.max(maxRange, p.getFirst().value().validRange());
                targets.add(p.getSecond());
            }

            // Custom: Use configurable minimum POI search distance
            maxRange = Math.max(maxRange, CONFIG.villagerPathfindingConfig.minimumPOISearchDistance);

            return body.getNavigation().createPath(targets, maxRange);
        }
    }

    private static boolean isBedOccupied(ServerLevel world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return blockState.is(BlockTags.BEDS) && blockState.getValue(BedBlock.OCCUPIED);
    }
}
