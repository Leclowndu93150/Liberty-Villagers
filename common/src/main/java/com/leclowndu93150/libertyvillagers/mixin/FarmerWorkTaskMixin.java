package com.leclowndu93150.libertyvillagers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.WorkAtComposter;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(WorkAtComposter.class)
public class FarmerWorkTaskMixin {

    @Shadow
    private static List<Item> COMPOSTABLE_ITEMS;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    static private void modifyStaticBlock(CallbackInfo ci) {
        if (CONFIG.villagersProfessionConfig.farmersHarvestMelons) {
            COMPOSTABLE_ITEMS = new ArrayList<>(COMPOSTABLE_ITEMS);
            COMPOSTABLE_ITEMS.add(Items.MELON_SEEDS);
        }
        if (CONFIG.villagersProfessionConfig.farmersHarvestPumpkins) {
            COMPOSTABLE_ITEMS = new ArrayList<>(COMPOSTABLE_ITEMS);
            COMPOSTABLE_ITEMS.add(Items.PUMPKIN_SEEDS);
        }
    }

    @Inject(method = "makeBread(Lnet/minecraft/world/entity/npc/Villager;)V",
            at = @At("HEAD"))
    private void craftAndDropPumpkinPie(Villager entity, CallbackInfo ci) {
        SimpleContainer simpleInventory = entity.getInventory();
        if (CONFIG.villagersProfessionConfig.farmersHarvestPumpkins) {
            if (simpleInventory.countItem(Items.PUMPKIN_PIE) > 36) {
                return;
            }
            int i = simpleInventory.countItem(Items.PUMPKIN);
            if (i == 0) {
                return;
            }

            // We are not going to ask where the eggs and the sugar come from.
            simpleInventory.removeItemType(Items.PUMPKIN, i);
            ItemStack itemStack = simpleInventory.addItem(new ItemStack(Items.PUMPKIN_PIE, i));
            if (!itemStack.isEmpty()) {
                entity.spawnAtLocation(itemStack, 0.5f);
            }
        }
    }
}
