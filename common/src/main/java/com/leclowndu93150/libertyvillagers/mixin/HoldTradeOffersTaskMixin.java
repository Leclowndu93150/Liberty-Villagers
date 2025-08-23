package com.leclowndu93150.libertyvillagers.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.ShowTradesToPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShowTradesToPlayer.class)
public class HoldTradeOffersTaskMixin {

    @Inject(method = "checkExtraStartConditions",
            at = @At("HEAD"),
            cancellable = true)
    public void shouldRunIfNotFishing(ServerLevel serverWorld, Villager villagerEntity,
                             CallbackInfoReturnable<Boolean> cir) {
        if (villagerEntity.getVillagerData().getProfession() == VillagerProfession.FISHERMAN &&
                villagerEntity.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.FISHING_ROD)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
