package com.leclowndu93150.libertyvillagers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

import net.minecraft.world.entity.ai.behavior.SleepInBed;

@Mixin(SleepInBed.class)
public class SleepTaskMixin {

    @ModifyConstant(
            method = "checkExtraStartConditions",
            constant = @Constant(doubleValue = 2.0))
    private double replaceGetReachableBedDistance(double value) {
        return CONFIG.villagerPathfindingConfig.minimumPOISearchDistance + 1;
    }
}
