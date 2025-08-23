package com.leclowndu93150.libertyvillagers.mixin;

import com.google.common.collect.ImmutableSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.TradeWithVillager;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(TradeWithVillager.class)
public abstract class GatherItemsVillagerTaskMixin {

    @Invoker("throwHalfStack")
    static void giveHalfOfStack(Villager villager, Set<Item> validItems, LivingEntity target) {
        throw new AssertionError();
    }

    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/Villager;J)V",
            at = @At("HEAD"))
    private void keepRunning(ServerLevel serverWorld, Villager villagerEntity, long l, CallbackInfo ci) {
        if (villagerEntity.getBrain().getMemoryInternal(MemoryModuleType.INTERACTION_TARGET).isEmpty()) {
            return;
        }
        Villager villagerEntity2 = (Villager)villagerEntity.getBrain().getMemoryInternal(MemoryModuleType.INTERACTION_TARGET).get();
        if (villagerEntity.distanceToSqr(villagerEntity2) > 5.0) {
            return;
        }
        if (villagerEntity2.getVillagerData().getProfession() == VillagerProfession.FARMER) {
            GatherItemsVillagerTaskMixin.giveHalfOfStack(villagerEntity, ImmutableSet.of(Items.PUMPKIN),
                    villagerEntity2);
        }
        if ((CONFIG.villagersProfessionConfig.leatherworkersFeedCows &&
                villagerEntity2.getVillagerData().getProfession() == VillagerProfession.LEATHERWORKER) ||
                (CONFIG.villagersProfessionConfig.butchersFeedCows &&
                        villagerEntity2.getVillagerData().getProfession() == VillagerProfession.BUTCHER) ||
                (CONFIG.villagersProfessionConfig.butchersFeedSheep &&
                        villagerEntity2.getVillagerData().getProfession() == VillagerProfession.BUTCHER) ||
                (CONFIG.villagersProfessionConfig.shepherdsFeedSheep &&
                        villagerEntity2.getVillagerData().getProfession() == VillagerProfession.SHEPHERD)) {
            GatherItemsVillagerTaskMixin.giveHalfOfStack(villagerEntity, ImmutableSet.of(Items.WHEAT), villagerEntity2);
        }
        if (CONFIG.villagersProfessionConfig.butchersFeedPigs &&
                villagerEntity2.getVillagerData().getProfession() == VillagerProfession.BUTCHER) {
            GatherItemsVillagerTaskMixin.giveHalfOfStack(villagerEntity, ImmutableSet.of(Items.CARROT, Items.POTATO),
                    villagerEntity2);
        }
        if ((CONFIG.villagersProfessionConfig.butchersFeedChickens &&
                villagerEntity2.getVillagerData().getProfession() == VillagerProfession.BUTCHER) ||
                (CONFIG.villagersProfessionConfig.fletchersFeedChickens &&
                        villagerEntity2.getVillagerData().getProfession() == VillagerProfession.FLETCHER)) {
            GatherItemsVillagerTaskMixin.giveHalfOfStack(villagerEntity,
                    ImmutableSet.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS),
                    villagerEntity2);
        }
        if (CONFIG.villagersProfessionConfig.butchersFeedRabbits &&
                villagerEntity2.getVillagerData().getProfession() == VillagerProfession.BUTCHER) {
            GatherItemsVillagerTaskMixin.giveHalfOfStack(villagerEntity, ImmutableSet.of(Items.CARROT),
                    villagerEntity2);
        }
    }
}
