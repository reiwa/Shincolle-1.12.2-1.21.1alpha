package com.lulan.shincolle.entity.hime;

import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShipCV;
import com.lulan.shincolle.entity.mounts.EntityMountIsH;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.utility.CombatHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.world.World;

public class EntityIsolatedHime
extends BasicEntityShipCV {
    public EntityIsolatedHime(World world) {
        super(world);
        this.setSize(0.6f, 1.6f);
        this.setStateMinor(19, 10);
        this.setStateMinor(20, 29);
        this.setStateMinor(25, 2);
        this.setStateMinor(13, 8);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[8]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[8]);
        this.ModelPos = new float[]{-6.0f, 30.0f, 0.0f, 40.0f};
        this.launchHeight = this.height * 0.7f;
        this.setFoodSaturationMax(18);
        this.postInit();
    }

    @Override
    public EntityAgeable createChild(EntityAgeable ageable) {
        return null;
    }

    @Override
    public int getEquipType() {
        return 2;
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(11, new EntityAIShipCarrierAttack(this));
        this.tasks.addTask(12, new EntityAIShipRangeAttack(this));
    }

    @Override
    public void calcShipAttributesAddEffect() {
        super.calcShipAttributesAddEffect();
        this.AttackEffectMap.put(15, new int[]{0, 100 + this.getLevel(), this.getLevel()});
        if (this.getStateFlag(1) && this.getStateFlag(9)) {
            this.AttackEffectMap.put(19, new int[]{this.getLevel() / 75, 80 + this.getLevel(), this.getLevel()});
        }
    }

    @Override
    public void calcShipAttributesAddEquip() {
        super.calcShipAttributesAddEquip();
        MissileData md = this.getMissileData(2);
        if (md.type == 0) {
            md.type = 5;
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
        return this.shipAttrs.getAttackDamage();
    }

    @Override
    public boolean hasShipMounts() {
        return true;
    }

    @Override
    public BasicEntityMount summonMountEntity() {
        return new EntityMountIsH(this.world);
    }

    @Override
        public double getMountedYOffset() {
        if (this.isSitting()) {
            if (this.getStateEmotion(1) == 4) {
                return this.height * 0.2f;
            }
            return this.height * 0.52f;
        }
        return this.height * 0.76f;
    }
}
