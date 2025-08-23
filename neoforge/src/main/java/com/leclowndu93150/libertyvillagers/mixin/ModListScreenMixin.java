package com.leclowndu93150.libertyvillagers.mixin;

import com.leclowndu93150.libertyvillagers.Constants;
import com.leclowndu93150.libertyvillagers.config.BaseConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforge.client.gui.widget.ModListWidget;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModListScreen.class, remap = false)
public class ModListScreenMixin {
    @Shadow
    private ModListWidget.ModEntry selected;

    @Shadow
    private Button configButton;

    @Inject(method = "updateCache", at = @At("HEAD"), cancellable = true)
    private void onUpdateCache(CallbackInfo ci) {
        if (selected == null) {
            configButton.active = false;
            return;
        }

        IModInfo selectedMod = selected.getInfo();
        if (Constants.MOD_ID.equals(selectedMod.getModId())) {
            configButton.active = true;
            ci.cancel();
        }
    }

    @Inject(method = "displayModConfig", at = @At("HEAD"), cancellable = true)
    private void onDisplayModConfig(CallbackInfo ci) {
        if (selected != null && Constants.MOD_ID.equals(selected.getInfo().getModId())) {
            Screen configScreen = AutoConfig.getConfigScreen(BaseConfig.class, (ModListScreen)(Object)this).get();
            Minecraft.getInstance().setScreen(configScreen);
            ci.cancel();
        }
    }
}