package com.lulan.shincolle.entity.carrier;

import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipCV;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.EmotionHelper;
import com.lulan.shincolle.utility.ParticleHelper;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;

public class EntityCarrierWo extends BasicEntityShipCV {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;

    public EntityCarrierWo(World world) {
        super(world);
        this.setSize(0.7f, 1.9f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, 5);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 12);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 1);
        this.setStateMinor(STATE_MINOR_RARITY, 5);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[6]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[6]);
        this.ModelPos = new float[]{0.0f, 20.0f, 0.0f, 30.0f};
        this.launchHeight = this.height * 0.9f;
        this.StateFlag[13] = false;
        this.StateFlag[14] = false;
        this.setFoodSaturationMax(18);
        this.postInit();
    }

    @Override
    public EntityAgeable createChild(EntityAgeable ageable) {
        return null;
    }

    @Override
    public int getEquipType() {
        return 3;
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(11, new EntityAIShipCarrierAttack(this));
    }

    @Override
    public void calcShipAttributesAddRaw() {
        super.calcShipAttributesAddRaw();
        this.maxAircraftLight += (int) (this.getLevel() * 0.25f);
        this.maxAircraftHeavy += (int) (this.getLevel() * 0.15f);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.world.isRemote) {
            updateClientEffects();
        } else {
            updateServerEffects();
        }
    }

    private void updateClientEffects() {
        if (this.ticksExisted % 4 != 0) return;
        boolean shouldGlow = EmotionHelper.checkModelState(0, this.getStateEmotion(0)) &&
                !this.getStateFlag(2) &&
                !(this.isSitting() && this.getStateEmotion(1) == 4);
        if (shouldGlow) {
            spawnEyeGlowParticles();
        }
        if ((this.ticksExisted & 0xF) == 0 && EmotionHelper.checkModelState(4, this.getStateEmotion(0)) && !this.isSitting() && !this.isRiding()) {
            ParticleHelper.spawnAttackParticleAtEntity(this, 1.28, 0.12, 0.17, (byte) 17);
        }
    }

    private void spawnEyeGlowParticles() {
        float radYaw = this.rotationYawHead * ((float) Math.PI / 180f);
        float radPitch = this.rotationPitch * ((float) Math.PI / 180f);
        float[] eyePosL;
        float[] eyePosR;
        if (this.isSitting()) {
            eyePosL = new float[]{-0.3f, 1.0f, -0.4f};
            eyePosR = new float[]{-0.7f, 0.8f, 0.6f};
        } else {
            eyePosL = new float[]{0.55f, 1.0f, 0.2f};
            eyePosR = new float[]{-0.55f, 1.0f, 0.2f};
        }
        if (this.getStateEmotion(2) == 1 && !this.isSitting()) {
            float[] tiltLeft = CalcHelper.rotateXZByAxis(eyePosL[0], eyePosL[1], -0.24f, 1.0f);
            float[] tiltRight = CalcHelper.rotateXZByAxis(eyePosR[0], eyePosR[1], -0.24f, 1.0f);
            eyePosL[0] = tiltLeft[0];
            eyePosL[1] = tiltLeft[1];
            eyePosR[0] = tiltRight[0];
            eyePosR[1] = tiltRight[1];
        }
        eyePosL = CalcHelper.rotateXYZByYawPitch(eyePosL[0], eyePosL[1], eyePosL[2], radYaw, radPitch, 1.0f);
        eyePosR = CalcHelper.rotateXYZByYawPitch(eyePosR[0], eyePosR[1], eyePosR[2], radYaw, radPitch, 1.0f);
        ParticleHelper.spawnAttackParticleAt(this.posX + eyePosL[0], this.posY + 1.5 + eyePosL[1], this.posZ + eyePosL[2], 0.0, 0.05, 1.0, (byte) 16);
        ParticleHelper.spawnAttackParticleAt(this.posX + eyePosR[0], this.posY + 1.5 + eyePosR[1], this.posZ + eyePosR[2], 0.0, 0.05, 1.0, (byte) 16);
    }

    private void updateServerEffects() {
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
                .forEach(ship -> ship.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 30 + this.getStateMinor(0), this.getStateMinor(0) / 80, false, false)));
    }

    @Override
    public double getMountedYOffset() {
        if (EmotionHelper.checkModelState(0, this.getStateEmotion(0))) {
            if (!this.isSitting()) {
                return this.height * 1.21;
            }
            return this.getStateEmotion(1) == 4 ? this.height * 0.2 : this.height * 0.43;
        }
        return this.height * 0.68;
    }
}