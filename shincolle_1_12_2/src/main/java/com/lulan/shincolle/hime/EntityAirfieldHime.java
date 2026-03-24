package com.lulan.shincolle.entity.hime;

import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipCV;
import com.lulan.shincolle.entity.mounts.EntityMountAfH;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.utility.CombatHelper;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.List;

public class EntityAirfieldHime
extends BasicEntityShipCV {
    public EntityAirfieldHime(World world) {
        super(world);
        this.setSize(0.7f, 1.9f);
        this.setStateMinor(19, 10);
        this.setStateMinor(20, 21);
        this.setStateMinor(25, 2);
        this.setStateMinor(13, 4);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[8]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[8]);
        this.ModelPos = new float[]{-6.0f, 30.0f, 0.0f, 40.0f};
        this.launchHeight = this.height * 0.7f;
        this.setFoodSaturationMax(16);
        this.postInit();
    }

    @Override
    public EntityAgeable createChild(EntityAgeable ageable) {
        return null;
    }

    @Override
    public int getEquipType() {
        return 2;
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(11, new EntityAIShipCarrierAttack(this));
        this.tasks.addTask(12, new EntityAIShipRangeAttack(this));
    }

    @Override
    public void onLivingUpdate() {
        if (!this.world.isRemote && this.ticksExisted % 128 == 0) {
            if (this.getStateMinor(6) > 0 && this.getHealth() < this.getMaxHealth()) {
                this.heal(this.getMaxHealth() * 0.06f + 1.0f);
            }
            if (this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 50) {
                int healCount = this.getLevel() / 15 + 2;
                List<EntityLivingBase> hitList = this.world.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().expand(12.0, 12.0, 12.0));
                NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
                for (EntityLivingBase target : hitList) {
                    boolean canHeal = false;
                    if (healCount <= 0) break;
                    if (target == this || !TeamHelper.checkIsAlly(this, target) || target.getHealth() / target.getMaxHealth() >= 0.96f) continue;
                    if (target instanceof EntityPlayer) {
                        target.heal(1.0f + target.getMaxHealth() * 0.04f + this.getLevel() * 0.04f);
                        canHeal = true;
                    } else if (target instanceof BasicEntityShip) {
                        target.heal(1.0f + target.getMaxHealth() * 0.04f + this.getLevel() * 0.1f);
                        canHeal = true;
                    }
                    if (!canHeal) continue;
                    CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, target, 1.0, 0.0, 0.0, 4, false), point);
                    --healCount;
                    this.decrGrudgeNum(50);
                }
            }
        }
        super.onLivingUpdate();
    }

    @Override
    public boolean hasShipMounts() {
        return true;
    }

    @Override
    public BasicEntityMount summonMountEntity() {
        return new EntityMountAfH(this.world);
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            if (this.getStateEmotion(1) == 4) {
                return this.height * 0.65f;
            }
            return this.height * 0.56f;
        }
        return this.height * 0.75f;
    }

    @Override
    public float getAttackBaseDamage(int type, Entity target) {
        switch (type) {
            case 1: {
                return CombatHelper.modDamageByAdditionAttrs(this, target, this.shipAttrs.getAttackDamage(), 0);
            }
            case 2: {
                return this.shipAttrs.getAttackDamageHeavy();
            }
            case 3: {
                return this.shipAttrs.getAttackDamageAir();
            }
            case 4: {
                return this.shipAttrs.getAttackDamageAirHeavy();
            }
            default:
        }
        return this.shipAttrs.getAttackDamage();
    }
}
