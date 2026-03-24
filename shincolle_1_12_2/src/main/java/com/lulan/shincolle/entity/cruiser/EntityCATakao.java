package com.lulan.shincolle.entity.cruiser;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.utility.EmotionHelper;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityCATakao
extends BasicEntityShipSmall {
    public EntityCATakao(World world) {
        super(world);
        this.setSize(0.7f, 1.75f);
        this.setStateMinor(19, 2);
        this.setStateMinor(20, 59);
        this.setStateMinor(25, 4);
        this.setStateMinor(13, 4);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[2]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[2]);
        this.ModelPos = new float[]{0.0f, 25.0f, 0.0f, 40.0f};
        this.StateFlag[15] = false;
        this.StateFlag[16] = false;
        this.setFoodSaturationMax(16);
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
    public double getMountedYOffset() {
        if (this.isSitting()) {
            if (EmotionHelper.checkModelState(0, this.getStateEmotion(0))) {
                return this.height * 0.42f;
            }
            if (this.getStateEmotion(1) == 4) {
                return 0.0;
            }
            return this.height * 0.35f;
        }
        return this.height * 0.75f;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float atk) {
        if (super.attackEntityFrom(source, atk) && source.getTrueSource() instanceof EntityLivingBase && !source.getTrueSource().equals(this.getHostEntity())) {
            ((EntityLivingBase)source.getTrueSource()).addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100 + this.getLevel(), this.getLevel() / 100, false, false));
            return true;
        }
        return false;
    }

    @Override
    public void calcShipAttributesAddEffect() {
        super.calcShipAttributesAddEffect();
        this.AttackEffectMap.put(2, new int[]{this.getLevel() / 100, 100 + this.getLevel(), this.getLevel()});
    }

    @Override
    public void calcShipAttributesAddEquip() {
        super.calcShipAttributesAddEquip();
        if (this.getStateFlag(1) && this.getStateFlag(9)) {
            MissileData md = this.getMissileData(2);
            if (md.type == 0) {
                md.type = 5;
            }
        }
    }
}
