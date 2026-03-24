package com.lulan.shincolle.entity.mounts;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.ParticleHelper;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class EntityMountBaH
extends BasicEntityMount {
    public EntityMountBaH(World world) {
        super(world);
        this.setSize(1.9f, 3.1f);
        this.seatPos = new float[]{1.05f, 2.6f, 0.0f};
        this.seatPos2 = new float[]{1.2f, 0.7f, -1.3f};
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
    public double getMountedYOffset() {
        return 2.754;
    }

    @Override
    public float getAttackBaseDamage(int type, Entity target) {
        if (this.host != null) {
            return this.host.getAttackBaseDamage(type, target);
        }
        return 0.0f;
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        if (this.host != null) {
            return this.host.attackEntityWithHeavyAmmo(target);
        }
        return super.attackEntityWithHeavyAmmo(target);
    }

    public void applyParticleAtAttacker(int type) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        if(type == 1){
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 10, 1.2, 1.8, 0.5), point);
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this.host, 0, true), point);
        } else {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this.host, 0, true), point);
        }
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(11, new EntityAIShipRangeAttack(this));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote && this.ticksExisted % 4 == 0) {
            ParticleHelper.spawnAttackParticleAt(this.posX, this.posY + 3.0, this.posZ, 0.0, 0.0, 0.0, (byte)20);
        }
    }

    @Override
    public int getTextureID() {
        return 9;
    }
}
