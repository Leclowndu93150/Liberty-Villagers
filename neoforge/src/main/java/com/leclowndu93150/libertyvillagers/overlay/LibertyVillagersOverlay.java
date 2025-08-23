package com.leclowndu93150.libertyvillagers.overlay;

import com.leclowndu93150.libertyvillagers.cmds.VillagerInfo;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@OnlyIn(Dist.CLIENT)
public class LibertyVillagersOverlay {

    static int WHITE = 0xffffff;
    static int TEXT_PADDING = 2;
    static int BACKGROUND_PADDING = 2;
    static int BACKGROUND_COLOR = 0x55200000;

    public static void register() {
        NeoForge.EVENT_BUS.register(new LibertyVillagersOverlay());
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            return;
        }
        
        if (!CONFIG.debugConfig.enableVillagerInfoOverlay) {
            return;
        }

        GuiGraphics context = event.getGuiGraphics();
        DeltaTracker tickDelta = event.getPartialTick();
        
        Minecraft client = Minecraft.getInstance();
        HitResult hit = client.hitResult;
        List<Component> lines = null;
        ServerLevel world = null;
        if (client.hasSingleplayerServer()) {
            world = client.getSingleplayerServer().getLevel(client.level.dimension());
        }

        switch (hit.getType()) {
            case MISS:
                break;
            case BLOCK:
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos blockPos = blockHit.getBlockPos();
                BlockState blockState = client.level.getBlockState(blockPos);
                lines = VillagerInfo.getBlockInfo(world, blockPos, blockState);
                break;
            case ENTITY:
                EntityHitResult entityHit = (EntityHitResult) hit;
                Entity entity = entityHit.getEntity();
                if (client.hasSingleplayerServer()) {
                    entity = world.getEntity(entity.getUUID());
                }
                lines = VillagerInfo.getEntityInfo(world, entity);
                break;
        }

        if (lines != null) {
            Font renderer = client.font;
            MultiLineLabel multilineText = MultiLineLabel.create(renderer, lines.toArray(new Component[0]));

            int windowScaledWidth = client.getWindow().getGuiScaledWidth();
            int multilineWidth = multilineText.getWidth() + TEXT_PADDING;
            int x = windowScaledWidth - multilineWidth;
            int width = x + (multilineWidth / 2) - (BACKGROUND_PADDING / 2);

            int i = TEXT_PADDING;
            for (Component line : lines) {
                context.drawStringWithBackdrop(renderer, line, x, i, width, WHITE);

                i += renderer.lineHeight;
            }
        }
    }
}