package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipEmotion;
import com.lulan.shincolle.entity.other.EntitySeat;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;

public class EntityAIShipLookIdle
extends EntityAIBase {
    private final EntityLiving host;
    private final IShipEmotion host2;
    private double lookX;
    private double lookZ;
    private int idleTime;

    public EntityAIShipLookIdle(EntityLiving entity) {
        this.host = entity;
        this.host2 = (IShipEmotion)entity;
        this.setMutexBits(0);
    }

    public boolean shouldExecute() {
        if (this.host2.getStateFlag(2) || this.host.getRidingEntity() instanceof BasicEntityShip || this.host.getRidingEntity() instanceof EntitySeat) {
            return false;
        }
        return this.host.getRNG().nextFloat() < 0.02f;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.idleTime >= 0;
    }

    @Override
    public void startExecuting() {
        double d0 = Math.PI * 2 * this.host.getRNG().nextDouble();
        this.lookX = Math.cos(d0);
        this.lookZ = Math.sin(d0);
        this.idleTime = 20 + this.host.getRNG().nextInt(20);
    }

    @Override
    public void updateTask() {
        --this.idleTime;
        this.host.getLookHelper().setLookPosition(this.host.posX + this.lookX, this.host.posY + this.host.getEyeHeight(), this.host.posZ + this.lookZ, this.host.getHorizontalFaceSpeed(), this.host.getVerticalFaceSpeed());
    }
}
