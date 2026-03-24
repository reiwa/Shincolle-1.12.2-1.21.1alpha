package com.lulan.shincolle.entity.battleship;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class EntityBattleshipNGTMob extends BasicEntityShipHostile {

    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int EMOTION_ATTACK_PHASE = 5;

    private float smokeX;
    private float smokeY;

    public EntityBattleshipNGTMob(World world) {
        super(world);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 37);
        this.smokeX = 0.0f;
        this.smokeY = 0.0f;
        this.setStateEmotion(0, 3, false);
    }

    @Override
    protected void setSizeWithScaleLevel() {
        switch (this.getScaleLevel()) {
            case 3:
                this.setSize(2.2f, 8.0f);
                this.smokeX = -2.24f;
                this.smokeY = 6.0f;
                break;
            case 2:
                this.setSize(1.7f, 6.0f);
                this.smokeX = -1.68f;
                this.smokeY = 4.5f;
                break;
            case 1:
                this.setSize(1.2f, 4.0f);
                this.smokeX = -1.12f;
                this.smokeY = 3.0f;
                break;
            default:
                this.setSize(0.7f, 2.0f);
                this.smokeX = -0.56f;
                this.smokeY = 1.5f;
                break;
        }
    }

    @Override
    public void initAttrsServerPost() {
        super.initAttrsServerPost();
        if (this.AttackEffectMap == null) {
            this.AttackEffectMap = new HashMap<Integer, int[]>();
        }
        this.AttackEffectMap.put(19, new int[]{this.getScaleLevel() / 2, 60 + this.getScaleLevel() * 40, 25 + this.getScaleLevel() * 25});
    }

    @Override
    protected void setBossInfo() {
        this.bossInfo = new BossInfoServer(this.getDisplayName(), BossInfo.Color.WHITE, BossInfo.Overlay.NOTCHED_10);
    }

    @Override
    protected void setAIList() {
        super.setAIList();
        this.tasks.addTask(1, new EntityAIShipRangeAttack(this));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.world.isRemote) {
            updateClientParticles();
        }
    }

    private void updateClientParticles() {
        if (this.ticksExisted % 4 == 0 && EmotionHelper.checkModelState(1, this.getStateEmotion(0))) {
            float[] partPos = CalcHelper.rotateXZByAxis(this.smokeX, 0.0f, this.renderYawOffset % 360.0f * ((float) Math.PI / 180), 1.0f);
            ParticleHelper.spawnAttackParticleAt(this.posX + partPos[1], this.posY + this.smokeY, this.posZ + partPos[0], 1.0 + this.scaleLevel, 0.0, 0.0, (byte) 43);
        }
        if (this.ticksExisted % 8 == 0) {
            int atkPhase = this.getStateEmotion(EMOTION_ATTACK_PHASE);
            if (atkPhase == 1 || atkPhase == 3) {
                ParticleHelper.spawnAttackParticleAtEntity(this, 0.12 + 0.1 * this.scaleLevel, 1.0, 0.0, (byte) 1);
            }
        }
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        int atkPhase = this.getStateEmotion(EMOTION_ATTACK_PHASE) + 1;
        playAttackSound(atkPhase);
        boolean isTargetHurt = executeAttackSequence(target, atkPhase);
        this.applyEmotesReaction(3);
        return isTargetHurt;
    }

    private void playAttackSound(int atkPhase) {
        switch (atkPhase) {
            case 1:
                this.playSound(ModSounds.SHIP_AP_P2, ConfigHandler.volumeFire, 1.0f);
                break;
            case 3:
                this.playSound(ModSounds.SHIP_AP_ATTACK, ConfigHandler.volumeFire, 1.0f);
                break;
            default:
                this.playSound(ModSounds.SHIP_AP_P1, ConfigHandler.volumeFire, 1.0f);
                break;
        }
        if (this.getRNG().nextInt(10) > 7) {
            this.playSound(ModSounds.SHIP_HIT, this.getSoundVolume(), this.getSoundPitch());
        }
    }

    private boolean executeAttackSequence(Entity target, int atkPhase) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        boolean isTargetHurt = false;
        if (atkPhase > 3) {
            isTargetHurt = performFinalAttack(target, point);
            this.setStateEmotion(EMOTION_ATTACK_PHASE, 0, true);
        } else {
            performInterimAttack(atkPhase, point);
            this.setStateEmotion(EMOTION_ATTACK_PHASE, atkPhase, true);
        }
        return isTargetHurt;
    }

    private void performInterimAttack(int atkPhase, NetworkRegistry.TargetPoint point) {
        if (atkPhase == 2) {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 23, this.posX, this.posY, this.posZ, 0.35, 0.3, 0.0, true), point);
        } else {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 22, this.posX, this.posY, this.posZ, 2.0 * (this.scaleLevel + 1), (this.scaleLevel + 1), 0.0, true), point);
        }
    }

    private boolean performFinalAttack(Entity target, NetworkRegistry.TargetPoint point) {
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 21, this.posX, this.posY, this.posZ, target.posX, target.posY, target.posZ, true), point);
        float baseDamage = this.shipAttrs.getAttackDamageHeavy();
        float primaryDamage = calculateDamage(target, baseDamage, (float) distVec.d);
        teleportToTargetSide(target, distVec);
        performAreaOfEffectAttack(primaryDamage * 0.5f, (float) distVec.d);
        return target.attackEntityFrom(DamageSource.causeMobDamage(this), primaryDamage);
    }

    private float calculateDamage(Entity target, float baseDamage, float dist) {
        float damage = CombatHelper.modDamageByAdditionAttrs(this, target, baseDamage, 2);
        damage = CombatHelper.applyCombatRateToDamage(this, false, dist, damage);
        damage = CombatHelper.applyDamageReduceOnPlayer(target, damage);
        if (!TeamHelper.doFriendlyFire(this, target)) {
            damage = 0.0f;
        }
        return damage;
    }

    private void teleportToTargetSide(Entity target, Dist4d distVec) {
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        this.setPosition(target.posX + distVec.x * 2.0, target.posY, target.posZ + distVec.z * 2.0);
    }

    private void performAreaOfEffectAttack(float aoeDamage, float dist) {
        double range = 3.5 + this.scaleLevel * 0.5;
        AxisAlignedBB impactBox = this.getEntityBoundingBox().expand(range, range, range);
        List<Entity> hitList = this.world.getEntitiesWithinAABB(Entity.class, impactBox);
        if (hitList.isEmpty()) return;
        for (Entity hitEntity : hitList) {
            if (hitEntity == this || TargetHelper.isEntityInvulnerable(hitEntity) || !hitEntity.canBeCollidedWith()) continue;
            float damage = calculateDamage(hitEntity, aoeDamage, dist);
            hitEntity.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
        }
    }

    public void applyParticleAtAttacker(int type) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        if (type == 1) {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 5, 0.9 * (this.scaleLevel + 1), (this.scaleLevel + 1), 1.1 * (this.scaleLevel + 1)), point);
        } else {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
        }
    }

    @Override
    public int getDamageType() {
        return 3;
    }
}