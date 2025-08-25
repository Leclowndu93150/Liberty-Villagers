package com.leclowndu93150.libertyvillagers.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
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
    private static float fraction(int value, int max) {
        return 0;
    }

    @Unique
    private static void renderFishingLineAsLine(float x, float y, float z, VertexConsumer buffer,
                                                PoseStack.Pose matrices, float segmentStart, float segmentEnd) {
        float f = x * segmentStart;
        float g = y * (segmentStart * segmentStart + segmentStart) * 0.5f + 0.25f;
        float h = z * segmentStart;
        float i = x * segmentEnd - f;
        float j = y * (segmentEnd * segmentEnd + segmentEnd) * 0.5f + 0.25f - g;
        float k = z * segmentEnd - h;
        float l = Mth.sqrt(i * i + j * j + k * k);

        // Todo TEST if vertex is automatically consumed
        buffer.addVertex(matrices.pose(), f, g, h)
                .setColor(0, 0, 0, 255)
                .setNormal(matrices, i /= l, j /= l, k /= l);

        // Switching from line strip to line, so add doubles of all the intermediate points. 0->1, 1->2, 2->3.
        if ((segmentStart != 0) && (segmentStart != 1.0f)) {
            buffer.addVertex(matrices.pose(), f, g, h)
                    .setColor(0, 0, 0, 255)
                    .setNormal(matrices, i, j, k);
        }
    }

    @Unique
    private static void vertex(VertexConsumer buffer, PoseStack.Pose matrix, int light, float x, int y, int u, int v) {
        buffer.addVertex(matrix, x - 0.5f, (float) y - 0.5f, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(matrix, 0.0f, 1.0f, 0.0f);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
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

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void renderVillagerFishing(FishingHookRenderState renderState, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int packedLight, CallbackInfo ci) {
        if (!(renderState instanceof VillagerFishingRenderState villagerState) || !villagerState.isVillagerFishing) {
            return;
        }
        
        matrixStack.pushPose();
        matrixStack.pushPose();
        matrixStack.scale(0.5f, 0.5f, 0.5f);
        matrixStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        matrixStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        PoseStack.Pose pose = matrixStack.last();
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RENDER_TYPE);
        vertex(vertexConsumer, pose, packedLight, 0.0f, 0, 0, 1);
        vertex(vertexConsumer, pose, packedLight, 1.0f, 0, 1, 1);
        vertex(vertexConsumer, pose, packedLight, 1.0f, 1, 1, 0);
        vertex(vertexConsumer, pose, packedLight, 0.0f, 1, 0, 0);
        matrixStack.popPose();
        
        Vec3 offset = villagerState.villagerHandOffset;
        float x = (float) offset.x;
        float y = (float) offset.y;
        float z = (float) offset.z;
        VertexConsumer lineConsumer = vertexConsumerProvider.getBuffer(RenderType.lines());
        PoseStack.Pose linePose = matrixStack.last();
        for (int i = 0; i <= 16; ++i) {
            FishingBobberEntityRendererMixin.renderFishingLineAsLine(x, y, z, lineConsumer, linePose,
                    FishingBobberEntityRendererMixin.fraction(i, 16),
                    FishingBobberEntityRendererMixin.fraction(i + 1, 16));
        }
        matrixStack.popPose();
        super.render(renderState, matrixStack, vertexConsumerProvider, packedLight);
        ci.cancel();
    }
}
