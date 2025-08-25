package com.leclowndu93150.libertyvillagers.mixin;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ReputationEventHandler;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.function.BiPredicate;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;
import static net.minecraft.world.entity.npc.Villager.POI_MEMORIES;

@Mixin(Villager.class)
public abstract class VillagerEntityMixin extends AbstractVillager implements ReputationEventHandler, VillagerDataHolder {

    public VillagerEntityMixin(EntityType<? extends AbstractVillager> entityType, Level world) {
        super(entityType, world);
    }

    @Shadow
    public abstract VillagerData getVillagerData();

    @Shadow
    public abstract void setVillagerData(VillagerData villagerData);

    @Shadow
    public static Map<Item, Integer> FOOD_POINTS;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    static private void modifyStaticBlock(CallbackInfo ci) {
        // Add extra food items to FOOD_POINTS
        if (CONFIG.villagersGeneralConfig.villagersEatMelons) {
            FOOD_POINTS = new HashMap<>(FOOD_POINTS);
            FOOD_POINTS.put(Items.MELON_SLICE, 1);
        }
        if (CONFIG.villagersGeneralConfig.villagersEatPumpkinPie) {
            FOOD_POINTS = new HashMap<>(FOOD_POINTS);
            FOOD_POINTS.put(Items.PUMPKIN_PIE, 1);
        }
        if (CONFIG.villagersGeneralConfig.villagersEatCookedFish) {
            FOOD_POINTS = new HashMap<>(FOOD_POINTS);
            FOOD_POINTS.put(Items.COOKED_COD, 1);
            FOOD_POINTS.put(Items.COOKED_SALMON, 1);
        }
    }

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V")
    public void villagerInit(EntityType<? extends AbstractVillager> entityType, Level world, CallbackInfo ci) {
        if (CONFIG.villagerPathfindingConfig.villagersAvoidCactus) {
            this.setPathfindingMalus(PathType.DANGER_OTHER, 16);
        }
        if (CONFIG.villagerPathfindingConfig.villagersAvoidWater) {
            this.setPathfindingMalus(PathType.WATER, -1);
            this.setPathfindingMalus(PathType.WATER_BORDER, 16);
        }
        if (CONFIG.villagerPathfindingConfig.villagersAvoidRail) {
            this.setPathfindingMalus(PathType.RAIL, -1);
        }
        if (CONFIG.villagerPathfindingConfig.villagersAvoidTrapdoor) {
            this.setPathfindingMalus(PathType.TRAPDOOR, -1);
        }
        if (CONFIG.villagerPathfindingConfig.villagersAvoidPowderedSnow) {
            this.setPathfindingMalus(PathType.POWDER_SNOW, -1);
            this.setPathfindingMalus(PathType.DANGER_POWDER_SNOW, 16);
        }
        if (CONFIG.villagersGeneralConfig.allBabyVillagers) {
            this.setBaby(true);
        }
    }

    @Inject(at = @At("HEAD"), method = "registerBrainGoals(Lnet/minecraft/world/entity/ai/Brain;)V")
    private void changeVillagerProfession(Brain<Villager> brain, CallbackInfo ci) {
        if (!(this.level() instanceof ServerLevel)) {
            return;
        }
        ServerLevel world = (ServerLevel) this.level();

        VillagerProfession profession = this.getVillagerData().getProfession();
        if (CONFIG.villagersGeneralConfig.noNitwitVillagers && profession == VillagerProfession.NITWIT) {
            this.setVillagerData(getVillagerData().setProfession(VillagerProfession.NONE));
            brain.stopAll(world, (Villager) ((Object) this));
        }
        if (CONFIG.villagersGeneralConfig.allNitwitVillagers && profession != VillagerProfession.NITWIT) {
            this.setVillagerData(getVillagerData().setProfession(VillagerProfession.NITWIT));
            this.releaseTicketFor(brain, world, MemoryModuleType.JOB_SITE);
            this.releaseTicketFor(brain, world, MemoryModuleType.POTENTIAL_JOB_SITE);
            brain.stopAll(world, (Villager) ((Object) this));
        }
    }

    // The brain is not yet assigned when initBrain is called, so it must be specified.
    public void releaseTicketFor(Brain<Villager> brain, ServerLevel world, MemoryModuleType<GlobalPos> memoryModuleType) {
        MinecraftServer minecraftServer = world.getServer();
        brain.getMemoryInternal(memoryModuleType).ifPresent(pos -> {
            ServerLevel serverWorld = minecraftServer.getLevel(pos.dimension());
            if (serverWorld == null) {
                return;
            }
            PoiManager pointOfInterestStorage = serverWorld.getPoiManager();
            Optional<Holder<PoiType>> optional = pointOfInterestStorage.getType(pos.pos());
            BiPredicate<Villager, Holder<PoiType>> biPredicate = POI_MEMORIES.get(memoryModuleType);
            if (optional.isPresent() && biPredicate.test((Villager) ((Object) this), optional.get())) {
                pointOfInterestStorage.release(pos.pos());
                DebugPackets.sendPoiTicketCountPacket(serverWorld, pos.pos());
            }
        });
    }

    @Inject(method = "hasFarmSeeds()Z",
            at = @At("HEAD"),
            cancellable = true)
    public void hasExtraSeedToPlant(CallbackInfoReturnable<Boolean> cir) {
        Set<Item> extraSeeds = new HashSet<>();
        if (CONFIG.villagersProfessionConfig.farmersHarvestMelons) {
            extraSeeds.add(Items.MELON_SLICE);
            extraSeeds.add(Items.MELON_SEEDS);
        }
        if (CONFIG.villagersProfessionConfig.farmersHarvestPumpkins) {
            extraSeeds.add(Items.PUMPKIN);
            extraSeeds.add(Items.PUMPKIN_SEEDS);
        }
        if (!extraSeeds.isEmpty() && this.getInventory().hasAnyOf(extraSeeds)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "ageBoundaryReached()V", at = @At("HEAD"), cancellable = true)
    private void babiesNeverGrowUp(CallbackInfo ci) {
        if (CONFIG.villagersGeneralConfig.allBabyVillagers || CONFIG.villagersGeneralConfig.foreverYoung) {
            this.setBaby(true);
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "stopSleeping()V")
    private void healOnWakeUp(CallbackInfo info) {
        if (CONFIG.villagersGeneralConfig.healOnWake) {
            // Heal villager upon waking up.
            this.heal(this.getMaxHealth());
        }
    }

    @Inject(at = @At("HEAD"), method = "canBreed()Z", cancellable = true)
    public void replaceIsReadyToBreed(CallbackInfoReturnable<Boolean> cir) {
        if (CONFIG.villagersGeneralConfig.villagersDontBreed) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "wantsToSpawnGolem(J)Z", cancellable = true)
    public void replaceCanSummonGolem(long time, CallbackInfoReturnable<Boolean> cir) {
        if (CONFIG.golemsConfig.villagersDontSummonGolems) {
            cir.setReturnValue(false);
            cir.cancel();
        }
        if (CONFIG.golemsConfig.golemSpawnLimit) {
            List<IronGolem> golems = this.level().getEntitiesOfClass(IronGolem.class,
                    this.getBoundingBox().inflate(CONFIG.golemsConfig.golemSpawnLimitRange));
            if (golems.size() >= CONFIG.golemsConfig.golemSpawnLimitCount) {
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }

    @Inject(method = "readAdditionalSaveData",
            at = @At("TAIL"))
   public void readCustomDataFromNbt(CompoundTag nbt, CallbackInfo ci) {
        // If initialized with a rod, get rid of it.
        if (this.getMainHandItem().is(Items.FISHING_ROD)) {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        // Get rid of items the villager can't gather.
        for (int i = this.getInventory().getContainerSize() - 1; i >= 0; i-- ) {
            ItemStack stack = this.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            // Keep food items and profession-specific items
            if (FOOD_POINTS.containsKey(stack.getItem())) continue;
            if (this.getVillagerData().getProfession().requestedItems().contains(stack.getItem())) continue;
            this.getInventory().removeItemNoUpdate(i);
        }
    }

    @Inject(method = "setLastHurtByMob",
            at = @At("TAIL"))
    public void setAttacker(@Nullable LivingEntity attacker, CallbackInfo ci) {
        // Drop the rod if attacked.
        if (this.getMainHandItem().is(Items.FISHING_ROD)) {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }
}