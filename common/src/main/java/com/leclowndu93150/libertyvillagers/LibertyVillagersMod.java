package com.leclowndu93150.libertyvillagers;

import com.leclowndu93150.libertyvillagers.cmds.VillagerInfo;
import com.leclowndu93150.libertyvillagers.cmds.VillagerReset;
import com.leclowndu93150.libertyvillagers.cmds.VillagerSetPOI;
import com.leclowndu93150.libertyvillagers.cmds.VillagerStats;
import com.leclowndu93150.libertyvillagers.config.BaseConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public class LibertyVillagersMod implements ModInitializer {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    public static ConfigHolder<BaseConfig> CONFIG_MANAGER;
    public static BaseConfig CONFIG;

    static boolean isClient = false;

    static public boolean isClient() {
        return isClient;
    }

    static public void setIsClient(boolean isClient) {
        LibertyVillagersMod.isClient = isClient;
    }

    static {
        CONFIG_MANAGER = AutoConfig.register(BaseConfig.class, GsonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(BaseConfig.class).getConfig();
    }

    @Override
    public void onInitialize() {
        VillagerInfo.register();
        VillagerReset.register();
        VillagerSetPOI.register();
        VillagerStats.register();
    }
}
