package com.lulan.shincolle.entity.mounts;

import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityMountLarge;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.ParticleHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import java.util.List;

public class EntityMountAfH
extends BasicEntityMountLarge {
    public EntityMountAfH(World world) {
        super(world);
        this.setSize(1.9f, 1.3f);
        this.seatPos = new float[]{0.59f, -0.25f, 0.0f};
        this.seatPos2 = new float[]{-0.85f, 1.0f, -1.12f};
        this.shipNavigator = new ShipPathNavigate(this);
        this.shipMoveHelper = new ShipMoveHelper(this, 45.0f);
    }

    @Override
    public void initAttrs(BasicEntityShip host) {
        this.host = host;
        this.posX = host.posX;
        this.posY = host.posY;
        this.posZ = host.posZ;
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.setPosition(this.posX, this.posY, this.posZ);
        this.setupAttrs();
        if (this.getHealth() < this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
        this.setAIList();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote && this.ticksExisted % 8 == 0) {
            float[] partPos1 = CalcHelper.rotateXZByAxis(0.0f, -1.0f, this.renderYawOffset * ((float)Math.PI / 180), 1.0f);
            float[] partPos2 = CalcHelper.rotateXZByAxis(0.0f, -1.8f, this.renderYawOffset * ((float)Math.PI / 180), 1.0f);
            ParticleHelper.spawnAttackParticleAt(this.posX + partPos1[1], this.posY + 0.9f, this.posZ + partPos1[0], 0.0, 0.1, 0.0, (byte)18);
            ParticleHelper.spawnAttackParticleAt(this.posX + partPos2[1], this.posY + 0.9f, this.posZ + partPos2[0], 0.0, 0.1, 0.0, (byte)18);
        }
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(10, new EntityAIShipCarrierAttack(this));
        this.tasks.addTask(11, new EntityAIShipRangeAttack(this));
    }

    @Override
    public int getTextureID() {
        return 8;
    }

    @Override
    protected void setRotationByRider() {
        List<Entity> riders = this.getPassengers();
        for (Entity rider : riders) {
            if (!(rider instanceof BasicEntityShip)) continue;
            rider.rotationYaw = ((BasicEntityShip)rider).renderYawOffset;
            this.prevRotationYawHead = ((EntityLivingBase)rider).prevRotationYawHead;
            this.rotationYawHead = ((EntityLivingBase)rider).rotationYawHead;
            this.prevRenderYawOffset = ((EntityLivingBase)rider).prevRenderYawOffset;
            this.renderYawOffset = ((EntityLivingBase)rider).renderYawOffset;
            this.prevRotationYaw = rider.prevRotationYaw;
            this.rotationYaw = rider.rotationYaw;
        }
    }
}
