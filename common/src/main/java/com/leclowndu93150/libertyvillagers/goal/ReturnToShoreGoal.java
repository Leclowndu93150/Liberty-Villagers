package com.leclowndu93150.libertyvillagers.goal;

import org.jetbrains.annotations.Nullable;


import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class ReturnToShoreGoal extends RandomStrollGoal {

        static final private int MAX_CHANCE = 12000;
        static final private int MIN_CHANCE = 120;

        @Nullable
        private Path path = null;

        public ReturnToShoreGoal(PathfinderMob pathAwareEntity, double speed) {
            super(pathAwareEntity, speed, MIN_CHANCE, false);
            this.forceTrigger = false;
        }

        @Override
        @Nullable
        protected Vec3 getPosition() {
            if (CONFIG.golemsConfig.golemMoveToShore) {
                if (this.mob.isInWater()) {
                    ServerLevel serverWorld = (ServerLevel) this.mob.level();
                    BlockPos blockPos = this.mob.blockPosition();
                    for (BlockPos blockPos2 : BlockPos.withinManhattan(blockPos, CONFIG.golemsConfig.golemPathfindToShoreRange,
                            CONFIG.golemsConfig.golemPathfindToShoreRange, CONFIG.golemsConfig.golemPathfindToShoreRange)) {
                        if (blockPos2.getX() == blockPos.getX() && blockPos2.getZ() == blockPos.getZ()) continue;
                        if (blockPos2.getY() < blockPos.getY()) continue;
                        BlockState blockState = serverWorld.getBlockState(blockPos2);
                        if (blockState.canOcclude()) {
                            continue;
                        }
                        if (!serverWorld.getFluidState(blockPos2).isEmpty()) {
                            continue;
                        }
                        BlockState blockStateUp = serverWorld.getBlockState(blockPos2.above());
                        if (blockStateUp.canOcclude()) {
                            continue;
                        }
                        BlockState blockStateDown = serverWorld.getBlockState(blockPos2.below());
                        if (!blockStateDown.canOcclude()) {
                            continue;
                        }
                        Path path = this.mob.getNavigation().createPath(blockPos2, 1);
                        if (path != null && path.getNodeCount() > 1 && path.canReach()) {
                            Vec3 dest = Vec3.atLowerCornerOf(blockPos2);
                            this.path = path;
                            this.setInterval(MIN_CHANCE);
                            return dest;
                        }
                    }
                    this.setInterval(MAX_CHANCE);
                }
            }
            return null;
        }

    @Override
    public void start() {
        if (this.path == null) {
            return;
        }
        this.mob.getNavigation().moveTo(this.path, this.speedModifier);
    }

    @Override
    public boolean canContinueToUse() {
            boolean shouldContinue = super.canContinueToUse();
            if (!shouldContinue && !this.path.isDone() && this.path.getNextNodeIndex() + 1 < this.path.getNodeCount()) {
                // Golem stuck on the edge.
                BlockPos pos = this.path.getNodePos(this.path.getNextNodeIndex() + 1);
                this.mob.randomTeleport(pos.getX(), pos.getY(), pos.getZ(), false);
                this.mob.getNavigation().moveTo(path, speedModifier);
                return true;
            }
            return shouldContinue;
    }
}
