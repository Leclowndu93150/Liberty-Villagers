package com.leclowndu93150.libertyvillagers.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

@Mixin(FishingHook.class)
public abstract class FishingBobberEntityMixin extends Projectile {

    // Why do I get mixin errors when I use my own enum? Why can't I use FishingBobberEntity's enum? :(
    boolean isFlying = true;
    boolean isBobbing = false;

    @Shadow
    private int life;
    @Final
    @Shadow
    private RandomSource syncronizedRandom;
    @Shadow
    private boolean biting;
    @Shadow
    private int outOfWaterTime;
    @Shadow
    private int nibble;
    @Shadow
    private int timeUntilHooked;
    @Shadow
    private boolean openWater = true;

    public FishingBobberEntityMixin(Level world) {
        super(EntityType.FISHING_BOBBER, world);
    }

    @Shadow
    protected abstract void checkCollision();

    @Shadow
    protected abstract void catchingFish(BlockPos pos);

    @Shadow
    protected abstract boolean calculateOpenWater(BlockPos pos);

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    void tickForVillagerOwnedBobber(CallbackInfo ci) {
        if (this.getOwner() == null || this.getOwner().getType() != EntityType.VILLAGER) {
            return;
        }

        if (!this.level().isClientSide && this.removeIfInvalidOwner()) {
            ci.cancel();
            return;
        }

        if (this.onGround()) {
            ++this.life;
            if (this.life >= 1200) {
                this.discard();
                ci.cancel();
                return;
            }
        } else {
            this.life = 0;
        }
        float f = 0.0f;
        BlockPos blockPos = this.blockPosition();
        FluidState fluidState = this.level().getFluidState(blockPos);
        if (fluidState.is(FluidTags.WATER)) {
            f = fluidState.getHeight(this.level(), blockPos);
        }
        boolean bl = f > 0.0f;
        if (isFlying) {
            if (bl) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.3, 0.3, 0.3));
                isFlying = false;
                isBobbing = true;
                ci.cancel();
                return;
            }
            this.checkCollision();
        } else {
            if (isBobbing) {
                Vec3 vec3d = this.getDeltaMovement();
                double d = this.getY() + vec3d.y - (double) blockPos.getY() - (double) f;
                if (Math.abs(d) < 0.01) {
                    d += Math.signum(d) * 0.1;
                }
                this.setDeltaMovement(vec3d.x * 0.9, vec3d.y - d * (double) this.random.nextFloat() * 0.2, vec3d.z * 0.9);
                this.openWater = this.nibble <= 0 && this.timeUntilHooked <= 0 ||
                        this.openWater && this.outOfWaterTime < 10 && this.calculateOpenWater(blockPos);
                if (bl) {
                    this.outOfWaterTime = Math.max(0, this.outOfWaterTime - 1);
                    if (this.biting) {
                        this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.1 * (double) this.syncronizedRandom.nextFloat() *
                                (double) this.syncronizedRandom.nextFloat(), 0.0));
                    }
                    if (!this.level().isClientSide) {
                        this.catchingFish(blockPos);
                    }
                } else {
                    this.outOfWaterTime = Math.min(10, this.outOfWaterTime + 1);
                }
            }
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.updateRotation();
        if (isFlying && (this.onGround() || this.horizontalCollision)) {
            this.setDeltaMovement(Vec3.ZERO);
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(0.92));
        this.reapplyPosition();
        ci.cancel();
    }

    private boolean removeIfInvalidOwner() {
        Entity owner = this.getOwner();
        if (owner == null || !owner.isAlive()) {
            this.discard();
            return true;
        }
        Villager villager = (Villager) owner;
        ItemStack itemStack = villager.getMainHandItem();
        boolean bl = itemStack.is(Items.FISHING_ROD);
        if (!bl || this.distanceToSqr(villager) > (CONFIG.villagersProfessionConfig.fishermanFishingWaterRange *
                CONFIG.villagersProfessionConfig.fishermanFishingWaterRange)) {
            this.discard();
            return true;
        }
        return false;
    }

    @Inject(method = "recreateFromPacket", at = @At("HEAD"),
            cancellable = true)
    public void onSpawnPacket(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        super.recreateFromPacket(packet);
        ci.cancel();
    }

    @Inject(method = "retrieve", at = @At("HEAD"), cancellable = true)
    public void use(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        if (this.level().isClientSide || this.getOwner() == null || this.getOwner().getType() != EntityType.VILLAGER) {
            return;
        }
        Villager villager = (Villager) this.getOwner();
        int i = 0;
        if (this.nibble > 0) {
            Item fish = this.level().getRandom().nextInt(2) == 0 ? Items.COD : Items.SALMON;
            ItemStack itemStack = new ItemStack(fish);
            ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), itemStack);
            double d = villager.getX() - this.getX();
            double e = villager.getY() - this.getY();
            double f = villager.getZ() - this.getZ();
            itemEntity.setDeltaMovement(d * 0.1, e * 0.1 + Math.sqrt(Math.sqrt(d * d + e * e + f * f)) * 0.08, f * 0.1);
            this.level().addFreshEntity(itemEntity);
            i = 1;
        }
        if (this.onGround()) {
            i = 2;
        }
        this.discard();
        cir.setReturnValue(i);
        cir.cancel();
    }

    @Inject(method = "onHitEntity",
            at = @At("HEAD"),
            cancellable = true)
    protected void onEntityHit(EntityHitResult entityHitResult, CallbackInfo ci) {
        if (this.getOwner() != null && this.getOwner().getType() == EntityType.VILLAGER) {
            // Don't "hook" entities.
            super.onHitEntity(entityHitResult);
            ci.cancel();
        }
    }
}
