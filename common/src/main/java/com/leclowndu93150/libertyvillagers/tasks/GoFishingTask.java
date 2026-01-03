package com.leclowndu93150.libertyvillagers.tasks;

import com.leclowndu93150.libertyvillagers.mixin.FishingBobberEntityAccessorMixin;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;


import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

public class GoFishingTask extends Behavior<Villager> {

    private static final int MAX_RUN_TIME = 40 * 20;
    private static final int TURN_TIME = 3 * 20;

    // Give the villager time to look at the water, so they aren't throwing the bobber behind their heads.
    private long bobberCountdown;

    private boolean hasThrownBobber = false;

    @Nullable
    private BlockPos targetBlockPos;

    @Nullable
    private FishingHook bobber = null;

    public GoFishingTask() {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.JOB_SITE,
                MemoryStatus.VALUE_PRESENT), MAX_RUN_TIME);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel serverWorld, Villager villagerEntity) {
        // This just looks wrong.
        if (villagerEntity.isInWater()) {
            return false;
        }

        // Look for water nearby.
        BlockPos villagerPos = villagerEntity.blockPosition();
        for (BlockPos blockPos : BlockPos.withinManhattan(villagerPos, CONFIG.villagersProfessionConfig.fishermanFishingWaterRange,
                CONFIG.villagersProfessionConfig.fishermanFishingWaterRange, CONFIG.villagersProfessionConfig.fishermanFishingWaterRange)) {
            // Don't fish "up".
            if (blockPos.getY() > villagerPos.getY()) continue;
            // Don't fish on ourselves (it looks odd).
            if (blockPos.closerThan(villagerPos, 1)) continue;
            if (serverWorld.getBlockState(blockPos).getFluidState().isSource() &&
                    serverWorld.getBlockState(blockPos.above()).is(Blocks.AIR)) {
                Vec3 bobberStartPosition = getBobberStartPosition(villagerEntity, blockPos);

                // Make sure the bobber won't be starting in a solid wall of a boat.
                if (serverWorld.getBlockState(BlockPos.containing(bobberStartPosition)).canOcclude()) {
                    continue;
                }

                Vec3 centerBlockPos = Vec3.atCenterOf(blockPos);
                // Ray trace to see if the villager can actually fish on that spot.
                // Use the lower edge of the bobber since it seems to get caught on the floor first.
                AABB box = EntityType.FISHING_BOBBER.getDimensions().makeBoundingBox(bobberStartPosition);
                Vec3 lowerEdge = new Vec3(0, -1 * box.getYsize() / 2, 0);
                if (doesNotHitValidWater(bobberStartPosition, lowerEdge, centerBlockPos, villagerEntity, serverWorld)) {
                    continue;
                }

                // Next, look for an entity between us and the block that the bobber might hit to avoid fishing
                // through buddies.
                if (ProjectileUtil.getEntityHitResult(villagerEntity, bobberStartPosition, centerBlockPos,
                        box, Entity::isAlive, Double.MAX_VALUE) != null) {
                    // We're going to hit someone.
                    continue;
                }

                // Now check if the lower right or lower left are going to hit something (like that fence)....
                Vec3 lowerLeftEdge = new Vec3(-1 * box.getYsize() / 2, -1 * box.getYsize() / 2, 0);
                if (doesNotHitValidWater(bobberStartPosition, lowerLeftEdge, centerBlockPos, villagerEntity,
                        serverWorld)) {
                    continue;
                }
                Vec3 lowerRightEdge = new Vec3(1 * box.getXsize() / 2, -1 * box.getXsize() / 2, 0);
                if (doesNotHitValidWater(bobberStartPosition, lowerRightEdge, centerBlockPos, villagerEntity,
                        serverWorld)) {
                    continue;
                }

                // This check is expensive, so stop on the first one we find that works, instead of looking
                // for more.
                targetBlockPos = blockPos;
                return true;
            }
        }

        return false;
    }

    @Override
    protected void start(ServerLevel serverWorld, Villager villagerEntity, long time) {
        villagerEntity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
        villagerEntity.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        villagerEntity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(targetBlockPos.above()));
        bobberCountdown = TURN_TIME + time;
    }

    @Override
    protected boolean canStillUse(ServerLevel world, Villager entity, long time) {
        // Villager dropped the fishing rod for some reason.
        if (!entity.getMainHandItem().is(Items.FISHING_ROD)) {
            return false;
        }

        if (!this.hasThrownBobber) {
            return true;
        }

        if (bobber == null || bobber.isRemoved() ||
                (bobber.onGround() && !bobber.getInBlockState().is(Blocks.WATER))) {
            return false;
        }

        // Still initilizing...
        if (bobber.getEntityData() == null) {
            return true;
        }

        boolean caughtFish = bobber.getEntityData().get(FishingBobberEntityAccessorMixin.getCaughtFish());
        return !caughtFish;
    }

    @Override
    protected void tick(ServerLevel serverWorld, Villager villagerEntity, long time) {
        if (!hasThrownBobber && time > bobberCountdown) {
            throwBobber(villagerEntity, serverWorld);
        }
    }

    @Override
    protected void stop(ServerLevel serverWorld, Villager villagerEntity, long l) {
        villagerEntity.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        villagerEntity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        if (bobber != null) {
            bobber.retrieve(ItemStack.EMPTY);
            bobber = null;
        }
        // Remove fishing pole.
        villagerEntity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.hasThrownBobber = false;
        this.bobberCountdown = 0;
    }

    Vec3 getBobberStartPosition(Villager thrower, BlockPos targetBlockPos) {
        Vec3 targetPosition = Vec3.atCenterOf(targetBlockPos);
        double d = targetPosition.x - thrower.getX();
        double f = targetPosition.z - thrower.getZ();

        double x = thrower.getX() + (d * 0.3);
        double y = thrower.getEyeY();
        double z = thrower.getZ() + (f * 0.3);
        return new Vec3(x, y, z);
    }

    void throwBobber(Villager thrower, ServerLevel serverWorld) {
        bobber = new FishingHook(EntityType.FISHING_BOBBER, serverWorld);
        bobber.setOwner(thrower);

        Vec3 bobberStartPosition = getBobberStartPosition(thrower, targetBlockPos);

        bobber.snapTo(bobberStartPosition.x, bobberStartPosition.y, bobberStartPosition.z,
                thrower.getYRot(),
                thrower.getXRot());

        Vec3 targetPosition = Vec3.atCenterOf(targetBlockPos);
        double d = targetPosition.x - bobberStartPosition.x;
        double e = targetPosition.y - bobberStartPosition.y;
        double f = targetPosition.z - bobberStartPosition.z;
        double g = 0.1;

        Vec3 vec3d = new Vec3(d * g, e * g, f * g);

        bobber.setDeltaMovement(vec3d);
        bobber.setYRot((float) (Mth.atan2(vec3d.x, vec3d.z) * 57.2957763671875));
        bobber.setXRot((float) (Mth.atan2(vec3d.y, vec3d.horizontalDistance()) * 57.2957763671875));
        bobber.yRotO = bobber.getYRot();
        bobber.xRotO = bobber.getXRot();

        serverWorld.addFreshEntity(bobber);
        serverWorld.playSound(null, thrower.getX(), thrower.getY(), thrower.getZ(),
                SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5f,
                0.4f / (serverWorld.getRandom().nextFloat() * 0.4f + 0.8f));
        hasThrownBobber = true;
    }

    boolean doesNotHitValidWater(Vec3 bobberStartPosition, Vec3 bobberEdge, Vec3 centerBlockPos,
                                 Villager villagerEntity, ServerLevel serverWorld) {
        BlockHitResult hitResult = serverWorld.clip(
                new ClipContext(bobberStartPosition.add(bobberEdge),
                        centerBlockPos,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.ANY,
                        villagerEntity));

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockState blockState = serverWorld.getBlockState(hitResult.getBlockPos());
            return !blockState.is(Blocks.WATER) || !blockState.getFluidState().isSource();
        }
        return true;
    }
}
