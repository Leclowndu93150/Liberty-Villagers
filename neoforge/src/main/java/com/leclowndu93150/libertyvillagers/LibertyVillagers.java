package com.leclowndu93150.libertyvillagers;

import com.leclowndu93150.libertyvillagers.cmds.VillagerInfo;
import com.leclowndu93150.libertyvillagers.cmds.VillagerReset;
import com.leclowndu93150.libertyvillagers.cmds.VillagerSetPOI;
import com.leclowndu93150.libertyvillagers.cmds.VillagerStats;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import static net.minecraft.commands.Commands.literal;

@Mod(Constants.MOD_ID)
public class LibertyVillagers {

    public LibertyVillagers(IEventBus eventBus) {
        CommonClass.init();
        NeoForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        
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
    }
}
