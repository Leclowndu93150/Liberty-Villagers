package com.leclowndu93150.libertyvillagers.util;

import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.world.phys.Vec3;

public class VillagerFishingRenderState extends FishingHookRenderState {
    public boolean isVillagerFishing;
    public Vec3 villagerHandOffset = Vec3.ZERO;
}