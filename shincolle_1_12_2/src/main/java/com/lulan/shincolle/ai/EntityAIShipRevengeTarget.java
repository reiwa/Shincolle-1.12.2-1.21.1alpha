package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.utility.TargetHelper;
import net.minecraft.entity.Entity;

public class EntityAIShipRevengeTarget
extends EntityAIShipRangeTarget {
    private int oldRevengeTime;

    public EntityAIShipRevengeTarget(IShipAttackBase host) {
        super(host, Entity.class);
        this.setMutexBits(1);
        this.oldRevengeTime = 0;
        this.targetSelector = host instanceof BasicEntityShipHostile ? new TargetHelper.RevengeSelectorForHostile(this.host2) : new TargetHelper.RevengeSelector(this.host2);
    }

    @Override
    public boolean shouldExecute() {
        if (this.oldRevengeTime != this.host.getEntityRevengeTime() && this.host.getEntityRevengeTarget() != null) {
            return this.targetSelector.apply(this.host.getEntityRevengeTarget());
        }
        return false;
    }

    @Override
    public void startExecuting() {
        this.host.setEntityTarget(this.host.getEntityRevengeTarget());
        this.oldRevengeTime = this.host.getEntityRevengeTime();
        this.host.setEntityRevengeTarget(null);
    }
}
