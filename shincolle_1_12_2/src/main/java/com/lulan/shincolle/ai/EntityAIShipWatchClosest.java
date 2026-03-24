package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipEmotion;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;

public class EntityAIShipWatchClosest
extends EntityAIWatchClosest {
    private final EntityLiving host;
    private final IShipEmotion host2;
    private final float range;

    public EntityAIShipWatchClosest(EntityLiving entity, Class target, float range, float rate) {
        super(entity, target, range, rate);
        this.setMutexBits(0);
        this.host = entity;
        this.host2 = (IShipEmotion)entity;
        this.range = range;
    }

    @Override
    public boolean shouldExecute() {
        EntityPlayer target = this.host.world.getClosestPlayerToEntity(this.host, this.range);
        if (this.host2 != null && (this.host2.getStateFlag(2) || this.host.getRidingEntity() instanceof BasicEntityShip)) {
            return false;
        }
        if (target != null && (target.isRiding() || target.isInvisible())) {
            return false;
        }
        return super.shouldExecute();
    }
}
