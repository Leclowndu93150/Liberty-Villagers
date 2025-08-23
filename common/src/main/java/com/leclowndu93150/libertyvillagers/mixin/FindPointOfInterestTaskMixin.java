package com.leclowndu93150.libertyvillagers.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.apache.commons.lang3.mutable.MutableLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(AcquirePoi.class)
public abstract class FindPointOfInterestTaskMixin {

    private static final long TICKS_PER_DAY = 24000;
    private static final long TIME_NIGHT = 13000;
    static private ServerLevel lastUsedWorld;

    // Calling into the lambda for Task.trigger.
    @SuppressWarnings("target")
    @Inject(method = "lambda$create$6(ZLorg/apache/commons/lang3/mutable/MutableLong;Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;Ljava/util/function/Predicate;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;Ljava/util/Optional;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)Z",
           at = @At(value = "HEAD"), cancellable = true)
    static private void dontFindWorkstationsAtNight(boolean onlyRunIfChild, MutableLong mutableLong,
                                                    Long2ObjectMap objectMap,
                                                    Predicate predicate, MemoryAccessor result, Optional optional,
                                                    ServerLevel serverWorld, PathfinderMob entity, long time,
                                                    CallbackInfoReturnable<Boolean> cir) {
        lastUsedWorld = serverWorld;
        long timeOfDay = serverWorld.getDayTime() % TICKS_PER_DAY;
        // Let villagers still find beds at night.
        MemoryQueryResultAccessorMixin accessorMixin = (MemoryQueryResultAccessorMixin) ((Object) result);
        if (accessorMixin.getMemory() == MemoryModuleType.HOME) {
            return;
        }
        if (CONFIG.villagersGeneralConfig.villagersDontLookForWorkstationsAtNight &&
                entity.getType() == EntityType.VILLAGER && timeOfDay > TIME_NIGHT) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @SuppressWarnings("target")
    @ModifyArgs(method = "lambda$create$6(ZLorg/apache/commons/lang3/mutable/MutableLong;Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;Ljava/util/function/Predicate;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;Ljava/util/Optional;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;" +
                    "findAllClosestFirstWithType(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)Ljava/util/stream/Stream;"))
    static private void filterForOccupiedBedsAndIncreasePOIRange(Args args) {
        Predicate<BlockPos> oldPredicate = args.get(1);
        Predicate<BlockPos> newPredicate = (blockPos -> {
            if (isBedOccupied(blockPos)) {
                return false;
            }
            return oldPredicate.test(blockPos);
        });
        args.set(1, newPredicate);

        int radius = args.get(3);
        args.set(3, Math.max(radius, CONFIG.villagerPathfindingConfig.findPOIRange));
    }

    private static boolean isBedOccupied(BlockPos pos) {
        BlockState blockState = lastUsedWorld.getBlockState(pos);
        return blockState.is(BlockTags.BEDS) && blockState.getValue(BedBlock.OCCUPIED);
    }

    @ModifyArgs(method = "findPathToPois(Lnet/minecraft/world/entity/Mob;Ljava/util/Set;)Lnet/minecraft/world/level/pathfinder/Path;", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;createPath(Ljava/util/Set;I)" +
                    "Lnet/minecraft/world/level/pathfinder/Path;"))
    static private void increaseMinimumPOIClaimDistance(Args args) {
        int distance = args.get(1);
        args.set(1, Math.max(distance, CONFIG.villagerPathfindingConfig.minimumPOISearchDistance));
    }
}
