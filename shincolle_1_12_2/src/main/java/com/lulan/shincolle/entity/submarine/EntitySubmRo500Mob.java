package com.lulan.shincolle.entity.submarine;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.entity.IShipInvisible;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.CombatHelper;
import com.lulan.shincolle.utility.ParticleHelper;
import net.minecraft.entity.Entity;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class EntitySubmRo500Mob
extends BasicEntityShipHostile
implements IShipInvisible {
    public EntitySubmRo500Mob(World world) {
        super(world);
        this.setStateMinor(20, 39);
        this.setStateEmotion(0, this.rand.nextInt(8), false);
    }

    @Override
    protected void setSizeWithScaleLevel() {
        switch (this.getScaleLevel()) {
            case 3: {
                this.setSize(1.5f, 5.6f);
                break;
            }
            case 2: {
                this.setSize(1.2f, 4.2f);
                break;
            }
            case 1: {
                this.setSize(0.9f, 2.8f);
                break;
            }
            default: {
                this.setSize(0.6f, 1.4f);
            }
        }
    }

    @Override
    protected void setBossInfo() {
        this.bossInfo = new BossInfoServer(this.getDisplayName(), BossInfo.Color.YELLOW, BossInfo.Overlay.NOTCHED_10);
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        if (this.rand.nextInt(8) == 0) {
            return ModSounds.SHIP_GARURU;
        }
        return super.getAmbientSound();
    }

    @Override
    protected void setAIList() {
        super.setAIList();
        this.tasks.addTask(1, new EntityAIShipRangeAttack(this));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote && (this.ticksExisted & 0x7F) == 0 && !this.isMorph && this.rand.nextInt(2) == 0) {
            this.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 40 + this.getScaleLevel() * 10, 0, false, false));
        }
    }

    @Override
    public boolean attackEntityWithAmmo(Entity target) {
        float atk = this.getAttrs().getAttackDamage();
        float kbValue = 0.15f;
        float launchPos = (float)this.posY + this.height * 0.5f;
        int moveType = CombatHelper.calcMissileMoveType(this, target.posY, 1);
        if (moveType == 0) {
            launchPos = (float)this.posY + this.height * 0.3f;
        }
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        this.applySoundAtAttacker(2);
        this.applyParticleAtAttacker(2, distVec);
        float tarX = (float)target.posX;
        float tarY = (float)target.posY;
        float tarZ = (float)target.posZ;
        if (this.rand.nextFloat() <= CombatHelper.calcMissRate(this, (float)distVec.d)) {
            tarX = tarX - 5.0f + this.rand.nextFloat() * 10.0f;
            tarY += this.rand.nextFloat() * 5.0f;
            tarZ = tarZ - 5.0f + this.rand.nextFloat() * 10.0f;
            ParticleHelper.spawnAttackTextParticle(this, 0);
        }
        float[] data = new float[]{atk, kbValue, launchPos, tarX, tarY + target.height * 0.1f, tarZ, 160.0f, 0.25f, 0.8f, 1.1f, 1.1f};
        EntityAbyssMissile missile = new EntityAbyssMissile(this.world, this, 1, moveType, data);
        this.world.spawnEntity(missile);
        this.applySoundAtTarget(2);
        this.applyParticleAtTarget(2, target);
        this.applyEmotesReaction(3);
        return true;
    }

    @Override
    public int getDamageType() {
        return 6;
    }

    @Override
    public float getInvisibleLevel() {
        return 0.35f;
    }

    @Override
    public double getShipFloatingDepth() {
        return 1.1 + this.scaleLevel * 0.3;
    }
}
