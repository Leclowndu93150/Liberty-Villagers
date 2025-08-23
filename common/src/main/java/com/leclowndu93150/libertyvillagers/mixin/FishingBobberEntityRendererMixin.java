package com.leclowndu93150.libertyvillagers.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHookRenderer.class)
public abstract class FishingBobberEntityRendererMixin extends EntityRenderer<FishingHook> {

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

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(FishingHook fishingBobberEntity, float f, float g, PoseStack matrixStack,
                       MultiBufferSource vertexConsumerProvider, int i, CallbackInfo ci) {
        double s;
        double q;
        double p;
        double o;
        if (fishingBobberEntity.getOwner() == null) {
            return;
        }
        if (fishingBobberEntity.getOwner().getType() != EntityType.VILLAGER) {
            return;
        }
        Villager villager = (Villager) fishingBobberEntity.getOwner();
        matrixStack.pushPose();
        matrixStack.pushPose();
        matrixStack.scale(0.5f, 0.5f, 0.5f);
        matrixStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        matrixStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        PoseStack.Pose entry = matrixStack.last();
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RENDER_TYPE);
        vertex(vertexConsumer, entry, i, 0.0f, 0, 0, 1);
        vertex(vertexConsumer, entry, i, 1.0f, 0, 1, 1);
        vertex(vertexConsumer, entry, i, 1.0f, 1, 1, 0);
        vertex(vertexConsumer, entry, i, 0.0f, 1, 0, 0);
        matrixStack.popPose();
        float l = Mth.lerp(g, villager.yBodyRotO, villager.yBodyRot) * ((float) Math.PI / 180);
        double d = Mth.sin(l);
        double e = Mth.cos(l);
        double n = 0.4;
        o = Mth.lerp(g, villager.xo, villager.getX()) - d * n;
        p = villager.yo + (double) villager.getEyeHeight() +
                (villager.getY() - villager.yo) * (double) g - 0.45;
        q = Mth.lerp(g, villager.zo, villager.getZ()) + e * n;

        s = Mth.lerp(g, fishingBobberEntity.xo, fishingBobberEntity.getX());
        double t = Mth.lerp(g, fishingBobberEntity.yo, fishingBobberEntity.getY()) + 0.25;
        double u = Mth.lerp(g, fishingBobberEntity.zo, fishingBobberEntity.getZ());
        float v = (float) (o - s);
        float w = (float) (p - t);
        float x = (float) (q - u);
        VertexConsumer vertexConsumer2 = vertexConsumerProvider.getBuffer(RenderType.lines());
        PoseStack.Pose entry2 = matrixStack.last();
        for (int z = 0; z <= 16; ++z) {
            // There's a bug in 1.19.3 which causes the line strips to not properly end when the layer is
            // switched, which means a line is drawn from the end of one fishing line to the next fishing line.
            FishingBobberEntityRendererMixin.renderFishingLineAsLine(v, w, x, vertexConsumer2, entry2,
                    FishingBobberEntityRendererMixin.fraction(z, 16),
                    FishingBobberEntityRendererMixin.fraction(z + 1, 16));
        }
        matrixStack.popPose();
        super.render(fishingBobberEntity, f, g, matrixStack, vertexConsumerProvider, i);
        ci.cancel();
    }
}
