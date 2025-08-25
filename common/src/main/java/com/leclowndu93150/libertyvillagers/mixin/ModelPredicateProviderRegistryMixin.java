package com.leclowndu93150.libertyvillagers.mixin;

//import com.google.common.collect.Maps;
//import net.minecraft.client.renderer.item.ConditionalItemModel;
//import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Mutable;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//import java.util.Map;
//import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
//import net.minecraft.client.renderer.item.ItemProperties;
//import net.minecraft.client.renderer.item.ItemPropertyFunction;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.item.Items;
//
//@Mixin(ItemProperties.class)
//public class ModelPredicateProviderRegistryMixin {
//
//    @Shadow
//    @Mutable
//    static Map<Item, Map<ResourceLocation, ItemPropertyFunction>> PROPERTIES;
//
//    @Inject(method = "register(Lnet/minecraft/world/item/Item;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/renderer/item/ClampedItemPropertyFunction;)V",
//            at = @At("HEAD"),
//            cancellable = true)
//    private static void register(Item item, ResourceLocation id, ClampedItemPropertyFunction provider, CallbackInfo ci) {
//        if (item != Items.FISHING_ROD) {
//            return;
//        }
//        // Villagers should show the used graphic for the fishing rod.
//        ClampedItemPropertyFunction newProvider = (stack, world, entity, seed) -> {
//            if (entity != null && entity.getType() == EntityType.VILLAGER) {
//                return 1.0f;
//            }
//            return provider.unclampedCall(stack, world, entity, seed);
//        };
//        PROPERTIES.computeIfAbsent(item, key -> Maps.newHashMap()).put(id, newProvider);
//        ci.cancel();
//    }
//}

