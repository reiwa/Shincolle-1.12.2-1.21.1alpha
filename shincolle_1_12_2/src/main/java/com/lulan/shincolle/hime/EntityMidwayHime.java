package com.lulan.shincolle.entity.hime;

import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipCV;
import com.lulan.shincolle.entity.mounts.EntityMountMiH;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.utility.CombatHelper;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import java.util.List;

public class EntityMidwayHime
extends BasicEntityShipCV {
    public EntityMidwayHime(World world) {
        super(world);
        this.setSize(0.7f, 2.0f);
        this.setStateMinor(19, 10);
        this.setStateMinor(20, 30);
        this.setStateMinor(25, 2);
        this.setStateMinor(13, 2);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[8]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[8]);
        this.ModelPos = new float[]{-6.0f, 30.0f, 0.0f, 40.0f};
        this.launchHeight = this.height * 0.7f;
        this.setFoodSaturationMax(35);
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
        if (!this.world.isRemote && (this.ticksExisted & 0x7F) == 0) {
            if (this.getStateMinor(6) > 0 && this.getHealth() < this.getMaxHealth()) {
                this.heal(this.getMaxHealth() * 0.09f + 1.0f);
            }
            if (this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 0) {
                EntityPlayer player;
                List<BasicEntityShip> shiplist = this.world.getEntitiesWithinAABB(BasicEntityShip.class, this.getEntityBoundingBox().expand(16.0, 16.0, 16.0));
                if (!shiplist.isEmpty()) {
                    for (BasicEntityShip s : shiplist) {
                        if (!TeamHelper.checkSameOwner(this, s)) continue;
                        s.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 50 + this.getStateMinor(0), this.getStateMinor(0) / 50, false, false));
                    }
                }
                if ((player = EntityHelper.getEntityPlayerByUID(this.getPlayerUID())) != null && this.getDistanceSq(player) < 256.0 && !this.isMorph) {
                    player.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 50 + this.getStateMinor(0), this.getStateMinor(0) / 50, false, false));
                }
            }
        }
        super.onLivingUpdate();
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

    @Override
    public boolean hasShipMounts() {
        return true;
    }

    @Override
    public BasicEntityMount summonMountEntity() {
        return new EntityMountMiH(this.world);
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            if (this.getStateEmotion(1) == 4) {
                return this.height * 0.2f;
            }
            return this.height * 0.52f;
        }
        return this.height * 0.76f;
    }
}
