package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.utility.LogHelper;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.Vec3d;

public class EntityAIShipWander
extends EntityAIBase {
    private final IShipAttackBase host;
    private final EntityCreature host2;
    private double xPosition;
    private double yPosition;
    private double zPosition;
    private final double speed;
    private final int ranXZ;
    private final int ranY;
    private int executionCooldown;

    public EntityAIShipWander(EntityCreature host, int rangeXZ, int rangeY, double speed) {
        if (host instanceof IShipAttackBase) {
            this.host = (IShipAttackBase)host;
            this.host2 = host;
        } else {
            LogHelper.debug("DEBUG: wander AI: host is not ship!");
            this.host = null;
            this.host2 = null;
        }
        this.ranXZ = rangeXZ;
        this.ranY = rangeY;
        this.speed = speed;
        this.setMutexBits(7);
    }

    public boolean shouldExecute() {
        if (this.executionCooldown > 0) {
            --this.executionCooldown;
            return false;
        }

        if (this.host.getIsRiding() || this.host.getIsSitting() || this.host.getStateFlag(2) || this.host.getStateMinor(43) > 0 || this.host2.getRNG().nextInt(180) != 0) {
            return false;
        }
        if (this.host instanceof BasicEntityShip ? ((BasicEntityShip)this.host).fishHook != null : this.host instanceof BasicEntityMount && this.host.getHostEntity() != null && ((BasicEntityShip) this.host.getHostEntity()).fishHook != null) {
            return false;
        }
        Vec3d vec3 = RandomPositionGenerator.findRandomTarget(this.host2, this.ranXZ, this.ranY);
        if (vec3 == null) {
            return false;
        }
        this.xPosition = vec3.x;
        this.yPosition = vec3.y;
        this.zPosition = vec3.z;
        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return !this.host.getShipNavigate().noPath();
    }

    @Override
    public void startExecuting() {
        this.host.getShipNavigate().tryMoveToXYZ(this.xPosition, this.yPosition, this.zPosition, this.speed);
    }

    @Override
    public void resetTask() {
        this.executionCooldown = 200 + this.host2.getRNG().nextInt(100) - 50;
        this.host.getShipNavigate().clearPathEntity();
    }
}
