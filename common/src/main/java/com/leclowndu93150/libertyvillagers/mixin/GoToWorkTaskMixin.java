package com.leclowndu93150.libertyvillagers.mixin;

import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(AssignProfessionFromJobSite.class)
public class GoToWorkTaskMixin {

    @Overwrite
    public static BehaviorControl<Villager> create() {
        return BehaviorBuilder.create(
            i -> i.group(i.present(MemoryModuleType.POTENTIAL_JOB_SITE), i.registered(MemoryModuleType.JOB_SITE))
                .apply(
                    i,
                    (potentialJobSite, jobSite) -> (level, body, timestamp) -> {
                        GlobalPos pos = i.get(potentialJobSite);
                        // Custom: Modified distance check based on config
                        double maxDistance = Math.max(2.0, CONFIG.villagerPathfindingConfig.minimumPOISearchDistance + 1);
                        if (!pos.pos().closerToCenterThan(body.position(), maxDistance) && !body.assignProfessionWhenSpawned()) {
                            return false;
                        } else {
                            potentialJobSite.erase();
                            jobSite.set(pos);
                            level.broadcastEntityEvent(body, (byte)14);
                            if (!body.getVillagerData().profession().is(VillagerProfession.NONE)) {
                                return true;
                            } else {
                                MinecraftServer server = level.getServer();
                                Optional.ofNullable(server.getLevel(pos.dimension()))
                                    .flatMap(l -> l.getPoiManager().getType(pos.pos()))
                                    .flatMap(
                                        poiType -> BuiltInRegistries.VILLAGER_PROFESSION
                                            .listElements()
                                            .filter(profession -> profession.value().heldJobSite().test((Holder<PoiType>)poiType))
                                            .findFirst()
                                    )
                                    .ifPresent(profession -> {
                                        body.setVillagerData(body.getVillagerData().withProfession(profession));
                                        body.refreshBrain(level);
                                    });
                                return true;
                            }
                        }
                    }
                )
        );
    }
}
