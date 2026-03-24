package com.lulan.shincolle.entity.mounts;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import java.util.List;

public class EntityMountSuH
extends BasicEntityMount {
    public EntityMountSuH(World world) {
        super(world);
        this.setSize(1.8f, 1.6f);
        this.seatPos = new float[]{-0.8f, 0.6f, 0.0f};
        this.seatPos2 = new float[]{0.55f, 1.2f, 0.0f};
        this.shipNavigator = new ShipPathNavigate(this);
        this.shipMoveHelper = new ShipMoveHelper(this, 60.0f);
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
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(11, new EntityAIShipRangeAttack(this));
    }

    @Override
    public int getTextureID() {
        return 15;
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

    @Override
    public boolean attackEntityWithAmmo(Entity target) {
        if (this.host != null) {
            return this.host.attackEntityWithAmmo(target);
        }
        return false;
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        if (this.host != null) {
            return this.host.attackEntityWithHeavyAmmo(target);
        }
        return false;
    }
}
