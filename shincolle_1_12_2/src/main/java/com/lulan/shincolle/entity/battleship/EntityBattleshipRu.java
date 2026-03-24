package com.lulan.shincolle.entity.battleship;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.CombatHelper;
import com.lulan.shincolle.utility.EmotionHelper;
import com.lulan.shincolle.utility.ParticleHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.Objects;

public class EntityBattleshipRu extends BasicEntityShip {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;
    private static final int EMOTION_SKILL_PHASE = 5;

    private int remainAttack;
    private BlockPos skillTarget;

    public EntityBattleshipRu(World world) {
        super(world);
        this.setSize(0.7f, 1.8f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, 6);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 13);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 3);
        this.setStateMinor(STATE_MINOR_RARITY, 4);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[7]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[7]);
        this.ModelPos = new float[]{0.0f, 25.0f, 0.0f, 40.0f};
        this.StateFlag[15] = false;
        this.StateFlag[16] = false;
        this.remainAttack = 0;
        this.skillTarget = BlockPos.ORIGIN;
        this.postInit();
    }

    @Override
    public EntityAgeable createChild(EntityAgeable ageable) {
        return null;
    }

    @Override
    public int getEquipType() {
        return 1;
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(12, new EntityAIShipRangeAttack(this));
    }

    @Override
    public void calcShipAttributesAddRaw() {
        super.calcShipAttributesAddRaw();
        this.getAttrs().setAttrsRaw(9, this.getAttrs().getAttrsRaw(9) + 0.05f);
        this.getAttrs().setAttrsRaw(10, this.getAttrs().getAttrsRaw(10) + 0.05f);
        this.getAttrs().setAttrsRaw(11, this.getAttrs().getAttrsRaw(11) + 0.05f);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.world.isRemote) {
            updateClientEffects();
        } else {
            updateServerEffects();
        }
    }

    private void updateClientEffects() {
        if ((this.ticksExisted & 0xF) != 0) return;
        if (EmotionHelper.checkModelState(3, this.getStateEmotion(0)) && (this.ticksExisted & 0x1FF) < 400 && !this.isSitting() && !this.isRiding()) {
            ParticleHelper.spawnAttackParticleAtEntity(this, 1.34, 0.12, 0.17, (byte) 17);
        }
        if ((this.ticksExisted & 0x3F) == 0) {
            if (this.getStateEmotion(1) == 4 && EmotionHelper.checkModelState(0, this.getStateEmotion(0)) && (this.ticksExisted & 0x1FF) > 400) {
                ParticleHelper.spawnAttackParticleAtEntity(this, 0.5, 0.0, this.rand.nextInt(2) == 0 ? 0.0 : 2.0, (byte) 36);
            }
            if ((this.ticksExisted & 0x7F) == 0 && this.world.isDaytime() && this.getStateFlag(9)) {
                this.addPotionEffect(new PotionEffect(MobEffects.LUCK, 150, this.getStateMinor(0) / 140, false, false));
            }
        }
    }

    private void updateServerEffects() {
        if (this.StateEmotion[EMOTION_SKILL_PHASE] > 0) {
            this.updateSkillEffect();
        }
    }

    private void updateSkillEffect() {
        if (this.remainAttack > 0) {
            if ((this.ticksExisted & 3) == 0) {
                --this.remainAttack;
                spawnSkillMissile();
            }
        } else {
            this.StateEmotion[EMOTION_SKILL_PHASE] = 0;
            this.remainAttack = 0;
        }
    }

    private void spawnSkillMissile() {
        MissileData md = this.getMissileData(2);
        float baseDamage = this.getAttackBaseDamage(2, null);
        float gravity = 0.15f;
        float launchY = (float)this.posY + this.height * 0.4f;
        float targetX = this.skillTarget.getX() + this.rand.nextFloat() * 8.0f - 4.0f;
        float targetY = this.skillTarget.getY() + this.rand.nextFloat() * 4.0f - 2.0f;
        float targetZ = this.skillTarget.getZ() + this.rand.nextFloat() * 8.0f - 4.0f;
        float range = 160.0f;
        float power = 0.35f;
        float[] data = new float[]{baseDamage, gravity, launchY, targetX, targetY, targetZ, range, power, md.vel0, md.accY1, md.accY2};
        EntityAbyssMissile missile = new EntityAbyssMissile(this.world, this, md.type, 1, data);
        this.world.spawnEntity(missile);
        this.applySoundAtAttacker(2);
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(BlockPos target) {
        BlockPos skillTargetPos = new BlockPos(target.getX(), target.getY() + 0.5, target.getZ());
        return initiateBarrageSkill(skillTargetPos, null);
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        BlockPos skillTargetPos = new BlockPos(target.posX, target.posY + target.height * 0.35, target.posZ);
        return initiateBarrageSkill(skillTargetPos, target);
    }

    private boolean initiateBarrageSkill(BlockPos targetPos, Entity targetEntity) {
        if (!this.decrAmmoNum(1, this.getAmmoConsumption())) {
            return false;
        }
        this.addShipExp(ConfigHandler.expGain[2]);
        this.decrGrudgeNum(ConfigHandler.consumeGrudgeAction[1]);
        this.decrMorale(2);
        this.setCombatTick(this.ticksExisted);
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this.getPosition(), targetPos);
        BlockPos finalTargetPos = targetPos;
        if (this.rand.nextFloat() <= CombatHelper.calcMissRate(this, (float) distVec.d)) {
            float tarX = targetPos.getX() - 5.0f + this.rand.nextFloat() * 10.0f;
            float tarY = targetPos.getY() + this.rand.nextFloat() * 5.0f;
            float tarZ = targetPos.getZ() - 5.0f + this.rand.nextFloat() * 10.0f;
            finalTargetPos = new BlockPos(tarX, tarY, tarZ);
            ParticleHelper.spawnAttackTextParticle(this, 0);
        }
        if (this.StateEmotion[EMOTION_SKILL_PHASE] == 0) {
            this.playSound(ModSounds.SHIP_HITMETAL, ConfigHandler.volumeFire, 1.0f);
            if (this.rand.nextInt(10) > 7) {
                this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
            }
            this.applyParticleAtAttacker(2, Dist4d.ONE);
            this.StateEmotion[EMOTION_SKILL_PHASE] = 1;
            this.remainAttack += 5 + (int) (this.getLevel() * 0.035f);
            this.skillTarget = finalTargetPos;
        }
        if (ConfigHandler.canFlare) {
            if (targetEntity != null) this.flareTarget(targetEntity);
            else this.flareTarget(finalTargetPos);
        }
        return true;
    }

    @Override
    public double getMountedYOffset() {
        if (!this.isSitting()) {
            return this.height * 0.72f;
        }
        if (EmotionHelper.checkModelState(0, this.getStateEmotion(0))) {
            if (this.getStateEmotion(1) == 4) return this.height * 0.51f;
            if (this.getStateEmotion(7) == 4) return 0.0;
            return this.height * 0.55f;
        }
        return this.height * 0.45f;
    }

    @Override
    public float getAttackBaseDamage(int type, Entity target) {
        switch (type) {
            case 1:
                return CombatHelper.modDamageByAdditionAttrs(this, target, this.shipAttrs.getAttackDamage(), 0);
            case 2:
                return this.shipAttrs.getAttackDamageHeavy() * 0.2f;
            case 3:
                return this.shipAttrs.getAttackDamageAir();
            case 4:
                return this.shipAttrs.getAttackDamageAirHeavy();
            default:
                return this.shipAttrs.getAttackDamage() * 2.0f;
        }
    }

    public void applyParticleAtAttacker(int type, Dist4d distVec) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        if (type == 1) {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 19, this.posX, this.posY, this.posZ, distVec.x, 0.7f, distVec.z, true), point);
        } else {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
        }
    }
}