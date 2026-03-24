package com.lulan.shincolle.entity.battleship;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.entity.other.EntityProjectileBeam;
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

import java.util.HashMap;
import java.util.Objects;

public class EntityBattleshipYMTMob extends BasicEntityShipHostile {

    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int EMOTION_ATTACK_PHASE = 5;

    private float smokeX;
    private float smokeY;

    public EntityBattleshipYMTMob(World world) {
        super(world);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 46);
        this.smokeX = 0.0f;
        this.smokeY = 0.0f;
        this.setStateEmotion(0, 15, false);
    }

    @Override
    protected void setSizeWithScaleLevel() {
        switch (this.getScaleLevel()) {
            case 3:
                this.setSize(2.3f, 8.4f);
                this.smokeX = -2.52f;
                this.smokeY = 6.6f;
                break;
            case 2:
                this.setSize(1.8f, 6.3f);
                this.smokeX = -1.89f;
                this.smokeY = 4.95f;
                break;
            case 1:
                this.setSize(1.3f, 4.2f);
                this.smokeX = -1.26f;
                this.smokeY = 3.3f;
                break;
            default:
                this.setSize(0.8f, 2.1f);
                this.smokeX = -0.63f;
                this.smokeY = 1.65f;
                break;
        }
    }

    @Override
    protected void setBossInfo() {
        this.bossInfo = new BossInfoServer(this.getDisplayName(), BossInfo.Color.PINK, BossInfo.Overlay.NOTCHED_10);
    }

    @Override
    protected void setAIList() {
        super.setAIList();
        this.tasks.addTask(1, new EntityAIShipRangeAttack(this));
    }

    @Override
    public void initAttrsServerPost() {
        super.initAttrsServerPost();
        if (this.AttackEffectMap == null) {
            this.AttackEffectMap = new HashMap<>();
        }
        this.AttackEffectMap.put(4, new int[]{(int) (this.getScaleLevel() / 1.5), 100 + this.getScaleLevel() * 50, 25 + this.getScaleLevel() * 25});
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.world.isRemote) {
            updateClientParticles();
        }
    }

    private void updateClientParticles() {
        if (this.ticksExisted % 4 == 0) {
            float[] partPos = CalcHelper.rotateXZByAxis(this.smokeX, 0.0f, this.renderYawOffset % 360.0f * ((float) Math.PI / 180), 1.0f);
            ParticleHelper.spawnAttackParticleAt(this.posX + partPos[1], this.posY + this.smokeY, this.posZ + partPos[0], 1.0 + this.scaleLevel, 0.0, 0.0, (byte) 43);
        }
        if (this.ticksExisted % 16 == 0 && this.getStateEmotion(EMOTION_ATTACK_PHASE) > 0) {
            ParticleHelper.spawnAttackParticleAtEntity(this, 0.1 + this.scaleLevel, 16.0, 1.0, (byte) 4);
        }
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        if (this.getStateEmotion(EMOTION_ATTACK_PHASE) > 0) {
            return executeBeamAttack(target);
        } else {
            return prepareBeamAttack();
        }
    }

    private boolean executeBeamAttack(Entity target) {
        this.playSound(ModSounds.SHIP_YAMATO_SHOT, ConfigHandler.volumeFire, 1.0f);
        if (this.getRNG().nextInt(10) > 7) {
            this.playSound(ModSounds.SHIP_HIT, this.getSoundVolume(), this.getSoundPitch());
        }

        float atk = CombatHelper.modDamageByAdditionAttrs(this, target, this.getAttackBaseDamage(2, target), 3);
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this.getPositionVector(), target.getPositionVector().addVector(0.0, this.scaleLevel * -0.6, 0.0));
        EntityProjectileBeam beam = new EntityProjectileBeam(this.world);
        beam.initAttrs(this, 0, (float) distVec.x, (float) distVec.y, (float) distVec.z, atk);
        this.world.spawnEntity(beam);

        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, beam, (float) distVec.x, (float) distVec.y, (float) distVec.z, 2, true), point);

        this.setStateEmotion(EMOTION_ATTACK_PHASE, 0, true);
        return true;
    }

    private boolean prepareBeamAttack() {
        this.playSound(ModSounds.SHIP_YAMATO_READY, ConfigHandler.volumeFire, 1.0f);

        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 7, 1.0 + this.scaleLevel * 0.4, 0.0, 0.0), point);

        this.setStateEmotion(EMOTION_ATTACK_PHASE, 1, true);
        this.applyEmotesReaction(3);
        return false;
    }

    public void applyParticleAtAttacker(int type) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        if (type == 1) {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 5,  (this.scaleLevel + 1),  (this.scaleLevel + 1), 1.6 *  (this.scaleLevel + 1)), point);
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 5, 0.9 *  (this.scaleLevel + 1), 1.2 *  (this.scaleLevel + 1),  (this.scaleLevel + 1)), point);
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 5, 1.1 *  (this.scaleLevel + 1), 1.1 *  (this.scaleLevel + 1), 0.5 *  (this.scaleLevel + 1)), point);
        } else {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
        }
    }

    @Override
    public int getDamageType() {
        return 3;
    }
}