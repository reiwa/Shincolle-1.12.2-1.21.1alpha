package com.lulan.shincolle.entity.destroyer;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.entity.other.EntityRensouhouMob;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.CombatHelper;
import com.lulan.shincolle.utility.ParticleHelper;
import net.minecraft.entity.Entity;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.Objects;

public class EntityDestroyerShimakazeMob
extends BasicEntityShipHostile {
    public int numRensouhou;

    public EntityDestroyerShimakazeMob(World world) {
        super(world);
        this.setStateMinor(20, 36);
        this.numRensouhou = 10;
        this.setStateEmotion(0, this.rand.nextInt(64), false);
    }

    @Override
    protected void setSizeWithScaleLevel() {
        switch (this.getScaleLevel()) {
            case 3: {
                this.setSize(1.7f, 6.4f);
                break;
            }
            case 2: {
                this.setSize(1.3f, 4.8f);
                break;
            }
            case 1: {
                this.setSize(0.9f, 3.2f);
                break;
            }
            default: {
                this.setSize(0.5f, 1.6f);
            }
        }
    }

    @Override
    protected void setBossInfo() {
        this.bossInfo = new BossInfoServer(this.getDisplayName(), BossInfo.Color.YELLOW, BossInfo.Overlay.NOTCHED_10);
    }

    @Override
    protected void setAIList() {
        super.setAIList();
        this.tasks.addTask(1, new EntityAIShipRangeAttack(this));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote && this.ticksExisted % 128 == 0 && !this.isMorph && this.numRensouhou < 10) {
            ++this.numRensouhou;
        }
    }

    @Override
    public boolean attackEntityWithAmmo(Entity target) {
        if (this.numRensouhou <= 0) {
            return false;
        }
        --this.numRensouhou;
        if (this.rand.nextInt(10) > 7) {
            this.playSound(ModSounds.SHIP_HIT, this.getSoundVolume(), this.getSoundPitch());
        }
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32.0);
        CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
        EntityRensouhouMob rensoho = new EntityRensouhouMob(this.world);
        rensoho.initAttrs(this, target, this.scaleLevel);
        this.world.spawnEntity(rensoho);
        this.applyEmotesReaction(3);
        return true;
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        float atkHeavy = this.getAttackBaseDamage(2, target) * 0.9f;
        float kbValue = 0.08f;
        float launchPos = (float)this.posY + this.height * 0.7f;
        int moveType = CombatHelper.calcMissileMoveType(this, target.posY, 2);
        if (moveType == 1) {
            moveType = 0;
        }
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        this.playSound(ModSounds.SHIP_FIREHEAVY, ConfigHandler.volumeFire, this.getSoundPitch() * 0.85f);
        this.playSound(ModSounds.SHIP_FIREHEAVY, ConfigHandler.volumeFire, this.getSoundPitch() * 0.8f);
        this.playSound(ModSounds.SHIP_FIREHEAVY, ConfigHandler.volumeFire, this.getSoundPitch() * 0.9f);
        if (this.getRNG().nextInt(10) > 7) {
            this.playSound(ModSounds.SHIP_HIT, this.getSoundVolume(), this.getSoundPitch());
        }
        float tarX = (float)target.posX;
        float tarY = (float)target.posY;
        float tarZ = (float)target.posZ;
        if (distVec.d < 6.0) {
            tarX = (float)(tarX + distVec.x * (6.0 - distVec.d));
            tarY = (float)(tarY + distVec.y * (6.0 - distVec.d));
            tarZ = (float)(tarZ + distVec.z * (6.0 - distVec.d));
        }
        if (this.rand.nextFloat() <= CombatHelper.calcMissRate(this, (float)distVec.d)) {
            tarX = tarX - 5.0f + this.rand.nextFloat() * 10.0f;
            tarY += this.rand.nextFloat() * 5.0f;
            tarZ = tarZ - 5.0f + this.rand.nextFloat() * 10.0f;
            ParticleHelper.spawnAttackTextParticle(this, 0);
        }
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32.0);
        CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
        float spread = 3.0f + this.scaleLevel * 0.5f;
        float[] data = new float[]{atkHeavy, kbValue, launchPos, tarX, tarY + target.height * 0.1f, tarZ, 160.0f, 0.25f, 0.5f, 1.04f, 1.04f};
        EntityAbyssMissile missile1 = new EntityAbyssMissile(this.world, this, 0, moveType, data);
        data = new float[]{atkHeavy, kbValue, launchPos, tarX + spread, tarY + target.height * 0.1f, tarZ + spread, 160.0f, 0.25f, 0.5f, 1.04f, 1.04f};
        EntityAbyssMissile missile2 = new EntityAbyssMissile(this.world, this, 0, moveType, data);
        data = new float[]{atkHeavy, kbValue, launchPos, tarX + spread, tarY + target.height * 0.1f, tarZ - spread, 160.0f, 0.25f, 0.5f, 1.04f, 1.04f};
        EntityAbyssMissile missile3 = new EntityAbyssMissile(this.world, this, 0, moveType, data);
        data = new float[]{atkHeavy, kbValue, launchPos, tarX - spread, tarY + target.height * 0.1f, tarZ + spread, 160.0f, 0.25f, 0.5f, 1.04f, 1.04f};
        EntityAbyssMissile missile4 = new EntityAbyssMissile(this.world, this, 0, moveType, data);
        data = new float[]{atkHeavy, kbValue, launchPos, tarX - spread, tarY + target.height * 0.1f, tarZ - spread, 160.0f, 0.25f, 0.5f, 1.04f, 1.04f};
        EntityAbyssMissile missile5 = new EntityAbyssMissile(this.world, this, 0, moveType, data);
        this.world.spawnEntity(missile1);
        this.world.spawnEntity(missile2);
        this.world.spawnEntity(missile3);
        this.world.spawnEntity(missile4);
        this.world.spawnEntity(missile5);
        this.applyEmotesReaction(3);
        return true;
    }

    @Override
    public int getDamageType() {
        return 5;
    }
}
