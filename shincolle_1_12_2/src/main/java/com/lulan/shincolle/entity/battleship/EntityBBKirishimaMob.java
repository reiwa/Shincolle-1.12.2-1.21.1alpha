package com.lulan.shincolle.entity.battleship;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.CombatHelper;
import com.lulan.shincolle.utility.EmotionHelper;
import com.lulan.shincolle.utility.ParticleHelper;
import net.minecraft.entity.Entity;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.HashMap;
import java.util.Objects;

public class EntityBBKirishimaMob extends BasicEntityShipHostile {

    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int ATTACK_EFFECT_KEY_CRIT = 15;

    private float smokeX1;
    private float smokeY1;

    public EntityBBKirishimaMob(World world) {
        super(world);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 63);
        this.smokeX1 = 0.0f;
        this.smokeY1 = 0.0f;
        int initialEmotion = this.rand.nextBoolean() ? 5 : 7;
        this.setStateEmotion(0, initialEmotion, false);
    }

    @Override
    protected void setSizeWithScaleLevel() {
        switch (this.getScaleLevel()) {
            case 3:
                this.setSize(2.4f, 7.5f);
                this.smokeX1 = -2.36f;
                this.smokeY1 = 4.6f;
                break;
            case 2:
                this.setSize(1.8f, 5.625f);
                this.smokeX1 = -1.76f;
                this.smokeY1 = 3.4f;
                break;
            case 1:
                this.setSize(1.2f, 3.75f);
                this.smokeX1 = -1.16f;
                this.smokeY1 = 2.3f;
                break;
            default:
                this.setSize(0.6f, 1.875f);
                this.smokeX1 = -0.6f;
                this.smokeY1 = 1.17f;
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
        this.AttackEffectMap.put(ATTACK_EFFECT_KEY_CRIT, new int[]{(int) (this.getScaleLevel() / 1.5), 40 + this.getScaleLevel() * 30, 25 + this.getScaleLevel() * 25});
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.world.isRemote) {
            updateClientParticles();
        }
    }

    private void updateClientParticles() {
        if (this.ticksExisted % 4 == 0 && EmotionHelper.checkModelState(0, this.getStateEmotion(0))) {
            float[] partPos = CalcHelper.rotateXZByAxis(this.smokeX1, 0.0f, this.renderYawOffset % 360.0f * ((float) Math.PI / 180), 1.0f);
            ParticleHelper.spawnAttackParticleAt(this.posX + partPos[1], this.posY + this.smokeY1, this.posZ + partPos[0], 1.0 + this.scaleLevel, 0.0, 0.0, (byte) 43);
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

    @Override
    public float getAttackBaseDamage(int type, Entity target) {
        switch (type) {
            case 1:
                return CombatHelper.modDamageByAdditionAttrs(this, target, this.shipAttrs.getAttackDamage(), 0);
            case 2:
                return this.shipAttrs.getAttackDamageHeavy();
            case 3:
                return this.shipAttrs.getAttackDamageAir();
            case 4:
                return this.shipAttrs.getAttackDamageAirHeavy();
            default:
                return this.shipAttrs.getAttackDamage() * 1.5f;
        }
    }
}