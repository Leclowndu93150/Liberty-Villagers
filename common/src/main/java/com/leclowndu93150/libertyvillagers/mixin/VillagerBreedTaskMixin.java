package com.leclowndu93150.libertyvillagers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.ai.behavior.VillagerMakeLove;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(VillagerMakeLove.class)
public abstract class VillagerBreedTaskMixin {

    @Shadow
    abstract boolean canReach(Villager villager, BlockPos pos, Holder<PoiType> poiType);

    @Inject(method = "tryToGiveBirth(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/Villager;Lnet/minecraft/world/entity/npc/Villager;)V",
            at = @At("HEAD"), cancellable = true)
    private void goHome(ServerLevel world, Villager first, Villager second, CallbackInfo ci) {
        if (CONFIG.villagersGeneralConfig.villagerBabiesRequireWorkstationAndBed) {
            Optional<BlockPos> optionalWorkstation = world.getPoiManager()
                    .take(VillagerProfession.NONE.acquirableJobSite(),
                            (poiType, pos) -> this.canReach(first, pos, poiType), first.blockPosition(),
                            CONFIG.villagerPathfindingConfig.findPOIRange);
            if (optionalWorkstation.isEmpty()) {
                world.broadcastEntityEvent(second, EntityEvent.VILLAGER_ANGRY);
                world.broadcastEntityEvent(first, EntityEvent.VILLAGER_ANGRY);
                ci.cancel();
            }
        }
    }

    @ModifyConstant(
            method = "takeVacantBed(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/Villager;)Ljava/util/Optional;",
            constant = @Constant(intValue = 48))
    private int replaceGetReachableHomeDistance(int value) {
        return CONFIG.villagerPathfindingConfig.findPOIRange;
    }


    @ModifyConstant(
            method = "breed",
            constant = @Constant(intValue = -24000))
    private int replaceBreedingAgeForBaby(int breedingAge) {
        return -1 * CONFIG.villagersGeneralConfig.growUpTime;
    }
}