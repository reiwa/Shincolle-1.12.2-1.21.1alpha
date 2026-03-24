package com.lulan.shincolle.entity.battleship;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.other.EntityProjectileBeam;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.List;
import java.util.Objects;

public class EntityBattleshipYMT extends BasicEntityShipSmall {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;
    private static final int EMOTION_ATTACK_PHASE = 5;

    public EntityBattleshipYMT(World world) {
        super(world);
        this.setSize(0.8f, 2.1f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, 6);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 46);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 3);
        this.setStateMinor(STATE_MINOR_RARITY, 4);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[7]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[7]);
        this.ModelPos = new float[]{0.0f, 25.0f, 0.0f, 40.0f};
        this.StateFlag[15] = false;
        this.StateFlag[16] = false;
        this.setFoodSaturationMax(30);
        this.postInit();
    }

    @Override
    public EntityAgeable createChild(EntityAgeable ageable) {
        return null;
    }

    @Override
    public int getEquipType() {
        return 1;
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(11, new EntityAIShipRangeAttack(this));
    }

    @Override
    public void calcShipAttributesAddEffect() {
        super.calcShipAttributesAddEffect();
        this.AttackEffectMap.put(4, new int[]{this.getLevel() / 70, 100 + this.getLevel(), this.getLevel()});
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.world.isRemote) {
            updateClientParticles();
        } else {
            updateServerLogic();
        }
    }

    private void updateClientParticles() {
        if (this.ticksExisted % 4 == 0 && EmotionHelper.checkModelState(0, this.getStateEmotion(0)) && !this.isSitting() && !this.getStateFlag(2)) {
            float[] partPos = CalcHelper.rotateXZByAxis(-0.63f, 0.0f, this.renderYawOffset % 360.0f * ((float) Math.PI / 180), 1.0f);
            ParticleHelper.spawnAttackParticleAt(this.posX + partPos[1], this.posY + 1.65, this.posZ + partPos[0], 0.0, 0.0, 0.0, (byte) 20);
        }
        if (this.ticksExisted % 16 == 0 && this.getStateEmotion(EMOTION_ATTACK_PHASE) > 0) {
            ParticleHelper.spawnAttackParticleAtEntity(this, 0.1, 16.0, 1.0, (byte) 4);
        }
    }

    private void updateServerLogic() {
        if (this.ticksExisted % 128 == 0) {
            applyBuffToNearbyAllies();
        }
    }

    private void applyBuffToNearbyAllies() {
        if (!(this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 0)) {
            return;
        }
        List<BasicEntityShip> nearbyShips = this.world.getEntitiesWithinAABB(BasicEntityShip.class, this.getEntityBoundingBox().expand(16.0, 16.0, 16.0));
        if (nearbyShips.isEmpty()) return;

        nearbyShips.stream()
                .filter(ship -> TeamHelper.checkSameOwner(this, ship))
                .forEach(ship -> {
                    ship.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 50 + this.getStateMinor(0), this.getStateMinor(0) / 70, false, false));
                    ship.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 50 + this.getStateMinor(0), this.getStateMinor(0) / 70, false, false));
                });
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        if (this.getStateEmotion(EMOTION_ATTACK_PHASE) > 0) {
            return executeBeamAttack(target);
        } else {
            return prepareBeamAttack(target);
        }
    }

    private boolean executeBeamAttack(Entity target) {
        this.addShipExp(ConfigHandler.expGain[2]);
        this.decrGrudgeNum(ConfigHandler.consumeGrudgeAction[1]);
        this.decrMorale(2);
        this.setCombatTick(this.ticksExisted);
        if (!this.decrAmmoNum(1, this.getAmmoConsumption())) {
            return false;
        }

        this.playSound(ModSounds.SHIP_YAMATO_SHOT, ConfigHandler.volumeFire, 1.0f);
        if (this.getRNG().nextInt(10) > 7) {
            this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
        }

        float atk = CombatHelper.modDamageByAdditionAttrs(this, target, this.getAttackBaseDamage(2, target), 3);
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        EntityProjectileBeam beam = new EntityProjectileBeam(this.world);
        beam.initAttrs(this, 0, (float) distVec.x, (float) distVec.y, (float) distVec.z, atk);
        this.world.spawnEntity(beam);

        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, beam, (float) distVec.x, (float) distVec.y, (float) distVec.z, 1, true), point);

        this.setStateEmotion(EMOTION_ATTACK_PHASE, 0, true);
        return true;
    }

    private boolean prepareBeamAttack(Entity target) {
        this.playSound(ModSounds.SHIP_YAMATO_READY, ConfigHandler.volumeFire, 1.0f);

        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 7, 1.0, 0.0, 0.0), point);

        this.setStateEmotion(EMOTION_ATTACK_PHASE, 1, true);
        this.applyEmotesReaction(3);
        if (ConfigHandler.canFlare) {
            this.flareTarget(target);
        }
        return false;
    }

    @Override
    public double getMountedYOffset() {
        if (!this.isSitting()) {
            return this.height * 0.75f;
        }
        if (EmotionHelper.checkModelState(0, this.getStateEmotion(0))) {
            return this.height * 0.5f;
        }
        if (this.getStateEmotion(1) == 4) {
            return this.height * 0.1f;
        }
        return this.height * 0.4f;
    }

    public void applyParticleAtAttacker(int type) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        if (type == 1) {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 5, 0.9, 1.3, 1.2), point);
        } else {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
        }
    }
}