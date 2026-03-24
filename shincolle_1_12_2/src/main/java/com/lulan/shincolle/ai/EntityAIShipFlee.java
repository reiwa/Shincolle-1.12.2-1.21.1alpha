package com.lulan.shincolle.ai;

import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.Vec3d;

public class EntityAIShipFlee
extends EntityAIBase {
    private final BasicEntityShip host;
    private EntityLivingBase owner;
    private final ShipPathNavigate shippathfinder;
    private float distSq;
    private int findCooldown;

    public EntityAIShipFlee(BasicEntityShip entity) {
        this.host = entity;
        this.shippathfinder = entity.getShipNavigate();
        this.setMutexBits(7);
    }

    public boolean shouldExecute() {
        EntityLivingBase ownerentity;
        float fleehp = this.host.getStateMinor(12) * 0.01f;
        if (!this.host.isSitting() && !this.host.getLeashed() && this.host.getHealth() / this.host.getMaxHealth() <= fleehp && this.host.getStateMinor(6) > 0 && (ownerentity = (EntityLivingBase)this.host.getHostEntity()) != null) {
            this.owner = ownerentity;
            double distX = this.owner.posX - this.host.posX;
            double distY = this.owner.posY - this.host.posY;
            double distZ = this.owner.posZ - this.host.posZ;
            this.distSq = (float)(distX * distX + distY * distY + distZ * distZ);
            return this.distSq > 6.0f && this.distSq < 3600.0f;
        }
        return false;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.shouldExecute();
    }

    @Override
    public void startExecuting() {
        this.findCooldown = 0;
    }

    @Override
    public void resetTask() {
        this.owner = null;
        this.shippathfinder.clearPathEntity();
    }

    @Override
    public void updateTask() {
        --this.findCooldown;
        this.host.getLookHelper().setLookPositionWithEntity(this.owner, 10.0f, this.host.getVerticalFaceSpeed());
        if (this.findCooldown <= 0) {
            this.findCooldown = 16;
            boolean canMove = false;
            canMove = this.host.isRiding() && this.host.getRidingEntity() instanceof BasicEntityMount ? ((BasicEntityMount)this.host.getRidingEntity()).getShipNavigate().tryMoveToEntityLiving(this.owner, 1.2) : this.shippathfinder.tryMoveToEntityLiving(this.owner, 1.2);
            if (!canMove) {
                if (!ConfigHandler.canTeleport) {
                    return;
                }
                if (this.distSq > 100.0f) {
                    LogHelper.debug("DEBUG: flee AI: moving fail, teleport entity " + this.host);
                    if (this.host.dimension == this.owner.dimension) {
                        EntityHelper.applyTeleport(this.host, this.distSq, new Vec3d(this.owner.posX, this.owner.posY + 0.5, this.owner.posZ));
                    }
                }
            }
        }
    }
}
