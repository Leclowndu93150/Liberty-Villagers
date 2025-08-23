package com.leclowndu93150.libertyvillagers.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.LecternMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LecternMenu.class)
public interface LecternScreenHandlerAccessorMixin {
    @Accessor("lectern")
    Container getInventory();
}

