package com.lulan.shincolle.entity.carrier;

import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.entity.BasicEntityAirplane;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipCV;
import com.lulan.shincolle.entity.other.EntityAirplaneT;
import com.lulan.shincolle.entity.other.EntityAirplaneZero;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import java.util.List;

public class EntityCarrierKaga
extends BasicEntityShipCV {
    public EntityCarrierKaga(World world) {
        super(world);
        this.setSize(0.6f, 1.875f);
        this.setStateMinor(19, 5);
        this.setStateMinor(20, 47);
        this.setStateMinor(25, 1);
        this.setStateMinor(13, 8);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[6]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[6]);
        this.ModelPos = new float[]{0.0f, 20.0f, 0.0f, 40.0f};
        this.launchHeight = this.height * 0.65f;
        this.StateFlag[13] = false;
        this.StateFlag[14] = false;
        this.setFoodSaturationMax(30);
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
    public void onLivingUpdate() {
        List<BasicEntityShip> shiplist;
        super.onLivingUpdate();
        shiplist = this.world.getEntitiesWithinAABB(BasicEntityShip.class, this.getEntityBoundingBox().expand(16.0, 16.0, 16.0));
        if (!this.world.isRemote && this.ticksExisted % 128 == 0 && this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 0 && !shiplist.isEmpty()) {
            for (BasicEntityShip s : shiplist) {
                if (!TeamHelper.checkSameOwner(this, s)) continue;
                s.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 50 + this.getStateMinor(0), this.getStateMinor(0) / 85, false, false));
            }
        }
    }

    @Override
    public void calcShipAttributesAddRaw() {
        super.calcShipAttributesAddRaw();
        this.maxAircraftLight = (int)(this.maxAircraftLight + this.getLevel() * 0.4f);
        this.maxAircraftHeavy = (int)(this.maxAircraftHeavy + this.getLevel() * 0.2f);
    }

    @Override
    public void calcShipAttributesAddEffect() {
        super.calcShipAttributesAddEffect();
        this.AttackEffectMap.put(17, new int[]{this.getLevel() / 120, 100 + this.getLevel(), this.getLevel()});
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(11, new EntityAIShipCarrierAttack(this));
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            if (this.getStateEmotion(1) == 4) {
                return (double)this.height * (double)0.35f;
            }
            return (double)this.height * (double)0.45f;
        }
        return (double)this.height * (double)0.72f;
    }

    @Override
    public BasicEntityAirplane getAttackAirplane(boolean isLightAirplane) {
        if (isLightAirplane) {
            return new EntityAirplaneZero(this.world);
        }
        return new EntityAirplaneT(this.world);
    }

    public void applySoundAtAttacker(int type) {
        switch (type) {
            case 1: 
            case 2: {
                break;
            }
            case 3: {
                this.playSound(SoundEvents.ENTITY_ARROW_SHOOT, ConfigHandler.volumeFire + 0.2f, 1.0f / (this.rand.nextFloat() * 0.4f + 1.2f) + 0.5f);
                break;
            }
            case 4: {
                this.playSound(SoundEvents.ENTITY_ARROW_SHOOT, ConfigHandler.volumeFire + 0.2f, 1.0f / (this.rand.nextFloat() * 0.4f + 1.2f) + 0.5f);
                break;
            }
            default: {
                if (this.getRNG().nextInt(2) != 0) break;
                this.playSound(EntityCarrierKaga.getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
            }
        }
    }
}
