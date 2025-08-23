package com.leclowndu93150.libertyvillagers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "onClimbable", at = @At(value = "HEAD"), cancellable = true)
    public void replaceIsClimbing(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity == null) return;
        if (entity.getType() == EntityType.VILLAGER && CONFIG.villagerPathfindingConfig.villagersDontClimb) {
            cir.setReturnValue(false);
            cir.cancel();
        }
        if (entity.getType() == EntityType.IRON_GOLEM && CONFIG.golemsConfig.golemsDontClimb) {
            cir.setReturnValue(false);
            cir.cancel();
        }
        if (entity.getType() == EntityType.CAT && CONFIG.catsConfig.catsDontClimb) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "getAttributeValue", at = @At(value = "HEAD"), cancellable = true)
        public void replaceAttributeValueForVillagersAndGolems(Holder<Attribute> attribute,
                                                         CallbackInfoReturnable<Double> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity == null) return;
        if ((entity.getType() == EntityType.VILLAGER || entity.getType() == EntityType.IRON_GOLEM) &&
                attribute == Attributes.FOLLOW_RANGE) {
            cir.setReturnValue((double) CONFIG.villagerPathfindingConfig.findPOIRange);
            cir.cancel();
        }
    }
}