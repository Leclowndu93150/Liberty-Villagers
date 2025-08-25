package com.leclowndu93150.libertyvillagers.mixin;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.YieldJobSite;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(YieldJobSite.class)
public class TakeJobSiteTaskMixin {

    @Shadow
    private static boolean nearbyWantsJobsite(Holder<PoiType> pPoi, Villager pVillager, BlockPos pPos) {
        throw new UnsupportedOperationException();
    }

    @Shadow
    private static boolean canReachPos(PathfinderMob pMob, BlockPos pPos, PoiType pPoi) {
        throw new UnsupportedOperationException();
    }

    @Overwrite
    public static BehaviorControl<Villager> create(float pSpeedModifier) {
        return BehaviorBuilder.create((p_258916_) -> p_258916_.group(p_258916_.present(MemoryModuleType.POTENTIAL_JOB_SITE), p_258916_.absent(MemoryModuleType.JOB_SITE), p_258916_.present(MemoryModuleType.NEAREST_LIVING_ENTITIES), p_258916_.registered(MemoryModuleType.WALK_TARGET), p_258916_.registered(MemoryModuleType.LOOK_TARGET)).apply(p_258916_, (p_258901_, p_258902_, p_258903_, p_258904_, p_258905_) -> (p_258912_, p_258913_, p_258914_) -> {
            if (p_258913_.isBaby()) {
                return false;
            } else if (p_258913_.getVillagerData().getProfession() != VillagerProfession.NONE) {
                return false;
            } else {
                BlockPos blockpos = ((GlobalPos)p_258916_.get(p_258901_)).pos();
                Optional<Holder<PoiType>> optional = p_258912_.getPoiManager().getType(blockpos);
                if (optional.isEmpty()) {
                    return true;
                } else {
                    ((List<LivingEntity>)p_258916_.get(p_258903_)).stream().filter((p_258898_) -> p_258898_ instanceof Villager && p_258898_ != p_258913_).map((p_258896_) -> (Villager)p_258896_).filter((p_258919_) -> p_258919_.isAlive()).filter((p_258919_) -> nearbyWantsJobsite((Holder)optional.get(), p_258919_, blockpos)).findFirst().ifPresent((p_340764_) -> {
                        p_258904_.erase();
                        p_258905_.erase();
                        p_258901_.erase();
                        Villager villager = (Villager) p_340764_;
                        if (villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty()) {
                            int completionRange = Math.max(1, CONFIG.villagerPathfindingConfig.minimumPOISearchDistance + 1);
                            BehaviorUtils.setWalkAndLookTargetMemories(villager, blockpos, pSpeedModifier, completionRange);
                            villager.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.of(p_258912_.dimension(), blockpos));
                            DebugPackets.sendPoiTicketCountPacket(p_258912_, blockpos);
                        }
                    });
                    return true;
                }
            }
        }));
    }
}
