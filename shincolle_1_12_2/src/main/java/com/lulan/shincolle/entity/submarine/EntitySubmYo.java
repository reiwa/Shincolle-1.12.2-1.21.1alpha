package com.lulan.shincolle.entity.submarine;

import com.lulan.shincolle.ai.EntityAIShipPickItem;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.IShipInvisible;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

public class EntitySubmYo
extends BasicEntityShipSmall
implements IShipInvisible {
    public EntitySubmYo(World world) {
        super(world);
        this.setSize(0.6f, 1.8f);
        this.setStateMinor(19, 8);
        this.setStateMinor(20, 18);
        this.setStateMinor(25, 6);
        this.setStateMinor(13, 2);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[9]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[9]);
        this.ModelPos = new float[]{0.0f, 25.0f, 0.0f, 45.0f};
        this.StateFlag[15] = false;
        this.StateFlag[16] = false;
        this.StateFlag[24] = true;
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
        this.tasks.addTask(20, new EntityAIShipPickItem(this, 4.0f));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote) {
            if (this.ticksExisted % 128 == 0 && this.getStateFlag(9) && this.getStateMinor(6) > 0) {
                EntityPlayerMP player;
                if (this.getStateFlag(1) && (player = (EntityPlayerMP)EntityHelper.getEntityPlayerByUID(this.getPlayerUID())) != null && this.getDistanceSq(player) < 256.0) {
                    player.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 40 + this.getLevel(), 0, false, false));
                }
                this.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 40 + this.getLevel(), 0, false, false));
            }
        } else if (this.ticksExisted % 4 == 0 && EmotionHelper.checkModelState(0, this.getStateEmotion(0)) && !this.getStateFlag(2) && (this.isSitting() && this.getStateEmotion(1) != 4 || !this.isSitting())) {
            float[] eyePosR;
            float[] eyePosL;
            float radYaw = this.renderYawOffset * ((float)Math.PI / 180);
            if (this.isSitting()) {
                eyePosL = new float[]{0.35f, 1.5f, -0.5f};
                eyePosR = new float[]{-0.35f, 1.5f, -0.5f};
            } else {
                eyePosL = new float[]{0.35f, 1.8f, -0.35f};
                eyePosR = new float[]{-0.35f, 1.8f, -0.35f};
            }
            eyePosL = CalcHelper.rotateXYZByYawPitch(eyePosL[0], eyePosL[1], eyePosL[2], radYaw, 0.0f, 1.0f);
            eyePosR = CalcHelper.rotateXYZByYawPitch(eyePosR[0], eyePosR[1], eyePosR[2], radYaw, 0.0f, 1.0f);
            ParticleHelper.spawnAttackParticleAt(this.posX + eyePosL[0], this.posY + eyePosL[1], this.posZ + eyePosL[2], 0.0, 0.05, 0.5, (byte)16);
            ParticleHelper.spawnAttackParticleAt(this.posX + eyePosR[0], this.posY + eyePosR[1], this.posZ + eyePosR[2], 0.0, 0.05, 0.5, (byte)16);
        }
    }

    @Override
    public double getMountedYOffset() {
        if (EmotionHelper.checkModelState(0, this.getStateEmotion(0))) {
            if (this.isSitting()) {
                if (this.getStateEmotion(1) == 4) {
                    return this.height * 0.55f;
                }
                return this.height * 0.42f;
            }
            return this.height * 0.58f;
        }
        if (this.isSitting()) {
            if (this.getStateEmotion(1) == 4) {
                return this.height * 0.48f;
            }
            return 0.0;
        }
        return this.height * 0.69f;
    }

    @Override
    public float getInvisibleLevel() {
        return 0.2f;
    }

    @Override
    public boolean attackEntityWithAmmo(Entity target) {
        if (!this.decrAmmoNum(0, this.getAmmoConsumption())) {
            return false;
        }
        this.addShipExp(ConfigHandler.expGain[1]);
        this.decrGrudgeNum(ConfigHandler.consumeGrudgeAction[0]);
        this.decrMorale(1);
        this.setCombatTick(this.ticksExisted);
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        this.applySoundAtAttacker(2);
        this.applyParticleAtAttacker(2, distVec);
        float tarX = (float)target.posX;
        float tarY = (float)target.posY;
        float tarZ = (float)target.posZ;
        if (this.rand.nextFloat() <= CombatHelper.calcMissRate(this, (float)distVec.d)) {
            tarX = tarX - 5.0f + this.rand.nextFloat() * 10.0f;
            tarY += this.rand.nextFloat() * 5.0f;
            tarZ = tarZ - 5.0f + this.rand.nextFloat() * 10.0f;
            ParticleHelper.spawnAttackTextParticle(this, 0);
        }
        float atk = this.getAttackBaseDamage(1, target);
        this.summonMissile(1, atk, tarX, tarY, tarZ, 1.0f);
        this.applySoundAtTarget(2);
        this.applyParticleAtTarget(2, target);
        this.applyEmotesReaction(3);
        if (ConfigHandler.canFlare) {
            this.flareTarget(target);
        }
        this.applyAttackPostMotion(1, this, true, atk);
        return true;
    }

    @Override
    public float getAttackBaseDamage(int type, Entity target) {
        switch (type) {
            case 1: {
                return this.shipAttrs.getAttackDamage();
            }
            case 2: {
                return this.shipAttrs.getAttackDamageHeavy();
            }
            case 3: {
                return this.shipAttrs.getAttackDamageAir();
            }
            case 4: {
                return this.shipAttrs.getAttackDamageAirHeavy();
            }
            default:
        }
        return this.shipAttrs.getAttackDamage() * 0.125f;
    }

    @Override
    public void calcShipAttributesAddEquip() {
        super.calcShipAttributesAddEquip();
        MissileData md = this.getMissileData(1);
        md.vel0 += 0.3f;
        md.accY1 += 0.06f;
        md.accY2 += 0.06f;
    }

    @Override
    public double getShipFloatingDepth() {
        return 1.0;
    }
}
