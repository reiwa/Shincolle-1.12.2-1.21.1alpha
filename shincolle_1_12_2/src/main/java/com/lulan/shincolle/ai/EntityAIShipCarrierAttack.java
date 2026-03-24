package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.IShipAircraftAttack;
import com.lulan.shincolle.utility.CombatHelper;
import com.lulan.shincolle.utility.LogHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;

public class EntityAIShipCarrierAttack
extends EntityAIBase {
    private final IShipAircraftAttack host;
    private final EntityLiving host2;
    private Entity target;
    private int launchDelay;
    private int launchDelayMax;
    private boolean launchType;
    private float range;
    private float rangeSq;
    private double distSq;
    private double distX;
    private double distY;
    private double distZ;

    public EntityAIShipCarrierAttack(IShipAircraftAttack host) {
        if (!(host instanceof IShipAircraftAttack)) {
            throw new IllegalArgumentException("CarrierAttack AI requires IShipAircraftAttack");
        }
        this.host = host;
        this.host2 = (EntityLiving)host;
        this.setMutexBits(2);
        this.launchDelay = 20;
        this.launchDelayMax = 40;
        this.launchType = false;
    }

    public boolean shouldExecute() {
        if (this.host.getIsSitting() || this.host.getStateMinor(43) > 0) {
            return false;
        }
        if (this.host.getIsRiding() && this.host2.getRidingEntity() instanceof BasicEntityMount) {
            return false;
        }
        Entity targetentity = this.host.getEntityTarget();
        if (targetentity != null && targetentity.isEntityAlive() && (this.host.getAttackType(15) && this.host.getStateFlag(6) && this.host.hasAmmoLight() && this.host.hasAirLight() || this.host.getAttackType(16) && this.host.getStateFlag(7) && this.host.hasAmmoHeavy() && this.host.hasAirHeavy())) {
            this.target = targetentity;
            return true;
        }
        return false;
    }

    @Override
    public void startExecuting() {
        this.distZ = 0.0;
        this.distY = 0.0;
        this.distX = 0.0;
        this.distSq = 0.0;
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
    }

    @Override
    public void updateTask() {
        if (this.target != null && this.host != null) {
            boolean onSight = this.host2.canEntityBeSeen(this.target);
            if (!onSight && this.host.getStateFlag(12)) {
                this.resetTask();
                return;
            }
            if (this.host2.ticksExisted % 64 == 0) {
                this.launchDelayMax = this.launchType ? CombatHelper.getAttackDelay(this.host.getAttrs().getAttackSpeed(), 3) : CombatHelper.getAttackDelay(this.host.getAttrs().getAttackSpeed(), 4);
                this.range = this.host.getAttrs().getAttackRange();
                this.rangeSq = this.range * this.range;
            }
            if (this.distSq >= this.rangeSq) {
                this.distX = this.target.posX - this.host2.posX;
                this.distY = this.target.posY - this.host2.posY;
                this.distZ = this.target.posZ - this.host2.posZ;
                this.distSq = this.distX * this.distX + this.distY * this.distY + this.distZ * this.distZ;
                if (this.distSq < this.rangeSq && onSight && !this.host.getStateFlag(3)) {
                    this.host.getShipNavigate().clearPathEntity();
                } else if (this.host2.ticksExisted % 32 == 0) {
                    this.host.getShipNavigate().tryMoveToEntityLiving(this.target, 1.0);
                }
            }
            this.host2.getLookHelper().setLookPosition(this.target.posX, this.target.posY + 2.0, this.target.posZ, 30.0f, 60.0f);
            --this.launchDelay;
            boolean canLight = this.host.getStateFlag(6) && this.host.hasAmmoLight() && this.host.hasAirLight();
            boolean canHeavy = this.host.getStateFlag(7) && this.host.hasAmmoHeavy() && this.host.hasAirHeavy();
            if (canLight) {
                this.launchType = true;
            } else if (canHeavy) {
                this.launchType = false;
            }
            if (onSight && this.distSq <= this.rangeSq && this.launchDelay <= 0) {
                if (this.launchType && this.host.hasAmmoLight() && this.host.hasAirLight()) {
                    this.host.attackEntityWithAircraft(this.target);
                    this.launchDelay = this.launchDelayMax;
                }
                if (!this.launchType && this.host.hasAmmoHeavy() && this.host.hasAirHeavy()) {
                    this.host.attackEntityWithHeavyAircraft(this.target);
                    this.launchDelay = this.launchDelayMax;
                }
            }
            if (this.launchDelay < -80) {
                this.launchDelay = 20;
                this.resetTask();
            }
        }
    }
}
