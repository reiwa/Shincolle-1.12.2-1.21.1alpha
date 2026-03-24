package com.lulan.shincolle.entity.cruiser;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.handler.ConfigHandler;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

public class EntityCARi
extends BasicEntityShipSmall {
    public EntityCARi(World world) {
        super(world);
        this.setSize(0.75f, 1.7f);
        this.setStateMinor(19, 2);
        this.setStateMinor(20, 9);
        this.setStateMinor(25, 4);
        this.setStateMinor(13, 4);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[2]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[2]);
        this.ModelPos = new float[]{0.0f, 20.0f, 0.0f, 40.0f};
        this.StateFlag[15] = false;
        this.StateFlag[16] = false;
        this.setFoodSaturationMax(14);
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
    public void calcShipAttributesAddRaw() {
        super.calcShipAttributesAddRaw();
        if (!this.world.isDaytime()) {
            this.getAttrs().setAttrsRaw(9, this.getAttrs().getAttrsRaw(9) + 0.15f);
            this.getAttrs().setAttrsRaw(12, this.getAttrs().getAttrsRaw(12) + 0.15f);
        }
    }

    @Override
    public void onLivingUpdate() {
        if (!this.world.isRemote && this.ticksExisted % 128 == 0 && !this.world.isDaytime() && this.getStateFlag(9)) {
            this.addPotionEffect(new PotionEffect(MobEffects.SPEED, 150, this.getStateMinor(0) / 70, false, false));
            this.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 150, this.getStateMinor(0) / 60, false, false));
        }
        super.onLivingUpdate();
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            if (this.getStateEmotion(1) == 4) {
                return this.height * 0.22f;
            }
            return this.height * 0.33f;
        }
        return this.height * 0.72f;
    }
}
