package com.lulan.shincolle.entity.other;

import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityAirplane;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.utility.ParticleHelper;
import com.lulan.shincolle.utility.TargetHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

public class EntityAirplaneTakoyaki
extends BasicEntityAirplane {
    public EntityAirplaneTakoyaki(World world) {
        super(world);
        this.setSize(0.6f, 0.6f);
    }

    @Override
    public void initAttrs(IShipAttackBase host, Entity target, int scaleLevel, float ... par2) {
        this.host = host;
        this.atkTarget = target;
        this.setScaleLevel(scaleLevel);
        if (host instanceof BasicEntityShip) {
            BasicEntityShip ship = (BasicEntityShip)host;
            this.targetSelector = new TargetHelper.Selector(ship);
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
            this.shipAttrs.setAttrsBuffed(0, ship.getLevel() + ship.getAttrs().getAttrsBuffed(0) * 0.15f);
            this.shipAttrs.setAttrsBuffed(1, ship.getAttackBaseDamage(3, target));
            this.shipAttrs.setAttrsBuffed(2, ship.getAttackBaseDamage(4, target));
            this.shipAttrs.setAttrsBuffed(3, ship.getAttackBaseDamage(3, target));
            this.shipAttrs.setAttrsBuffed(4, ship.getAttackBaseDamage(4, target));
            this.shipAttrs.setAttrsBuffed(5, ship.getAttrs().getDefense() * 0.5f);
            this.shipAttrs.setAttrsBuffed(6, ship.getAttrs().getAttackSpeed() * 2.5f);
            this.shipAttrs.setAttrsBuffed(7, ship.getAttrs().getMoveSpeed() * 0.1f + 0.23f);
            this.shipAttrs.setAttrsBuffed(8, 16.0f);
            this.shipAttrs.setAttrsBuffed(15, this.shipAttrs.getAttrsBuffed(15) + 0.2f);
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
        this.numAmmoLight = 0;
        this.numAmmoHeavy = 3;
        this.shipNavigator = new ShipPathNavigate(this);
        this.shipMoveHelper = new ShipMoveHelper(this, 30.0f);
        this.setAIList();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote) {
            if (this.ticksExisted % 2 == 0) {
                this.applyFlyParticle();
            }
        } else if (!this.hasAmmoHeavy()) {
            this.backHome = true;
            this.setEntityTarget(null);
        }
    }

    @Override
    public boolean useAmmoLight() {
        return false;
    }

    @Override
    public boolean useAmmoHeavy() {
        return true;
    }

    protected void applyFlyParticle() {
        ParticleHelper.spawnAttackParticleAt(this.posX, this.posY + 0.1, this.posZ, -this.motionX * 0.5, 0.07, -this.motionZ * 0.5, (byte)18);
    }

    @Override
    public int getTextureID() {
        return 3;
    }
}
