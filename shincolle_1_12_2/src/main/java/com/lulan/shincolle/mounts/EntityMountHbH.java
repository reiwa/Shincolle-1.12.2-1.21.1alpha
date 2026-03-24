package com.lulan.shincolle.entity.mounts;

import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityMountLarge;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.utility.ParticleHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import java.util.List;

public class EntityMountHbH
extends BasicEntityMountLarge {
    public EntityMountHbH(World world) {
        super(world);
        this.setSize(1.9f, 1.6f);
        this.seatPos = new float[]{0.0f, -0.29f, 0.0f};
        this.seatPos2 = new float[]{-1.5f, 1.06f, 0.44f};
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
        if (this.world.isRemote && this.ticksExisted % 16 == 0) {
            ParticleHelper.spawnAttackParticleAtEntity(this, 0.0, 0.0, 0.0, (byte)3);
            if (this.rand.nextInt(3) == 0) {
                ParticleHelper.spawnAttackParticleAtEntity(this, 0.0, 0.0, 0.0, (byte)3);
            }
            if (this.rand.nextInt(3) == 0) {
                ParticleHelper.spawnAttackParticleAtEntity(this, 0.0, 0.0, 0.0, (byte)3);
            }
            if (this.rand.nextInt(3) == 0) {
                ParticleHelper.spawnAttackParticleAtEntity(this, 0.0, 0.0, 0.0, (byte)3);
            }
        }
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        if (this.host != null) {
            return this.host.attackEntityWithHeavyAmmo(target);
        }
        return super.attackEntityWithHeavyAmmo(target);
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(10, new EntityAIShipCarrierAttack(this));
        this.tasks.addTask(11, new EntityAIShipRangeAttack(this));
    }

    @Override
    public int getTextureID() {
        return 11;
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
