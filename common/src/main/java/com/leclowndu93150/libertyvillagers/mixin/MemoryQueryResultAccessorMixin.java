package com.leclowndu93150.libertyvillagers.mixin;

import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MemoryAccessor.class)
public interface MemoryQueryResultAccessorMixin {

    @Accessor("memoryType")
    MemoryModuleType<?> getMemory();
}
