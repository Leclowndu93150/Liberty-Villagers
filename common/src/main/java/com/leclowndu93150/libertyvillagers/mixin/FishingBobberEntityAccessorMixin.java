package com.leclowndu93150.libertyvillagers.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FishingHook.class)
public interface FishingBobberEntityAccessorMixin {
    @Accessor("DATA_BITING")
    static EntityDataAccessor<Boolean> getCaughtFish() {
        throw new AssertionError();
    }
}
