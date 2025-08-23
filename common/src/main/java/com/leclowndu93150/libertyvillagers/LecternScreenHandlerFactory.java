package com.leclowndu93150.libertyvillagers;

import com.leclowndu93150.libertyvillagers.mixin.LecternScreenHandlerAccessorMixin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class LecternScreenHandlerFactory implements MenuProvider {
    private final ItemStack bookStack;

    public LecternScreenHandlerFactory(ItemStack bookStack) {
        this.bookStack = bookStack;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("text.LibertyVillagers.villagerStats.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
        final LecternMenu lecternScreenHandler = new LecternMenu(i);
        ((LecternScreenHandlerAccessorMixin) lecternScreenHandler).getInventory().setItem(0, bookStack);
        return lecternScreenHandler;
    }
}

