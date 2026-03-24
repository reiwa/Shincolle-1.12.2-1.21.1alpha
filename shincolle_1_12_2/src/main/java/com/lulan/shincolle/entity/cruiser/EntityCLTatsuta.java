package com.lulan.shincolle.entity.cruiser;

import com.google.common.base.Predicate;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.ai.EntityAIShipSkillAttack;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.IShipEmotion;
import com.lulan.shincolle.entity.other.EntityProjectileBeam;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EntityCLTatsuta extends BasicEntityShipSmall {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;
    private static final int EMOTION_SKILL_PHASE = 5;
    private static final int SKILL_TIMER_INDEX = 14;

    private static final int SKILL_STATE_NONE = 0;
    private static final int SKILL_STATE_READY = -1;
    private static final int SKILL_STATE_CHARGING = 1;
    private static final int SKILL_STATE_WW_ATTACK = 2;
    private static final int SKILL_STATE_FINAL_ATTACK = 3;

    private final Predicate<Entity> targetSelector;
    private int remainAttack;
    private Vec3d skillMotion;
    private final List<Entity> damagedTarget;

    public EntityCLTatsuta(World world) {
        super(world);
        this.setSize(0.75f, 1.65f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, 1);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 57);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 4);
        this.setStateMinor(STATE_MINOR_RARITY, 3);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[1]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[1]);
        this.ModelPos = new float[]{0.0f, 22.0f, 0.0f, 42.0f};
        this.targetSelector = new TargetHelper.Selector(this);
        this.remainAttack = 0;
        this.skillMotion = Vec3d.ZERO;
        this.damagedTarget = new ArrayList<>();
        this.StateFlag[15] = false;
        this.StateFlag[16] = false;
        this.setFoodSaturationMax(12);
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
    public boolean canBePushed() {
        return this.getStateEmotion(EMOTION_SKILL_PHASE) <= 0 && super.canBePushed();
    }

    @Override
    public boolean canFly() {
        return this.getStateEmotion(EMOTION_SKILL_PHASE) <= 0 && super.canFly();
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(0, new EntityAIShipSkillAttack(this));
        this.tasks.addTask(11, new EntityAIShipRangeAttack(this));
    }

    @Override
    public void calcShipAttributesAddRaw() {
        super.calcShipAttributesAddRaw();
        if (!this.world.isDaytime()) {
            this.getAttrs().setAttrsRaw(9, this.getAttrs().getAttrsRaw(9) + 0.15f);
            this.getAttrs().setAttrsRaw(15, this.getAttrs().getAttrsRaw(15) + 0.15f);
        }
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote) {
            updateServerLogic();
        }
    }

    private void updateServerLogic() {
        if ((this.ticksExisted & 0x7F) == 0 && !this.isMorph && this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 0) {
            EntityPlayer player = EntityHelper.getEntityPlayerByUID(this.getPlayerUID());
            if (player != null && this.getDistanceSq(player) < 256.0) {
                player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 100 + this.getStateMinor(0), 0, false, false));
            }
        }
        this.updateSkillEffect();
    }

    private void updateSkillEffect() {
        int skillState = this.getStateEmotion(EMOTION_SKILL_PHASE);
        if (skillState == SKILL_STATE_CHARGING || skillState == SKILL_STATE_WW_ATTACK) {
            this.motionX = this.skillMotion.x;
            this.motionY = this.skillMotion.y;
            this.motionZ = this.skillMotion.z;
            if (skillState == SKILL_STATE_WW_ATTACK) {
                this.damageNearbyEntity();
            }
            this.sendSyncPacket((byte) 54, true);
        } else if (skillState == SKILL_STATE_FINAL_ATTACK) {
            this.motionX = 0.0;
            this.motionY = 0.1;
            this.motionZ = 0.0;
            this.sendSyncPacket((byte) 54, true);
        }
    }

    private void damageNearbyEntity() {
        float rawAtk = this.getAttackBaseDamage(2, null);
        List<Entity> potentialTargets = EntityHelper.getEntitiesWithinAABB(this.world, Entity.class, this.getEntityBoundingBox().expand(4.0, 3.0, 4.0), this.targetSelector);
        for (Entity target : potentialTargets) {
            if (!this.damagedTarget.contains(target)) {
                this.damagedTarget.add(target);
                dealWhirlwindDamage(target, rawAtk);
            }
        }
    }

    private void dealWhirlwindDamage(Entity target, float rawAtk) {
        if (!target.canBeCollidedWith() || !EntityHelper.isNotHost(this, target)) return;
        float atk = CombatHelper.modDamageByAdditionAttrs(this, target, rawAtk, 0);
        if (TeamHelper.checkSameOwner(this, target) || !TeamHelper.doFriendlyFire(this, target)) {
            atk = 0.0f;
        }
        if (atk > 0) {
            atk = CombatHelper.applyCombatRateToDamage(this, true, 1.0f, atk);
            atk = CombatHelper.applyDamageReduceOnPlayer(target, atk);
        }
        if (target.attackEntityFrom(DamageSource.causeMobDamage(this), atk)) {
            this.applyParticleAtTarget(1, target, Dist4d.ONE);
            if (this.rand.nextInt(2) == 0) {
                this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, ConfigHandler.volumeFire, this.getSoundPitch());
            }
            if (ConfigHandler.canFlare) {
                this.flareTarget(target);
            }
            if (target.canBePushed()) {
                target.addVelocity(0.0, target instanceof IShipAttackBase ? 0.25 : 0.5, 0.0);
                this.sendSyncPacket((byte) 54, true);
            }
        }
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        if (this.isMorph) return super.attackEntityWithHeavyAmmo(target);
        if (!this.decrAmmoNum(1, this.getAmmoConsumption())) return false;
        this.addShipExp(ConfigHandler.expGain[2]);
        this.decrGrudgeNum(ConfigHandler.consumeGrudgeAction[1]);
        this.decrMorale(2);
        this.setCombatTick(this.ticksExisted);
        int skillState = this.getStateEmotion(EMOTION_SKILL_PHASE);
        if (skillState == SKILL_STATE_NONE) {
            this.playSound(ModSounds.SHIP_AP_P1, ConfigHandler.volumeFire, 1.0f);
            if (this.rand.nextInt(10) > 7) {
                this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
            }
            this.applyParticleAtAttacker(2, Dist4d.ONE);
            this.StateEmotion[EMOTION_SKILL_PHASE] = SKILL_STATE_READY;
        } else if (skillState == SKILL_STATE_READY) {
            this.setStateEmotion(EMOTION_SKILL_PHASE, SKILL_STATE_CHARGING, true);
            this.StateTimer[SKILL_TIMER_INDEX] = 10;
            this.remainAttack = 2 + (int) (this.getLevel() * 0.015f);
        }
        this.applyEmotesReaction(3);
        if (ConfigHandler.canFlare) {
            this.flareTarget(target);
        }
        return true;
    }

    private Entity checkSkillTarget(Entity target) {
        if (target == null || !target.isEntityAlive() || target.getDistanceSq(this) > (this.getAttrs().getAttackRange() * this.getAttrs().getAttackRange())) {
            if (this.remainAttack > 0) {
                List<Entity> potentialTargets = EntityHelper.getEntitiesWithinAABB(this.world, Entity.class, this.getEntityBoundingBox().expand(16.0, 16.0, 16.0), this.targetSelector);
                if (!potentialTargets.isEmpty()) {
                    Entity newTarget = potentialTargets.get(this.rand.nextInt(potentialTargets.size()));
                    this.setEntityTarget(newTarget);
                    return newTarget;
                }
            }
            return null;
        }
        return target;
    }

    private void updateSkillCharge(Entity target) {
        if (this.StateTimer[SKILL_TIMER_INDEX] == 8) {
            Vec3d vecpos = new Vec3d(target.posX - this.posX, target.posY - this.posY, target.posZ - this.posZ);
            this.skillMotion = vecpos.scale(0.14);
            vecpos.normalize();
            float[] degree = CalcHelper.getLookDegree(vecpos.x, vecpos.y, vecpos.z, true);
            this.rotationYaw = degree[0];
            this.rotationYawHead = degree[0];
            this.sendSyncPacket((byte) 54, true);
            this.sendSyncPacket((byte) 53, true);
            this.applyParticleAtAttacker(5, Dist4d.ONE);
        } else if (this.StateTimer[SKILL_TIMER_INDEX] == 6) {
            this.applyParticleAtTarget(5, target, new Dist4d(this.skillMotion.x, this.skillMotion.y, this.skillMotion.z, 1.0));
        }
    }

    private void updateSkillWWAttack() {
        if (this.StateTimer[SKILL_TIMER_INDEX] <= 0) {
            this.skillMotion = new Vec3d(0.0, 0.3, 0.0);
            this.StateTimer[SKILL_TIMER_INDEX] = 25;
            this.applyParticleAtAttacker(5, Dist4d.ONE);
        }
        if ((this.StateTimer[SKILL_TIMER_INDEX] & 1) == 0) {
            this.applyParticleAtAttacker(6, Dist4d.ONE);
            if ((this.StateTimer[SKILL_TIMER_INDEX] & 7) == 0) {
                this.remainAttack--;
                this.damagedTarget.clear();
                if (this.remainAttack <= 1) {
                    this.StateTimer[SKILL_TIMER_INDEX] = 0;
                }
                this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, ConfigHandler.volumeFire, this.getSoundPitch() * 1.1f);
                this.playSound(ModSounds.SHIP_JET, ConfigHandler.volumeFire, this.getSoundPitch());
            }
        }
    }

    private void updateSkillFinalAttack(Entity target) {
        if (this.StateTimer[SKILL_TIMER_INDEX] <= 0) {
            Vec3d vecpos = new Vec3d(target.posX - this.posX, target.posY - this.posY - 1.0, target.posZ - this.posZ);
            float[] degree = CalcHelper.getLookDegree(vecpos.x, vecpos.y, vecpos.z, true);
            this.rotationYaw = degree[0];
            this.rotationYawHead = degree[0];
            this.skillMotion = vecpos.normalize();
            this.remainAttack = 0;
            this.StateTimer[SKILL_TIMER_INDEX] = 15;
            this.sendSyncPacket((byte) 53, true);
            this.applyParticleAtAttacker(5, Dist4d.ONE);
        } else if (this.StateTimer[SKILL_TIMER_INDEX] == 6) {
            EntityProjectileBeam beam = new EntityProjectileBeam(this.world);
            beam.initAttrs(this, 1, (float) this.skillMotion.x, (float) this.skillMotion.y, (float) this.skillMotion.z, this.getAttackBaseDamage(3, target));
            this.world.spawnEntity(beam);
        } else if (this.StateTimer[SKILL_TIMER_INDEX] == 4) {
            this.playSound(ModSounds.SHIP_AP_ATTACK, ConfigHandler.volumeFire * 1.1f, this.getSoundPitch() * 0.6f);
            this.applyParticleAtTarget(6, target, new Dist4d(this.skillMotion.x, this.skillMotion.y, this.skillMotion.z, 1.0));
        }
    }

    @Override
    public boolean updateSkillAttack(Entity target) {
        Entity newTarget = this.checkSkillTarget(target);
        if (newTarget == null) {
            this.setStateEmotion(EMOTION_SKILL_PHASE, SKILL_STATE_NONE, true);
            this.remainAttack = 0;
            this.skillMotion = Vec3d.ZERO;
            this.StateTimer[SKILL_TIMER_INDEX] = 0;
            return false;
        }

        if (this.StateTimer[SKILL_TIMER_INDEX] <= 0) {
            int currentSkillState = this.getStateEmotion(EMOTION_SKILL_PHASE);
            if (currentSkillState == SKILL_STATE_FINAL_ATTACK) {
                this.setStateEmotion(EMOTION_SKILL_PHASE, SKILL_STATE_NONE, true);
                return false;
            }
            this.setStateEmotion(EMOTION_SKILL_PHASE, currentSkillState + 1, true);
        }

        switch (this.getStateEmotion(EMOTION_SKILL_PHASE)) {
            case SKILL_STATE_CHARGING:
                this.updateSkillCharge(newTarget);
                break;
            case SKILL_STATE_WW_ATTACK:
                this.updateSkillWWAttack();
                break;
            case SKILL_STATE_FINAL_ATTACK:
                this.updateSkillFinalAttack(newTarget);
                break;
            default:
        }

        if (this.StateTimer[SKILL_TIMER_INDEX] > 0) {
            this.StateTimer[SKILL_TIMER_INDEX]--;
        }
        return false;
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            return this.getStateEmotion(1) == 4 ? this.height * 0.2f : this.height * 0.27f;
        }
        return this.height * 0.7f;
    }

    public void applyParticleAtAttacker(int type) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        switch (type) {
            case 1:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 16, 1.0, 0.85, 1.0), point);
                break;
            case 2:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 11, 1.0, 0.8, 1.0), point);
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 12, 1.0, 0.8, 1.0), point);
                break;
            case 5:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
                break;
            case 6:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 14, 1.0, 0.7, 1.0), point);
                break;
            default:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 16, 1.0, 0.95, 1.0), point);
                break;
        }
    }

    public void applySoundAtAttacker(int type) {
        if(type == 1){
            this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, ConfigHandler.volumeFire * 1.2f, this.getSoundPitch() * 0.85f);
            if (this.rand.nextInt(10) > 7) {
                this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
            }
        } else if (this.getRNG().nextInt(2) == 0) {
            this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
        }
    }

    @Override
    public float getAttackBaseDamage(int type, Entity target) {
        switch (type) {
            case 1:
                return CombatHelper.modDamageByAdditionAttrs(this, target, this.shipAttrs.getAttackDamage(), 0);
            case 2:
                return this.shipAttrs.getAttackDamageHeavy() * 0.5f;
            case 3:
                return this.shipAttrs.getAttackDamageHeavy() * 1.5f;
            default:
                return this.shipAttrs.getAttackDamage() * 2.0f;
        }
    }

    public void applyParticleAtTarget(int type, Entity target, Dist4d distVec) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        switch (type) {
            case 1:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 9, false), point);
                break;
            case 2:
            case 3:
            case 4:
                break;
            case 5:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 46, this.posX + distVec.x * 2.0, this.posY + distVec.y * 2.0 + this.height * 0.7, this.posZ + distVec.z * 2.0, distVec.x * 1.5, distVec.y * 1.5, distVec.z * 1.5, false), point);
                break;
            case 6:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 45, this.posX + distVec.x * 10.0, this.posY + distVec.y * 10.0 + this.height * 0.7, this.posZ + distVec.z * 10.0, distVec.x * 1.5, distVec.y * 1.5, distVec.z * 1.5, true), point);
                break;
            default:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 1, false), point);
                break;
        }
    }

    public void applySoundAtTarget(int type, Entity target) {
        switch (type) {
            case 2:
                this.playSound(ModSounds.SHIP_EXPLODE, ConfigHandler.volumeFire, this.getSoundPitch());
                break;
            case 3:
            case 4:
                break;
            default:
                if (target instanceof IShipEmotion) {
                    this.playSound(ModSounds.SHIP_HITMETAL, ConfigHandler.volumeFire, this.getSoundPitch());
                } else {
                    this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, ConfigHandler.volumeFire, this.getSoundPitch());
                }
                break;
        }
    }
}