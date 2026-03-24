package com.lulan.shincolle.entity.other;

import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityAirplane;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.utility.ParticleHelper;
import com.lulan.shincolle.utility.TargetHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class EntityAirplane
extends BasicEntityAirplane {
    public EntityAirplane(World world) {
        super(world);
        this.setSize(0.5f, 0.5f);
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
            this.shipAttrs.setAttrsBuffed(0, ship.getLevel() + ship.getAttrs().getAttrsBuffed(0) * 0.1f);
            this.shipAttrs.setAttrsBuffed(1, ship.getAttackBaseDamage(3, target));
            this.shipAttrs.setAttrsBuffed(2, ship.getAttackBaseDamage(4, target));
            this.shipAttrs.setAttrsBuffed(3, ship.getAttackBaseDamage(3, target));
            this.shipAttrs.setAttrsBuffed(4, ship.getAttackBaseDamage(4, target));
            this.shipAttrs.setAttrsBuffed(5, ship.getAttrs().getDefense() * 0.5f);
            this.shipAttrs.setAttrsBuffed(6, ship.getAttrs().getAttackSpeed() * 3.0f);
            this.shipAttrs.setAttrsBuffed(7, ship.getAttrs().getMoveSpeed() * 0.2f + 0.3f);
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
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote) {
            this.applyFlyParticle();
        } else if (!this.hasAmmoLight()) {
            this.backHome = true;
            this.setEntityTarget(null);
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float atk) {
        if (this.world.isRemote) {
            return false;
        }
        if (atk > this.getMaxHealth() * 0.5f && this.getRNG().nextInt(3) == 0) {
            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 34, false), point);
            return false;
        }
        return super.attackEntityFrom(source, atk);
    }

    protected void applyFlyParticle() {
        ParticleHelper.spawnAttackParticleAt(this.posX - this.motionX * 1.5, this.posY + 0.5 - this.motionY * 1.5, this.posZ - this.motionZ * 1.5, -this.motionX * 0.5, -this.motionY * 0.5, -this.motionZ * 0.5, (byte)17);
    }

    @Override
    public int getTextureID() {
        return 1;
    }

    @Override
    public boolean useAmmoLight() {
        return true;
    }

    @Override
    public boolean useAmmoHeavy() {
        return false;
    }
}
