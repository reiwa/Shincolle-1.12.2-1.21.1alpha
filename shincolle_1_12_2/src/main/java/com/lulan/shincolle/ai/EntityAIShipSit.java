package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityShip;
import net.minecraft.entity.ai.EntityAIBase;

public class EntityAIShipSit
extends EntityAIBase {
    private final BasicEntityShip host;

    public EntityAIShipSit(BasicEntityShip entity) {
        this.host = entity;
        this.setMutexBits(7);
    }

    public boolean shouldExecute() {
        return this.host.isSitting();
    }

    @Override
    public void startExecuting() {
        this.host.setSitting(true);
        this.host.setJumping(false);
    }

    @Override
    public void updateTask() {
        this.host.getNavigator().clearPath();
        this.host.setAttackTarget(null);
        this.host.setEntityTarget(null);
    }

    @Override
    public void resetTask() {
        this.host.setSitting(false);
    }
}
