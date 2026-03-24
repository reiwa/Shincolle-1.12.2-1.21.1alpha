package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.other.EntitySeat;
import net.minecraft.entity.ai.EntityAIBase;

public class EntityAIShipGetOffChair extends EntityAIBase {

    private final BasicEntityShip host;

    public EntityAIShipGetOffChair(BasicEntityShip entity) {
        this.host = entity;
        this.setMutexBits(7);
    }

    @Override
    public boolean shouldExecute() {
        if (this.host.isAIDisabled()) {
            return false;
        }
        if (this.host.getRNG().nextInt(300) != 0) {
            return false;
        }
        return this.host.isRiding() && this.host.getRidingEntity() instanceof EntitySeat;
    }

    @Override
    public void startExecuting() {
        this.host.dismountRidingEntity();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return false;
    }
}