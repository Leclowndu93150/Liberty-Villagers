package com.leclowndu93150.libertyvillagers.cmds;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import java.util.List;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;
import static net.minecraft.commands.Commands.literal;

public class VillagerReset {

    public static void processVillagerReset(CommandContext<CommandSourceStack> command) {
        CommandSourceStack source = command.getSource();
        ServerPlayer player = source.getPlayer();
        ServerLevel serverWorld = source.getLevel();

        List<Villager> villagers = serverWorld.getEntitiesOfClass(Villager.class,
                player.getBoundingBox().inflate(CONFIG.debugConfig.villagerStatRange));

        for (Villager villager : villagers) {
            villager.releasePoi(MemoryModuleType.JOB_SITE);
            villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
            villager.releasePoi(MemoryModuleType.MEETING_POINT);
            villager.getBrain().eraseMemory(MemoryModuleType.MEETING_POINT);
            villager.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
            villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
        }

        player.sendSystemMessage(Component.translatable("text.LibertyVillagers.villagerreset"));
    }
}
