package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.utility.CombatHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

public class EntityAIShipAttackOnCollide
extends EntityAIBase {
    private final IShipAttackBase host;
    private final EntityLiving host2;
    private Entity target;
    private final double moveSpeed;
    private int delayAttack;
    private int delayMax;

    public EntityAIShipAttackOnCollide(IShipAttackBase host, double speed) {
        this.host = host;
        this.host2 = (EntityLiving)host;
        this.moveSpeed = speed;
        this.delayMax = 20;
        this.delayAttack = 20;
        this.setMutexBits(4);
    }

    public boolean shouldExecute() {
        if (this.host2.isRiding() || this.host.getIsSitting()) {
            return false;
        }
        this.target = this.host.getEntityTarget();
        if (this.target == null) {
            return false;
        }
        return this.target.isEntityAlive();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.shouldExecute();
    }

    @Override
    public void startExecuting() {
    }

    @Override
    public void resetTask() {
    }

    @Override
    public void updateTask() {
        if (this.host2 == null || this.target == null || !this.target.isEntityAlive()) {
            this.resetTask();
            return;
        }
        this.host2.getLookHelper().setLookPositionWithEntity(this.target, 30.0f, 30.0f);
        double distTarget = this.host2.getDistanceSq(this.target.posX, this.target.getEntityBoundingBox().minY, this.target.posZ);
        double distAttack = this.host2.width * this.host2.width * 16.0f;
        if (this.host2.ticksExisted % 32 == 0) {
            if (this.host != null) {
                this.delayMax = CombatHelper.getAttackDelay(this.host.getAttrs().getAttackSpeed(), 0);
            } else {
                return;
            }
            if (distTarget > distAttack) {
                this.host.getShipNavigate().tryMoveToEntityLiving(this.target, this.moveSpeed);
            } else {
                this.host.getShipNavigate().clearPathEntity();
            }
        }
        if (distTarget <= distAttack && --this.delayAttack == 0) {
            this.delayAttack = this.delayMax;
            if (this.host2.getHeldItem(EnumHand.MAIN_HAND) != ItemStack.EMPTY) {
                this.host2.swingArm(EnumHand.MAIN_HAND);
            }
            this.host2.attackEntityAsMob(this.target);
        }
    }
}
