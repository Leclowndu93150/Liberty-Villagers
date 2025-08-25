package com.leclowndu93150.libertyvillagers.mixin;

import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.StrollAroundPoi;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(StrollAroundPoi.class)
public class GoToIfNearbyTaskMixin {

    /**
     * @author LibertyVillagers
     * @reason Prevent fisherman villagers from wandering while fishing
     */
    @Overwrite
    public static OneShot<PathfinderMob> create(MemoryModuleType<GlobalPos> pPoiPosMemory, float pSpeedModifier, int pMaxDistFromPoi) {
        MutableLong mutablelong = new MutableLong(0L);
        return BehaviorBuilder.create((p_258827_) -> p_258827_.group(p_258827_.registered(MemoryModuleType.WALK_TARGET), p_258827_.present(pPoiPosMemory)).apply(p_258827_, (p_258821_, p_258822_) -> (p_258834_, p_258835_, p_258836_) -> {
            // Check if the entity is a fisherman villager holding a fishing rod
            if (p_258835_.getType() == EntityType.VILLAGER) {
                Villager villager = (Villager) p_258835_;
                if (villager.getVillagerData().getProfession() == VillagerProfession.FISHERMAN &&
                        villager.getMainHandItem().is(Items.FISHING_ROD)) {
                    return false; // Prevent wandering while fishing
                }
            }
            
            GlobalPos globalpos = (GlobalPos)p_258827_.get(p_258822_);
            if (p_258834_.dimension() == globalpos.dimension() && globalpos.pos().closerToCenterThan(p_258835_.position(), (double)pMaxDistFromPoi)) {
                if (p_258836_ <= mutablelong.getValue()) {
                    return true;
                } else {
                    Optional<Vec3> optional = Optional.ofNullable(LandRandomPos.getPos(p_258835_, 8, 6));
                    p_258821_.setOrErase(optional.map((p_258816_) -> new WalkTarget(p_258816_, pSpeedModifier, 1)));
                    mutablelong.setValue(p_258836_ + 180L);
                    return true;
                }
            } else {
                return false;
            }
        }));
    }
}
