package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityAirplane;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.utility.BlockHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;

public class EntityAIShipAircraftAttack
extends EntityAIBase {
    private final BasicEntityAirplane host;
    private Entity target;
    private int atkDelay = 0;
    private int maxDelay = 0;
    private float rangeSq;
    private double[] randPos = new double[3];
    private double distSq;
    private double distX;
    private double distY;
    private double distZ;

    public EntityAIShipAircraftAttack(BasicEntityAirplane host) {
        if (!(host instanceof BasicEntityAirplane)) {
            throw new IllegalArgumentException("AircraftAttack AI requires BasicEntityAirplane");
        }
        this.host = host;
        this.setMutexBits(3);
    }

    public boolean shouldExecute() {
        Entity targetentity = this.host.getEntityTarget();
        if (!this.host.canFindTarget()) {
            return false;
        }
        if (this.host.ticksExisted > 20 && targetentity != null && targetentity.isEntityAlive() && (this.host.useAmmoLight() && this.host.hasAmmoLight() || this.host.useAmmoHeavy() && this.host.hasAmmoHeavy())) {
            this.target = targetentity;
            return true;
        }
        return false;
    }

    @Override
    public void startExecuting() {
        this.maxDelay = (int)(ConfigHandler.baseAttackSpeed[4] / this.host.getAttrs().getAttackSpeed()) + ConfigHandler.fixedAttackDelay[4];
        float attackRange = this.host.useAmmoHeavy() ? 16.0f : 6.0f;
        this.rangeSq = attackRange * attackRange;
        this.distZ = 0.0;
        this.distY = 0.0;
        this.distX = 0.0;
        this.distSq = 0.0;
        this.randPos[0] = this.target.posX;
        this.randPos[1] = this.target.posX;
        this.randPos[2] = this.target.posX;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (!this.host.canFindTarget()) {
            return false;
        }
        return this.shouldExecute() || this.target != null && this.target.isEntityAlive() && !this.host.getShipNavigate().noPath();
    }

    @Override
    public void resetTask() {
        this.target = null;
        this.randPos = this.host.useAmmoHeavy() ? BlockHelper.findRandomPosition(this.host, this.host, 12.0, 4.0, 2) : BlockHelper.findRandomPosition(this.host, this.host, 4.5, 1.5, 2);
        this.host.getShipNavigate().tryMoveToXYZ(this.randPos[0], this.randPos[1], this.randPos[2], 1.0);
    }

    @Override
    public void updateTask() {
        boolean onSight = false;
        if (this.target != null) {
            this.distX = this.target.posX - this.host.posX;
            this.distY = this.target.posY + 2.0 - this.host.posY;
            this.distZ = this.target.posZ - this.host.posZ;
            this.distSq = this.distX * this.distX + this.distY * this.distY + this.distZ * this.distZ;
            if ((this.host.ticksExisted & 0xF) == 0) {
                this.randPos = this.host.useAmmoHeavy() ? BlockHelper.findRandomPosition(this.host, this.target, 12.0, 4.0, 2) : BlockHelper.findRandomPosition(this.host, this.target, 4.5, 1.5, 2);
                if (this.distSq > this.rangeSq) {
                    this.host.getShipNavigate().tryMoveToXYZ(this.randPos[0], this.randPos[1], this.randPos[2], 1.0);
                } else {
                    this.host.getShipNavigate().tryMoveToXYZ(this.randPos[0], this.randPos[1], this.randPos[2], 0.4);
                }
            }
            --this.atkDelay;
            onSight = this.host.canEntityBeSeen(this.target);
            if (this.atkDelay <= 0 && onSight && this.distSq < this.rangeSq) {
                if (this.host.useAmmoLight() && this.host.hasAmmoLight()) {
                    this.host.attackEntityWithAmmo(this.target);
                    this.atkDelay = this.maxDelay;
                }
                if (this.host.useAmmoHeavy() && this.host.hasAmmoHeavy()) {
                    this.host.attackEntityWithHeavyAmmo(this.target);
                    this.atkDelay = this.maxDelay;
                }
            }
        }
    }
}
