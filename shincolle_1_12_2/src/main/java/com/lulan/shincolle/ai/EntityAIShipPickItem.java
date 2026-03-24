package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipEmotion;
import com.lulan.shincolle.entity.IShipNavigator;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.utility.TargetHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;

import java.util.List;

public class EntityAIShipPickItem
extends EntityAIBase {
    protected TargetHelper.Sorter targetSorter;
    private BasicEntityShip hostShip;
    private BasicEntityMount hostMount;
    private final EntityLivingBase hostLiving;
    private Entity entItem;
    private int pickDelay;
    private int pickDelayMax;
    private float pickRange;
    private final float pickRangeBase;

    public EntityAIShipPickItem(IShipEmotion entity, float pickRangeBase) {
        this.setMutexBits(7);
        this.hostLiving = (EntityLivingBase)entity;
        this.pickRangeBase = pickRangeBase;
        this.pickDelay = 0;
        if (entity instanceof BasicEntityShip) {
            this.hostShip = (BasicEntityShip)entity;
            this.targetSorter = new TargetHelper.Sorter(this.hostShip);
        } else if (entity instanceof BasicEntityMount) {
            this.hostMount = (BasicEntityMount)entity;
            this.hostShip = (BasicEntityShip)this.hostMount.getHostEntity();
            this.targetSorter = new TargetHelper.Sorter(this.hostMount);
        }
        this.updateShipParms();
    }

    public boolean shouldExecute() {
        if (this.hostMount != null) {
            BasicEntityShip mountHost = (BasicEntityShip) this.hostMount.getHostEntity();
            if (mountHost.fishHook != null) return false;
            if (mountHost.isSitting() ||
                    !mountHost.getStateFlag(23) ||
                    mountHost.getStateMinor(43) > 0 ||
                    mountHost.getStateFlag(2)) {
                return false;
            }
            return mountHost.getCapaShipInventory().findFirstSlotForNewItem() > 0;
        }
        if (this.hostShip != null) {
            if (this.hostShip.fishHook != null) return false;
            if (this.hostShip.isRiding() ||
                    this.hostShip.isSitting() ||
                    !this.hostShip.getStateFlag(23) ||
                    this.hostShip.getStateMinor(43) > 0 ||
                    this.hostShip.getStateFlag(2)) {
                return false;
            }
            return this.hostShip.getCapaShipInventory().findFirstSlotForNewItem() > 0;
        }
        return false;
    }

    @Override
    public void updateTask() {
        if (this.hostShip == null) return;
        --this.pickDelay;
        if (this.hostShip.ticksExisted % 16 == 0) {
            this.updateShipParms();
            this.entItem = this.getNearbyEntityItem();
            if (this.entItem != null && this.entItem.isEntityAlive()) {
                ((IShipNavigator)this.hostLiving).getShipNavigate().tryMoveToEntityLiving(this.entItem, 1.0);
            }
        }
        if (this.pickDelay <= 0 && this.entItem != null) {
            this.pickDelay = this.pickDelayMax;
            if (this.hostLiving.getDistanceSq(this.entItem) < 9.0) {
                EntityItem entitem = (EntityItem)this.entItem;
                ItemStack itemstack = entitem.getItem();
                int i = itemstack.getCount();
                if (!entitem.cannotPickup() && this.hostShip.getCapaShipInventory().addItemStackToInventory(itemstack)) {
                    this.hostShip.world.playSound(null, this.hostShip.posX, this.hostShip.posY, this.hostShip.posZ, SoundEvents.ENTITY_ITEM_PICKUP, this.hostShip.getSoundCategory(), ConfigHandler.volumeShip, ((this.hostShip.getRNG().nextFloat() - this.hostShip.getRNG().nextFloat()) * 0.7f + 1.0f) * 2.0f);
                    if (this.hostShip.getStateTimer(6) <= 0 && this.hostShip.getRNG().nextInt(2) == 0) {
                        this.hostShip.setStateTimer(6, 40 + this.hostShip.getRNG().nextInt(10));
                        this.hostShip.playSound(BasicEntityShip.getCustomSound(6, this.hostShip), ConfigHandler.volumeShip, 1.0f);
                    }
                    this.hostShip.onItemPickup(entitem, i);
                    this.hostShip.applyParticleAtAttacker(0,  null);
                    this.hostShip.addShipExp(ConfigHandler.expGain[6]);
                    if (itemstack.getCount() <= 0) {
                        entitem.setDead();
                        this.entItem = null;
                    }
                }
                ((IShipNavigator)this.hostLiving).getShipNavigate().clearPathEntity();
            }
        }
    }

    private EntityItem getNearbyEntityItem() {
        List<EntityItem> getlist = this.hostShip.world.getEntitiesWithinAABB(EntityItem.class, this.hostLiving.getEntityBoundingBox().expand(this.pickRange, this.pickRange * 0.5f + 1.0, this.pickRange));
        if (getlist.isEmpty()) {
            return null;
        }
        getlist.sort(this.targetSorter);
        return getlist.get(0);
    }

    private void updateShipParms() {
        float speed = this.hostShip.getAttrs().getAttackSpeed();
        if (speed < 1.0f) {
            speed = 1.0f;
        }
        this.pickDelayMax = (int)(10.0f / speed);
        float tempran = this.pickRangeBase + this.hostShip.getStateMinor(11);
        this.pickRange = this.pickRangeBase + this.hostShip.getAttrs().getAttackRange() * 0.5f;
        this.pickRange = Math.min(tempran, this.pickRange);
    }
}
