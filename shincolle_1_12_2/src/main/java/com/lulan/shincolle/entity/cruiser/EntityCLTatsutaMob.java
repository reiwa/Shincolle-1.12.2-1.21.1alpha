package com.lulan.shincolle.entity.cruiser;

import com.google.common.base.Predicate;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.ai.EntityAIShipSkillAttack;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
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
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class EntityCLTatsutaMob extends BasicEntityShipHostile {

    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int EMOTION_SKILL_PHASE = 5;
    private static final int SKILL_PHASE_IDLE = 0;
    private static final int SKILL_PHASE_TRIGGERED = -1;
    private static final int SKILL_PHASE_CHARGE = 1;
    private static final int SKILL_PHASE_SPIN = 2;
    private static final int SKILL_PHASE_FINISH = 3;

    private final Predicate<Entity> targetSelector;
    private int remainAttack;
    private Vec3d skillMotion;
    private final Set<Entity> damagedTarget;

    public EntityCLTatsutaMob(World world) {
        super(world);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 57);
        this.targetSelector = new TargetHelper.SelectorForHostile(this);
        this.remainAttack = 0;
        this.skillMotion = Vec3d.ZERO;
        this.damagedTarget = new HashSet<>();
        this.setStateEmotion(0, this.rand.nextInt(8), false);
    }

    @Override
    protected void setSizeWithScaleLevel() {
        switch (this.getScaleLevel()) {
            case 3:
                this.setSize(1.7f, 6.4f);
                break;
            case 2:
                this.setSize(1.3f, 4.8f);
                break;
            case 1:
                this.setSize(0.9f, 3.2f);
                break;
            default:
                this.setSize(0.75f, 1.65f);
                break;
        }
    }

    @Override
    protected void setBossInfo() {
        this.bossInfo = new BossInfoServer(this.getDisplayName(), BossInfo.Color.PURPLE, BossInfo.Overlay.NOTCHED_10);
    }

    @Override
    public int getDamageType() {
        return 4;
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
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote) {
            updateSkillMotion();
        }
    }

    private void updateSkillMotion() {
        int skillPhase = this.getStateEmotion(EMOTION_SKILL_PHASE);
        if (skillPhase == SKILL_PHASE_CHARGE || skillPhase == SKILL_PHASE_SPIN) {
            this.motionX = this.skillMotion.x;
            this.motionY = this.skillMotion.y;
            this.motionZ = this.skillMotion.z;
            if (skillPhase == SKILL_PHASE_SPIN) {
                this.damageNearbyEntities();
            }
            this.sendSyncPacket(1);
        } else if (skillPhase == SKILL_PHASE_FINISH) {
            this.motionX = 0.0;
            this.motionY = 0.1;
            this.motionZ = 0.0;
            this.sendSyncPacket(1);
        }
    }

    private void damageNearbyEntities() {
        float rawAtk = this.getAttackBaseDamage(2, null);
        List<Entity> nearbyEntities = EntityHelper.getEntitiesWithinAABB(this.world, Entity.class, this.getEntityBoundingBox().expand(4.0, 3.0, 4.0), this.targetSelector);
        for (Entity target : nearbyEntities) {
            if (!this.damagedTarget.add(target)) continue;
            if (!target.canBeCollidedWith() || !EntityHelper.isNotHost(this, target) || TeamHelper.checkSameOwner(this, target)) continue;

            float atk = CombatHelper.modDamageByAdditionAttrs(this, target, rawAtk, 0);
            atk = CombatHelper.applyCombatRateToDamage(this, true, 1.0f, atk);
            atk = CombatHelper.applyDamageReduceOnPlayer(target, atk);
            if (!TeamHelper.doFriendlyFire(this, target)) {
                atk = 0.0f;
            }
            if (target.attackEntityFrom(DamageSource.causeMobDamage(this), atk)) {
                applySpinAttackEffects(target);
            }
        }
    }

    private void applySpinAttackEffects(Entity target) {
        this.applyParticleAtTarget(1, target, Dist4d.ONE);
        if (this.rand.nextInt(2) == 0) {
            this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, ConfigHandler.volumeFire, this.getSoundPitch());
        }
        if (target.canBePushed()) {
            double kbX = -MathHelper.sin(this.rotationYaw * (float) Math.PI / 180f);
            double kbZ = MathHelper.cos(this.rotationYaw * (float) Math.PI / 180f);
            if (target instanceof IShipAttackBase) {
                target.addVelocity(kbX * 0.02f, 0.2, kbZ * 0.02f);
            } else {
                target.addVelocity(kbX * 0.05f, 0.4, kbZ * 0.05f);
            }
            this.sendSyncPacket(1);
        }
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        int skillPhase = this.getStateEmotion(EMOTION_SKILL_PHASE);
        if (skillPhase == SKILL_PHASE_IDLE) {
            this.playSound(ModSounds.SHIP_AP_P1, ConfigHandler.volumeFire, 1.0f);
            if (this.rand.nextInt(10) > 7) {
                this.playSound(ModSounds.SHIP_HIT, this.getSoundVolume(), this.getSoundPitch());
            }
            this.applyParticleAtAttacker(2, Dist4d.ONE);
            this.setStateEmotion(EMOTION_SKILL_PHASE, SKILL_PHASE_TRIGGERED, true);
        } else if (skillPhase == SKILL_PHASE_TRIGGERED) {
            this.setStateEmotion(EMOTION_SKILL_PHASE, SKILL_PHASE_CHARGE, true);
            this.remainAttack = 1 + this.scaleLevel;
            this.attackTime3 = 10;
        }
        this.applyEmotesReaction(3);
        return true;
    }

    @Nullable
    private Entity checkSkillTarget(Entity target) {
        if (target == null || !target.isEntityAlive() || target.getDistanceSq(this) > (this.getAttrs().getAttackRange() * this.getAttrs().getAttackRange())) {
            if (this.remainAttack > 0) {
                List<Entity> list = EntityHelper.getEntitiesWithinAABB(this.world, Entity.class, this.getEntityBoundingBox().expand(13.0, 13.0, 13.0), this.targetSelector);
                if (!list.isEmpty()) {
                    Entity newTarget = list.get(this.rand.nextInt(list.size()));
                    this.setEntityTarget(newTarget);
                    return newTarget;
                }
            }
            return null;
        }
        return target;
    }

    private void updateSkillCharge(Entity target) {
        if (this.attackTime3 == 8) {
            Vec3d vecpos = new Vec3d(target.posX - this.posX, target.posY - this.posY, target.posZ - this.posZ);
            this.skillMotion = vecpos.scale(0.14);
            float[] degree = CalcHelper.getLookDegree(vecpos.normalize().x, vecpos.normalize().y, vecpos.normalize().z, true);
            this.rotationYaw = degree[0];
            this.rotationYawHead = degree[0];
            this.sendSyncPacket(1);
            this.sendSyncPacket(3);
            this.applyParticleAtAttacker(5, Dist4d.ONE);
        } else if (this.attackTime3 == 6) {
            this.applyParticleAtTarget(5, target, new Dist4d(this.skillMotion.x, this.skillMotion.y, this.skillMotion.z, 1.0));
        }
    }

    private void updateSkillSpin() {
        if (this.attackTime3 <= 0) {
            this.skillMotion = new Vec3d(0.0, 0.3 + this.scaleLevel * 0.1, 0.0);
            this.attackTime3 = 25;
            this.applyParticleAtAttacker(5, Dist4d.ONE);
        }
        if ((this.attackTime3 & 1) == 0) {
            this.applyParticleAtAttacker(6, Dist4d.ONE);
            if ((this.attackTime3 & 7) == 0) {
                --this.remainAttack;
                this.damagedTarget.clear();
                if (this.remainAttack <= 1) {
                    this.attackTime3 = 0;
                }
                this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, ConfigHandler.volumeFire, this.getSoundPitch() * 1.1f);
                this.playSound(ModSounds.SHIP_JET, ConfigHandler.volumeFire, this.getSoundPitch());
            }
        }
    }

    private void updateSkillFinish(Entity target) {
        if (this.attackTime3 <= 0) {
            Vec3d vecpos = new Vec3d(target.posX - this.posX, target.posY - this.posY - 1.0, target.posZ - this.posZ);
            float[] degree = CalcHelper.getLookDegree(vecpos.normalize().x, vecpos.normalize().y, vecpos.normalize().z, true);
            this.rotationYaw = degree[0];
            this.rotationYawHead = degree[0];
            this.skillMotion = vecpos.normalize();
            this.remainAttack = 0;
            this.attackTime3 = 15;
            this.sendSyncPacket(3);
            this.applyParticleAtAttacker(5, Dist4d.ONE);
        } else if (this.attackTime3 == 6) {
            EntityProjectileBeam beam = new EntityProjectileBeam(this.world);
            beam.initAttrs(this, 1, (float) this.skillMotion.x, (float) this.skillMotion.y, (float) this.skillMotion.z, this.getAttackBaseDamage(3, target));
            this.world.spawnEntity(beam);
        } else if (this.attackTime3 == 4) {
            this.playSound(ModSounds.SHIP_AP_ATTACK, ConfigHandler.volumeFire * 1.1f, this.getSoundPitch() * 0.6f);
            this.applyParticleAtTarget(6, target, new Dist4d(this.skillMotion.x, this.skillMotion.y, this.skillMotion.z, 1.0));
        }
    }

    @Override
    public boolean updateSkillAttack(Entity target) {
        target = this.checkSkillTarget(target);
        if (target == null) {
            this.setStateEmotion(EMOTION_SKILL_PHASE, SKILL_PHASE_IDLE, true);
            this.remainAttack = 0;
            this.skillMotion = Vec3d.ZERO;
            this.attackTime3 = 0;
            return false;
        }

        if (this.attackTime3 <= 0) {
            int currentPhase = this.getStateEmotion(EMOTION_SKILL_PHASE);
            if (currentPhase == SKILL_PHASE_FINISH) {
                this.setStateEmotion(EMOTION_SKILL_PHASE, SKILL_PHASE_IDLE, true);
                this.remainAttack = 0;
                this.skillMotion = Vec3d.ZERO;
                this.attackTime3 = 0;
                return false;
            }
            this.setStateEmotion(EMOTION_SKILL_PHASE, currentPhase + 1, true);
        }

        switch (this.getStateEmotion(EMOTION_SKILL_PHASE)) {
            case SKILL_PHASE_CHARGE:
                this.updateSkillCharge(target);
                break;
            case SKILL_PHASE_SPIN:
                this.updateSkillSpin();
                break;
            case SKILL_PHASE_FINISH:
                this.updateSkillFinish(target);
                break;
            default:
        }

        if (this.attackTime3 > 0) {
            --this.attackTime3;
        }
        return false;
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
        if (type == 1) {
            this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, ConfigHandler.volumeFire * 1.2f, this.getSoundPitch() * 0.85f);
            if (this.rand.nextInt(10) > 7) {
                this.playSound(ModSounds.SHIP_HIT, this.getSoundVolume(), this.getSoundPitch());
            }
        } else {
            if (this.getRNG().nextInt(2) == 0) {
                this.playSound(ModSounds.SHIP_HIT, this.getSoundVolume(), this.getSoundPitch());
            }
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
                return this.shipAttrs.getAttackDamage();
        }
    }

    public void applyParticleAtTarget(int type, Entity target, Dist4d distVec) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        switch (type) {
            case 1:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 9, false), point);
                break;
            case 5:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 46, this.posX + distVec.x * 2.0, this.posY + distVec.y * 2.0 + this.height * 0.7, this.posZ + distVec.z * 2.0, distVec.x * (1.5 + this.scaleLevel * 0.8), distVec.y * (1.5 + this.scaleLevel * 0.8), distVec.z * (1.5 + this.scaleLevel * 0.8), false), point);
                break;
            case 6:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 45, this.posX + distVec.x * 10.0, this.posY + distVec.y * 10.0 + this.height * 0.7, this.posZ + distVec.z * 10.0, distVec.x * 1.5, distVec.y * 1.5, distVec.z * 1.5, true), point);
                break;
            case 2:
            case 3:
            case 4:
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