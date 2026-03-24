package com.lulan.shincolle.entity.hime;

import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipCV;
import com.lulan.shincolle.entity.mounts.EntityMountCaWD;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.CombatHelper;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.List;

public class EntityCarrierWD
extends BasicEntityShipCV {
    public EntityCarrierWD(World world) {
        super(world);
        this.setSize(0.7f, 1.9f);
        this.setStateMinor(19, 9);
        this.setStateMinor(20, 33);
        this.setStateMinor(25, 1);
        this.setStateMinor(13, 2);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[6]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[6]);
        this.ModelPos = new float[]{-6.0f, 30.0f, 0.0f, 40.0f};
        this.launchHeight = this.height * 1.2f;
        this.StateFlag[14] = false;
        this.setFoodSaturationMax(30);
        this.postInit();
    }

    @Override
    public EntityAgeable createChild(EntityAgeable ageable) {
        return null;
    }

    @Override
    public int getEquipType() {
        return 3;
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(11, new EntityAIShipCarrierAttack(this));
        this.tasks.addTask(12, new EntityAIShipRangeAttack(this));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote && this.ticksExisted % 128 == 0) {
            List<BasicEntityShip> shiplist;
            shiplist = this.world.getEntitiesWithinAABB(BasicEntityShip.class, this.getEntityBoundingBox().expand(16.0, 16.0, 16.0));
            if (this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 0 && !shiplist.isEmpty()) {
                for (BasicEntityShip s : shiplist) {
                    if (!TeamHelper.checkSameOwner(this, s)) continue;
                    s.addPotionEffect(new PotionEffect(MobEffects.HASTE, 50 + this.getStateMinor(0), this.getStateMinor(0) / 70, false, false));
                }
            }
            this.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 150, 0, false, false));
        }
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

    public void applyParticleAtAttacker(int type, Entity target) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        if(type == 1){
            double shotHeight = 1.85;
            if (this.isRiding() && this.getRidingEntity() instanceof BasicEntityMount) {
                shotHeight = 0.8;
            }
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, target, shotHeight, 0.0, 0.0, 0, true), point);
        } else {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
        }
    }

    public void applySoundAtAttacker(int type) {
        switch (type) {
            case 1: {
                this.playSound(ModSounds.SHIP_LASER, ConfigHandler.volumeFire, this.getSoundPitch() * 0.85f);
                if (this.rand.nextInt(10) <= 7) break;
                this.playSound(EntityCarrierWD.getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
                break;
            }
            case 2: {
                this.playSound(ModSounds.SHIP_FIREHEAVY, ConfigHandler.volumeFire, this.getSoundPitch() * 0.85f);
                if (this.getRNG().nextInt(10) <= 7) break;
                this.playSound(EntityCarrierWD.getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
                break;
            }
            case 3: 
            case 4: {
                this.playSound(ModSounds.SHIP_AIRCRAFT, ConfigHandler.volumeFire * 0.5f, this.getSoundPitch() * 0.85f);
                break;
            }
            default: {
                if (this.getRNG().nextInt(2) != 0) break;
                this.playSound(EntityCarrierWD.getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
            }
        }
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(BlockPos target) {
        return false;
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        return false;
    }

    @Override
    public boolean hasShipMounts() {
        return true;
    }

    @Override
    public BasicEntityMount summonMountEntity() {
        return new EntityMountCaWD(this.world);
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            if (this.getStateEmotion(1) == 4) {
                return this.height * 0.29f;
            }
            return this.height * 0.33f;
        }
        return this.height * 0.75f;
    }
}
