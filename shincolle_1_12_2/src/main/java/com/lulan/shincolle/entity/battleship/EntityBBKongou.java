package com.lulan.shincolle.entity.battleship;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.EmotionHelper;
import com.lulan.shincolle.utility.ParticleHelper;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.List;
import java.util.Objects;

public class EntityBBKongou extends BasicEntityShipSmall {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;
    private static final int ATTACK_EFFECT_KEY_HEALTH_AURA = 18;

    public EntityBBKongou(World world) {
        super(world);
        this.setSize(0.6f, 1.875f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, 6);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 60);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 3);
        this.setStateMinor(STATE_MINOR_RARITY, 4);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[7]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[7]);
        this.ModelPos = new float[]{0.0f, 25.0f, 0.0f, 40.0f};
        this.StateFlag[15] = false;
        this.StateFlag[16] = false;
        this.setFoodSaturationMax(19);
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
            float[] partPos = CalcHelper.rotateXZByAxis(-0.7f, 0.0f, this.renderYawOffset % 360.0f * ((float) Math.PI / 180), 1.0f);
            ParticleHelper.spawnAttackParticleAt(this.posX + partPos[1], this.posY + 1.17, this.posZ + partPos[0], 1.0, 0.0, 0.0, (byte) 43);
            partPos = CalcHelper.rotateXZByAxis(-0.45f, 0.0f, this.renderYawOffset % 360.0f * ((float) Math.PI / 180), 1.0f);
            ParticleHelper.spawnAttackParticleAt(this.posX + partPos[1], this.posY + 1.32, this.posZ + partPos[0], 1.0, 0.0, 0.0, (byte) 43);
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
        if (nearbyShips.isEmpty()) {
            return;
        }
        nearbyShips.stream()
                .filter(ship -> TeamHelper.checkSameOwner(this, ship))
                .forEach(ship -> ship.addPotionEffect(new PotionEffect(MobEffects.HEALTH_BOOST, 140 + this.getStateMinor(0), this.getStateMinor(0) / 120, false, false)));
    }

    @Override
    public void calcShipAttributesAddEffect() {
        super.calcShipAttributesAddEffect();
        this.AttackEffectMap.put(ATTACK_EFFECT_KEY_HEALTH_AURA, new int[]{this.getLevel() / 60, 100 + this.getLevel(), this.getLevel()});
    }

    @Override
    public double getMountedYOffset() {
        if (!this.isSitting()) {
            return this.height * 0.75f;
        }
        if (EmotionHelper.checkModelState(1, this.getStateEmotion(0))) {
            return this.height * 0.42f;
        }
        if (this.getStateEmotion(1) == 4) {
            return 0.0;
        }
        return this.height * 0.35f;
    }

    public void applyParticleAtAttacker(int type, Dist4d distVec) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        if (type == 1) {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 19, this.posX, this.posY + 0.3, this.posZ, distVec.x, 1.0, distVec.z, true), point);
        } else {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
        }
    }
}