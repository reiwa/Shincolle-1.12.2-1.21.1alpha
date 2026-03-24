package com.lulan.shincolle.entity.hime;

import com.lulan.shincolle.ai.EntityAIShipPickItem;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.IShipInvisible;
import com.lulan.shincolle.entity.mounts.EntityMountSuH;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.handler.ConfigHandler;
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
import net.minecraft.world.World;

public class EntitySubmHime
extends BasicEntityShipSmall
implements IShipInvisible {
    public EntitySubmHime(World world) {
        super(world);
        this.setSize(0.7f, 1.85f);
        this.setStateMinor(19, 10);
        this.setStateMinor(20, 44);
        this.setStateMinor(25, 6);
        this.setStateMinor(13, 3);
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
        if (!this.world.isRemote && this.ticksExisted % 128 == 0 && this.getStateFlag(9) && this.getStateMinor(6) > 0) {
            EntityPlayerMP player;
            if (this.getStateFlag(1) && (player = (EntityPlayerMP)EntityHelper.getEntityPlayerByUID(this.getPlayerUID())) != null && this.getDistanceSq(player) < 256.0) {
                player.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 80 + this.getLevel(), 0, false, false));
            }
            this.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 80 + this.getLevel(), 0, false, false));
        }
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            if (this.getStateEmotion(1) == 4) {
                return this.height * 0.25f;
            }
            return 0.0;
        }
        return this.height * 0.69f;
    }

    @Override
    public float getInvisibleLevel() {
        return 0.3f;
    }

    @Override
    public double getShipFloatingDepth() {
        return 1.0;
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
    public void summonMissile(int attackType, float atk, float tarX, float tarY, float tarZ, float targetHeight) {
        float[] mPos1 = CalcHelper.rotateXZByAxis(0.0f, 1.0f, this.renderYawOffset % 360.0f * ((float)Math.PI / 180), 1.0f);
        float[] mPos2 = CalcHelper.rotateXZByAxis(0.0f, -1.0f, this.renderYawOffset % 360.0f * ((float)Math.PI / 180), 1.0f);
        float launchPos = (float)this.posY + this.height * 0.6f;
        if (this.isMorph) {
            launchPos += 0.5f;
        }
        int moveType = CombatHelper.calcMissileMoveType(this, tarY, attackType);
        MissileData md = this.getMissileData(attackType);
        float[] data1 = new float[]{atk, 0.15f, launchPos, tarX, tarY + targetHeight * 0.1f, tarZ, 160.0f, 0.25f, md.vel0, md.accY1, md.accY2, (float)this.posX + mPos1[1], launchPos, (float)this.posZ + mPos1[0], 4.0f};
        float[] data2 = new float[]{atk, 0.15f, launchPos, tarX, tarY + targetHeight * 0.1f, tarZ, 160.0f, 0.25f, md.vel0, md.accY1, md.accY2, (float)this.posX + mPos2[1], launchPos, (float)this.posZ + mPos2[0], 4.0f};
        EntityAbyssMissile missile1 = new EntityAbyssMissile(this.world, this, md.type, moveType, data1);
        EntityAbyssMissile missile2 = new EntityAbyssMissile(this.world, this, md.type, moveType, data2);
        this.world.spawnEntity(missile1);
        this.world.spawnEntity(missile2);
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
    public boolean hasShipMounts() {
        return true;
    }

    @Override
    public BasicEntityMount summonMountEntity() {
        return new EntityMountSuH(this.world);
    }
}
