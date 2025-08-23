package com.leclowndu93150.libertyvillagers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

@Mixin(Mob.class)
public abstract class MobEntityMixin extends LivingEntity {

    public MobEntityMixin(EntityType<? extends Mob> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "getMaxFallDistance", at = @At(value = "HEAD"), cancellable = true)
    public void replaceGetSafeFallDistance(CallbackInfoReturnable<Integer> cir) {
        LivingEntity entity = this;
        if (entity.getType() == EntityType.VILLAGER) {
            cir.setReturnValue(CONFIG.villagerPathfindingConfig.villagerSafeFallDistance);
            cir.cancel();
        }
    }
}