package com.leclowndu93150.libertyvillagers.mixin;

import com.leclowndu93150.libertyvillagers.tasks.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.GiveGiftToHero;
import net.minecraft.world.entity.ai.behavior.HarvestFarmland;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetLookAndInteract;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromBlockMemory;
import net.minecraft.world.entity.ai.behavior.ShowTradesToPlayer;
import net.minecraft.world.entity.ai.behavior.StrollAroundPoi;
import net.minecraft.world.entity.ai.behavior.StrollToPoi;
import net.minecraft.world.entity.ai.behavior.StrollToPoiList;
import net.minecraft.world.entity.ai.behavior.UpdateActivityFromSchedule;
import net.minecraft.world.entity.ai.behavior.UseBonemeal;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.ai.behavior.WorkAtComposter;
import net.minecraft.world.entity.ai.behavior.WorkAtPoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(VillagerGoalPackages.class)
public abstract class VillagerTaskListProviderMixin {

    private static final int SECONDARY_WORK_TASK_PRIORITY = 6; // Mojang default: 5.
    private static final int THIRD_WORK_TASK_PRIORITY = 7;
    private static final int PRIMARY_WORK_TASK_PRIORITY = 8;

    @Invoker("getMinimalLookBehavior")
    public static Pair<Integer, BehaviorControl<LivingEntity>> invokeCreateBusyFollowTask() {
        throw new AssertionError();
    }

    @Inject(method = "getWorkPackage", at = @At("HEAD"), cancellable = true)
    private static void replaceCreateWorkTasks(Holder<VillagerProfession> profession, float speed,
                                               CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        BehaviorControl<? super Villager> villagerWorkTask = new WorkAtPoi(); // Plays working sounds on the job site.
        BehaviorControl<? super Villager> secondaryWorkTask = null;
        // GoToIfNearby makes the villager wander around the job site.
        BehaviorControl<? super Villager> thirdWorkTask = StrollAroundPoi.create(MemoryModuleType.JOB_SITE, 0.4f, 4);

        if (profession.is(VillagerProfession.ARMORER)) {
            if (CONFIG.villagersProfessionConfig.armorerHealsGolems) {
                secondaryWorkTask = new HealGolemTask();
            }
        } else if (profession.is(VillagerProfession.BUTCHER)) {
            ArrayList<Pair<BehaviorControl<? super Villager>, Integer>> randomTasks = new ArrayList<>();
            if (CONFIG.villagersProfessionConfig.butchersFeedChickens) {
                randomTasks.add(Pair.of(new FeedTargetTask(Chicken.class, ImmutableSet.of(Items.WHEAT_SEEDS,
                        Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS),
                        CONFIG.villagersProfessionConfig.feedAnimalsRange,
                        CONFIG.villagersProfessionConfig.feedMaxAnimals), SECONDARY_WORK_TASK_PRIORITY));
            }
            if (CONFIG.villagersProfessionConfig.butchersFeedCows) {
                randomTasks.add(Pair.of(new FeedTargetTask(Cow.class, ImmutableSet.of(Items.WHEAT),
                        CONFIG.villagersProfessionConfig.feedAnimalsRange,
                        CONFIG.villagersProfessionConfig.feedMaxAnimals), SECONDARY_WORK_TASK_PRIORITY));
            }
            if (CONFIG.villagersProfessionConfig.butchersFeedPigs) {
                randomTasks.add(Pair.of(new FeedTargetTask(Pig.class, ImmutableSet.of(Items.BEETROOT,
                        Items.POTATO, Items.CARROT),
                        CONFIG.villagersProfessionConfig.feedAnimalsRange,
                        CONFIG.villagersProfessionConfig.feedMaxAnimals), SECONDARY_WORK_TASK_PRIORITY));
            }
            if (CONFIG.villagersProfessionConfig.butchersFeedRabbits) {
                randomTasks.add(Pair.of(new FeedTargetTask(Rabbit.class, ImmutableSet.of(Items.CARROT),
                        CONFIG.villagersProfessionConfig.feedAnimalsRange,
                        CONFIG.villagersProfessionConfig.feedMaxAnimals), SECONDARY_WORK_TASK_PRIORITY));
            }
            if (CONFIG.villagersProfessionConfig.butchersFeedSheep) {
                randomTasks.add(Pair.of(new FeedTargetTask(Sheep.class, ImmutableSet.of(Items.WHEAT),
                        CONFIG.villagersProfessionConfig.feedAnimalsRange,
                        CONFIG.villagersProfessionConfig.feedMaxAnimals), SECONDARY_WORK_TASK_PRIORITY));
            }
            if (randomTasks.size() > 0) {
                secondaryWorkTask = new RunOne<>(ImmutableList.copyOf(randomTasks));
            }
        } else if (profession.is(VillagerProfession.CLERIC)) {
            if (CONFIG.villagersProfessionConfig.clericThrowsPotionsAtPlayers ||
                    CONFIG.villagersProfessionConfig.clericThrowsPotionsAtVillagers) {
                secondaryWorkTask = new ThrowRegenPotionAtTask();
            }
        } else if (profession.is(VillagerProfession.FARMER)) {
            villagerWorkTask = new HarvestFarmland(); // Harvest / plant seeds.
            secondaryWorkTask = new WorkAtComposter(); // Compost.
            thirdWorkTask = new UseBonemeal(); // Apply bonemeal to crops.
        } else if (profession.is(VillagerProfession.FISHERMAN)) {
            if (CONFIG.villagersProfessionConfig.fishermanFish) {
                villagerWorkTask = new GoFishingTask();
                secondaryWorkTask = new FisherWorkTask(); // Cook fish.
            }
        } else if (profession.is(VillagerProfession.FLETCHER)) {
            if (CONFIG.villagersProfessionConfig.fletchersFeedChickens) {
                secondaryWorkTask = new FeedTargetTask(Chicken.class, ImmutableSet.of(Items.WHEAT_SEEDS,
                        Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS),
                        CONFIG.villagersProfessionConfig.feedAnimalsRange,
                        CONFIG.villagersProfessionConfig.feedMaxAnimals);
            }
        } else if (profession.is(VillagerProfession.LEATHERWORKER)) {
            if (CONFIG.villagersProfessionConfig.leatherworkersFeedCows) {
                secondaryWorkTask = new FeedTargetTask(Cow.class, ImmutableSet.of(Items.WHEAT),
                        CONFIG.villagersProfessionConfig.feedAnimalsRange,
                        CONFIG.villagersProfessionConfig.feedMaxAnimals);
            }
        } else if (profession.is(VillagerProfession.SHEPHERD)) {
            if (CONFIG.villagersProfessionConfig.shepherdsFeedSheep) {
                secondaryWorkTask = new FeedTargetTask(Sheep.class, ImmutableSet.of(Items.WHEAT),
                        CONFIG.villagersProfessionConfig.feedAnimalsRange,
                        CONFIG.villagersProfessionConfig.feedMaxAnimals);
            }
        }

        ArrayList<Pair<BehaviorControl<? super Villager>, Integer>> randomTasks = new ArrayList<>(
                ImmutableList.of(Pair.of(villagerWorkTask, PRIMARY_WORK_TASK_PRIORITY),
                        Pair.of(StrollToPoi.create(MemoryModuleType.JOB_SITE, 0.4f,
                                CONFIG.villagerPathfindingConfig.minimumPOISearchDistance, 10), 5),
                        Pair.of(StrollToPoiList.create(MemoryModuleType.SECONDARY_JOB_SITE, speed,
                                CONFIG.villagerPathfindingConfig.minimumPOISearchDistance, 6,
                                MemoryModuleType.JOB_SITE), 5)));

        if (secondaryWorkTask != null) {
            randomTasks.add(Pair.of(secondaryWorkTask, SECONDARY_WORK_TASK_PRIORITY));
        }

        if (thirdWorkTask != null) {
            randomTasks.add(Pair.of(thirdWorkTask, THIRD_WORK_TASK_PRIORITY));
        }

        RunOne<Villager> randomTask = new RunOne<>(ImmutableList.copyOf(randomTasks));
        List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> tasks =
                List.of(VillagerTaskListProviderMixin.invokeCreateBusyFollowTask(),
                        Pair.of(5, randomTask),
                        Pair.of(10, new ShowTradesToPlayer(400, 1600)),
                        Pair.of(10, SetLookAndInteract.create(EntityType.PLAYER, 4)),
                        Pair.of(2, SetWalkTargetFromBlockMemory.create(MemoryModuleType.JOB_SITE, speed, 9,
                                        CONFIG.villagerPathfindingConfig.pathfindingMaxRange, 1200)),
                        Pair.of(3, new GiveGiftToHero(100)),
                        Pair.of(99, UpdateActivityFromSchedule.create()));
        cir.setReturnValue(ImmutableList.copyOf(tasks));
        cir.cancel();
    }

    @ModifyArg(method = "getMeetPackage(Lnet/minecraft/core/Holder;F)Lcom/google/common/collect/ImmutableList;",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/behavior/SetWalkTargetFromBlockMemory;create(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;FIII)Lnet/minecraft/world/entity/ai/behavior/OneShot;"),
            index = 2)
    private static int replaceCompletionRangeForWalkTowardsMeetTask(int completionRange) {
        return Math.max(CONFIG.villagerPathfindingConfig.minimumPOISearchDistance, completionRange) + 3;
    }
}
