package com.leclowndu93150.libertyvillagers.mixin;

import com.leclowndu93150.libertyvillagers.util.JitteredLinearRetry;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
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
import org.jetbrains.annotations.Nullable;
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
        Predicate<Holder<PoiType>> acquirablePois, MemoryModuleType<GlobalPos> acquiringMemory, boolean onlyIfAdult, Optional<Byte> entityEventId
    ) {
        throw new UnsupportedOperationException();
    }

    /**
     * @author LibertyVillagers
     * @reason Don't find workstations at night, filter occupied beds, increase POI range
     */
    @Overwrite
    public static BehaviorControl<PathfinderMob> create(
        Predicate<Holder<PoiType>> acquirablePois,
        MemoryModuleType<GlobalPos> existingAbsentMemory,
        MemoryModuleType<GlobalPos> acquiringMemory,
        boolean onlyIfAdult,
        Optional<Byte> entityEventId
    ) {
        int i = 5;
        int j = 20;
        MutableLong mutableLong = new MutableLong(0L);
        Long2ObjectMap<JitteredLinearRetry> long2ObjectMap = new Long2ObjectOpenHashMap<>();
        OneShot<PathfinderMob> oneShot = BehaviorBuilder.create(
            instance -> instance.group(instance.absent(acquiringMemory))
                    .apply(
                        instance,
                        memoryAccessor -> (serverLevel, pathfinderMob, l) -> {
                                // Don't find workstations at night for villagers (except beds)
                                if (CONFIG.villagersGeneralConfig.villagersDontLookForWorkstationsAtNight &&
                                        pathfinderMob.getType() == EntityType.VILLAGER && 
                                        acquiringMemory != MemoryModuleType.HOME) {
                                    long timeOfDay = serverLevel.getDayTime() % TICKS_PER_DAY;
                                    if (timeOfDay > TIME_NIGHT) {
                                        return false;
                                    }
                                }
                                
                                if (onlyIfAdult && pathfinderMob.isBaby()) {
                                    return false;
                                } else if (mutableLong.getValue() == 0L) {
                                    mutableLong.setValue(serverLevel.getGameTime() + (long)serverLevel.random.nextInt(20));
                                    return false;
                                } else if (serverLevel.getGameTime() < mutableLong.getValue()) {
                                    return false;
                                } else {
                                    mutableLong.setValue(l + 20L + (long)serverLevel.getRandom().nextInt(20));
                                    PoiManager poiManager = serverLevel.getPoiManager();
                                    long2ObjectMap.long2ObjectEntrySet().removeIf(entry -> !((JitteredLinearRetry)entry.getValue()).isStillValid(l));
                                    Predicate<BlockPos> predicate2 = blockPos -> {
                                        // Filter out occupied beds
                                        if (isBedOccupied(serverLevel, blockPos)) {
                                            return false;
                                        }
                                        
                                        JitteredLinearRetry jitteredLinearRetry = (JitteredLinearRetry)long2ObjectMap.get(blockPos.asLong());
                                        if (jitteredLinearRetry == null) {
                                            return true;
                                        } else if (!jitteredLinearRetry.shouldRetry(l)) {
                                            return false;
                                        } else {
                                            jitteredLinearRetry.markAttempt(l);
                                            return true;
                                        }
                                    };

                                    int poiRange = Math.max(48, CONFIG.villagerPathfindingConfig.findPOIRange);
                                    
                                    Set<Pair<Holder<PoiType>, BlockPos>> set = (Set<Pair<Holder<PoiType>, BlockPos>>)poiManager.findAllClosestFirstWithType(
                                            acquirablePois, predicate2, pathfinderMob.blockPosition(), poiRange, PoiManager.Occupancy.HAS_SPACE
                                        )
                                        .limit(5L)
                                        .collect(Collectors.toSet());
                                    Path path = findPathToPois(pathfinderMob, set);
                                    if (path != null && path.canReach()) {
                                        BlockPos blockPos = path.getTarget();
                                        poiManager.getType(blockPos).ifPresent(holder -> {
                                            poiManager.take(acquirablePois, (holderx, blockPos2) -> blockPos2.equals(blockPos), blockPos, 1);
                                            memoryAccessor.set(GlobalPos.of(serverLevel.dimension(), blockPos));
                                            entityEventId.ifPresent(byte_ -> serverLevel.broadcastEntityEvent(pathfinderMob, byte_));
                                            long2ObjectMap.clear();
                                            DebugPackets.sendPoiTicketCountPacket(serverLevel, blockPos);
                                        });
                                    } else {
                                        for (Pair<Holder<PoiType>, BlockPos> pair : set) {
                                            long2ObjectMap.computeIfAbsent(
                                                pair.getSecond().asLong(),
                                                (Long2ObjectFunction<? extends JitteredLinearRetry>)(m -> new JitteredLinearRetry(serverLevel.random, l))
                                            );
                                        }
                                    }

                                    return true;
                                }
                            }
                    )
        );
        return acquiringMemory == existingAbsentMemory
            ? oneShot
            : BehaviorBuilder.create(instance -> instance.group(instance.absent(existingAbsentMemory)).apply(instance, memoryAccessor -> oneShot));
    }

    /**
     * @author LibertyVillagers
     * @reason Increase minimum POI claim distance based on config
     */
    @Overwrite
    @Nullable
    public static Path findPathToPois(Mob mob, Set<Pair<Holder<PoiType>, BlockPos>> poiPositions) {
        if (poiPositions.isEmpty()) {
            return null;
        } else {
            Set<BlockPos> set = new HashSet();
            int i = 1;

            for (Pair<Holder<PoiType>, BlockPos> pair : poiPositions) {
                i = Math.max(i, ((Holder<PoiType>)pair.getFirst()).value().validRange());
                set.add((BlockPos)pair.getSecond());
            }

            i = Math.max(i, CONFIG.villagerPathfindingConfig.minimumPOISearchDistance);

            return mob.getNavigation().createPath(set, i);
        }
    }
    
    private static boolean isBedOccupied(ServerLevel world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return blockState.is(BlockTags.BEDS) && blockState.getValue(BedBlock.OCCUPIED);
    }
}