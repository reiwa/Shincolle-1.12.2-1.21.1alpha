package com.lulan.shincolle.ai;

import com.lulan.shincolle.ai.path.ShipPath;
import com.lulan.shincolle.ai.path.ShipPathPoint;
import com.lulan.shincolle.entity.IShipNavigator;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;

public class EntityAIShipOpenDoor
extends EntityAIBase {
    private final Entity host;
    private final IShipNavigator host2;
    private ArrayList<BlockPos> doors;
    private BlockPos pathPoint;
    private boolean hasPassed;
    private float vecX;
    private float vecZ;
    private final boolean closeDoor;
    private int delay;
    private final float dist;

    public EntityAIShipOpenDoor(IShipNavigator host, boolean closeDoor) {
        this.setMutexBits(0);
        this.host = (Entity)host;
        this.host2 = host;
        this.closeDoor = closeDoor;
        this.dist = (this.host.width + 1.0f) * (this.host.width + 1.0f);
        this.doors = new ArrayList<>();
        this.pathPoint = BlockPos.ORIGIN;
    }

    public boolean shouldExecute() {
        ShipPath path = this.host2.getShipNavigate().getPath();
        if (!this.host.collidedHorizontally) {
            return false;
        }
        if (path != null && !path.isFinished()) {
            BlockPos pos;
            for (int i = 0; i < Math.min(path.getCurrentPathIndex() + 2, path.getCurrentPathLength()); ++i) {
                ShipPathPoint pp = path.getPathPointFromIndex(i);
                pos = new BlockPos(pp.xCoord, pp.yCoord, pp.zCoord);
                if (this.host.getDistanceSqToCenter(pos) > this.dist) continue;
                EntityAIShipOpenDoor.checkDoors(pos, this.host, this.doors);
                this.pathPoint = pos;
            }
            pos = new BlockPos(this.host);
            EntityAIShipOpenDoor.checkDoors(pos, this.host, this.doors);
            this.pathPoint = pos;
            return !this.doors.isEmpty();
        }
        return false;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.closeDoor && this.delay > 0 && !this.hasPassed;
    }

    @Override
    public void startExecuting() {
        this.delay = 30;
        this.hasPassed = false;
        this.setDoorOpen(true);
        if (this.pathPoint != BlockPos.ORIGIN) {
            this.vecX = this.pathPoint.getX() + 0.5f - (float)this.host.posX;
            this.vecZ = this.pathPoint.getZ() + 0.5f - (float)this.host.posZ;
        }
    }

    @Override
    public void resetTask() {
        if (this.closeDoor && !this.doors.isEmpty()) {
            this.setDoorOpen(false);
        }
        this.doors = new ArrayList<>();
    }

    @Override
    public void updateTask() {
        --this.delay;
        if (this.delay <= 0 && (this.vecX * (this.pathPoint.getX() + 0.5f - (float)this.host.posX) + this.vecZ * (this.pathPoint.getZ() + 0.5f - (float)this.host.posZ)) < 0.0f) {
            this.hasPassed = true;
        }
    }

    private static void checkDoors(BlockPos pos, Entity host, ArrayList<BlockPos> list) {
        int range = MathHelper.floor(host.width + 1.0f);
        int height = MathHelper.floor(host.height + 1.0f);
        BlockPos startPos = new BlockPos(pos.getX() - range, pos.getY(), pos.getZ() - range);
        BlockPos endPos = new BlockPos(pos.getX() + range, pos.getY() + height, pos.getZ() + range);
        for (BlockPos currentPos : BlockPos.getAllInBox(startPos, endPos)) {
            IBlockState state = host.world.getBlockState(currentPos);
            Block block = state.getBlock();
            boolean isTrapDoor = block instanceof BlockTrapDoor && state.getMaterial() == Material.WOOD;
            boolean isFenceGate = block instanceof BlockFenceGate;
            boolean isWoodDoor = block instanceof BlockDoor && state.getMaterial() == Material.WOOD;
            if (isTrapDoor || isFenceGate || (currentPos.getY() == pos.getY() && isWoodDoor)) {
                list.add(currentPos.toImmutable());
            }
        }
    }

    private void setDoorOpen(boolean open) {
        IBlockState state;
        for (BlockPos pos : this.doors) {
            state = this.host.world.getBlockState(pos);
            Block block = state.getBlock();
            if (block instanceof BlockDoor) {
                ((BlockDoor) block).toggleDoor(this.host.world, pos, open);
            } else if (block instanceof BlockTrapDoor) {
                boolean isOpen = state.getValue(BlockTrapDoor.OPEN);
                state = state.withProperty(BlockTrapDoor.OPEN, !isOpen);
                this.host.world.setBlockState(pos, state, 2);
                this.host.world.playEvent(null, isOpen ? 1006 : 1005, pos, 0);
            } else if (block instanceof BlockFenceGate) {
                EntityAIShipOpenDoor.toggleGate(pos, this.host, state, open);
            }
        }
    }

    private static void toggleGate(BlockPos pos, Entity host, IBlockState state, boolean openGate) {
        if (!(state.getBlock() instanceof BlockFenceGate)) return;
        boolean isCurrentlyOpen = state.getValue(BlockFenceGate.OPEN);
        if (openGate && !isCurrentlyOpen) {
            EnumFacing facing = EnumFacing.fromAngle(host.rotationYaw);
            if (state.getValue(BlockHorizontal.FACING) == facing.getOpposite()) {
                state = state.withProperty(BlockHorizontal.FACING, facing);
            }
            state = state.withProperty(BlockFenceGate.OPEN, true);
            host.world.setBlockState(pos, state, 10);
            host.world.playEvent(null, 1008, pos, 0);
        } else if (!openGate && isCurrentlyOpen) {
            state = state.withProperty(BlockFenceGate.OPEN, false);
            host.world.setBlockState(pos, state, 10);
            host.world.playEvent(null, 1014, pos, 0);
        }
    }
}
