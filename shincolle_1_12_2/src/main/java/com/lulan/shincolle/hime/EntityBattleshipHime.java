package com.lulan.shincolle.entity.hime;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.mounts.EntityMountBaH;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.utility.CombatHelper;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.List;

public class EntityBattleshipHime
extends BasicEntityShipSmall {
    public EntityBattleshipHime(World world) {
        super(world);
        this.setSize(0.7f, 2.05f);
        this.setStateMinor(19, 10);
        this.setStateMinor(20, 26);
        this.setStateMinor(25, 3);
        this.setStateMinor(13, 1);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[7]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[7]);
        this.ModelPos = new float[]{-6.0f, 30.0f, 0.0f, 40.0f};
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
        this.tasks.addTask(12, new EntityAIShipRangeAttack(this));
    }

    @Override
    public void calcShipAttributesAddEffect() {
        super.calcShipAttributesAddEffect();
        this.AttackEffectMap.put(4, new int[]{this.getLevel() / 70, 100 + this.getLevel(), this.getLevel()});
    }

    @Override
    public void calcShipAttributesAddEquip() {
        super.calcShipAttributesAddEquip();
        MissileData md = this.getMissileData(2);
        if (md.type == 0) {
            md.type = 3;
            md.movetype = 1;
        }
    }

    @Override
    public void onLivingUpdate() {
        List<BasicEntityShip> shiplist;
        super.onLivingUpdate();
        shiplist = this.world.getEntitiesWithinAABB(BasicEntityShip.class, this.getEntityBoundingBox().expand(16.0, 16.0, 16.0));
        if (!this.world.isRemote && this.ticksExisted % 128 == 0 && this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 0 && !shiplist.isEmpty()) {
            for (BasicEntityShip s : shiplist) {
                if (!TeamHelper.checkSameOwner(this, s)) continue;
                s.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 50 + this.getStateMinor(0), this.getStateMinor(0) / 70, false, false));
                s.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 50 + this.getStateMinor(0), this.getStateMinor(0) / 70, false, false));
            }
        }
    }

    public void applyParticleAtAttacker(int type, Dist4d distVec) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        if(type == 1){
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 19, this.posX, this.posY + 0.3, this.posZ, distVec.x, 1.0, distVec.z, true), point);
        } else {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
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
        return this.shipAttrs.getAttackDamage() * 3.0f;
    }

    @Override
    public boolean hasShipMounts() {
        return true;
    }

    @Override
    public BasicEntityMount summonMountEntity() {
        return new EntityMountBaH(this.world);
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            if (this.getStateEmotion(1) == 4) {
                return 0.0;
            }
            return this.height * 0.62f;
        }
        return this.height * 0.76f;
    }
}
