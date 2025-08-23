package com.leclowndu93150.libertyvillagers.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AzaleaBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(WalkNodeEvaluator.class)
public abstract class LandPathNodeMakerMixin extends NodeEvaluator {

    private static Mob lastUsedEntity;

    @Inject(method = "getPathTypeFromState", at = @At("HEAD"), cancellable = true)
    private static void getCommonNodeType(BlockGetter world, BlockPos pos, CallbackInfoReturnable<PathType> cir) {
        if (lastUsedEntity != null && ((lastUsedEntity.getType() == EntityType.VILLAGER) ||
                (lastUsedEntity.getType() == EntityType.IRON_GOLEM))) {
            BlockState blockState = world.getBlockState(pos);
            Block block = blockState.getBlock();
            if (block instanceof AzaleaBlock) {
                cir.setReturnValue(PathType.LEAVES);
                cir.cancel();
            }
            if (block instanceof StairBlock) {
                cir.setReturnValue(PathType.BLOCKED);
                cir.cancel();
            }
            if (CONFIG.villagerPathfindingConfig.villagersAvoidGlassPanes && block instanceof IronBarsBlock) {
                cir.setReturnValue(PathType.BLOCKED);
                cir.cancel();
            }
        }
    }

    @Inject(method = "getPathType", at = @At("HEAD"))
    public void getDefaultNodeType(PathfindingContext context, int x, int y, int z, CallbackInfoReturnable<PathType> cir) {
        lastUsedEntity = this.mob;
    }
}
