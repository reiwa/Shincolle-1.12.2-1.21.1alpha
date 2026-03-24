package com.lulan.shincolle.entity.cruiser;

import com.google.common.base.Predicate;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.ai.EntityAIShipSkillAttack;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.IShipEmotion;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class EntityCLTenryuu extends BasicEntityShipSmall {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;
    private static final int EMOTION_SKILL_PHASE = 5;
    private static final int SKILL_TIMER_ID = 14;

    private enum SkillState {
        IDLE(0),
        PREPARED(-1),
        CHARGING(1),
        DASH_ATTACK(2),
        FINAL_ATTACK(3);

        final int stateCode;
        SkillState(int code) { this.stateCode = code; }

        static SkillState fromCode(int code) {
            for (SkillState state : values()) {
                if (state.stateCode == code) return state;
            }
            return IDLE;
        }
    }

    private final Predicate<Entity> targetSelector;
    private int remainAttack;
    private Vec3d skillMotion;
    private final Set<Entity> damagedTargets;

    public EntityCLTenryuu(World world) {
        super(world);
        this.setSize(0.75f, 1.65f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, 1);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 56);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 4);
        this.setStateMinor(STATE_MINOR_RARITY, 5);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[1]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[1]);
        this.ModelPos = new float[]{0.0f, 22.0f, 0.0f, 42.0f};
        this.targetSelector = new TargetHelper.Selector(this);
        this.remainAttack = 0;
        this.skillMotion = Vec3d.ZERO;
        this.damagedTargets = new HashSet<>();
        this.StateFlag[15] = false;
        this.StateFlag[16] = false;
        this.setFoodSaturationMax(12);
        this.postInit();
    }

    private SkillState getSkillState() {
        return SkillState.fromCode(this.StateEmotion[EMOTION_SKILL_PHASE]);
    }

    private void setSkillState(SkillState state) {
        this.setStateEmotion(EMOTION_SKILL_PHASE, state.stateCode, true);
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
        return getSkillState() == SkillState.IDLE && super.canBePushed();
    }

    @Override
    public boolean canFly() {
        return getSkillState() == SkillState.IDLE && super.canFly();
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
        if (this.world.isRemote) {
            updateClientEffects();
        } else {
            updateServerEffects();
        }
    }

    private void updateClientEffects() {
        if (getSkillState() == SkillState.FINAL_ATTACK) {
            ParticleHelper.spawnAttackParticleAtEntity(this, 1.0, 1.0, 0.6, (byte) 14);
        }
    }

    private void updateServerEffects() {
        if ((this.ticksExisted & 0x7F) == 0) {
            applyNightVisionBuffToPlayer();
        }
        updateSkillEffect();
    }

    private void applyNightVisionBuffToPlayer() {
        if (!this.isMorph && this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 0) {
            EntityPlayer player = EntityHelper.getEntityPlayerByUID(this.getPlayerUID());
            if (player != null && this.getDistanceSq(player) < 256.0) {
                player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 100 + this.getStateMinor(0), 0, false, false));
            }
        }
    }

    private void updateSkillEffect() {
        if (getSkillState().stateCode > 1) {
            int skillTimer = this.StateTimer[SKILL_TIMER_ID];
            if (skillTimer == 6) {
                this.damagedTargets.clear();
                this.playSound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, ConfigHandler.volumeFire, this.getSoundPitch());
                this.playSound(ModSounds.SHIP_JET, ConfigHandler.volumeFire, this.getSoundPitch());
                this.applyParticleAtAttacker(5, Dist4d.ONE);
            } else if (skillTimer == 3) {
                this.applyParticleAtTarget(5, null, new Dist4d(this.skillMotion.x, this.skillMotion.y, this.skillMotion.z, 1.0));
                if (getSkillState() == SkillState.FINAL_ATTACK) {
                    this.playSound(ModSounds.SHIP_AP_ATTACK, ConfigHandler.volumeFire * 1.1f, this.getSoundPitch() * 0.6f);
                }
            }
            if (skillTimer <= 7 && skillTimer >= 0) {
                this.motionX = this.skillMotion.x;
                this.motionY = this.skillMotion.y;
                this.motionZ = this.skillMotion.z;
                this.damageNearbyEntities();
            } else {
                this.motionX = 0.0;
                this.motionY = 0.0;
                this.motionZ = 0.0;
            }
            this.sendSyncPacket((byte) 54, true);
        }
    }

    private void damageNearbyEntities() {
        float rawAtk = this.getAttackBaseDamage(getSkillState() == SkillState.DASH_ATTACK ? 2 : 3, null);
        List<Entity> list = EntityHelper.getEntitiesWithinAABB(this.world, Entity.class, this.getEntityBoundingBox().expand(2.0, 1.5, 2.0), this.targetSelector);
        for (Entity target : list) {
            if (this.damagedTargets.add(target)) {
                dealSkillDamageToTarget(target, rawAtk);
            }
        }
    }

    private void dealSkillDamageToTarget(Entity target, float rawAtk) {
        if (!target.canBeCollidedWith() || !EntityHelper.isNotHost(this, target) || TeamHelper.checkSameOwner(this, target)) {
            return;
        }
        float atk = CombatHelper.modDamageByAdditionAttrs(this, target, rawAtk, 0);
        atk = CombatHelper.applyCombatRateToDamage(this, true, 1.0f, atk);
        atk = CombatHelper.applyDamageReduceOnPlayer(target, atk);
        if (!TeamHelper.doFriendlyFire(this, target)) {
            atk = 0.0f;
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
                if (target instanceof IShipAttackBase) {
                    target.addVelocity(-MathHelper.sin(this.rotationYaw * ((float) Math.PI / 180f)) * 0.02f, 0.2, MathHelper.cos(this.rotationYaw * ((float) Math.PI / 180f)) * 0.02f);
                } else {
                    target.addVelocity(-MathHelper.sin(this.rotationYaw * ((float) Math.PI / 180f)) * 0.05f, 0.4, MathHelper.cos(this.rotationYaw * ((float) Math.PI / 180f)) * 0.05f);
                }
                this.sendSyncPacket((byte) 54, true);
            }
        }
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        if (this.isMorph) {
            return super.attackEntityWithHeavyAmmo(target);
        }
        if (!this.decrAmmoNum(1, this.getAmmoConsumption())) {
            return false;
        }
        this.addShipExp(ConfigHandler.expGain[2]);
        this.decrGrudgeNum(ConfigHandler.consumeGrudgeAction[1]);
        this.decrMorale(2);
        this.setCombatTick(this.ticksExisted);
        if (getSkillState() == SkillState.IDLE) {
            this.playSound(ModSounds.SHIP_AP_P1, ConfigHandler.volumeFire, 1.0f);
            if (this.rand.nextInt(10) > 7) {
                this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
            }
            this.applyParticleAtAttacker(2, Dist4d.ONE);
            setSkillState(SkillState.PREPARED);
        } else if (getSkillState() == SkillState.PREPARED) {
            setSkillState(SkillState.CHARGING);
            this.remainAttack = 3 + (int) (this.getLevel() * 0.03f);
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
                List<Entity> list = EntityHelper.getEntitiesWithinAABB(this.world, Entity.class, this.getEntityBoundingBox().expand(10.0, 10.0, 10.0), this.targetSelector);
                if (!list.isEmpty()) {
                    target = list.get(this.rand.nextInt(list.size()));
                    this.setEntityTarget(target);
                    return target;
                }
            }
            return null;
        }
        return target;
    }

    private void updateSkillHoriAttack(Entity target) {
        BlockPos pos = BlockHelper.findRandomSafePos(target);
        Vec3d vecpos = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        this.skillMotion = CalcHelper.getUnitVectorFromA2B(new Vec3d(target.posX, target.posY, target.posZ), vecpos).scale(-1.25);
        float[] degree = CalcHelper.getLookDegree(this.skillMotion.x, this.skillMotion.y, this.skillMotion.z, true);
        this.rotationYaw = degree[0];
        this.rotationYawHead = degree[0];
        EntityHelper.applyTeleport(this, this.getDistanceSqToCenter(pos), vecpos);
        this.remainAttack--;
        this.StateTimer[SKILL_TIMER_ID] = 7;
        setSkillState(SkillState.DASH_ATTACK);
        this.sendSyncPacket((byte) 53, true);
    }

    private void updateSkillFinalAttack(Entity target) {
        BlockPos pos = BlockHelper.findTopSafePos(target);
        Vec3d vecpos = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        this.skillMotion = new Vec3d(0.0, Math.abs(vecpos.y - target.posY) * -0.25, 0.0);
        EntityHelper.applyTeleport(this, this.getDistanceSqToCenter(pos), vecpos);
        this.remainAttack--;
        this.StateTimer[SKILL_TIMER_ID] = 9;
        setSkillState(SkillState.FINAL_ATTACK);
        this.sendSyncPacket((byte) 53, true);
    }

    @Override
    public boolean updateSkillAttack(Entity target) {
        target = this.checkSkillTarget(target);
        if (target == null) {
            setSkillState(SkillState.IDLE);
            this.remainAttack = 0;
            this.StateTimer[SKILL_TIMER_ID] = 0;
            this.skillMotion = Vec3d.ZERO;
            return false;
        }
        if (getSkillState() == SkillState.CHARGING) {
            if (this.remainAttack > 1) {
                this.updateSkillHoriAttack(target);
            } else {
                this.updateSkillFinalAttack(target);
            }
        }
        if (this.StateTimer[SKILL_TIMER_ID] <= 0) {
            if (getSkillState() == SkillState.DASH_ATTACK) {
                setSkillState(SkillState.CHARGING);
            } else if (getSkillState() == SkillState.FINAL_ATTACK) {
                setSkillState(SkillState.IDLE);
            }
        }
        if (this.StateTimer[SKILL_TIMER_ID] > 0) {
            this.StateTimer[SKILL_TIMER_ID]--;
        }
        return false;
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            return this.getStateEmotion(1) == 4 ? this.height * 0.2f : this.height * 0.3f;
        }
        return this.height * 0.7f;
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
        if(type == 1){
            this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, ConfigHandler.volumeFire * 1.2f, this.getSoundPitch() * 0.85f);
            if (this.rand.nextInt(10) > 7) {
                this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
            }
        } else if(this.getRNG().nextInt(2) == 0){
            this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
        }
    }

    @Override
    public float getAttackBaseDamage(int type, Entity target) {
        switch (type) {
            case 1:
                return CombatHelper.modDamageByAdditionAttrs(this, target, this.shipAttrs.getAttackDamage(), 0);
            case 2:
                return this.shipAttrs.getAttackDamageHeavy() * 0.3f;
            case 3:
                return this.shipAttrs.getAttackDamageHeavy() * 1.2f;
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