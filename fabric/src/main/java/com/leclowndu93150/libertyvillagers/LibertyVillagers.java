package com.leclowndu93150.libertyvillagers;

import com.leclowndu93150.libertyvillagers.cmds.VillagerInfo;
import com.leclowndu93150.libertyvillagers.cmds.VillagerReset;
import com.leclowndu93150.libertyvillagers.cmds.VillagerSetPOI;
import com.leclowndu93150.libertyvillagers.cmds.VillagerStats;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import static net.minecraft.commands.Commands.literal;

public class LibertyVillagers implements ModInitializer {

    @Override
    public void onInitialize() {
        // Initialize common mod
        CommonClass.init();
        
        // Register commands for Fabric
        registerCommands();
    }
    
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // VillagerInfo command
            dispatcher.register(literal("villagerinfo").executes(context -> {
                VillagerInfo.processVillagerInfo(context);
                return 1;
            }));
            dispatcher.register(literal("vi").executes(context -> {
                VillagerInfo.processVillagerInfo(context);
                return 1;
            }));
            
            // VillagerReset command
            dispatcher.register(literal("villagerreset").executes(context -> {
                VillagerReset.processVillagerReset(context);
                return 1;
            }));
            dispatcher.register(literal("vr").executes(context -> {
                VillagerReset.processVillagerReset(context);
                return 1;
            }));
            
            // VillagerSetPOI command
            dispatcher.register(literal("villagersetpoi").executes(context -> {
                VillagerSetPOI.processVillagerSetPOI(context);
                return 1;
            }));
            dispatcher.register(literal("vsp").executes(context -> {
                VillagerSetPOI.processVillagerSetPOI(context);
                return 1;
            }));
            
            // VillagerStats command
            dispatcher.register(literal("villagerstats").executes(context -> {
                VillagerStats.processVillagerStats(context);
                return 1;
            }));
            dispatcher.register(literal("vs").executes(context -> {
                VillagerStats.processVillagerStats(context);
                return 1;
            }));
        });
    }
}
