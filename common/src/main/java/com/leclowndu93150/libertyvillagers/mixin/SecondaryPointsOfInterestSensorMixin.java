package com.leclowndu93150.libertyvillagers.mixin;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.sensing.SecondaryPoiSensor;

@Mixin(SecondaryPoiSensor.class)
public class SecondaryPointsOfInterestSensorMixin {

    private Villager villagerEntity;

    @Inject(method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;)V",
            at = @At("HEAD"))
    protected void getLocalVariablesFromSense(ServerLevel serverWorld, Villager villagerEntity, CallbackInfo ci) {
        this.villagerEntity = villagerEntity;
    }

    @ModifyConstant(
            method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;)V",
            constant = @Constant(intValue = 4))
    private int replacePosXZ(int xz) {
        if (villagerEntity.getVillagerData().profession().is(VillagerProfession.FARMER)) {
            return CONFIG.villagersProfessionConfig.findCropRangeHorizontal;
        }
        if (villagerEntity.getVillagerData().profession().is(VillagerProfession.FISHERMAN)) {
            return CONFIG.villagersProfessionConfig.fishermanFindWaterRange;
        }
        return xz;
    }

    @ModifyConstant(
            method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;)V",
            constant = @Constant(intValue = -4))
    private int replaceNegXZ(int xz) {
        if (villagerEntity.getVillagerData().profession().is(VillagerProfession.FARMER)) {
            return -1 * CONFIG.villagersProfessionConfig.findCropRangeHorizontal;
        }
        if (villagerEntity.getVillagerData().profession().is(VillagerProfession.FISHERMAN)) {
            return -1 * CONFIG.villagersProfessionConfig.fishermanFindWaterRange;
        }
        return xz;
    }

    @ModifyConstant(
            method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;)V",
            constant = @Constant(intValue = 2))
    private int replacePosY(int y) {
        if (villagerEntity.getVillagerData().profession().is(VillagerProfession.FARMER)) {
            return CONFIG.villagersProfessionConfig.findCropRangeVertical;
        }
        return y;
    }

    @ModifyConstant(
            method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;)V",
            constant = @Constant(intValue = -2))
    private int replaceNegY(int y) {
        if (villagerEntity.getVillagerData().profession().is(VillagerProfession.FARMER)) {
            return -1 * CONFIG.villagersProfessionConfig.findCropRangeVertical;
        }
        return y;
    }
}
