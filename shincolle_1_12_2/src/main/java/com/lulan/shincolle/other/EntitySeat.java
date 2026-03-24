package com.lulan.shincolle.entity.other;

import com.lulan.shincolle.block.BlockChair;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EntitySeat extends Entity {
    public EntitySeat(World worldIn) {
        super(worldIn);
        noClip = true;
        setSize(0.0F, 0.0F);
    }

    @Override
    protected void entityInit() {}

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {}

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {}

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (!this.world.isRemote) {
            if (!(this.world.getBlockState(new BlockPos(this)).getBlock() instanceof BlockChair)) {
                this.removePassengers();
                this.setDead();
                return;
            }
            if (this.ticksExisted > 20 && this.getPassengers().isEmpty()) {
                this.setDead();
            }
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public double getMountedYOffset() {
        return 0.15f;
    }
}