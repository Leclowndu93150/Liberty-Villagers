package com.leclowndu93150.libertyvillagers.cmds;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;


public class VillagerInfo {

    final static String BLANK_COORDS = "                 ";


    public static void processVillagerInfo(CommandContext<CommandSourceStack> command) {
        CommandSourceStack source = command.getSource();
        ServerPlayer player = source.getPlayer();
        ServerLevel serverWorld = source.getLevel();

        float maxDistance = 50;
        float tickDelta = 0;
        Vec3 vec3d = player.getEyePosition(tickDelta);
        Vec3 vec3d2 = player.getViewVector(tickDelta);
        Vec3 vec3d3 = vec3d.add(vec3d2.x * maxDistance, vec3d2.y * maxDistance, vec3d2.z * maxDistance);
        HitResult hit = serverWorld.clip(
                new ClipContext(vec3d, vec3d3, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                        player));

        // Found a block between us and the max distance, update the max distance for the entity check.
        if (hit.getType() != HitResult.Type.MISS) {
            vec3d3 = hit.getLocation();
        }

        HitResult hitResult2;
        // Look for an entity between us and the block.
        if ((hitResult2 = ProjectileUtil.getEntityHitResult(serverWorld, player, vec3d2, vec3d3,
                player.getBoundingBox().expandTowards(player.getDeltaMovement()).inflate(maxDistance), Entity::isAlive)) != null) {
            hit = hitResult2;
        }

        List<Component> lines = null;
        switch (hit.getType()) {
            case MISS:
                break;
            case BLOCK:
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos blockPos = blockHit.getBlockPos();
                BlockState blockState = serverWorld.getBlockState(blockPos);
                if (blockState != null) {
                    lines = getBlockInfo(serverWorld, blockPos, blockState);
                }
                break;
            case ENTITY:
                EntityHitResult entityHit = (EntityHitResult) hit;
                Entity entity = entityHit.getEntity();
                if (entity != null) {
                    lines = getEntityInfo(serverWorld, entity);
                }
                break;
        }

        if (lines != null) {
            for (Component line : lines) {
                player.sendSystemMessage(line);
            }
        }
    }


    public static List<Component> getEntityInfo(ServerLevel serverWorld, Entity entity) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.title"));
        if (entity == null) {
            return lines;
        }
        Component name = entity.getDisplayName();
        lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.name", name));

        if (!(entity instanceof Villager)) {
            return lines;
        }

        Villager villager = (Villager)entity;
        String occupation =
                VillagerStats.translatedProfession(villager.getVillagerData().getProfession());
        lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.occupation", occupation));

        // Client-side villagers don't have memories.
        if (serverWorld == null) {
            lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.needsServer"));
            return lines;
        }

        Optional<GlobalPos> home = villager.getBrain().getMemoryInternal(MemoryModuleType.HOME);
        String homeCoords = home.isPresent() ? home.get().pos().toShortString() : BLANK_COORDS;
        lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.home", homeCoords));

        Optional<GlobalPos> jobSite = villager.getBrain().getMemoryInternal(MemoryModuleType.JOB_SITE);
        String jobSiteCoords = jobSite.isPresent() ? jobSite.get().pos().toShortString() : BLANK_COORDS;
        lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.jobSite", jobSiteCoords));

        Optional<GlobalPos> potentialJobSite =
                villager.getBrain().getMemoryInternal(MemoryModuleType.POTENTIAL_JOB_SITE);
        String potentialJobSiteCoords =
                potentialJobSite.isPresent() ? potentialJobSite.get().pos().toShortString() : BLANK_COORDS;
        lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.potentialJobSite", potentialJobSiteCoords));

        Optional<GlobalPos> meetingPoint = villager.getBrain().getMemoryInternal(MemoryModuleType.MEETING_POINT);
        String meetingPointCoords =
                meetingPoint.isPresent() ? meetingPoint.get().pos().toShortString() : BLANK_COORDS;
        lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.meetingPoint", meetingPointCoords));

        Optional<WalkTarget> walkTarget = villager.getBrain().getMemoryInternal(MemoryModuleType.WALK_TARGET);
        String walkTargetCoords =
                walkTarget.isPresent() ? walkTarget.get().getTarget().currentBlockPosition().toShortString() : BLANK_COORDS;
        lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.walkTarget", walkTargetCoords,
                walkTarget.map(WalkTarget::getCloseEnoughDist).orElse(0)));

        lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.inventory"));

        if (villager.getInventory().isEmpty()) {
            lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.empty"));
        } else {
            for (ItemStack stack : villager.getInventory().getItems()) {
                if (!stack.isEmpty()) {
                    lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.inventoryLine", stack.getCount(),
                            stack.getHoverName()));
                }
            }
        }

        if (villager.getNavigation().getPath() != null && CONFIG.debugConfig.villagerInfoShowsPath) {
            lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.path"));
            Path path = villager.getNavigation().getPath();
            for (int i = path.getNextNodeIndex(); i < path.getNodeCount(); i++) {
                lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.pathnode", i,
                        path.getNode(i).asBlockPos().toShortString()));
            }
        }

        return lines;
    }

    public static List<Component> getBlockInfo(ServerLevel serverWorld, BlockPos blockPos, BlockState blockState) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.title"));
        Block block = blockState.getBlock();
        if (block == null) {
            return lines;
        }
        Component name = block.getName();
        lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.name", name));

        if (serverWorld == null) {
            lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.needsServer"));
            return lines;
        }
        if (block instanceof BeehiveBlock) {
            BlockEntity blockEntity = serverWorld.getBlockEntity(blockPos);
            if (blockEntity instanceof BeehiveBlockEntity) {
                BeehiveBlockEntity beehiveBlockEntity = (BeehiveBlockEntity) blockEntity;
                int numBees = beehiveBlockEntity.getOccupantCount();
                lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.numBees", numBees));
            }

            int numHoney = blockState.getAnalogOutputSignal(serverWorld, blockPos);
            lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.numHoney", numHoney));
        }

        Optional<Holder<PoiType>> optionalRegistryEntry =
                PoiTypes.forState(blockState);
        if (optionalRegistryEntry.isEmpty()) {
            lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.poiType",
                    Component.translatable("text.LibertyVillagers.villagerInfo.none")));
            return lines;
        }

        PoiType poiType = optionalRegistryEntry.get().value();
        Optional<ResourceKey<PoiType>> optionalRegistryKey = optionalRegistryEntry.get().unwrapKey();
        if (optionalRegistryKey.isEmpty()) {
            lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.poiType",
                    Component.translatable("text.LibertyVillagers.villagerInfo.none")));
            return lines;
        }

        String poiTypeName = optionalRegistryKey.get().location().toString();

        lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.poiType", poiTypeName));

        PoiManager storage = serverWorld.getPoiManager();
        if (!storage.existsAtPosition(optionalRegistryKey.get(), blockPos)) {
            lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.notAPOI"));
            return lines;
        }

        @SuppressWarnings("deprecation")
        int freeTickets = storage.getFreeTickets(blockPos);
        Component isOccupied =
                freeTickets < poiType.maxTickets() ? Component.translatable("text.LibertyVillagers.villagerInfo.true") :
                        Component.translatable("text" + ".LibertyVillagers.villagerInfo.false");
        lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.isOccupied", isOccupied));
        lines.add(Component.translatable("text.LibertyVillagers.villagerInfo.freeTickets", freeTickets,
                poiType.maxTickets()));

        return lines;

    }
}