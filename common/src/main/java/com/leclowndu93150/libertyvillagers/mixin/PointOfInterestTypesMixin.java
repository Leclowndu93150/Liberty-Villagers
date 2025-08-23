package com.leclowndu93150.libertyvillagers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

import net.minecraft.world.entity.ai.village.poi.PoiTypes;

@Mixin(PoiTypes.class)
public class PointOfInterestTypesMixin {

    @ModifyArg(method = "bootstrap(Lnet/minecraft/core/Registry;)Lnet/minecraft/world/entity/ai/village/poi/PoiType;",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/village/poi/PoiTypes;register(Lnet/minecraft/core/Registry;Lnet/minecraft/resources/ResourceKey;Ljava/util/Set;II)Lnet/minecraft/world/entity/ai/village/poi/PoiType;"),
            index = 4)
    private static int modifySearchDistance(int searchDistance) {
        return Math.max(searchDistance, CONFIG.villagerPathfindingConfig.minimumPOISearchDistance);
    }
}