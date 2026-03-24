package com.lulan.shincolle.entity.hime;

import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipCV;
import com.lulan.shincolle.entity.other.EntityFloatingFort;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.C2SInputPackets;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class EntityNorthernHime extends BasicEntityShipCV {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;

    private int goRidingTicks;
    private boolean goRiding;
    private Entity goRideEntity;

    public EntityNorthernHime(World world) {
        super(world);
        this.setSize(0.5f, 0.9f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, 10);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 31);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 2);
        this.setStateMinor(STATE_MINOR_RARITY, 4);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[8]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[8]);
        this.ModelPos = new float[]{-6.0f, 8.0f, 0.0f, 50.0f};
        this.launchHeight = this.height;
        this.setFoodSaturationMax(16);
        this.postInit();
    }

    @Override
    public EntityAgeable createChild(EntityAgeable ageable) {
        return null;
    }

    @Override
    public int getEquipType() {
        return 2;
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(11, new EntityAIShipCarrierAttack(this));
        this.tasks.addTask(12, new EntityAIShipRangeAttack(this));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.world.isRemote) {
            updateClientLogic();
        } else {
            updateServerLogic();
        }
    }

    private void updateClientLogic() {
        if (this.ticksExisted % 8 == 0 && EmotionHelper.checkModelState(2, this.getStateEmotion(0)) && !this.getStateFlag(2)) {
            double y = this.isSitting() || this.isRiding() ? this.posY + 0.9 : this.posY + 1.1;
            ParticleHelper.spawnAttackParticleAt(this.posX, y, this.posZ, 0.0, 0.0, 0.0, (byte) 28);
        }
    }

    private void updateServerLogic() {
        if (this.ticksExisted % 64 == 0) {
            handlePeriodicEffects();
        }
        if (this.goRiding && !this.isMorph) {
            updateGoRidingState();
        }
        if (this.getRidingEntity() != null && this.getRidingEntity().isSneaking()) {
            this.dismountRidingEntity();
            this.sendSyncPacketRiders();
        }
    }

    private void handlePeriodicEffects() {
        if (this.isRiding() && !this.isMorph && this.getMorale() < 7650) {
            this.addMorale(150);
        }
        if (this.getStateMinor(6) > 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(this.getMaxHealth() * 0.03f + 1.0f);
        }
        if (this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 25) {
            healNearbyAllies();
        }
        if (this.ticksExisted % 256 == 0 && this.rand.nextInt(3) == 0 && !this.isMorph) {
            checkRiding();
        }
    }

    private void updateGoRidingState() {
        this.goRidingTicks++;
        if (this.goRidingTicks > 200 || this.goRideEntity == null || !this.goRideEntity.isEntityAlive()) {
            cancelGoRiding();
            return;
        }
        float distRiding = this.getDistance(this.goRideEntity);
        if (distRiding <= 2.0f && !this.goRideEntity.isRiding() && this.getPassengers().isEmpty() && this.goRideEntity.getPassengers().isEmpty()) {
            this.startRiding(this.goRideEntity, true);
            this.getShipNavigate().clearPathEntity();
            cancelGoRiding();
        } else if (this.ticksExisted % 32 == 0 && distRiding > 2.0f) {
            this.getShipNavigate().tryMoveToEntityLiving(this.goRideEntity, 1.0);
        }
    }

    private void healNearbyAllies() {
        AtomicInteger remainingHeals = new AtomicInteger(this.getLevel() / 25 + 1);
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);

        this.world.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().expand(8.0, 8.0, 8.0))
                .stream()
                .filter(target -> remainingHeals.get() > 0 && canBeHealed(target))
                .forEach(target -> {
                    float healAmount = calculateHealAmount(target);
                    target.heal(healAmount);
                    CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, target, 1.0, 0.0, 0.0, 4, false), point);
                    this.decrGrudgeNum(25);
                    remainingHeals.decrementAndGet();
                });
    }

    private boolean canBeHealed(EntityLivingBase target) {
        if (target == this || !TeamHelper.checkIsAlly(this, target)) return false;
        if ((target.getHealth() / target.getMaxHealth()) >= 0.98f) return false;
        return target instanceof EntityPlayer || target instanceof BasicEntityShip;
    }

    private float calculateHealAmount(EntityLivingBase target) {
        if (target instanceof EntityPlayer) {
            return 1.0f + target.getMaxHealth() * 0.02f + this.getLevel() * 0.02f;
        }
        return 1.0f + target.getMaxHealth() * 0.02f + this.getLevel() * 0.1f;
    }

    private void cancelGoRiding() {
        this.goRidingTicks = 0;
        this.goRideEntity = null;
        this.goRiding = false;
    }

    private void checkRiding() {
        cancelGoRiding();
        if (this.isSitting() || this.getLeashed() || this.getStateFlag(2)) {
            return;
        }
        if (this.isRiding() && this.rand.nextInt(2) == 0) {
            this.dismountRidingEntity();
        } else {
            List<EntityLivingBase> hitList = this.world.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().expand(6.0, 4.0, 6.0));
            hitList.removeIf(target -> !isRideable(target));
            if (!hitList.isEmpty()) {
                this.goRideEntity = hitList.get(this.rand.nextInt(hitList.size()));
                this.goRidingTicks = 0;
                this.goRiding = true;
            }
        }
    }

    private boolean isRideable(Entity target) {
        if (!(target instanceof EntityPlayer || target instanceof BasicEntityShip)) return false;
        if (this.equals(target) || target.isRiding() || !target.getPassengers().isEmpty()) return false;
        return TeamHelper.checkSameOwner(this, target);
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (hand == EnumHand.OFF_HAND) return false;
        if (!this.isEntityAlive()) return false;
        ItemStack stack = player.getHeldItem(hand);
        if (this.world.isRemote && this.isSitting() && this.getStateEmotion(1) == 4 && (stack.getItem() != ModItems.PointerItem)) {
            CommonProxy.channelI.sendToServer(new C2SInputPackets((byte) 7, this.getEntityId(), this.world.provider.getDimension()));
            return false;
        }
        return super.processInteract(player, hand);
    }

    @Override
    public boolean attackEntityFrom(DamageSource attacker, float atk) {
        if (this.getRidingEntity() instanceof BasicEntityShip || this.getRidingEntity() instanceof EntityPlayer) {
            this.dismountRidingEntity();
        }
        return super.attackEntityFrom(attacker, atk);
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        this.addShipExp(ConfigHandler.expGain[2]);
        this.decrGrudgeNum(ConfigHandler.consumeGrudgeAction[1]);
        this.decrMorale(2);
        this.setCombatTick(this.ticksExisted);
        this.applySoundAtAttacker(2);
        this.applyParticleAtAttacker(2, CalcHelper.getDistanceFromA2B(this, target));
        if (!this.decrAmmoNum(1, this.getAmmoConsumption())) {
            return false;
        }
        spawnFloatingFortress(target);
        this.applyEmotesReaction(3);
        if (ConfigHandler.canFlare) {
            this.flareTarget(target);
        }
        return true;
    }

    private void spawnFloatingFortress(Entity target) {
        float launchPos = (float) this.posY + this.height;
        if (this.getShipDepth() > 0.0) {
            launchPos += 0.2f;
        }
        EntityFloatingFort ffort = new EntityFloatingFort(this.world);
        ffort.initAttrs(this, target, 0, launchPos);
        this.world.spawnEntity(ffort);
        this.applySoundAtTarget(2);
        this.applyParticleAtTarget(2, target);
    }

    public double getYOffset() {
        return 0.25;
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            return this.getStateEmotion(1) == 4 ? 0.0 : this.height * 0.08f;
        }
        return this.height * 0.48f;
    }

    public void applyParticleAtAttacker(int type, Dist4d distVec) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        if (type == 1) {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 31, this.posX, this.posY, this.posZ, distVec.x, distVec.y, distVec.z, true), point);
        } else {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
        }
    }

    public void applyParticleAtTarget(int type, Entity target) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        switch (type) {
            case 1:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 30, false), point);
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

    public void applySoundAtAttacker(int type) {
        switch (type) {
            case 1:
            case 2:
                this.playSound(SoundEvents.ENTITY_ARROW_SHOOT, ConfigHandler.volumeFire * 1.3f, 0.4f / (this.rand.nextFloat() * 0.4f + 0.8f));
                playCustomSoundWithChance();
                break;
            case 3:
            case 4:
                this.playSound(ModSounds.SHIP_AIRCRAFT, ConfigHandler.volumeFire * 0.5f, this.getSoundPitch() * 0.85f);
                playCustomSoundWithChance();
                break;
            default:
                if (this.getRNG().nextInt(2) == 0) {
                    this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
                }
                break;
        }
    }

    private void playCustomSoundWithChance() {
        if (this.getRNG().nextInt(10) > 7) {
            this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
        }
    }

    @Override
    public float getAttackBaseDamage(int type, Entity target) {
        switch (type) {
            case 1:
                return CombatHelper.modDamageByAdditionAttrs(this, target, this.shipAttrs.getAttackDamage(), 0);
            case 2:
                return this.shipAttrs.getAttackDamageHeavy() * 0.75f;
            case 3:
                return this.shipAttrs.getAttackDamageAir();
            case 4:
                return this.shipAttrs.getAttackDamageAirHeavy();
            default:
                return this.shipAttrs.getAttackDamage();
        }
    }
}