package com.lulan.shincolle.entity.other;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.utility.ParticleHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Objects;

public class EntityShipFishingHook
extends Entity {
    public EntityLivingBase host;

    @Override
    protected void entityInit() {
    }

    public EntityShipFishingHook(World worldIn) {
        super(worldIn);
        this.setSize(0.25f, 0.25f);
        this.ignoreFrustumCheck = true;
    }

    public EntityShipFishingHook(World worldIn, IShipAttackBase host) {
        this(worldIn);
        if (!(host instanceof BasicEntityShip)) {
            throw new IllegalArgumentException("EntityShipFishingHook requires BasicEntityShip!");
        }
        this.host = (EntityLivingBase)host;
        ((BasicEntityShip)host).fishHook = this;
    }
    @SideOnly(value=Side.CLIENT)
    public boolean isInRangeToRenderDist(double distance) {
        double d0 = this.getEntityBoundingBox().getAverageEdgeLength() * 4.0;
        if (Double.isNaN(d0)) {
            d0 = 4.0;
        }
        return distance < (d0 *= 64.0) * d0;
    }

    public boolean isEntityInvulnerable(DamageSource source) {
        return true;
    }

    public boolean canBeAttackedWithItem() {
        return false;
    }

    public void onUpdate() {
        super.onUpdate();
        if (this.ticksExisted > ConfigHandler.tickFishing[0] + ConfigHandler.tickFishing[1]) {
            this.setDead();
        }
        if (!this.world.isRemote) {
            if (this.host == null) {
                this.setDead();
                return;
            }
            ItemStack rod = this.host.getHeldItemMainhand();
            if (rod == null || rod.getItem() != Items.FISHING_ROD) {
                rod = this.host.getHeldItemOffhand();
            }
            if (this.host.isDead || !this.host.isEntityAlive() || rod == null || rod.getItem() != Items.FISHING_ROD || this.getDistanceSq(this.host) > 1024.0) {
                this.setDead();
                return;
            }
            if (this.ticksExisted == 4) {
                NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
                CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, (byte)55), point);
                this.playSound(SoundEvents.ENTITY_BOBBER_SPLASH, 0.25f, 1.0f + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4f);
            }
        } else {
            this.motionX *= 0.9;
            this.motionY *= 0.9;
            this.motionZ *= 0.9;
            this.setPosition(this.posX, this.posY, this.posZ);
            if (this.ticksExisted == 4) {
                ParticleHelper.spawnAttackParticleAt(this.posX, this.posY - 0.1, this.posZ, 1.5, 0.25, 1.5, (byte)48);
            }
            if ((this.ticksExisted & 0x3F) == 0 && this.rand.nextFloat() < 0.35f) {
                ParticleHelper.spawnAttackParticleAt(this.posX, this.posY - 0.1, this.posZ, 1.5, 0.25, 1.5, (byte)48);
            }
        }
    }

    public void writeEntityToNBT(NBTTagCompound compound) {
    }

    public void readEntityFromNBT(NBTTagCompound compound) {
    }

    public void setDead() {
        super.setDead();
        if (this.host instanceof BasicEntityShip) {
            ((BasicEntityShip)this.host).fishHook = null;
        }
    }
}
