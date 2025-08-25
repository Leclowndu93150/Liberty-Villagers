package com.leclowndu93150.libertyvillagers.mixin;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StrollToPoi;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.mutable.MutableLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(StrollToPoi.class)
public class GoToNearbyPositionTaskMixin {

    @Overwrite
    public static BehaviorControl<PathfinderMob> create(MemoryModuleType<GlobalPos> poiPosMemory, float speedModifier, int closeEnoughDist, int maxDistFromPoi) {
        MutableLong mutableLong = new MutableLong(0L);
        return BehaviorBuilder.create(
            instance -> instance.group(instance.registered(MemoryModuleType.WALK_TARGET), instance.present(poiPosMemory))
                    .apply(instance, (memoryAccessor, memoryAccessor2) -> (serverLevel, pathfinderMob, l) -> {
                            // Check if the entity is a fisherman villager holding a fishing rod
                            if (pathfinderMob.getType() == EntityType.VILLAGER) {
                                Villager villager = (Villager) pathfinderMob;
                                if (villager.getVillagerData().getProfession() == VillagerProfession.FISHERMAN &&
                                        villager.getMainHandItem().is(Items.FISHING_ROD)) {
                                    return false; // Prevent strolling while fishing
                                }
                            }
                            
                            GlobalPos globalPos = (GlobalPos)instance.get(memoryAccessor2);
                            if (serverLevel.dimension() != globalPos.dimension() || !globalPos.pos().closerToCenterThan(pathfinderMob.position(), (double)maxDistFromPoi)) {
                                return false;
                            } else if (l <= mutableLong.getValue()) {
                                return true;
                            } else {
                                memoryAccessor.set(new WalkTarget(globalPos.pos(), speedModifier, closeEnoughDist));
                                mutableLong.setValue(l + 80L);
                                return true;
                            }
                        })
        );
    }
}
