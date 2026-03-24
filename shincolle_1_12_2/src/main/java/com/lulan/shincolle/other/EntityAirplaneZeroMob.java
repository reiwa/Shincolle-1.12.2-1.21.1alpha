package com.lulan.shincolle.entity.other;

import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.utility.TargetHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

public class EntityAirplaneZeroMob
extends EntityAirplaneZero {
    public EntityAirplaneZeroMob(World world) {
        super(world);
    }

    @Override
    public void initAttrs(IShipAttackBase host, Entity target, int scaleLevel, float ... par2) {
        this.host = host;
        this.atkTarget = target;
        this.setScaleLevel(scaleLevel);
        if (host instanceof BasicEntityShipHostile) {
            BasicEntityShipHostile ship = (BasicEntityShipHostile)host;
            this.targetSelector = new TargetHelper.SelectorForHostile(ship);
            this.targetSorter = new TargetHelper.Sorter(ship);
            float launchPos = (float)ship.posY;
            if (par2 != null) {
                launchPos = par2[0];
            }
            this.posX = ship.posX;
            this.posY = launchPos;
            this.posZ = ship.posZ;
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            this.setPosition(this.posX, this.posY, this.posZ);
            this.shipAttrs = Attrs.copyAttrs(ship.getAttrs());
            this.shipAttrs.setAttrsBuffed(0, ship.getMaxHealth() * 0.06f);
            this.shipAttrs.setAttrsBuffed(1, ship.getAttackBaseDamage(3, target));
            this.shipAttrs.setAttrsBuffed(2, ship.getAttackBaseDamage(4, target));
            this.shipAttrs.setAttrsBuffed(3, ship.getAttackBaseDamage(3, target));
            this.shipAttrs.setAttrsBuffed(4, ship.getAttackBaseDamage(4, target));
            this.shipAttrs.setAttrsBuffed(5, ship.getAttrs().getDefense() * 0.5f);
            this.shipAttrs.setAttrsBuffed(6, ship.getAttrs().getAttackSpeed() * 3.0f);
            this.shipAttrs.setAttrsBuffed(7, ship.getAttrs().getMoveSpeed() * 0.2f + 0.2f + this.getScaleLevel() * 0.05f);
            this.shipAttrs.setAttrsBuffed(8, 16.0f);
            this.shipAttrs.setAttrsBuffed(15, this.shipAttrs.getAttrsBuffed(15) + 0.3f);
            this.getEntityAttribute(MAX_HP).setBaseValue(this.shipAttrs.getAttrsBuffed(0));
            this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(this.shipAttrs.getAttrsBuffed(7));
            this.getEntityAttribute(SWIM_SPEED).setBaseValue(this.shipAttrs.getAttrsBuffed(7));
            this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64.0);
            this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
            if (this.getHealth() < this.getMaxHealth()) {
                this.setHealth(this.getMaxHealth());
            }
        } else {
            return;
        }
        this.numAmmoLight = 9;
        this.numAmmoHeavy = 0;
        this.shipNavigator = new ShipPathNavigate(this);
        this.shipMoveHelper = new ShipMoveHelper(this, 36.0f);
        this.setAIList();
    }

    @Override
    public int getPlayerUID() {
        return -100;
    }

    @Override
    protected void setSizeWithScaleLevel() {
        switch (this.getScaleLevel()) {
            case 3: {
                this.setSize(2.0f, 2.0f);
                break;
            }
            case 2: {
                this.setSize(1.5f, 1.5f);
                break;
            }
            case 1: {
                this.setSize(1.0f, 1.0f);
                break;
            }
            default: {
                this.setSize(0.5f, 0.5f);
            }
        }
    }

    @Override
    protected void setAttrsWithScaleLevel() {
    }
}
