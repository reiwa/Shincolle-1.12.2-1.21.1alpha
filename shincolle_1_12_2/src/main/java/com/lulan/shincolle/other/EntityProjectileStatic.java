package com.lulan.shincolle.entity.other;

import com.lulan.shincolle.client.render.IShipCustomTexture;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.IShipAttrs;
import com.lulan.shincolle.entity.IShipOwner;
import com.lulan.shincolle.entity.IShipProjectile;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.List;

public class EntityProjectileStatic
extends Entity
implements IShipOwner,
IShipAttrs,
IShipCustomTexture,
IShipProjectile {
    private IShipAttackBase host;
    private Entity host2;
    private int playerUID;
    public int type;
    public int lifeLength;
    public double pullForce;
    public double range;
    private double[] data;

    public EntityProjectileStatic(World world) {
        super(world);
        this.setSize(0.5f, 0.5f);
        this.ignoreFrustumCheck = true;
        this.noClip = true;
        this.stepHeight = 0.0f;
        this.type = 0;
        this.lifeLength = 0;
        this.pullForce = 0.0;
        this.range = 0.0;
    }

    public void initAttrs(IShipAttackBase host, int type, double[] data) {
        this.host = host;
        this.host2 = (Entity)host;
        this.playerUID = host.getPlayerUID();
        this.type = type;
        this.data = data;
        this.setPosition(data[0], data[1], data[2]);
        this.lifeLength = (int)data[3];
        this.pullForce = data[4];
        this.range = data[5];
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
    }

    @Override
    public int getPlayerUID() {
        return this.playerUID;
    }

    @Override
    public void setPlayerUID(int uid) {
    }

    @Override
    public Entity getHostEntity() {
        return this.host2;
    }

    protected void entityInit() {
    }

    protected void readEntityFromNBT(NBTTagCompound nbt) {
    }

    protected void writeEntityToNBT(NBTTagCompound nbt) {
    }

    public boolean isEntityInvulnerable(DamageSource attacker) {
        return true;
    }

    public boolean canBeCollidedWith() {
        return false;
    }

    public boolean canBePushed() {
        return false;
    }

    public void onUpdate() {
        super.onUpdate();
        if (!this.world.isRemote) {
            if (this.ticksExisted > this.lifeLength || this.host == null) {
                this.setDead();
                return;
            }
            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
            if (this.ticksExisted == 1) {
                CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, (byte)56, new float[]{1.0f, this.type, (float)this.data[3], (float)this.data[4], (float)this.data[5]}), point);
            }
            if ((this.ticksExisted & 3) == 0) {
                List<Entity> hitList = this.world.getEntitiesWithinAABB(Entity.class, this.getEntityBoundingBox().expand(this.range, this.range, this.range));
                for (Entity ent : hitList) {
                    if (!ent.canBeCollidedWith() || !EntityHelper.isNotHost(this, ent) || !ent.canBePushed() || TeamHelper.checkSameOwner(this.host2, ent) || TargetHelper.isEntityInvulnerable(ent)) continue;
                    Dist4d dist = CalcHelper.getDistanceFromA2B(this, ent);
                    if (dist.d <= 1.0) continue;
                    ent.addVelocity(dist.x * -this.pullForce, dist.y * -this.pullForce, dist.z * -this.pullForce);
                    CommonProxy.channelE.sendToAllAround(new S2CEntitySync(ent, 0, (byte)54), point);
                }
            }
        } else if (this.ticksExisted == 1) {
            ParticleHelper.spawnAttackParticleAtEntity(this, 5.0, this.lifeLength, this.range * 2.0, (byte)24);
        }
    }

    public boolean attackEntityFrom(DamageSource attacker, float atk) {
        return false;
    }

    @Override
    public int getTextureID() {
        return -1;
    }

    @Override
    public Attrs getAttrs() {
        return this.host.getAttrs();
    }

    @Override
    public void setAttrs(Attrs data) {
    }

    @Override
    public void setProjectileType(int type) {
        this.type = type;
    }
}
