package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipFloating;
import com.lulan.shincolle.entity.IShipGuardian;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

public class EntityAIShipFloating
extends EntityAIBase {
    private final IShipFloating host;
    private BasicEntityShip hostShip;
    private BasicEntityMount hostMount;
    private final EntityLivingBase hostLiving;

    public EntityAIShipFloating(IShipFloating entity) {
        this.host = entity;
        this.hostLiving = (EntityLivingBase)entity;
        if (entity instanceof BasicEntityShip) {
            this.hostShip = (BasicEntityShip)entity;
        } else if (entity instanceof BasicEntityMount) {
            this.hostMount = (BasicEntityMount)entity;
        }
        this.setMutexBits(8);
    }

    public boolean shouldExecute() {
        if (this.hostShip != null) {
            if (this.hostShip.getStateFlag(0) && this.hostShip.getShipDepth() > this.hostShip.getShipFloatingDepth()) {
                return !this.hostShip.isRiding() && this.hostShip.getStateMinor(43) <= 0 && this.hostShip.getShipNavigate().noPath() && !EntityAIShipFloating.isInGuardPosition(this.hostShip);
            }
            return false;
        }
        if (this.hostMount != null && this.hostMount.getHostEntity() != null) {
            if (this.hostMount.getShipDepth() > this.hostMount.getShipFloatingDepth()) {
                BasicEntityShip hostentity = (BasicEntityShip)this.hostMount.getHostEntity();
                if (hostentity.getStateMinor(43) > 0 || !hostentity.getShipNavigate().noPath() || EntityAIShipFloating.isInGuardPosition(hostentity)) {
                    return false;
                }
                return this.hostMount.getShipNavigate().noPath() && !EntityAIShipFloating.isInGuardPosition(this.hostMount);
            }
            return false;
        }
        return this.host.getShipDepth() > this.host.getShipFloatingDepth();
    }

    @Override
    public void updateTask() {
        double depth = this.host.getShipDepth();
        double upwardForce = 0.0;
        if (depth > 0.15) {
            upwardForce = (0.035 * Math.pow(depth, 0.6)) - 0.005;
        }
        this.hostLiving.motionY += upwardForce;
        this.hostLiving.motionY *= 0.80;
        if (this.hostLiving.motionY > 0.1) {
            this.hostLiving.motionY = 0.1;
        } else if (this.hostLiving.motionY < -0.1) {
            this.hostLiving.motionY = -0.1;
        }
    }

    public static boolean isInGuardPosition(IShipGuardian host) {
        Entity ent = (Entity)host;
        if (ent.world.getBlockState(new BlockPos(ent).up()).getBlock() == Blocks.AIR) {
            return false;
        }
        if (!host.getStateFlag(11)) {
            float fMin = host.getStateMinor(10) + ((Entity)host).width * 0.5f;
            fMin *= fMin;
            return host.getGuardedEntity() != null ? ((Entity) host).getDistanceSq(host.getGuardedEntity()) < fMin : host.getStateMinor(15) > 0 && ((Entity) host).getDistanceSq(host.getStateMinor(14), host.getStateMinor(15), host.getStateMinor(16)) < fMin && ((Entity) host).posY >= host.getStateMinor(15);
        } else {
            float fMax = host.getStateMinor(11) + ((Entity)host).width * 0.5f;
            fMax *= fMax;
            return host.getHostEntity() != null && host.getHostEntity().getDistanceSq((Entity) host) <= fMax;
        }
    }
}
