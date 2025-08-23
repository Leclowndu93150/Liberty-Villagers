package com.leclowndu93150.libertyvillagers.mixin;

import com.google.common.collect.Maps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.pathfinder.Path;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(Brain.class)
public abstract class BrainMixin<E extends LivingEntity> {

    private LivingEntity entity;

    @Shadow
    private Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = Maps.newHashMap();

    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"))
    public void getEntityFromTick(ServerLevel world, E entity, CallbackInfo ci) {
        this.entity = entity;
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "setMemoryInternal(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Ljava/util/Optional;)V",
            at = @At(value = "HEAD"))
    <U> void setMemory(MemoryModuleType<U> type, Optional<? extends ExpirableValue<?>> memory, CallbackInfo ci) {
        if (!CONFIG.debugConfig.enableVillagerBrainDebug) {
            return;
        }
        // Only look for villagers.
        Optional<? extends ExpirableValue<?>> optional = this.memories.get(MemoryModuleType.MEETING_POINT);
        if (optional == null) {
            return;
        }
        // Only look for certain memories.
        if (type != MemoryModuleType.WALK_TARGET && type != MemoryModuleType.HOME &&
                type != MemoryModuleType.POTENTIAL_JOB_SITE && type != MemoryModuleType.JOB_SITE &&
                type != MemoryModuleType.PATH) { //  && type != MemoryModuleType.SECONDARY_JOB_SITE) {
            return;
        }
        String className = "";
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for (int i = 1; i < elements.length; i++) {
            StackTraceElement s = elements[i];
            String fileName = s.getFileName();
            if (fileName == null) {
                continue;
            }
            // Find who is calling LookTargetUtil, not LookTargetUtil itself.
            if (fileName.contains("LookTargetUtil")) {
                continue;
            }
            className = s.getClassName();
            if (className.contains("ai.brain.task") || className.contains("ai.brain.sensor")) {
                fileName = fileName.substring(0, fileName.lastIndexOf('.'));
                className = fileName + ":" + s.getMethodName() + ":" + s.getLineNumber();
                break;
            }
        }

        StringBuilder name = new StringBuilder(entity != null ? entity.getName().toString() : "null");
        if (entity != null) {
            if (entity.getName().getContents() instanceof TranslatableContents) {
                TranslatableContents content = (TranslatableContents) entity.getName().getContents();
                String key = content.getKey();
                name = new StringBuilder(key.substring(key.lastIndexOf('.') + 1));
            } else {
                name = new StringBuilder();
                List<Component> withoutStyle = entity.getName().toFlatList();
                for (Component text : withoutStyle) {
                    name.append(text.getString());
                }
            }
        }

        StringBuilder target = new StringBuilder();
        if (memory.isEmpty()) {
            target = new StringBuilder("null");
        } else if (type == MemoryModuleType.WALK_TARGET) {
            WalkTarget walkTarget = (WalkTarget)memory.get().getValue();
            target = new StringBuilder(String.format("Walk Target set to position %s with range %d",
                    walkTarget.getTarget().currentBlockPosition().toShortString(), walkTarget.getCloseEnoughDist()));
         } else if (type == MemoryModuleType.HOME || type ==
                MemoryModuleType.POTENTIAL_JOB_SITE || type == MemoryModuleType.JOB_SITE) {
            GlobalPos globalPos = (GlobalPos)memory.get().getValue();
            target = new StringBuilder(String.format("Position set to %s", globalPos.pos().toShortString()));
        } else if (type == MemoryModuleType.SECONDARY_JOB_SITE) {
            List<GlobalPos> globalPosList;
            globalPosList = (List<GlobalPos>)memory.get().getValue();
            for (GlobalPos globalPos : globalPosList) {
                target.append("{ ").append(globalPos.pos().toShortString()).append(" } ");
            }
        } else if (type == MemoryModuleType.PATH) {
            Path path = (Path)memory.get().getValue();
            for (int i = path.getNextNodeIndex(); i < path.getNodeCount(); i++) {
                target.append("{ ").append(path.getNode(i).asBlockPos().toShortString()).append(" } ");
            }
        }
        String pos = entity == null ? "" : entity.blockPosition().toShortString();
        String memoryType = type.toString();
        memoryType = memoryType.substring(memoryType.lastIndexOf(':') + 1);
        System.out.printf("===== %s at %s memoryType %s set by %s to %s\n", name, pos,
                memoryType, className, target);
    }
}
