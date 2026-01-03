package com.leclowndu93150.libertyvillagers.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.CatVariant;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(Cat.class)
public abstract class CatEntityMixin extends TamableAnimal {

    @Shadow
    public abstract void setVariant(Holder<CatVariant> variant);

    public CatEntityMixin(EntityType<? extends Cat> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "finalizeSpawn",
            at = @At("RETURN"))
    void addPersistentToInitialize(ServerLevelAccessor level, DifficultyInstance difficulty,
                                   EntitySpawnReason spawnReason, SpawnGroupData groupData,
                                   CallbackInfoReturnable<SpawnGroupData> cir) {
        if (CONFIG.catsConfig.villageCatsDontDespawn) {
            this.setPersistenceRequired();
        }

        if (CONFIG.catsConfig.allBlackCats) {
            Registry<CatVariant> catVariantRegistry = level.registryAccess().lookupOrThrow(Registries.CAT_VARIANT);
            Identifier blackCatId = Identifier.withDefaultNamespace("all_black");
            catVariantRegistry.get(blackCatId).ifPresent(this::setVariant);
        }
    }

    @Inject(method = "finalizeSpawn",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"))
    private void injectBlackCat(ServerLevelAccessor level, DifficultyInstance difficulty,
                                EntitySpawnReason spawnReason, SpawnGroupData groupData,
                                CallbackInfoReturnable<SpawnGroupData> cir) {
        if (CONFIG.catsConfig.blackCatsAtAnyTime) {
            Registry<CatVariant> catVariantRegistry = level.registryAccess().lookupOrThrow(Registries.CAT_VARIANT);
            Identifier blackCatId = Identifier.withDefaultNamespace("all_black");
            catVariantRegistry.get(blackCatId).ifPresent(this::setVariant);
        }
    }
}
