package com.lulan.shincolle.entity;

import com.lulan.shincolle.entity.other.EntityAirplane;
import com.lulan.shincolle.entity.other.EntityAirplaneTakoyaki;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.item.EquipAirplane;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.BlockHelper;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.Objects;

public abstract class BasicEntityShipCV
extends BasicEntityShip
implements IShipAircraftAttack {
    protected int maxAircraftLight;
    protected int maxAircraftHeavy;
    protected int delayAircraft = 0;
    protected double launchHeight;

    protected BasicEntityShipCV(World world) {
        super(world);
    }

    @Override
    public int getNumAircraftLight() {
        return this.StateMinor[7];
    }

    @Override
    public int getNumAircraftHeavy() {
        return this.StateMinor[8];
    }

    @Override
    public boolean hasAirLight() {
        return this.StateMinor[7] > 0;
    }

    @Override
    public boolean hasAirHeavy() {
        return this.StateMinor[8] > 0;
    }

    @Override
    public boolean hasAmmoLight() {
        return this.StateMinor[4] >= 6 * this.StateMinor[29];
    }

    @Override
    public boolean hasAmmoHeavy() {
        return this.StateMinor[5] >= 2 * this.StateMinor[29];
    }

    @Override
    public void setNumAircraftLight(int par1) {
        if (this.world.isRemote) {
            this.StateMinor[7] = par1;
        } else {
            this.StateMinor[7] = par1;
            if (this.getNumAircraftLight() > this.maxAircraftLight) {
                this.StateMinor[7] = this.maxAircraftLight;
            }
            if (this.getNumAircraftLight() < 0) {
                this.StateMinor[7] = 0;
            }
        }
    }

    @Override
    public void setNumAircraftHeavy(int par1) {
        if (this.world.isRemote) {
            this.StateMinor[8] = par1;
        } else {
            this.StateMinor[8] = par1;
            if (this.getNumAircraftHeavy() > this.maxAircraftHeavy) {
                this.StateMinor[8] = this.maxAircraftHeavy;
            }
            if (this.getNumAircraftHeavy() < 0) {
                this.StateMinor[8] = 0;
            }
        }
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote) {
            --this.delayAircraft;
            if (this.delayAircraft <= 0) {
                this.delayAircraft = (int)(ConfigHandler.airplaneDelay / this.getAttrs().getAttackSpeed());
                if (this.delayAircraft > ConfigHandler.airplaneDelay) {
                    this.delayAircraft = ConfigHandler.airplaneDelay;
                }
                this.delayAircraft += 20;
                this.setNumAircraftLight(this.getNumAircraftLight() + 1);
                this.setNumAircraftHeavy(this.getNumAircraftHeavy() + 1);
            }
        }
    }

    @Override
    public void calcShipAttributesAddEquip() {
        super.calcShipAttributesAddEquip();
        int numair = this.getNumOfAircraftEquip();
        this.maxAircraftLight += numair * 4;
        this.maxAircraftHeavy += numair * 2;
    }

    @Override
    public void calcShipAttributesAddRaw() {
        super.calcShipAttributesAddRaw();
        this.maxAircraftLight = 8 + this.StateMinor[0] / 5;
        this.maxAircraftHeavy = 4 + this.StateMinor[0] / 10;
    }

    public int getNumOfAircraftEquip() {
        int airNum = 0;
        for (int i = 0; i < 6; ++i) {
            ItemStack stack = this.itemHandler.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof EquipAirplane)) continue;
            ++airNum;
        }
        return airNum;
    }

    public BasicEntityAirplane getAttackAirplane(boolean isLightAirplane) {
        if (isLightAirplane) {
            return new EntityAirplane(this.world);
        }
        return new EntityAirplaneTakoyaki(this.world);
    }

    @Override
    public boolean attackEntityWithAircraft(Entity target) {
        if (this.getNumAircraftLight() <= 0 || !this.decrAmmoNum(0, 6 * this.getAmmoConsumption())) {
            return false;
        }
        if (this.rand.nextInt(2) == 0) {
            this.setEntityTarget(null);
        }
        this.setNumAircraftLight(this.getNumAircraftLight() - 1);
        this.addShipExp(ConfigHandler.expGain[3]);
        this.decrGrudgeNum(ConfigHandler.consumeGrudgeAction[2]);
        this.decrMorale(3);
        this.setCombatTick(this.ticksExisted);
        this.applySoundAtAttacker(3);
        this.applyParticleAtAttacker(3, Dist4d.ONE);
        float summonHeight = (float)(this.posY + this.launchHeight);
        if (!BlockHelper.checkBlockSafe(this.world, (int)this.posX, (int)(this.posY + this.launchHeight), (int)this.posZ)) {
            summonHeight = (float)this.posY + 1.0f;
        }
        if (this.getRidingEntity() instanceof BasicEntityMount) {
            summonHeight -= 1.5f;
        }
        BasicEntityAirplane plane = this.getAttackAirplane(true);
        plane.initAttrs(this, target, 0, summonHeight);
        this.world.spawnEntity(plane);
        this.applySoundAtTarget(3);
        this.applyParticleAtTarget(3, target);
        this.applyEmotesReaction(3);
        this.applyAttackPostMotion(3, target, true, 0.0f);
        return true;
    }

    @Override
    public boolean attackEntityWithHeavyAircraft(Entity target) {
        if (this.getNumAircraftHeavy() <= 0 || !this.decrAmmoNum(1, 2 * this.getAmmoConsumption())) {
            return false;
        }
        if (this.rand.nextInt(2) == 0) {
            this.setEntityTarget(null);
        }
        this.setNumAircraftHeavy(this.getNumAircraftHeavy() - 1);
        this.addShipExp(ConfigHandler.expGain[4]);
        this.decrGrudgeNum(ConfigHandler.consumeGrudgeAction[3]);
        this.decrMorale(4);
        this.setCombatTick(this.ticksExisted);
        this.applySoundAtAttacker(4);
        this.applyParticleAtAttacker(4, Dist4d.ONE);
        float summonHeight = (float)(this.posY + this.launchHeight);
        if (!BlockHelper.checkBlockSafe(this.world, (int)this.posX, (int)(this.posY + this.launchHeight), (int)this.posZ)) {
            summonHeight = (float)this.posY + 0.5f;
        }
        if (this.getRidingEntity() instanceof BasicEntityMount) {
            summonHeight -= 1.5f;
        }
        BasicEntityAirplane plane = this.getAttackAirplane(false);
        plane.initAttrs(this, target, 0, summonHeight);
        this.world.spawnEntity(plane);
        this.applySoundAtTarget(4);
        this.applyParticleAtTarget(4, target);
        this.applyEmotesReaction(3);
        this.applyAttackPostMotion(4, target, true, 0.0f);
        return true;
    }
}