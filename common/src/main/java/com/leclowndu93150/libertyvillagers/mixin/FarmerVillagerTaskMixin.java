package com.leclowndu93150.libertyvillagers.mixin;

import com.leclowndu93150.libertyvillagers.ReflectionHelper;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.HarvestFarmland;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.PotatoBlock;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(HarvestFarmland.class)
public abstract class FarmerVillagerTaskMixin {
    private static final int MAX_RUN_TIME = 20 * 60; // One minute.

    @Shadow
    @Nullable
    private BlockPos aboveFarmlandPos;

    @Shadow
    private long nextOkStartTime;

    @Shadow
    private int timeWorkedSoFar;

    @Shadow
    private List<BlockPos> validFarmlandAroundVillager;

    @Shadow
    @Nullable
    abstract BlockPos getValidFarmland(ServerLevel world);

    @Inject(method = "checkExtraStartConditions", at = @At(value = "HEAD"), cancellable = true)
    protected void replaceShouldRun(ServerLevel serverWorld, Villager villagerEntity,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (!serverWorld.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            cir.setReturnValue(false);
            cir.cancel();
        } else if (villagerEntity.getVillagerData().getProfession() != VillagerProfession.FARMER) {
            cir.setReturnValue(false);
            cir.cancel();
        } else {
            BlockPos.MutableBlockPos mutable = villagerEntity.blockPosition().mutable();
            this.validFarmlandAroundVillager.clear();

            for (int i = -1 * CONFIG.villagersProfessionConfig.findCropRangeHorizontal;
                 i <= CONFIG.villagersProfessionConfig.findCropRangeHorizontal; ++i) {
                for (int j = -1 * CONFIG.villagersProfessionConfig.findCropRangeVertical;
                     j <= CONFIG.villagersProfessionConfig.findCropRangeVertical; ++j) {
                    for (int k = -CONFIG.villagersProfessionConfig.findCropRangeHorizontal;
                         k <= CONFIG.villagersProfessionConfig.findCropRangeHorizontal; ++k) {
                        mutable.set(villagerEntity.getX() + (double) i, villagerEntity.getY() + (double) j,
                                villagerEntity.getZ() + (double) k);
                        if (this.replaceIsSuitableTarget(mutable, serverWorld, villagerEntity)) {
                            this.validFarmlandAroundVillager.add(new BlockPos(mutable));
                        }
                    }
                }
            }
            this.aboveFarmlandPos = this.getValidFarmland(serverWorld);
            cir.setReturnValue(this.aboveFarmlandPos != null);
            cir.cancel();
        }
    }

    private boolean isGourd(Block block) {
        return block instanceof PumpkinBlock || isMelon(block);
    }

    private boolean isMelon(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).equals(BuiltInRegistries.BLOCK.getKey(Blocks.MELON));
    }

    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    protected void keepRunning(ServerLevel serverWorld, Villager villagerEntity, long l, CallbackInfo cir) {
        if (!CONFIG.villagersProfessionConfig.preferPlantSameCrop &&
                !CONFIG.villagersProfessionConfig.farmersHarvestPumpkins &&
                !CONFIG.villagersProfessionConfig.farmersHarvestMelons) {
            // Use default logic.
            return;
        }

        if (villagerEntity.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
            // Wait for the villager to reach the walk target.
            cir.cancel();
            return;
        }

        Item preferredSeeds = null;
        BlockPos aboveFarmlandPos = this.aboveFarmlandPos;
        // Can't stand directly on top of the gourd bottom, so increase the distance.
        int distance = 2;
        BlockState blockState = serverWorld.getBlockState(aboveFarmlandPos);
        Block block = blockState.getBlock();
        Block block2 = serverWorld.getBlockState(aboveFarmlandPos.below()).getBlock();
        if (aboveFarmlandPos.closerToCenterThan(villagerEntity.position(), distance)) {
            boolean foundBlockCrop = false;
            if (CONFIG.villagersProfessionConfig.preferPlantSameCrop) {
                if (block instanceof CropBlock && ((CropBlock) block).isMaxAge(blockState)) {
                    foundBlockCrop = true;
                    preferredSeeds = getPreferredSeedsForCropBlock(block);
                }
            }

            if (CONFIG.villagersProfessionConfig.farmersHarvestMelons && isMelon(block)) {
                foundBlockCrop = true;
            }
            if (CONFIG.villagersProfessionConfig.farmersHarvestPumpkins && block instanceof PumpkinBlock) {
                foundBlockCrop = true;
            }

            if (foundBlockCrop) {
                serverWorld.destroyBlock(aboveFarmlandPos, true, villagerEntity);
                blockState = serverWorld.getBlockState(aboveFarmlandPos);
            }

            if (blockState.isAir() && block2 instanceof FarmBlock && villagerEntity.hasFarmSeeds()) {
                SimpleContainer simpleInventory = villagerEntity.getInventory();

                // If we don't know what to plant on a piece of farmland, look to nearby blocks to see if
                // we should plant the same item.
                if (CONFIG.villagersProfessionConfig.preferPlantSameCrop && preferredSeeds == null) {
                    for (BlockPos blockPos : BlockPos.betweenClosed(aboveFarmlandPos.offset(-4, 0, -4),
                            aboveFarmlandPos.offset(4, 1, 4))) {
                        BlockState possibleCropState = serverWorld.getBlockState(blockPos);
                        Block possibleCrop = possibleCropState.getBlock();
                        if (possibleCrop instanceof CropBlock || isGourd(possibleCrop) || possibleCrop instanceof StemBlock) {
                            preferredSeeds = getPreferredSeedsForCropBlock(possibleCrop);
                            if (preferredSeeds != null) {
                                break;
                            }
                        }
                    }
                }

                // First look for the preferred seed.
                boolean plantedPreferredSeeds = false;
                if (preferredSeeds != null) {
                    for (int i = 0; i < simpleInventory.getContainerSize(); ++i) {
                        ItemStack itemStack = simpleInventory.getItem(i);
                        if (!itemStack.isEmpty() && itemStack.is(preferredSeeds)) {
                            plantedPreferredSeeds = plantSeed(itemStack, i, serverWorld, villagerEntity);
                            if (plantedPreferredSeeds) {
                                break;
                            }
                        }
                    }
                }

                if (!plantedPreferredSeeds && (!CONFIG.villagersProfessionConfig.preferPlantSameCrop ||
                        preferredSeeds == null)) {
                    // Look for any seed to plant.
                    for (int i = 0; i < simpleInventory.getContainerSize(); ++i) {
                        ItemStack itemStack = simpleInventory.getItem(i);
                        // Plant pumpkin seeds over pumpkins if possible.
                        if (CONFIG.villagersProfessionConfig.farmersHarvestPumpkins && itemStack.is(Items.PUMPKIN)) {
                            if (simpleInventory.hasAnyOf(ImmutableSet.of(Items.PUMPKIN_SEEDS))) {
                                continue;
                            }
                        }
                        // Plant melon seeds over melon slices if possible.
                        if (CONFIG.villagersProfessionConfig.farmersHarvestPumpkins && itemStack.is(Items.MELON_SLICE)) {
                            if (simpleInventory.hasAnyOf(ImmutableSet.of(Items.MELON_SEEDS))) {
                                continue;
                            }
                        }
                        if (!itemStack.isEmpty()) {
                            if (plantSeed(itemStack, i, serverWorld, villagerEntity)) {
                                break;
                            }
                        }
                    }
                }
            }

            this.validFarmlandAroundVillager.remove(aboveFarmlandPos);
            this.aboveFarmlandPos = null;
        }

        if (this.aboveFarmlandPos == null) {
            this.aboveFarmlandPos = this.getValidFarmland(serverWorld);
        }

        if (this.aboveFarmlandPos != null) {
            this.nextOkStartTime = l + 20L;
            int completionRange = 0;
            if (isGourd(serverWorld.getBlockState(this.aboveFarmlandPos).getBlock())) {
                completionRange = 2;
            }
            villagerEntity.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(new BlockPosTracker(this.aboveFarmlandPos), 0.5F, completionRange));
            villagerEntity.getBrain()
                    .setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.aboveFarmlandPos));
        }

        this.timeWorkedSoFar++;
        cir.cancel();
    }

    private Item getPreferredSeedsForCropBlock(Block block) {
        if (CONFIG.villagersProfessionConfig.farmersHarvestMelons && block instanceof AttachedStemBlock) {
            ResourceKey<Block> blockKey = (ResourceKey<Block>)ReflectionHelper.getPrivateField(block, "fruit");

            if (blockKey.equals(Blocks.MELON)) {
                return Items.MELON_SEEDS;
            } else {
                return Items.PUMPKIN_SEEDS;
            }
        }

        if (block instanceof BeetrootBlock) {
            return Items.BEETROOT_SEEDS;
        } else if (block instanceof PotatoBlock) {
            return Items.POTATO;
        } else if (block instanceof CarrotBlock) {
            return Items.CARROT;
        } else if (CONFIG.villagersProfessionConfig.farmersHarvestMelons && isMelon(block)) {
            return Items.MELON_SEEDS;
        } else if (CONFIG.villagersProfessionConfig.farmersHarvestPumpkins && block instanceof PumpkinBlock) {
            return Items.PUMPKIN_SEEDS;
        } else if (block instanceof CropBlock) {
            return Items.WHEAT_SEEDS;
        }
        return null;
    }

    private boolean plantSeed(ItemStack itemStack, int stackIndex, ServerLevel serverWorld,
                              Villager villagerEntity) {
        BlockState blockState2;
        BlockPos aboveFarmlandPos = this.aboveFarmlandPos;
        if (itemStack.is(Items.WHEAT_SEEDS)) {
            blockState2 = Blocks.WHEAT.defaultBlockState();
        } else if (itemStack.is(Items.POTATO)) {
            blockState2 = Blocks.POTATOES.defaultBlockState();
        } else if (itemStack.is(Items.CARROT)) {
            blockState2 = Blocks.CARROTS.defaultBlockState();
        } else if (itemStack.is(Items.BEETROOT_SEEDS)) {
            blockState2 = Blocks.BEETROOTS.defaultBlockState();
        } else if (itemStack.is(Items.MELON_SEEDS) || itemStack.is(Items.MELON_SLICE)) {
            // Melon slices convert 1:1 to melon seeds.
            blockState2 = Blocks.MELON_STEM.defaultBlockState();
        } else if (itemStack.is(Items.PUMPKIN_SEEDS) || itemStack.is(Items.PUMPKIN)) {
            if (itemStack.is(Items.PUMPKIN)) {
                // Give the remaining seeds to the farmer, the fourth is used for planting.
                ItemStack pumpkinSeedItemStack = new ItemStack(Items.PUMPKIN_SEEDS, 3);
                villagerEntity.getInventory().addItem(pumpkinSeedItemStack);
            }
            blockState2 = Blocks.PUMPKIN_STEM.defaultBlockState();
        } else {
            return false;
        }

        serverWorld.setBlockAndUpdate(aboveFarmlandPos, blockState2);
        serverWorld.gameEvent(GameEvent.BLOCK_PLACE, aboveFarmlandPos,
                GameEvent.Context.of(villagerEntity, blockState2));

        serverWorld.playSound(null, aboveFarmlandPos.getX(), aboveFarmlandPos.getY(), aboveFarmlandPos.getZ(),
                SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
        itemStack.shrink(1);
        if (itemStack.isEmpty()) {
            villagerEntity.getInventory().setItem(stackIndex, ItemStack.EMPTY);
        }
        return true;
    }

    protected boolean replaceIsSuitableTarget(BlockPos pos, ServerLevel world, Villager villagerEntity) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();
        Block block2 = world.getBlockState(pos.below()).getBlock();
        if (CONFIG.villagersProfessionConfig.farmersHarvestMelons && isMelon(block)) {
            return true;
        }
        if (CONFIG.villagersProfessionConfig.farmersHarvestPumpkins && block instanceof PumpkinBlock) {
            return true;
        }
        return ((block instanceof CropBlock && ((CropBlock) block).isMaxAge(blockState)) ||
                (blockState.isAir() && block2 instanceof FarmBlock && villagerEntity.hasFarmSeeds()));
    }

    @Inject(method = "canStillUse", at = @At(value = "HEAD"), cancellable = true)
    protected void replaceShouldKeepRunning(ServerLevel serverWorld, Villager villagerEntity, long l,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (this.aboveFarmlandPos == null) {
            this.aboveFarmlandPos = this.getValidFarmland(serverWorld);
        }
        cir.setReturnValue(this.aboveFarmlandPos != null && this.timeWorkedSoFar < MAX_RUN_TIME);
        cir.cancel();
    }
}