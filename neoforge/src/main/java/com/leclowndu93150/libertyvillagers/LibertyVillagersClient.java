package com.leclowndu93150.libertyvillagers;

import com.leclowndu93150.libertyvillagers.overlay.LibertyVillagersOverlay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class LibertyVillagersClient {
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        LibertyVillagersClientInitializer.init();
        LibertyVillagersOverlay.register();
    }
}