package com.leclowndu93150.libertyvillagers.mixin;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import org.apache.commons.lang3.mutable.MutableLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.SetClosestHomeAsWalkTarget;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(value = SetClosestHomeAsWalkTarget.class)
public abstract class WalkHomeTaskMixin {

    private static ServerLevel world;

    /* Prevents villagers from getting confused about a door directly below their bed. */
    @SuppressWarnings("target")
    @Redirect(method = "lambda$create$4(Lorg/apache/commons/lang3/mutable/MutableLong;Lit/unimi/dsi/fastutil/longs/Long2LongMap;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;FLnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;distSqr(Lnet/minecraft/core/Vec3i;)D"))
    private static double replaceSquaredDistanceWithManhattan(BlockPos origin, Vec3i dest) {
        return origin.distManhattan(dest);
    }

    @ModifyConstant(method = "lambda$create$4(Lorg/apache/commons/lang3/mutable/MutableLong;Lit/unimi/dsi/fastutil/longs/Long2LongMap;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;FLnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)Z",
            constant = @Constant(doubleValue = 4.0))
    private static double replaceSquaredDistanceWithManhattanConstant(double constant) {
        return 2.0f;
    }

    @SuppressWarnings("target")
    @ModifyArgs(method = "lambda$create$4(Lorg/apache/commons/lang3/mutable/MutableLong;Lit/unimi/dsi/fastutil/longs/Long2LongMap;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;FLnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;findClosest(Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)Ljava/util/Optional;"))
    private static void modifyShouldRunGetNearestPositionArgs(Args args) {
        args.set(2, CONFIG.villagerPathfindingConfig.findPOIRange);
    }

    @SuppressWarnings("target")
    @Inject(method = "lambda$create$4(Lorg/apache/commons/lang3/mutable/MutableLong;Lit/unimi/dsi/fastutil/longs/Long2LongMap;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;FLnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)Z",
            at = @At("HEAD"))
    private static void runHead(MutableLong mutableLong, Long2LongMap map, MemoryAccessor result, float speed,
                           ServerLevel serverWorld,
                           PathfinderMob entity,
                           long time, CallbackInfoReturnable<Boolean> cir) {
        world = serverWorld;
    }

    @Redirect(method = "lambda$create$4(Lorg/apache/commons/lang3/mutable/MutableLong;Lit/unimi/dsi/fastutil/longs/Long2LongMap;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;FLnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;findAllWithType(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)Ljava/util/stream/Stream;"))
    private static Stream<Pair<Holder<PoiType>, BlockPos>> modifyGetTypesAndPositions(
            PoiManager pointOfInterestStorage, Predicate<Holder<PoiType>> typePredicate,
            Predicate<BlockPos> posPredicate, BlockPos pos, int radius,
            PoiManager.Occupancy occupationStatus) {
        Predicate<BlockPos> newBlockPosPredicate = blockPos -> {
            if (isBedOccupied(world, blockPos)) {
                return false;
            }
            return posPredicate.test(blockPos);
        };

        Stream<Pair<Holder<PoiType>, BlockPos>> stream =
                pointOfInterestStorage.findAllClosestFirstWithType(typePredicate, newBlockPosPredicate, pos,
                        CONFIG.villagerPathfindingConfig.findPOIRange, PoiManager.Occupancy.HAS_SPACE);
        Set<Pair<Holder<PoiType>, BlockPos>> set = stream.collect(Collectors.toSet());

        if (!set.isEmpty()) {
            return set.stream();
        }

        // All beds are occupied, go back to default behavior of meeping around the nearest bed at night, worst
        // roommate ever.
        return pointOfInterestStorage.findAllClosestFirstWithType(typePredicate, posPredicate, pos,
                CONFIG.villagerPathfindingConfig.findPOIRange, occupationStatus);
    }

    private static boolean isBedOccupied(ServerLevel world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return blockState.is(BlockTags.BEDS) && blockState.getValue(BedBlock.OCCUPIED);
    }
}
