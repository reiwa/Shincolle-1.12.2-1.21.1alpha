package com.lulan.shincolle.entity.battleship;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.IShipEmotion;
import com.lulan.shincolle.entity.hime.EntityNorthernHime;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EntityBattleshipNGT extends BasicEntityShipSmall {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;
    private static final int EMOTION_ATTACK_PHASE = 5;
    private static final int[] LOVE_PARTICLES = {31, 1, 7, 16, 29};

    public EntityBattleshipNGT(World world) {
        super(world);
        this.setSize(0.7f, 2.0f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, 6);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 37);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 3);
        this.setStateMinor(STATE_MINOR_RARITY, 2);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[7]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[7]);
        this.ModelPos = new float[]{0.0f, 25.0f, 0.0f, 40.0f};
        this.StateFlag[15] = false;
        this.StateFlag[16] = false;
        this.setFoodSaturationMax(20);
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
        this.tasks.addTask(11, new EntityAIShipRangeAttack(this));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.world.isRemote) {
            onLivingUpdateClient();
        } else {
            onLivingUpdateServer();
        }
    }

    private void onLivingUpdateClient() {
        if (this.ticksExisted % 4 == 0 && EmotionHelper.checkModelState(1, this.getStateEmotion(0)) && !this.isSitting() && !this.getStateFlag(2)) {
            float[] partPos = CalcHelper.rotateXZByAxis(-0.56f, 0.0f, this.renderYawOffset % 360.0f * ((float) Math.PI / 180), 1.0f);
            ParticleHelper.spawnAttackParticleAt(this.posX + partPos[1], this.posY + 1.5, this.posZ + partPos[0], 1.0, 0.0, 0.0, (byte) 43);
        }
        if (this.ticksExisted % 8 == 0) {
            int atkPhase = this.getStateEmotion(EMOTION_ATTACK_PHASE);
            if (atkPhase == 1 || atkPhase == 3) {
                ParticleHelper.spawnAttackParticleAtEntity(this, 0.12, 1.0, 0.0, (byte) 1);
            }
        }
    }

    private void onLivingUpdateServer() {
        if (this.ticksExisted % 128 == 0) {
            addMoraleSpecialEvent(this);
            if (this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 0) {
                applyBuffToNearbyAllies();
            }
        }
    }

    private void applyBuffToNearbyAllies() {
        this.world.getEntitiesWithinAABB(BasicEntityShip.class, this.getEntityBoundingBox().expand(16.0, 16.0, 16.0))
                .stream()
                .filter(ship -> TeamHelper.checkSameOwner(this, ship))
                .forEach(ship -> ship.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 50 + this.getStateMinor(0), this.getStateMinor(0) / 70, false, false)));
    }

    protected static void addMoraleSpecialEvent(BasicEntityShip host) {
        if (host.isMorph()) return;
        List<EntityLivingBase> nearbyEntities = host.world.getEntitiesWithinAABB(EntityLivingBase.class, host.getEntityBoundingBox().expand(16.0, 12.0, 16.0));
        List<EntityLivingBase> targets = nearbyEntities.stream()
                .filter(ent -> ent instanceof EntityNorthernHime || (ent instanceof IShipEmotion && ((IShipEmotion) ent).getStateMinor(STATE_MINOR_FACTION_ID) != -1))
                .collect(Collectors.toList());
        if (!targets.isEmpty()) {
            if (host.getMorale() < 7650) {
                host.addMorale(150 * targets.size());
            }
            if (!host.isSitting() && !host.isRiding() && !host.getStateFlag(2) && EntityHelper.checkShipOutOfCombat(host) && host.getRand().nextFloat() > 0.5f) {
                host.getShipNavigate().tryMoveToEntityLiving(targets.get(host.getRand().nextInt(targets.size())), 1.0);
                int particleId = LOVE_PARTICLES[host.getRand().nextInt(LOVE_PARTICLES.length)];
                host.applyParticleEmotion(particleId);
            }
        }
    }

    @Override
    public void calcShipAttributesAddEffect() {
        super.calcShipAttributesAddEffect();
        this.AttackEffectMap.put(19, new int[]{this.getLevel() / 75, 60 + this.getLevel(), this.getLevel()});
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        if (!prepareAndConsumeForAttack()) return false;
        int atkPhase = this.getStateEmotion(EMOTION_ATTACK_PHASE) + 1;
        playAttackSound(atkPhase);
        boolean isTargetHurt = executeAttackSequence(target, atkPhase);
        handlePostAttackEffects(target);
        return isTargetHurt;
    }

    private boolean prepareAndConsumeForAttack() {
        this.addShipExp(ConfigHandler.expGain[2]);
        this.decrGrudgeNum(ConfigHandler.consumeGrudgeAction[1]);
        this.decrMorale(2);
        this.setCombatTick(this.ticksExisted);
        return this.decrAmmoNum(1, this.getAmmoConsumption());
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
            this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
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
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 22, this.posX, this.posY, this.posZ, 2.0, 1.0, 0.0, true), point);
        }
    }

    private boolean performFinalAttack(Entity target, NetworkRegistry.TargetPoint point) {
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 21, this.posX, this.posY, this.posZ, target.posX, target.posY, target.posZ, true), point);
        float baseDamage = this.getAttackBaseDamage(2, target);
        float primaryDamage = calculateDamage(target, baseDamage, (float) distVec.d);
        teleportToTargetSide(target, distVec);
        performAreaOfEffectAttack(target, primaryDamage * 0.5f, (float) distVec.d);
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

    private void performAreaOfEffectAttack(Entity primaryTarget, float aoeDamage, float dist) {
        AxisAlignedBB impactBox = this.getEntityBoundingBox().expand(3.5, 3.5, 3.5);
        List<Entity> hitList = this.world.getEntitiesWithinAABB(Entity.class, impactBox);
        for (Entity hitEntity : hitList) {
            if (hitEntity == this || hitEntity == primaryTarget || TargetHelper.isEntityInvulnerable(hitEntity) || !hitEntity.canBeCollidedWith()) {
                continue;
            }
            float damage = calculateDamage(hitEntity, aoeDamage, dist);
            hitEntity.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
        }
    }

    private void handlePostAttackEffects(Entity target) {
        this.applyEmotesReaction(3);
        if (ConfigHandler.canFlare) {
            this.flareTarget(target);
        }
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            if (EmotionHelper.checkModelState(1, this.getStateEmotion(0))) return this.height * 0.42f;
            if (this.getStateEmotion(1) == 4) return 0.0;
            return this.height * 0.35f;
        }
        return this.height * 0.75f;
    }

    public void applyParticleAtAttacker(int type) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        if (type == 1) {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 5, 0.9, 1.0, 1.1), point);
        } else {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
        }
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
                return this.shipAttrs.getAttackDamage() * 3.0f;
        }
    }
}