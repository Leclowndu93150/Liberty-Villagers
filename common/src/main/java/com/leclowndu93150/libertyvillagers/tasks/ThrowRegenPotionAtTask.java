package com.leclowndu93150.libertyvillagers.tasks;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.Vec3;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;

public class ThrowRegenPotionAtTask extends HealTargetTask {

    private static final int COMPLETION_RANGE = 5;

    public ThrowRegenPotionAtTask() {
        super(COMPLETION_RANGE);
    }

    protected List<LivingEntity> getPossiblePatients(ServerLevel serverWorld, Villager villagerEntity) {
        List<LivingEntity> possiblePatients = Lists.newArrayList();
        if (!CONFIG.villagersProfessionConfig.clericThrowsPotionsAtVillagers &&
                !CONFIG.villagersProfessionConfig.clericThrowsPotionsAtPlayers) {
            return possiblePatients;
        }

        if (CONFIG.villagersProfessionConfig.clericThrowsPotionsAtVillagers) {
            List<Villager> villagers = villagerEntity.level().getEntitiesOfClass(Villager.class,
                    villagerEntity.getBoundingBox()
                            .inflate(CONFIG.villagersProfessionConfig.clericThrowsPotionsAtRange));
            possiblePatients.addAll(villagers);
        }

        if (CONFIG.villagersProfessionConfig.clericThrowsPotionsAtPlayers) {
            List<Player> players = villagerEntity.level().getEntitiesOfClass(Player.class,
                    villagerEntity.getBoundingBox()
                            .inflate(CONFIG.villagersProfessionConfig.clericThrowsPotionsAtRange));
            possiblePatients.addAll(players);
        }

        return possiblePatients;
    }

    protected void healTarget(ServerLevel serverWorld, Villager villagerEntity, LivingEntity currentPatient) {
        Vec3 vec3d = currentPatient.getDeltaMovement();
        double d = currentPatient.getX() + vec3d.x - villagerEntity.getX();
        double e = currentPatient.getEyeY() - (double) 1.1f - villagerEntity.getY();
        double f = currentPatient.getZ() + vec3d.z - villagerEntity.getZ();
        double g = Math.sqrt(d * d + f * f);

        ThrownPotion potionEntity = new ThrownPotion(serverWorld, villagerEntity);
        potionEntity.setItem(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.REGENERATION));
        potionEntity.setXRot(potionEntity.getXRot() + 20.0f);
        potionEntity.shoot(d, e + g * 0.2, f, 0.75f, 8.0f);
        serverWorld.playSound(null, villagerEntity.getX(), villagerEntity.getY(), villagerEntity.getZ(),
                SoundEvents.LINGERING_POTION_THROW, villagerEntity.getSoundSource(), 1.0f,
                0.8f + serverWorld.getRandom().nextFloat() * 0.4f);
        serverWorld.addFreshEntity(potionEntity);
    }
}
