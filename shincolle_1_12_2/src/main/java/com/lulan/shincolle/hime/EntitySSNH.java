package com.lulan.shincolle.entity.hime;

import com.lulan.shincolle.ai.EntityAIShipPickItem;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.IShipInvisible;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.network.C2SInputPackets;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

import java.util.List;
import java.util.stream.Collectors;

public class EntitySSNH extends BasicEntityShipSmall implements IShipInvisible {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;
    private static final int GO_RIDING_TIMEOUT_TICKS = 200;

    private int goRidingTicks;
    private boolean goRiding;
    private Entity goRideEntity;

    public EntitySSNH(World world) {
        super(world);
        this.setSize(0.5f, 0.9f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, 10);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 72);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 6);
        this.setStateMinor(STATE_MINOR_RARITY, 3);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[9]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[9]);
        this.ModelPos = new float[]{-6.0f, 8.0f, 0.0f, 50.0f};
        this.goRidingTicks = 0;
        this.goRideEntity = null;
        this.goRiding = false;
        this.StateFlag[15] = false;
        this.StateFlag[16] = false;
        this.StateFlag[24] = true;
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
        this.tasks.addTask(20, new EntityAIShipPickItem(this, 4.0f));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote) {
            updateServerLogic();
        }
    }

    private void updateServerLogic() {
        if (this.ticksExisted % 64 == 0) {
            handlePeriodicChecks();
        }
        if (this.goRiding && !this.isMorph) {
            updateGoRidingState();
        }
        if (this.getRidingEntity() != null && this.getRidingEntity().isSneaking()) {
            this.dismountRidingEntity();
            this.sendSyncPacketRiders();
        }
    }

    private void handlePeriodicChecks() {
        if (this.isRiding() && !this.isMorph && this.getMorale() < 7650) {
            this.addMorale(150);
        }
        if (this.ticksExisted % 128 == 0) {
            applyInvisibilityBuff();
            if (this.ticksExisted % 256 == 0 && this.rand.nextInt(3) == 0 && !this.isMorph) {
                tryFindingRideTarget();
            }
        }
    }

    private void applyInvisibilityBuff() {
        if (this.getStateFlag(9) && this.getStateMinor(6) > 0) {
            if (this.getStateFlag(1)) {
                EntityPlayerMP player = (EntityPlayerMP) EntityHelper.getEntityPlayerByUID(this.getPlayerUID());
                if (player != null && this.getDistanceSq(player) < 256.0) {
                    player.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 80 + this.getLevel(), 0, false, false));
                }
            }
            this.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 80 + this.getLevel(), 0, false, false));
        }
    }

    private void updateGoRidingState() {
        this.goRidingTicks++;
        if (this.goRidingTicks > GO_RIDING_TIMEOUT_TICKS || this.goRideEntity == null || !this.goRideEntity.isEntityAlive()) {
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

    public double getYOffset() {
        return 0.25;
    }

    @Override
    public double getMountedYOffset() {
        if (!this.isSitting()) {
            return this.height * 0.48f;
        }
        return this.getStateEmotion(1) == 4 ? 0.0 : this.height * 0.08f;
    }

    @Override
    public float getInvisibleLevel() {
        return 0.35f;
    }

    @Override
    public double getShipFloatingDepth() {
        return 1.0;
    }

    @Override
    public boolean attackEntityWithAmmo(Entity target) {
        if (!this.decrAmmoNum(0, this.getAmmoConsumption())) {
            return false;
        }
        this.addShipExp(ConfigHandler.expGain[1]);
        this.decrGrudgeNum(ConfigHandler.consumeGrudgeAction[0]);
        this.decrMorale(1);
        this.setCombatTick(this.ticksExisted);

        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        this.applySoundAtAttacker(2);
        this.applyParticleAtAttacker(2, distVec);

        float tarX = (float) target.posX;
        float tarY = (float) target.posY;
        float tarZ = (float) target.posZ;
        if (this.rand.nextFloat() <= CombatHelper.calcMissRate(this, (float) distVec.d)) {
            tarX += (this.rand.nextFloat() - 0.5f) * 10.0f;
            tarY += this.rand.nextFloat() * 5.0f;
            tarZ += (this.rand.nextFloat() - 0.5f) * 10.0f;
            ParticleHelper.spawnAttackTextParticle(this, 0);
        }

        float atk = this.getAttackBaseDamage(1, target);
        summonPairedMissiles(1, atk, tarX, tarY, tarZ, target.height);

        this.applySoundAtTarget(2);
        this.applyParticleAtTarget(2, target);
        this.applyEmotesReaction(3);
        if (ConfigHandler.canFlare) {
            this.flareTarget(target);
        }
        this.applyAttackPostMotion(1, this, true, atk);
        return true;
    }

    @Override
    public float getAttackBaseDamage(int type, Entity target) {
        switch (type) {
            case 1: return this.shipAttrs.getAttackDamage();
            case 2: return this.shipAttrs.getAttackDamageHeavy();
            case 3: return this.shipAttrs.getAttackDamageAir();
            case 4: return this.shipAttrs.getAttackDamageAirHeavy();
            default: return this.shipAttrs.getAttackDamage();
        }
    }

    public void summonPairedMissiles(int attackType, float atk, float tarX, float tarY, float tarZ, float targetHeight) {
        float launchY = (float) this.posY + this.height * 0.6f + (this.isMorph ? 0.5f : 0.0f);
        float renderYawRad = this.renderYawOffset * ((float)Math.PI / 180f);
        float[] mPos1 = CalcHelper.rotateXZByAxis(0.0f, 1.0f, renderYawRad, 1.0f);
        float[] mPos2 = CalcHelper.rotateXZByAxis(0.0f, -1.0f, renderYawRad, 1.0f);

        int moveType = CombatHelper.calcMissileMoveType(this, tarY, attackType);
        MissileData md = this.getMissileData(attackType);

        float[] data1 = {atk, 0.15f, launchY, tarX, tarY + targetHeight * 0.1f, tarZ, 160.0f, 0.25f, md.vel0, md.accY1, md.accY2, (float)this.posX + mPos1[1], launchY, (float)this.posZ + mPos1[0], 4.0f};
        float[] data2 = {atk, 0.15f, launchY, tarX, tarY + targetHeight * 0.1f, tarZ, 160.0f, 0.25f, md.vel0, md.accY1, md.accY2, (float)this.posX + mPos2[1], launchY, (float)this.posZ + mPos2[0], 4.0f};

        this.world.spawnEntity(new EntityAbyssMissile(this.world, this, md.type, moveType, data1));
        this.world.spawnEntity(new EntityAbyssMissile(this.world, this, md.type, moveType, data2));
    }

    @Override
    public void calcShipAttributesAddEquip() {
        super.calcShipAttributesAddEquip();
        MissileData md = this.getMissileData(1);
        md.vel0 += 0.3f;
        md.accY1 += 0.06f;
        md.accY2 += 0.06f;
    }

    private void cancelGoRiding() {
        this.goRidingTicks = 0;
        this.goRideEntity = null;
        this.goRiding = false;
    }

    private void tryFindingRideTarget() {
        cancelGoRiding();
        if (this.isSitting() || this.getLeashed() || this.getStateFlag(2)) {
            return;
        }

        if (this.isRiding() && this.rand.nextInt(2) == 0) {
            this.dismountRidingEntity();
        } else if (!this.isRiding()) {
            List<EntityLivingBase> rideableEntities = this.world.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().expand(6.0, 4.0, 6.0));

            List<EntityLivingBase> targets = rideableEntities.stream()
                    .filter(this::isValidRideTarget)
                    .collect(Collectors.toList());

            if (!targets.isEmpty()) {
                this.goRideEntity = targets.get(this.rand.nextInt(targets.size()));
                this.goRidingTicks = 0;
                this.goRiding = true;
            }
        }
    }

    private boolean isValidRideTarget(EntityLivingBase target) {
        if (this.equals(target) || target.isRiding() || !target.getPassengers().isEmpty()) {
            return false;
        }
        if (!(target instanceof BasicEntityShip) && !(target instanceof EntityPlayer)) {
            return false;
        }
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
        Entity ridingEntity = this.getRidingEntity();
        if (ridingEntity instanceof BasicEntityShip || ridingEntity instanceof EntityPlayer) {
            this.dismountRidingEntity();
        }
        return super.attackEntityFrom(attacker, atk);
    }
}