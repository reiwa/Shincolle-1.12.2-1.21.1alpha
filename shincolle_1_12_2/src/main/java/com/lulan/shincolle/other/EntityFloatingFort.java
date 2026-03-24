package com.lulan.shincolle.entity.other;

import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityAirplane;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.List;

public class EntityFloatingFort
extends BasicEntityAirplane {
    public EntityFloatingFort(World world) {
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
            this.numAmmoLight = 0;
            this.numAmmoHeavy = 1;
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
            this.shipAttrs.setAttrsBuffed(0, ship.getMaxHealth() * 0.1f);
            this.shipAttrs.setAttrsBuffed(1, ship.getAttackBaseDamage(1, target));
            this.shipAttrs.setAttrsBuffed(2, ship.getAttackBaseDamage(2, target));
            this.shipAttrs.setAttrsBuffed(3, ship.getAttackBaseDamage(1, target));
            this.shipAttrs.setAttrsBuffed(4, ship.getAttackBaseDamage(2, target));
            this.shipAttrs.setAttrsBuffed(5, ship.getAttrs().getDefense() * 0.5f);
            this.shipAttrs.setAttrsBuffed(6, ship.getAttrs().getAttackSpeed());
            this.shipAttrs.setAttrsBuffed(7, ship.getAttrs().getMoveSpeed() * 0.1f + 0.3f);
            this.shipAttrs.setAttrsBuffed(8, 6.0f);
            this.shipAttrs.setAttrsBuffed(15, this.shipAttrs.getAttrsBuffed(15) + 0.3f);
            this.getEntityAttribute(MAX_HP).setBaseValue(this.shipAttrs.getAttrsBuffed(0));
            this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(this.shipAttrs.getAttrsBuffed(7));
            this.getEntityAttribute(SWIM_SPEED).setBaseValue(this.shipAttrs.getAttrsBuffed(7));
            this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64.0);
            this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.5);
            if (this.getHealth() < this.getMaxHealth()) {
                this.setHealth(this.getMaxHealth());
            }
        } else {
            return;
        }
        this.shipNavigator = new ShipPathNavigate(this);
        this.shipMoveHelper = new ShipMoveHelper(this, 45.0f);
        this.setAIList();
    }

    @Override
    protected void setAIList() {
        this.clearAITasks();
        this.clearAITargetTasks();
        this.setEntityTarget(this.atkTarget);
    }

    @Override
    public void onUpdate() {
        if (this.world.isRemote) {
            if (this.ticksExisted % 2 == 0) {
                ParticleHelper.spawnAttackParticleAt(this.posX, this.posY + 0.2, this.posZ, -this.motionX * 0.5, 0.07, -this.motionZ * 0.5, (byte)29);
            }
        } else {
            if (this.backHome || this.atkTarget == null || !this.atkTarget.isEntityAlive() || this.ticksExisted >= 500) {
                this.onImpact();
                return;
            }
            this.updateAttackAI();
        }
        super.onUpdate();
    }

    private void updateAttackAI() {
        if (this.atkTarget != null) {
            float distX = (float)(this.atkTarget.posX - this.posX);
            float distY = (float)(this.atkTarget.posY + 1.0 - this.posY);
            float distZ = (float)(this.atkTarget.posZ - this.posZ);
            float distSq = distX * distX + distY * distY + distZ * distZ;
            if (this.ticksExisted % 16 == 0 && distSq > 4.0f) {
                this.getShipNavigate().tryMoveToEntityLiving(this.atkTarget, 1.0);
            }
            if (distSq <= 6.0f) {
                this.getShipNavigate().clearPathEntity();
                this.onImpact();
            }
        }
    }

    private void onImpact() {
        BasicEntityShip host2 = (BasicEntityShip)this.host;
        if (host2 == null) {
            return;
        }
        int type = this.getMissileData(2).type;
        CombatHelper.specialAttackEffect(this.host, type, new float[]{(float)this.posX, (float)this.posY, (float)this.posZ});
        List<Entity> hitList = this.world.getEntitiesWithinAABB(Entity.class, this.getEntityBoundingBox().expand(4.5, 4.5, 4.5));
        for (Entity ent : hitList) {
            float atk = this.shipAttrs.getAttackDamageHeavy();
            if (!ent.canBeCollidedWith() || !EntityHelper.isNotHost(this, ent) || TargetHelper.isEntityInvulnerable(ent)) continue;
            atk = CombatHelper.modDamageByAdditionAttrs(this.host, ent, atk, 0);
            if (TeamHelper.checkSameOwner(host2, ent)) {
                atk = 0.0f;
            }
            atk = CombatHelper.applyCombatRateToDamage(this.host, false, 1.0f, atk);
            atk = CombatHelper.applyDamageReduceOnPlayer(ent, atk);
            if (!TeamHelper.doFriendlyFire(this.host, ent)) {
                atk = 0.0f;
            }
            if (!ent.attackEntityFrom(DamageSource.causeMobDamage(host2).setExplosion(), atk) || TeamHelper.checkSameOwner(this.getHostEntity(), ent)) continue;
            BuffHelper.applyBuffOnTarget(ent, this.getAttackEffectMap());
        }
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 2, false), point);
        this.setDead();
    }

    @Override
    public boolean useAmmoLight() {
        return false;
    }

    @Override
    public boolean useAmmoHeavy() {
        return true;
    }

    @Override
    public int getTextureID() {
        return 5;
    }
}
