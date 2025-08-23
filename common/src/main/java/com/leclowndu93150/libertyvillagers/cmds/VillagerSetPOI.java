package com.leclowndu93150.libertyvillagers.cmds;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.Optional;

import static net.minecraft.commands.Commands.literal;

public class VillagerSetPOI {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("villagersetpoi").executes(context -> {
                    processVillagerSetPOI(context);
                    return 1;
                })));
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> dispatcher.register(literal("vsp").executes(context -> {
                    processVillagerSetPOI(context);
                    return 1;
                })));
    }

    public static void processVillagerSetPOI(CommandContext<CommandSourceStack> command) {
        CommandSourceStack source = command.getSource();
        ServerPlayer player = source.getPlayer();
        ServerLevel serverWorld = source.getLevel();

        float maxDistance = 50;
        float tickDelta = 0;
        HitResult hit = player.pick(maxDistance, tickDelta, false);

        switch (hit.getType()) {
            case MISS -> player.sendSystemMessage(Component.translatable("text.LibertyVillagers.villagerSetPOI.miss"));
            case BLOCK -> {
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos blockPos = blockHit.getBlockPos();
                BlockState blockState = serverWorld.getBlockState(blockPos);
                handleBlockHit(player, serverWorld, blockPos, blockState);
            }
            case ENTITY -> player.sendSystemMessage(Component.translatable("text.LibertyVillagers.villagerSetPOI.entity"));
        }
    }

    protected static void handleBlockHit(ServerPlayer player, ServerLevel serverWorld, BlockPos blockPos,
                                         BlockState blockState) {
        Block block = blockState.getBlock();
        Component name = block.getName();

        Optional<Holder<PoiType>> optionalRegistryEntry =
                PoiTypes.forState(blockState);
        if (optionalRegistryEntry.isEmpty()) {
            player.sendSystemMessage(Component.translatable("text.LibertyVillagers.villagerSetPOI.notPOIType", name));
            return;
        }

        Optional<ResourceKey<PoiType>> optionalRegistryKey = optionalRegistryEntry.get().unwrapKey();
        if (optionalRegistryKey.isEmpty()) {
            player.sendSystemMessage(Component.translatable("text.LibertyVillagers.villagerSetPOI.notPOIType", name));
            return;
        }

        String poiTypeName = optionalRegistryKey.get().location().toString();
        PoiManager storage = serverWorld.getPoiManager();

        if (!storage.existsAtPosition(optionalRegistryKey.get(), blockPos)) {
            storage.add(blockPos, optionalRegistryEntry.get());
            DebugPackets.sendPoiAddedPacket(serverWorld, blockPos);
            player.sendSystemMessage(Component.translatable("text.LibertyVillagers.villagerSetPOI.enable", name, poiTypeName));
        } else {
            storage.remove(blockPos);
            DebugPackets.sendPoiRemovedPacket(serverWorld, blockPos);
            player.sendSystemMessage(Component.translatable("text.LibertyVillagers.villagerSetPOI.disable", name, poiTypeName));
        }
    }
}
