package com.lulan.shincolle.ai;

import com.lulan.shincolle.ai.path.ShipPath;
import com.lulan.shincolle.ai.path.ShipPathPoint;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttackBase;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.BlockPos;

public class EntityAIShipClimbLadder
        extends EntityAIBase {
    private final BasicEntityShip host;

    public EntityAIShipClimbLadder(BasicEntityShip entity) {
        this.host = entity;
    }

    @Override
    public boolean shouldExecute() {
        if (this.host.isAIDisabled()) {
            return false;
        }
        if(this.host.ticksExisted % 10 != 0){
            return false;
        }
        BlockPos pos = new BlockPos(this.host.posX, this.host.posY, this.host.posZ);
        IBlockState state = this.host.world.getBlockState(pos);
        Block block = state.getBlock();
        return !this.host.getShipNavigate().noPath() && block instanceof BlockLadder;
    }

    @Override
    public boolean shouldContinueExecuting() {
        BlockPos pos = new BlockPos(this.host.posX, this.host.posY, this.host.posZ);
        IBlockState state = this.host.world.getBlockState(pos);
        Block block = state.getBlock();
        return !this.host.getShipNavigate().noPath() && block instanceof BlockLadder;
    }

    @Override
    public void updateTask() {
        ShipPath path = this.host.getShipNavigate().getPath();
        if(path == null) return;
        ShipPathPoint point = path.getFinalPathPoint();
        if(point == null) return;
        int targety = point.yCoord;
        if(targety < this.host.posY){
            this.host.motionY -= 0.5;
        }
    }
}