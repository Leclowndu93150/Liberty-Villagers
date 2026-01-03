package com.leclowndu93150.libertyvillagers.compat;

import com.leclowndu93150.libertyvillagers.Constants;
import com.leclowndu93150.libertyvillagers.platform.Services;
import com.leclowndu93150.villager_placement.api.VillagerPlacementAPI;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.npc.Villager;

/**
 * Compatibility layer for Villager Placement mod integration.
 * Safely handles the case when Villager Placement is not installed.
 */
public final class VillagerPlacementCompat {

    private static final String MOD_ID = "villager_placement";
    private static Boolean isLoaded = null;

    private VillagerPlacementCompat() {}

    /**
     * Checks if Villager Placement mod is loaded.
     * Result is cached after first check.
     */
    public static boolean isLoaded() {
        if (isLoaded == null) {
            isLoaded = Services.PLATFORM.isModLoaded(MOD_ID);
            if (isLoaded) {
                Constants.LOG.info("Villager Placement mod detected, enabling integration");
            }
        }
        return isLoaded;
    }

    /**
     * Creates the StayAtIdlePosition behavior if Villager Placement is loaded.
     * @return The behavior, or null if mod is not loaded
     */
    public static BehaviorControl<Villager> createStayAtIdleBehavior() {
        if (!isLoaded()) {
            return null;
        }
        try {
            return VillagerPlacementAPI.createStayAtIdleBehavior();
        } catch (Throwable e) {
            Constants.LOG.error("Failed to create StayAtIdleBehavior from Villager Placement", e);
            return null;
        }
    }

    /**
     * Creates the ReturnToIdlePosition behavior if Villager Placement is loaded.
     * @return The behavior, or null if mod is not loaded
     */
    public static BehaviorControl<Villager> createReturnToIdleBehavior() {
        if (!isLoaded()) {
            return null;
        }
        try {
            return VillagerPlacementAPI.createReturnToIdleBehavior();
        } catch (Throwable e) {
            Constants.LOG.error("Failed to create ReturnToIdleBehavior from Villager Placement", e);
            return null;
        }
    }
}
