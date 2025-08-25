package com.leclowndu93150.libertyvillagers.mixin;

import net.minecraft.world.entity.EntitySpawnReason;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

@Mixin(Cat.class)
public abstract class CatEntityMixin extends TamableAnimal {

    @Shadow
    public abstract void setVariant(Holder<CatVariant> registryEntry);

    public CatEntityMixin(EntityType<? extends Cat> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "finalizeSpawn",
            at = @At("RETURN"))
    void addPersistantToInitialize(ServerLevelAccessor p_28134_, DifficultyInstance p_28135_, EntitySpawnReason p_362361_, SpawnGroupData p_28137_, CallbackInfoReturnable<SpawnGroupData> cir) {
        if (CONFIG.catsConfig.villageCatsDontDespawn) {
            this.setPersistenceRequired();
        }

        if (CONFIG.catsConfig.allBlackCats) {
            BuiltInRegistries.CAT_VARIANT.get(CatVariant.ALL_BLACK).ifPresent(this::setVariant);
        }
    }

    @Redirect(method = "finalizeSpawn",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/ServerLevelAccessor;getMoonBrightness()F"))
    private float replaceMoonSize(ServerLevelAccessor world) {
        if (CONFIG.catsConfig.blackCatsAtAnyTime) {
            return 1.0f;
        }

        return world.getMoonBrightness();
    }
}