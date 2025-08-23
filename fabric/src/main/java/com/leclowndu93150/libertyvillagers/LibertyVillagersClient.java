package com.leclowndu93150.libertyvillagers;

import com.leclowndu93150.libertyvillagers.overlay.LibertyVillagersOverlay;
import net.fabricmc.api.ClientModInitializer;

public class LibertyVillagersClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        LibertyVillagersClientInitializer.init();
        LibertyVillagersOverlay.register();
    }
}