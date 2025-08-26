package com.gitsh01.libertyvillagers.mixin;

import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.gitsh01.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(LandPathNodeMaker.class)
public abstract class LandPathNodeMakerMixin extends PathNodeMaker {

    private static final ThreadLocal<MobEntity> contextEntity = new ThreadLocal<>();

    @Inject(method = "getNodeType(Lnet/minecraft/world/BlockView;IIILnet/minecraft/entity/mob/MobEntity;)Lnet/minecraft/entity/ai/pathing/PathNodeType;", at = @At("HEAD"))
    public void captureEntity(BlockView world, int x, int y, int z, MobEntity mob, CallbackInfoReturnable<PathNodeType> cir) {
        contextEntity.set(mob);
    }

    @Inject(method = "getNodeType(Lnet/minecraft/world/BlockView;IIILnet/minecraft/entity/mob/MobEntity;)Lnet/minecraft/entity/ai/pathing/PathNodeType;", at = @At("RETURN"))
    public void clearEntity(BlockView world, int x, int y, int z, MobEntity mob, CallbackInfoReturnable<PathNodeType> cir) {
        contextEntity.remove();
    }

    @Inject(method = "getCommonNodeType", at = @At("HEAD"), cancellable = true)
    private static void getCommonNodeType(BlockView world, BlockPos pos, CallbackInfoReturnable<PathNodeType> cir) {
        MobEntity mob = contextEntity.get();
        if (mob != null && ((mob.getType() == EntityType.VILLAGER) || (mob.getType() == EntityType.IRON_GOLEM))) {
            BlockState blockState = world.getBlockState(pos);
            Block block = blockState.getBlock();
            if (block instanceof AzaleaBlock) {
                cir.setReturnValue(PathNodeType.LEAVES);
                cir.cancel();
            }
            if (block instanceof StairsBlock) {
                cir.setReturnValue(PathNodeType.BLOCKED);
                cir.cancel();
            }
            if (CONFIG.villagerPathfindingConfig.villagersAvoidGlassPanes && block instanceof PaneBlock) {
                cir.setReturnValue(PathNodeType.BLOCKED);
                cir.cancel();
            }
        }
    }
}
