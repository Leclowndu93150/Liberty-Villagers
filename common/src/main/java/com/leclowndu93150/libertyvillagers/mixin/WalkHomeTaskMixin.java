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
import net.minecraft.network.protocol.game.DebugPackets;
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

    /**
     * @author LibertyVillagers
     * @reason Replace squared distance with Manhattan distance, modify POI range, and filter occupied beds
     */
    @Overwrite
    public static BehaviorControl<PathfinderMob> create(float speedModifier) {
        Long2LongMap long2LongMap = new Long2LongOpenHashMap();
        MutableLong mutableLong = new MutableLong(0L);
        return BehaviorBuilder.create(
            instance -> instance.group(instance.absent(MemoryModuleType.WALK_TARGET), instance.absent(MemoryModuleType.HOME))
                    .apply(
                        instance,
                        (memoryAccessor, memoryAccessor2) -> (serverLevel, pathfinderMob, l) -> {
                                if (serverLevel.getGameTime() - mutableLong.getValue() < 20L) {
                                    return false;
                                } else {
                                    PoiManager poiManager = serverLevel.getPoiManager();
                                    Optional<BlockPos> optional = poiManager.findClosest(holder -> holder.is(PoiTypes.HOME), pathfinderMob.blockPosition(), CONFIG.villagerPathfindingConfig.findPOIRange, PoiManager.Occupancy.ANY);
                                    // Use Manhattan distance instead of squared distance
                                    if (!optional.isEmpty() && !(((BlockPos)optional.get()).distManhattan(pathfinderMob.blockPosition()) <= 2)) {
                                        MutableInt mutableInt = new MutableInt(0);
                                        mutableLong.setValue(serverLevel.getGameTime() + (long)serverLevel.getRandom().nextInt(20));
                                        Predicate<BlockPos> predicate = blockPosx -> {
                                            long lx = blockPosx.asLong();
                                            if (long2LongMap.containsKey(lx)) {
                                                return false;
                                            } else if (mutableInt.incrementAndGet() >= 5) {
                                                return false;
                                            } else {
                                                long2LongMap.put(lx, mutableLong.getValue() + 40L);
                                                return true;
                                            }
                                        };
                                        
                                        // Modified predicate to filter occupied beds
                                        Predicate<BlockPos> newBlockPosPredicate = blockPos -> {
                                            if (isBedOccupied(serverLevel, blockPos)) {
                                                return false;
                                            }
                                            return predicate.test(blockPos);
                                        };
                                        
                                        Set<Pair<Holder<PoiType>, BlockPos>> set = (Set<Pair<Holder<PoiType>, BlockPos>>)poiManager.findAllClosestFirstWithType(
                                                holder -> holder.is(PoiTypes.HOME), newBlockPosPredicate, pathfinderMob.blockPosition(), 
                                                CONFIG.villagerPathfindingConfig.findPOIRange, PoiManager.Occupancy.HAS_SPACE
                                            )
                                            .collect(Collectors.toSet());
                                        
                                        // If all beds are occupied, fall back to default behavior
                                        if (set.isEmpty()) {
                                            set = (Set<Pair<Holder<PoiType>, BlockPos>>)poiManager.findAllClosestFirstWithType(
                                                    holder -> holder.is(PoiTypes.HOME), predicate, pathfinderMob.blockPosition(),
                                                    CONFIG.villagerPathfindingConfig.findPOIRange, PoiManager.Occupancy.ANY
                                                )
                                                .collect(Collectors.toSet());
                                        }
                                        
                                        Path path = AcquirePoi.findPathToPois(pathfinderMob, set);
                                        if (path != null && path.canReach()) {
                                            BlockPos blockPos = path.getTarget();
                                            Optional<Holder<PoiType>> optional2 = poiManager.getType(blockPos);
                                            if (optional2.isPresent()) {
                                                memoryAccessor.set(new WalkTarget(blockPos, speedModifier, 1));
                                                DebugPackets.sendPoiTicketCountPacket(serverLevel, blockPos);
                                            }
                                        } else if (mutableInt.getValue() < 5) {
                                            long2LongMap.long2LongEntrySet().removeIf(entry -> entry.getLongValue() < mutableLong.getValue());
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
    
    private static boolean isBedOccupied(ServerLevel world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return blockState.is(BlockTags.BEDS) && blockState.getValue(BedBlock.OCCUPIED);
    }
}