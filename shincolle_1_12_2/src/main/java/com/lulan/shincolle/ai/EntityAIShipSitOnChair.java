package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.tileentity.TileEntityChair;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class EntityAIShipSitOnChair extends EntityAIBase {

    private final BasicEntityShip host;
    private final double moveSpeed;
    private final int searchRange;
    private final int searchHeight;

    private BlockPos targetChairPos;

    public EntityAIShipSitOnChair(BasicEntityShip entity, double speed) {
        this.host = entity;
        this.moveSpeed = speed;
        this.searchRange = 16;
        this.searchHeight = 8;
        this.setMutexBits(7);
    }

    @Override
    public boolean shouldExecute() {
        if (this.host.getIsRiding() || this.host.getIsSitting() || this.host.isAIDisabled()) {
            return false;
        }
        if (this.host.getRNG().nextInt(100) != 0) {
            return false;
        }
        this.targetChairPos = this.findNearestAvailableChair();
        return this.targetChairPos != null;
    }

    @Override
    public void startExecuting() {
        this.host.getShipNavigate().tryMoveToXYZ(
            this.targetChairPos.getX() + 0.5D,
            this.targetChairPos.getY() + 0.5D,
            this.targetChairPos.getZ() + 0.5D,
            this.moveSpeed
        );
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (this.host.getIsRiding() || this.host.getShipNavigate().noPath()) {
            return false;
        }
        TileEntity te = this.host.world.getTileEntity(this.targetChairPos);
        if (!(te instanceof TileEntityChair)) {
            return false;
        }
        return !((TileEntityChair) te).isOccupied();
    }

    @Override
    public void updateTask() {
        if (this.host.getDistanceSq(this.targetChairPos) < 5.5f) {
            this.host.getShipNavigate().clearPathEntity();

            TileEntity te = this.host.world.getTileEntity(this.targetChairPos);
            if (te instanceof TileEntityChair) {
                ((TileEntityChair) te).sit(this.host);
            }
        }
    }

    @Override
    public void resetTask() {
        this.host.getShipNavigate().clearPathEntity();
    }

    private BlockPos findNearestAvailableChair() {
        BlockPos hostPos = this.host.getPosition();
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        
        BlockPos nearestChair = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (int y = -this.searchHeight; y <= this.searchHeight; y++) {
            for (int x = -this.searchRange; x <= this.searchRange; x++) {
                for (int z = -this.searchRange; z <= this.searchRange; z++) {
                    checkPos.setPos(hostPos.getX() + x, hostPos.getY() + y, hostPos.getZ() + z);
                    TileEntity te = this.host.world.getTileEntity(checkPos);
                    if (te instanceof TileEntityChair && !((TileEntityChair) te).isOccupied()) {
                        double distSq = this.host.getDistanceSq(checkPos);
                        if (distSq < nearestDistSq) {
                            nearestDistSq = distSq;
                            nearestChair = checkPos.toImmutable();
                        }
                    }
                }
            }
        }
        return nearestChair;
    }
}