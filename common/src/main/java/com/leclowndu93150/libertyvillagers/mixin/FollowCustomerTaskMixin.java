package com.leclowndu93150.libertyvillagers.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.LookAndFollowTradingPlayerSink;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LookAndFollowTradingPlayerSink.class)
public class FollowCustomerTaskMixin {

    @Inject(method = "checkExtraStartConditions", at = @At(value = "HEAD"), cancellable = true)
    protected void replaceShouldRun(ServerLevel serverWorld, Villager villager,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (villager.getVillagerData().profession().is(VillagerProfession.FISHERMAN) &&
                villager.getMainHandItem().is(Items.FISHING_ROD)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
