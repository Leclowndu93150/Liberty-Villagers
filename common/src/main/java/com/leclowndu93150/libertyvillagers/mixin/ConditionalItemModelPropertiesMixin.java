package com.leclowndu93150.libertyvillagers.mixin;

import com.leclowndu93150.libertyvillagers.util.VillagerHoldingProperty;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConditionalItemModelProperties.class)
public class ConditionalItemModelPropertiesMixin {

    @Shadow
    private static ExtraCodecs.LateBoundIdMapper ID_MAPPER;

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void addVillagerHoldingProperty(CallbackInfo ci) {
        ID_MAPPER.put(Identifier.fromNamespaceAndPath("libertyvillagers", "villager_holding"), VillagerHoldingProperty.MAP_CODEC);
    }
}