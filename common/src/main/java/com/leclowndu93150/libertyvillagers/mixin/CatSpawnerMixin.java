package com.leclowndu93150.libertyvillagers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

import net.minecraft.world.entity.npc.CatSpawner;

@Mixin(CatSpawner.class)
public class CatSpawnerMixin {

    @ModifyConstant(
            method = "spawnInVillage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)I",
            constant = @Constant(doubleValue = 48.0))
    private double replaceCatSpawnDistanceXZ(double value) {
        if (CONFIG.catsConfig.catsSpawnLimit) {
            return CONFIG.catsConfig.catsSpawnLimitRange;
        }
        return value;
    }

    @ModifyConstant(
            method = "spawnInVillage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)I",
            constant = @Constant(doubleValue = 8.0))
    private double replaceCatSpawnDistanceY(double value) {
        if (CONFIG.catsConfig.catsSpawnLimit) {
            return CONFIG.catsConfig.catsSpawnLimitRange;
        }
        return value;
    }

    @ModifyConstant(
            method = "spawnInVillage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)I",
            constant = @Constant(intValue = 5))
    private int replaceCatSpawnLimitCount(int value) {
        if (CONFIG.catsConfig.catsSpawnLimit) {
            return CONFIG.catsConfig.catsSpawnLimitCount;
        }
        return value;
    }
}
