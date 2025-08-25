package com.leclowndu93150.libertyvillagers.mixin;

import java.util.List;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StrollToPoiList;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.mutable.MutableLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(StrollToPoiList.class)
public class GoToSecondaryPositionTaskMixin {

    @Overwrite
    public static BehaviorControl<Villager> create(
        MemoryModuleType<List<GlobalPos>> poiListMemory,
        float speedModifier,
        int closeEnoughDist,
        int maxDistFromPoi,
        MemoryModuleType<GlobalPos> mustBeCloseToMemory
    ) {
        MutableLong mutableLong = new MutableLong(0L);
        return BehaviorBuilder.create(
            instance -> instance.group(instance.registered(MemoryModuleType.WALK_TARGET), instance.present(poiListMemory), instance.present(mustBeCloseToMemory))
                    .apply(
                        instance,
                        (memoryAccessor, memoryAccessor2, memoryAccessor3) -> (serverLevel, villager, l) -> {
                                // Check if the villager is a fisherman holding a fishing rod
                                if (villager.getVillagerData().getProfession() == VillagerProfession.FISHERMAN &&
                                        villager.getMainHandItem().is(Items.FISHING_ROD)) {
                                    return false; // Prevent strolling while fishing
                                }
                                
                                List<GlobalPos> list = (List<GlobalPos>)instance.get(memoryAccessor2);
                                GlobalPos globalPos = (GlobalPos)instance.get(memoryAccessor3);
                                if (list.isEmpty()) {
                                    return false;
                                } else {
                                    GlobalPos globalPos2 = (GlobalPos)list.get(serverLevel.getRandom().nextInt(list.size()));
                                    if (globalPos2 != null
                                        && serverLevel.dimension() == globalPos2.dimension()
                                        && globalPos.pos().closerToCenterThan(villager.position(), (double)maxDistFromPoi)) {
                                        if (l > mutableLong.getValue()) {
                                            memoryAccessor.set(new WalkTarget(globalPos2.pos(), speedModifier, closeEnoughDist));
                                            mutableLong.setValue(l + 100L);
                                        }

                                        return true;
                                    } else {
                                        return false;
                                    }
                                }
                            }
                    )
        );
    }
}
