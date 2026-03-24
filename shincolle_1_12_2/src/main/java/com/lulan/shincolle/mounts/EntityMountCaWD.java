package com.lulan.shincolle.entity.mounts;

import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityMountLarge;
import com.lulan.shincolle.entity.BasicEntityShip;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public class EntityMountCaWD
extends BasicEntityMountLarge {
    public EntityMountCaWD(World world) {
        super(world);
        this.setSize(1.9f, 2.1f);
        this.seatPos = new float[]{0.25f, 1.1f, 0.0f};
        this.seatPos2 = new float[]{0.14f, -0.39f, 0.0f};
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
    public boolean attackEntityWithAmmo(Entity target) {
        if (this.host != null) {
            return this.host.attackEntityWithAmmo(target);
        }
        return false;
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        return false;
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(10, new EntityAIShipCarrierAttack(this));
        this.tasks.addTask(11, new EntityAIShipRangeAttack(this));
    }

    @Override
    public int getTextureID() {
        return 10;
    }
}
