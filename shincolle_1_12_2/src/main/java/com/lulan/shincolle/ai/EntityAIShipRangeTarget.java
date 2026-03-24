package com.lulan.shincolle.ai;

import com.google.common.base.Predicate;
import com.lulan.shincolle.entity.*;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.TargetHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EntityAIShipRangeTarget
extends EntityAIBase {
    protected Class<Entity> targetClass;
    protected TargetHelper.Sorter targetSorter;
    protected Predicate<Entity> targetSelector;
    protected IShipAttackBase host;
    protected EntityLiving host2;
    protected BasicEntityShip hostShip;
    protected Entity targetEntity;
    protected int range;

    

    public EntityAIShipRangeTarget(IShipAttackBase host, Class<Entity> targetClass) {
        this.setMutexBits(1);
        this.host = host;
        this.host2 = (EntityLiving)host;
        this.targetClass = targetClass;
        this.targetSorter = new TargetHelper.Sorter(this.host2);
        if (host instanceof BasicEntityShipHostile) {
            this.targetSelector = new TargetHelper.SelectorForHostile(this.host2);
        } else if (host instanceof BasicEntityShip) {
            this.hostShip = (BasicEntityShip)host;
            this.targetSelector = new TargetHelper.Selector(this.host2);
        } else {
            this.targetSelector = new TargetHelper.Selector(this.host2);
        }
        this.updateRange();
    }

    public boolean shouldExecute() {
        if (this.host != null) {
            if (this.host.getIsSitting() || this.host.getStateMinor(43) > 0 || this.host.getTickExisted() % 8 != 0) {
                return false;
            }
            this.updateRange();
            List<Entity> list1 = null;
            List<Entity> list2;
            if (this.hostShip != null) {
                if (this.hostShip.getStateFlag(19)) {
                    list1 = EntityHelper.getEntitiesWithinAABB(this.host2.world, IShipFlyable.class, this.host2.getEntityBoundingBox().expand(this.range, this.range * 0.75, this.range), this.targetSelector);
                    list2 = this.host2.world.getEntitiesWithinAABB(EntityFlying.class, this.host2.getEntityBoundingBox().expand(this.range, this.range * 0.75, this.range), this.targetSelector);
                    list1 = CalcHelper.listUnion(list1, list2);
                }
                if (list1 == null || list1.isEmpty()) {
                    if (this.hostShip.getStateFlag(20)) {
                        list1 = EntityHelper.getEntitiesWithinAABB(this.host2.world, IShipInvisible.class, this.host2.getEntityBoundingBox().expand(this.range, this.range * 0.75, this.range), this.targetSelector);
                    }
                    if ((list1 == null || list1.isEmpty()) && this.hostShip.getStateFlag(18)) {
                        list1 = this.host2.world.getEntitiesWithinAABB(BasicEntityShip.class, this.host2.getEntityBoundingBox().expand(this.range, this.range * 0.75, this.range), this.targetSelector);
                    }
                }
            }
            if (list1 == null || list1.isEmpty()) {
                list1 = EntityHelper.getEntitiesWithinAABB(this.host2.world, this.targetClass, this.host2.getEntityBoundingBox().expand(this.range, this.range * 0.75, this.range), this.targetSelector);
            }
            if (list1 != null && !list1.isEmpty()) {
                Collections.sort(list1, this.targetSorter);
                this.targetEntity = list1.get(0);
                if (list1.size() > 2) {
                    this.targetEntity = list1.get(this.host2.world.rand.nextInt(3));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void resetTask() {
    }

    @Override
    public void startExecuting() {
        if (this.host != null) {
            this.host.setEntityTarget(this.targetEntity);
        }
    }

    @Override
    public boolean shouldContinueExecuting() {
        Entity target = this.host.getEntityTarget();
        if (target == null || !target.isEntityAlive()) {
            return false;
        }
        double d0 = ((double)this.range) * this.range;
        if (this.host2.getDistanceSq(target) > d0) {
            return false;
        }
        return !(target instanceof EntityPlayer) || !((EntityPlayer)target).capabilities.disableDamage;
    }

    private void updateRange() {
        this.range = (int)this.host.getAttrs().getAttackRange();
        if (this.range < 2) {
            this.range = Math.max(2, this.host.getStateMinor(11) + 2);
        }
    }
}
