package com.lulan.shincolle.entity.cruiser;

import com.google.common.base.Predicate;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.ai.EntityAIShipSkillAttack;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.IShipEmotion;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EntityCLTenryuuMob extends BasicEntityShipHostile {

    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int EMOTION_SKILL_PHASE = 5;

    private static final int SKILL_PHASE_IDLE = 0;
    private static final int SKILL_PHASE_READY = 1;
    private static final int SKILL_PHASE_DASH = 2;
    private static final int SKILL_PHASE_FINAL_DIVE = 3;
    private static final int SKILL_PHASE_TRIGGER_HEAVY = -1;

    private final Predicate<Entity> targetSelector;
    private int remainAttack;
    private Vec3d skillMotion;
    private final ArrayList<Entity> damagedTarget;

    public EntityCLTenryuuMob(World world) {
        super(world);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 56);
        this.targetSelector = new TargetHelper.SelectorForHostile(this);
        this.remainAttack = 0;
        this.skillMotion = Vec3d.ZERO;
        this.damagedTarget = new ArrayList<>();
        this.setStateEmotion(0, this.rand.nextInt(32), false);
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
        return this.getStateEmotion(EMOTION_SKILL_PHASE) <= SKILL_PHASE_IDLE && super.canBePushed();
    }

    @Override
    public boolean canFly() {
        return this.getStateEmotion(EMOTION_SKILL_PHASE) <= SKILL_PHASE_IDLE && super.canFly();
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
        if (this.world.isRemote) {
            if (this.getStateEmotion(EMOTION_SKILL_PHASE) == SKILL_PHASE_FINAL_DIVE) {
                ParticleHelper.spawnAttackParticleAtEntity(this, 1.0, 1.0, 0.6, (byte) 14);
            }
        } else {
            this.attackEntityWithSkill();
        }
    }

    private void attackEntityWithSkill() {
        if (this.getStateEmotion(EMOTION_SKILL_PHASE) > SKILL_PHASE_READY) {
            if (this.attackTime3 == 6) {
                this.damagedTarget.clear();
                this.playSound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, ConfigHandler.volumeFire, this.getSoundPitch());
                this.playSound(ModSounds.SHIP_JET, ConfigHandler.volumeFire, this.getSoundPitch());
                this.applyParticleAtAttacker(5, Dist4d.ONE);
            } else if (this.attackTime3 == 3) {
                this.applyParticleAtTarget(5, null, new Dist4d(this.skillMotion.x, this.skillMotion.y, this.skillMotion.z, 1.0));
                if (this.getStateEmotion(EMOTION_SKILL_PHASE) == SKILL_PHASE_FINAL_DIVE) {
                    this.playSound(ModSounds.SHIP_AP_ATTACK, ConfigHandler.volumeFire * 1.1f, this.getSoundPitch() * 0.6f);
                }
            }
            if (this.attackTime3 <= 12 && this.attackTime3 >= 0) {
                this.motionX = this.skillMotion.x;
                this.motionY = this.skillMotion.y;
                this.motionZ = this.skillMotion.z;
                this.damageNearbyEntities();
            } else {
                this.motionX = 0.0;
                this.motionY = 0.0;
                this.motionZ = 0.0;
            }
            this.sendSyncPacket(1);
        }
    }

    private void damageNearbyEntities() {
        float rawDamage = this.getStateEmotion(EMOTION_SKILL_PHASE) == SKILL_PHASE_DASH ?
                this.getAttackBaseDamage(2, null) : this.getAttackBaseDamage(3, null);
        List<Entity> list = EntityHelper.getEntitiesWithinAABB(this.world, Entity.class, this.getEntityBoundingBox().expand(1.5, 1.5, 1.5), this.targetSelector);
        list.stream()
                .filter(this::isNewTargetForSkill)
                .forEach(target -> {
                    this.damagedTarget.add(target);
                    this.applySkillDamage(target, rawDamage);
                });
    }

    private boolean isNewTargetForSkill(Entity target) {
        return this.damagedTarget.stream().noneMatch(damaged -> damaged.equals(target));
    }

    private void applySkillDamage(Entity target, float rawDamage) {
        if (!target.canBeCollidedWith() || !EntityHelper.isNotHost(this, target)) return;
        if (TeamHelper.checkSameOwner(this, target)) return;
        float finalDamage = CombatHelper.modDamageByAdditionAttrs(this, target, rawDamage, 0);
        finalDamage = CombatHelper.applyCombatRateToDamage(this, true, 1.0f, finalDamage);
        finalDamage = CombatHelper.applyDamageReduceOnPlayer(target, finalDamage);
        if (!TeamHelper.doFriendlyFire(this, target)) finalDamage = 0.0f;
        if (target.attackEntityFrom(DamageSource.causeMobDamage(this), finalDamage)) {
            this.applyParticleAtTarget(1, target, Dist4d.ONE);
            if (this.rand.nextInt(2) == 0) {
                this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, ConfigHandler.volumeFire, this.getSoundPitch());
            }
            if (target.canBePushed()) {
                applySkillKnockback(target);
            }
            this.sendSyncPacket(1);
        }
    }

    private void applySkillKnockback(Entity target) {
        float knockbackStrength = (target instanceof IShipAttackBase) ? 0.02f : 0.05f;
        float yVelocity = (target instanceof IShipAttackBase) ? 0.2f : 0.4f;
        target.addVelocity(-MathHelper.sin(this.rotationYaw * ((float) Math.PI / 180f)) * knockbackStrength, yVelocity, MathHelper.cos(this.rotationYaw * ((float) Math.PI / 180f)) * knockbackStrength);
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        if (this.getStateEmotion(EMOTION_SKILL_PHASE) == SKILL_PHASE_IDLE) {
            this.playSound(ModSounds.SHIP_AP_P1, ConfigHandler.volumeFire, 1.0f);
            if (this.rand.nextInt(10) > 7) {
                this.playSound(ModSounds.SHIP_HIT, this.getSoundVolume(), this.getSoundPitch());
            }
            this.applyParticleAtAttacker(2, Dist4d.ONE);
            this.setStateEmotion(EMOTION_SKILL_PHASE, SKILL_PHASE_TRIGGER_HEAVY, true);
        } else if (this.getStateEmotion(EMOTION_SKILL_PHASE) == SKILL_PHASE_TRIGGER_HEAVY) {
            this.setStateEmotion(EMOTION_SKILL_PHASE, SKILL_PHASE_READY, true);
            this.remainAttack = 2 + this.scaleLevel * 2;
        }
        this.applyEmotesReaction(3);
        return true;
    }

    private Entity checkSkillTarget(Entity target) {
        boolean isInvalidTarget = target == null || !target.isEntityAlive() || target.getDistanceSq(this) > (this.getAttrs().getAttackRange() * this.getAttrs().getAttackRange());
        if (!isInvalidTarget) return target;
        if (this.remainAttack > 0) {
            List<Entity> list = EntityHelper.getEntitiesWithinAABB(this.world, Entity.class, this.getEntityBoundingBox().expand(8.0, 8.0, 8.0), this.targetSelector);
            if (!list.isEmpty()) {
                Entity newTarget = list.get(this.rand.nextInt(list.size()));
                this.setEntityTarget(newTarget);
                return newTarget;
            }
        }
        return null;
    }

    private void updateSkillHoriAttack(Entity target) {
        BlockPos pos = BlockHelper.findRandomSafePos(target);
        Vec3d vecpos = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        this.skillMotion = CalcHelper.getUnitVectorFromA2B(new Vec3d(target.posX, target.posY, target.posZ), vecpos).scale(-1.25);
        float[] degree = CalcHelper.getLookDegree(this.skillMotion.x, this.skillMotion.y, this.skillMotion.z, true);
        this.rotationYaw = degree[0];
        this.rotationYawHead = degree[0];
        EntityHelper.applyTeleport(this, this.getDistanceSqToCenter(pos), vecpos);
        --this.remainAttack;
        this.attackTime3 = 7;
        this.setStateEmotion(EMOTION_SKILL_PHASE, SKILL_PHASE_DASH, true);
        this.sendSyncPacket(3);
    }

    private void updateSkillFinalAttack(Entity target) {
        BlockPos pos = BlockHelper.findTopSafePos(target);
        Vec3d vecpos = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        this.skillMotion = new Vec3d(0.0, Math.abs(vecpos.y - target.posY) * -0.25, 0.0);
        EntityHelper.applyTeleport(this, this.getDistanceSqToCenter(pos), vecpos);
        --this.remainAttack;
        this.attackTime3 = 9;
        this.setStateEmotion(EMOTION_SKILL_PHASE, SKILL_PHASE_FINAL_DIVE, true);
        this.sendSyncPacket(3);
    }

    @Override
    public boolean updateSkillAttack(Entity target) {
        target = this.checkSkillTarget(target);
        if (target == null) {
            this.setStateEmotion(EMOTION_SKILL_PHASE, SKILL_PHASE_IDLE, true);
            this.remainAttack = 0;
            this.attackTime3 = 0;
            this.skillMotion = Vec3d.ZERO;
            return false;
        }
        if (this.getStateEmotion(EMOTION_SKILL_PHASE) == SKILL_PHASE_READY) {
            if (this.remainAttack > 1) this.updateSkillHoriAttack(target);
            else this.updateSkillFinalAttack(target);
        }
        if (this.attackTime3 <= 0) {
            if (this.getStateEmotion(EMOTION_SKILL_PHASE) == SKILL_PHASE_DASH) {
                this.setStateEmotion(EMOTION_SKILL_PHASE, SKILL_PHASE_READY, true);
            } else if (this.getStateEmotion(EMOTION_SKILL_PHASE) == SKILL_PHASE_FINAL_DIVE) {
                this.setStateEmotion(EMOTION_SKILL_PHASE, SKILL_PHASE_IDLE, true);
            }
        }
        if (this.attackTime3 > 0) --this.attackTime3;
        return false;
    }

    public void applyParticleAtAttacker(int type) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        switch (type) {
            case 1:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 15, 1.0, 1.0, 0.9), point);
                break;
            case 2:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 11, 1.0, 1.0, 0.7), point);
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 12, 1.0, 1.0, 0.7), point);
                break;
            case 5:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
                break;
            default:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 15, 0.9, 1.0, 1.0), point);
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
                return this.shipAttrs.getAttackDamageHeavy() * 0.4f;
            case 3:
                return this.shipAttrs.getAttackDamageHeavy();
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
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 44, this.posX + this.skillMotion.x * 2.0, this.posY + this.height * 0.4 + this.skillMotion.y * 2.5, this.posZ + this.skillMotion.z * 2.0, distVec.x, distVec.y, distVec.z, false), point);
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