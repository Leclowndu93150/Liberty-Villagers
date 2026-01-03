package com.leclowndu93150.libertyvillagers.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;
import com.leclowndu93150.libertyvillagers.util.VillagerFishingRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHookRenderer.class)
public abstract class FishingBobberEntityRendererMixin extends EntityRenderer<FishingHook, FishingHookRenderState> {

    @Final
    @Shadow
    private static RenderType RENDER_TYPE;

    public FishingBobberEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Shadow
    private static float fraction(int i, int steps) {
        return 0;
    }

    @Unique
    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int lightCoords, float x, int y, int u, int v) {
        buffer.addVertex(pose, x - 0.5f, (float) y - 0.5f, 0.0f)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightCoords)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }

    @Unique
    private static void stringVertex(float xa, float ya, float za, VertexConsumer buffer, PoseStack.Pose pose, float aa, float nexta, float width) {
        float x = xa * aa;
        float y = ya * (aa * aa + aa) * 0.5f + 0.25f;
        float z = za * aa;
        float nx = xa * nexta - x;
        float ny = ya * (nexta * nexta + nexta) * 0.5f + 0.25f - y;
        float nz = za * nexta - z;
        float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= length;
        ny /= length;
        nz /= length;
        buffer.addVertex(pose, x, y, z).setColor(-16777216).setNormal(pose, nx, ny, nz).setLineWidth(width);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/projectile/FishingHook;Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;F)V", at = @At("TAIL"))
    public void extractVillagerFishingState(FishingHook entity, FishingHookRenderState renderState, float partialTick, CallbackInfo ci) {
        if (renderState instanceof VillagerFishingRenderState villagerState && entity.getOwner() != null && entity.getOwner().getType() == EntityType.VILLAGER) {
            Villager villager = (Villager) entity.getOwner();
            villagerState.isVillagerFishing = true;
            float bodyRot = Mth.lerp(partialTick, villager.yBodyRotO, villager.yBodyRot) * ((float) Math.PI / 180);
            double sin = Mth.sin(bodyRot);
            double cos = Mth.cos(bodyRot);
            double handOffset = 0.4;
            double handX = Mth.lerp(partialTick, villager.xo, villager.getX()) - sin * handOffset;
            double handY = villager.yo + villager.getEyeHeight() + (villager.getY() - villager.yo) * partialTick - 0.45;
            double handZ = Mth.lerp(partialTick, villager.zo, villager.getZ()) + cos * handOffset;
            Vec3 hookPos = entity.getPosition(partialTick).add(0.0, 0.25, 0.0);
            villagerState.villagerHandOffset = new Vec3(handX - hookPos.x, handY - hookPos.y, handZ - hookPos.z);
        }
    }

    @Inject(method = "createRenderState", at = @At("HEAD"), cancellable = true)
    public void createVillagerRenderState(CallbackInfoReturnable<FishingHookRenderState> cir) {
        cir.setReturnValue(new VillagerFishingRenderState());
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    public void submitVillagerFishing(FishingHookRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        if (!(renderState instanceof VillagerFishingRenderState villagerState) || !villagerState.isVillagerFishing) {
            return;
        }

        poseStack.pushPose();
        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 0.5f);
        poseStack.mulPose(camera.orientation);
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            vertex(buffer, pose, renderState.lightCoords, 0.0f, 0, 0, 1);
            vertex(buffer, pose, renderState.lightCoords, 1.0f, 0, 1, 1);
            vertex(buffer, pose, renderState.lightCoords, 1.0f, 1, 1, 0);
            vertex(buffer, pose, renderState.lightCoords, 0.0f, 1, 0, 0);
        });
        poseStack.popPose();

        Vec3 offset = villagerState.villagerHandOffset;
        float xa = (float) offset.x;
        float ya = (float) offset.y;
        float za = (float) offset.z;
        float width = Minecraft.getInstance().getWindow().getAppropriateLineWidth();
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
            for (int i = 0; i < 16; i++) {
                float a0 = fraction(i, 16);
                float a1 = fraction(i + 1, 16);
                stringVertex(xa, ya, za, buffer, pose, a0, a1, width);
                stringVertex(xa, ya, za, buffer, pose, a1, a0, width);
            }
        });
        poseStack.popPose();
        super.submit(renderState, poseStack, submitNodeCollector, camera);
        ci.cancel();
    }
}
