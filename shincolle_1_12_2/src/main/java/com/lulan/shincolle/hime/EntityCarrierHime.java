package com.lulan.shincolle.entity.hime;

import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipCV;
import com.lulan.shincolle.entity.mounts.EntityMountCaH;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.utility.CombatHelper;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import java.util.List;

public class EntityCarrierHime
extends BasicEntityShipCV {
    public EntityCarrierHime(World world) {
        super(world);
        this.setSize(0.7f, 1.9f);
        this.setStateMinor(19, 10);
        this.setStateMinor(20, 20);
        this.setStateMinor(25, 1);
        this.setStateMinor(13, 3);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[6]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[6]);
        this.ModelPos = new float[]{-6.0f, 30.0f, 0.0f, 40.0f};
        this.launchHeight = this.height * 0.9f;
        this.StateFlag[13] = false;
        this.StateFlag[14] = false;
        this.setFoodSaturationMax(24);
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
    public void onLivingUpdate() {
        List<BasicEntityShip> shiplist;
        super.onLivingUpdate();
        shiplist = this.world.getEntitiesWithinAABB(BasicEntityShip.class, this.getEntityBoundingBox().expand(16.0, 16.0, 16.0));
        if (!this.world.isRemote && this.ticksExisted % 128 == 0 && this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 0 && !shiplist.isEmpty()) {
            for (BasicEntityShip s : shiplist) {
                if (!TeamHelper.checkSameOwner(this, s)) continue;
                s.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 50 + this.getStateMinor(0), this.getStateMinor(0) / 70, false, false));
            }
        }
    }

    @Override
    public boolean hasShipMounts() {
        return true;
    }

    @Override
    public BasicEntityMount summonMountEntity() {
        return new EntityMountCaH(this.world);
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            if (this.getStateEmotion(1) == 4) {
                return this.height * 0.0f;
            }
            return this.height * 0.33f;
        }
        return this.height * 0.75f;
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        float atk = this.getAttackBaseDamage(0, target);
        this.addShipExp(ConfigHandler.expGain[0]);
        this.decrMorale(0);
        this.setCombatTick(this.ticksExisted);
        this.applySoundAtAttacker(0);
        this.applyParticleAtAttacker(0, Dist4d.ONE);
        MissileData md = this.getMissileData(0);
        float[] data = new float[]{atk, 0.0f, (float)this.posY + 1.3f, (float)target.posX, (float)target.posY + target.height * 0.2f, (float)target.posZ, 100.0f, 0.0f, md.vel0, md.accY1, md.accY2};
        EntityAbyssMissile missile = new EntityAbyssMissile(this.world, this, md.type, 0, data);
        this.world.spawnEntity(missile);
        this.applyEmotesReaction(3);
        if (ConfigHandler.canFlare) {
            this.flareTarget(target);
        }
        return true;
    }

    @Override
    public void calcShipAttributesAddEquip() {
        super.calcShipAttributesAddEquip();
        MissileData md = this.getMissileData(0);
        if (md.vel0 < 0.8f) {
            md.vel0 = 0.8f;
        }
        if (md.accY1 < 1.1f) {
            md.accY1 = 1.1f;
        }
        if (md.accY2 < 1.1f) {
            md.accY2 = 1.1f;
        }
    }

    @Override
    public float getAttackBaseDamage(int type, Entity target) {
        switch (type) {
            case 1: {
                return CombatHelper.modDamageByAdditionAttrs(this, target, this.shipAttrs.getAttackDamage(), 0);
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
        return this.shipAttrs.getAttackDamage() * 2.0f;
    }
}
