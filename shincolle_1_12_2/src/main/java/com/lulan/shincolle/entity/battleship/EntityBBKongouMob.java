package com.lulan.shincolle.entity.battleship;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.EmotionHelper;
import com.lulan.shincolle.utility.ParticleHelper;
import net.minecraft.entity.Entity;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.HashMap;
import java.util.Objects;

public class EntityBBKongouMob extends BasicEntityShipHostile {

    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int ATTACK_EFFECT_KEY_BURN = 18;
    private static final int EMOTION_BASE_IDLE = 7;
    private static final int EMOTION_ALT_IDLE = 15;

    private float smokeX1;
    private float smokeY1;
    private float smokeX2;
    private float smokeY2;

    public EntityBBKongouMob(World world) {
        super(world);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 60);
        this.smokeX1 = 0.0f;
        this.smokeY1 = 0.0f;
        this.smokeX2 = 0.0f;
        this.smokeY2 = 0.0f;
        int initialEmotion = this.rand.nextBoolean() ? EMOTION_BASE_IDLE : EMOTION_ALT_IDLE;
        this.setStateEmotion(0, initialEmotion, false);
    }

    @Override
    protected void setSizeWithScaleLevel() {
        switch (this.getScaleLevel()) {
            case 3:
                this.setSize(2.4f, 7.5f);
                this.smokeX1 = -1.64f;
                this.smokeY1 = 4.74f;
                this.smokeX2 = -2.61f;
                this.smokeY2 = 4.27f;
                break;
            case 2:
                this.setSize(1.8f, 5.625f);
                this.smokeX1 = -1.25f;
                this.smokeY1 = 3.57f;
                this.smokeX2 = -1.98f;
                this.smokeY2 = 3.18f;
                break;
            case 1:
                this.setSize(1.2f, 3.75f);
                this.smokeX1 = -0.82f;
                this.smokeY1 = 2.47f;
                this.smokeX2 = -1.33f;
                this.smokeY2 = 2.21f;
                break;
            default:
                this.setSize(0.6f, 1.875f);
                this.smokeX1 = -0.7f;
                this.smokeY1 = 1.17f;
                this.smokeX2 = -0.45f;
                this.smokeY2 = 1.32f;
                break;
        }
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
    public void initAttrsServerPost() {
        super.initAttrsServerPost();
        if (this.AttackEffectMap == null) {
            this.AttackEffectMap = new HashMap<>();
        }
        this.AttackEffectMap.put(ATTACK_EFFECT_KEY_BURN, new int[]{(int) (this.getScaleLevel() / 1.5), 80 + this.getScaleLevel() * 50, 25 + this.getScaleLevel() * 25});
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.world.isRemote && this.ticksExisted % 4 == 0 && EmotionHelper.checkModelState(0, this.getStateEmotion(0))) {
            spawnSmokeParticles(this.smokeX1, this.smokeY1);
            spawnSmokeParticles(this.smokeX2, this.smokeY2);
        }
    }

    private void spawnSmokeParticles(float smokeX, float smokeY) {
        float yawRad = this.renderYawOffset * ((float) Math.PI / 180f);
        float[] partPos = CalcHelper.rotateXZByAxis(smokeX, 0.0f, yawRad, 1.0f);
        double particleX = this.posX + partPos[1];
        double particleY = this.posY + smokeY;
        double particleZ = this.posZ + partPos[0];
        double particleScale = 1.0 + this.scaleLevel;
        ParticleHelper.spawnAttackParticleAt(particleX, particleY, particleZ, particleScale, 0.0, 0.0, (byte) 43);
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