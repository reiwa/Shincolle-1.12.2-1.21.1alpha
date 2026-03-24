package com.lulan.shincolle.entity.submarine;

import com.lulan.shincolle.ai.EntityAIShipPickItem;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.IShipInvisible;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.CombatHelper;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.ParticleHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class EntitySubmRo500
extends BasicEntityShipSmall
implements IShipInvisible {
    public EntitySubmRo500(World world) {
        super(world);
        this.setSize(0.6f, 1.4f);
        this.setStateMinor(19, 8);
        this.setStateMinor(20, 39);
        this.setStateMinor(25, 6);
        this.setStateMinor(13, 3);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[9]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[9]);
        this.ModelPos = new float[]{0.0f, 20.0f, 0.0f, 45.0f};
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
    @Nullable
    protected SoundEvent getAmbientSound() {
        if (this.rand.nextInt(8) == 0) {
            return ModSounds.SHIP_GARURU;
        }
        return EntitySubmRo500.getCustomSound(0, this);
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
        if (!this.world.isRemote && this.ticksExisted % 128 == 0 && this.getStateFlag(9) && this.getStateMinor(6) > 0) {
            EntityPlayerMP player;
            if (this.getStateFlag(1) && (player = (EntityPlayerMP)EntityHelper.getEntityPlayerByUID(this.getPlayerUID())) != null && this.getDistanceSq(player) < 256.0) {
                player.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 40 + this.getLevel(), 0, false, false));
            }
            this.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 40 + this.getLevel(), 0, false, false));
        }
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
        md.vel0 += 0.5f;
        md.accY1 += 0.08f;
        md.accY2 += 0.08f;
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            if (this.getStateEmotion(1) == 4) {
                return this.height * 0.05f;
            }
            return this.height * 0.14f;
        }
        return this.height * 0.6f;
    }

    @Override
    public float getInvisibleLevel() {
        return 0.35f;
    }

    @Override
    public double getShipFloatingDepth() {
        return 1.1;
    }
}
