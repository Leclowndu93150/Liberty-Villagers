package com.leclowndu93150.libertyvillagers.mixin;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.ValidateNearbyPoi;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ValidateNearbyPoi.class)
public abstract class ForgetCompletedPointOfInterestTaskMixin  {

    @Shadow
    private static boolean bedIsOccupied(ServerLevel level, BlockPos pos, LivingEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Overwrite
    public static BehaviorControl<LivingEntity> create(Predicate<Holder<PoiType>> poiValidator, MemoryModuleType<GlobalPos> poiPosMemory) {
        return BehaviorBuilder.create(
            instance -> instance.group(instance.present(poiPosMemory)).apply(instance, memoryAccessor -> (serverLevel, livingEntity, l) -> {
                        GlobalPos globalPos = (GlobalPos)instance.get(memoryAccessor);
                        BlockPos blockPos = globalPos.pos();
                        // Use Manhattan distance instead of Euclidean distance
                        if (serverLevel.dimension() == globalPos.dimension() && blockPos.distManhattan(livingEntity.blockPosition()) < 4) {
                            ServerLevel serverLevel2 = serverLevel.getServer().getLevel(globalPos.dimension());
                            if (serverLevel2 == null || !serverLevel2.getPoiManager().exists(blockPos, poiValidator)) {
                                memoryAccessor.erase();
                            } else if (bedIsOccupied(serverLevel2, blockPos, livingEntity)) {
                                memoryAccessor.erase();
                                serverLevel.getPoiManager().release(blockPos);
                                DebugPackets.sendPoiTicketCountPacket(serverLevel, blockPos);
                            }

                            return true;
                        } else {
                            return false;
                        }
                    })
        );
    }
}

