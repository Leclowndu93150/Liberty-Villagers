package com.leclowndu93150.libertyvillagers.tasks;

import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.WorkAtPoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class FisherWorkTask extends WorkAtPoi {

    @Override
    protected void useWorkstation(ServerLevel world, Villager entity) {
        Optional<GlobalPos> optional = entity.getBrain().getMemoryInternal(MemoryModuleType.JOB_SITE);
        if (optional.isEmpty()) {
            return;
        }
        GlobalPos globalPos = optional.get();
        BlockState blockState = world.getBlockState(globalPos.pos());
        if (blockState.is(Blocks.BARREL)) {
            this.cookAndDropFish(entity);
        }
    }

    private void cookAndDropFish(Villager entity) {
        SimpleContainer simpleInventory = entity.getInventory();
        int cod = simpleInventory.countItem(Items.COD);
        int salmon = simpleInventory.countItem(Items.SALMON);
        simpleInventory.removeItemType(Items.COD, cod);
        simpleInventory.removeItemType(Items.SALMON, salmon);
        ItemStack cookedSalmon = simpleInventory.addItem(new ItemStack(Items.COOKED_SALMON, salmon));
        if (!cookedSalmon.isEmpty()) {
            entity.spawnAtLocation(cookedSalmon, 0.5f);
        }
        ItemStack cookedCod = simpleInventory.addItem(new ItemStack(Items.COOKED_COD, cod));
        if (!cookedCod.isEmpty()) {
            entity.spawnAtLocation(cookedCod, 0.5f);
        }
    }
}

