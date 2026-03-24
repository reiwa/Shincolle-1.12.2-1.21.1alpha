package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.IShipCannonAttack;
import com.lulan.shincolle.utility.CombatHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;

public class EntityAIShipRangeAttack
extends EntityAIBase {
    private final IShipCannonAttack host;
    private final EntityLiving host2;
    private Entity target;
    private int delayLight;
    private int maxDelayLight;
    private int delayHeavy;
    private int maxDelayHeavy;
    private int onSightTime;
    private float rangeSq;
    private int aimTime;
    private double distSq;
    private double distX;
    private double distY;
    private double distZ;

    public EntityAIShipRangeAttack(IShipCannonAttack host) {
        if (!(host instanceof IShipCannonAttack)) {
            throw new IllegalArgumentException("RangeAttack AI requires interface IShipCannonAttack");
        }
        this.host = host;
        this.host2 = (EntityLiving)host;
        this.setMutexBits(1);
        this.delayLight = 20;
        this.delayHeavy = 40;
        this.maxDelayLight = 20;
        this.maxDelayHeavy = 40;
    }

    public boolean shouldExecute() {
        if (this.host2 != null) {
            if (this.host.getIsSitting() || this.host.getStateMinor(43) > 0) {
                return false;
            }
            if (this.host.getIsRiding() && this.host2.getRidingEntity() instanceof BasicEntityMount) {
                return false;
            }
            Entity targetentity = this.host.getEntityTarget();
            if (targetentity != null && targetentity.isEntityAlive() && (this.host.getAttackType(13) && this.host.getStateFlag(4) && this.host.hasAmmoLight() || this.host.getAttackType(14) && this.host.getStateFlag(5) && this.host.hasAmmoHeavy())) {
                this.target = targetentity;
                return true;
            }
        }
        return false;
    }

    @Override
    public void startExecuting() {
        if (this.host != null) {
            this.updateAttackParms();
            if (this.delayLight <= this.aimTime) {
                this.delayLight = this.aimTime;
            }
            if (this.delayHeavy <= this.aimTime * 2) {
                this.delayHeavy = this.aimTime * 2;
            }
            this.distZ = 0.0;
            this.distY = 0.0;
            this.distX = 0.0;
            this.distSq = 0.0;
        }
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (this.host != null) {
            if (this.target != null && this.target.isEntityAlive() && !this.host.getShipNavigate().noPath()) {
                return true;
            }
            return this.shouldExecute();
        }
        return false;
    }

    @Override
    public void resetTask() {
        this.target = null;
        this.onSightTime = 0;
    }

    @Override
    public void updateTask() {
        boolean onSight;
        if (this.host != null && this.target != null) {
            if (this.host2.ticksExisted % 64 == 0) {
                this.updateAttackParms();
            }
            --this.delayLight;
            --this.delayHeavy;
            this.distX = this.target.posX - this.host2.posX;
            this.distY = this.target.posY - this.host2.posY;
            this.distZ = this.target.posZ - this.host2.posZ;
            this.distSq = this.distX * this.distX + this.distY * this.distY + this.distZ * this.distZ;
            onSight = this.host2.canEntityBeSeen(this.target);
            if (onSight) {
                ++this.onSightTime;
            } else {
                this.onSightTime = 0;
                if (this.host.getStateFlag(12)) {
                    this.resetTask();
                    return;
                }
            }
            if (this.distSq < this.rangeSq && onSight && !this.host.getStateFlag(3)) {
                this.host.getShipNavigate().clearPathEntity();
            } else if (this.host2.ticksExisted % 32 == 0) {
                this.host.getShipNavigate().tryMoveToEntityLiving(this.target, 1.0);
            }
            this.host2.getLookHelper().setLookPositionWithEntity(this.target, 30.0f, 30.0f);
            if (onSight && this.distSq <= this.rangeSq && this.onSightTime >= this.aimTime) {
                if (this.delayLight <= 0 && this.host.useAmmoLight() && this.host.hasAmmoLight()) {
                    this.host.attackEntityWithAmmo(this.target);
                    this.delayLight = this.maxDelayLight;
                }
                if (this.delayHeavy <= 0 && this.host.useAmmoHeavy() && this.host.hasAmmoHeavy()) {
                    this.host.attackEntityWithHeavyAmmo(this.target);
                    this.delayHeavy = this.maxDelayHeavy;
                }
            }
            if (this.delayHeavy < -40 && this.delayLight < -40) {
                this.delayLight = 20;
                this.delayHeavy = 20;
                this.resetTask();
            }
        }
    }

    private void updateAttackParms() {
        this.maxDelayLight = CombatHelper.getAttackDelay(this.host.getAttrs().getAttackSpeed(), 1);
        this.maxDelayHeavy = CombatHelper.getAttackDelay(this.host.getAttrs().getAttackSpeed(), 2);
        this.aimTime = (int)(20.0f * (150 - this.host.getLevel()) / 150.0f) + 10;
        float range = this.host.getAttrs().getAttackRange();
        this.rangeSq = range * range;
    }
}
