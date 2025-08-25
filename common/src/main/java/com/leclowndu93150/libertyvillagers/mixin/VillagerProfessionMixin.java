package com.leclowndu93150.libertyvillagers.mixin;

import com.google.common.collect.ImmutableSet;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(VillagerProfession.class)
public abstract class VillagerProfessionMixin {

    @Shadow
    private String name;

    @Inject(method = "secondaryPoi",
            at = @At("HEAD"),
            cancellable = true)
    void replaceSecondaryJobSites(CallbackInfoReturnable<ImmutableSet<Block>> cir) {
        switch (name) {
            case "librarian" -> {
                if (CONFIG.villagersProfessionConfig.librariansLookAtBooks) {
                    cir.setReturnValue(ImmutableSet.of(Blocks.BOOKSHELF));
                    cir.cancel();
                }
            }
            case "fisherman" -> {
                if (CONFIG.villagersProfessionConfig.fishermanFish) {
                    cir.setReturnValue(ImmutableSet.of(Blocks.WATER));
                    cir.cancel();
                }
            }
        }
    }

    @Inject(method = "requestedItems",
            at = @At("RETURN"),
            cancellable = true)
    void replaceGatherableItems(CallbackInfoReturnable<ImmutableSet<Item>> cir) {
        ImmutableSet<Item> originalSet = cir.getReturnValue();
        ImmutableSet.Builder<Item> setBuilder = ImmutableSet.<Item>builder().addAll(originalSet);
        
        // Add general food items that all villagers should pick up
        if (CONFIG.villagersGeneralConfig.villagersEatMelons) {
            setBuilder.add(Items.MELON_SLICE);
        }
        if (CONFIG.villagersGeneralConfig.villagersEatPumpkinPie) {
            setBuilder.add(Items.PUMPKIN_PIE);
        }
        if (CONFIG.villagersGeneralConfig.villagersEatCookedFish) {
            setBuilder.add(Items.COOKED_COD);
            setBuilder.add(Items.COOKED_SALMON);
        }
        
        // Add profession-specific items
        switch (name) {
            case "butcher" -> {
                if (CONFIG.villagersProfessionConfig.butchersFeedChickens) {
                    setBuilder.addAll(ImmutableSet.of(Items.PUMPKIN_SEEDS, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS));
                }
                if (CONFIG.villagersProfessionConfig.butchersFeedCows || CONFIG.villagersProfessionConfig.butchersFeedSheep) {
                    setBuilder.addAll(ImmutableSet.of(Items.WHEAT));
                }
            }
            case "farmer" -> {
                if (CONFIG.villagersProfessionConfig.farmersHarvestPumpkins) {
                    setBuilder.addAll(ImmutableSet.of(Items.PUMPKIN_SEEDS, Items.PUMPKIN));
                }
                if (CONFIG.villagersProfessionConfig.farmersHarvestMelons) {
                    setBuilder.addAll(ImmutableSet.of(Items.MELON_SEEDS, Items.MELON_SLICE));
                }
            }
            case "fisherman" -> {
                if (CONFIG.villagersProfessionConfig.fishermanFish) {
                    setBuilder.addAll(ImmutableSet.of(Items.COD, Items.SALMON));
                }
            }
            case "fletcher" -> {
                if (CONFIG.villagersProfessionConfig.fletchersFeedChickens) {
                    setBuilder.addAll(ImmutableSet.of(Items.PUMPKIN_SEEDS, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS));
                }
            }
            case "leatherworker" -> {
                if (CONFIG.villagersProfessionConfig.leatherworkersFeedCows) {
                    setBuilder.addAll(ImmutableSet.of(Items.WHEAT));
                }
            }
            case "shepherd" -> {
                if (CONFIG.villagersProfessionConfig.shepherdsFeedSheep) {
                    setBuilder.addAll(ImmutableSet.of(Items.WHEAT));
                }
            }
        }
        cir.setReturnValue(setBuilder.build());
        cir.cancel();
    }
}
