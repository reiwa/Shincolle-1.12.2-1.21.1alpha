package com.lulan.shincolle.entity.destroyer;

import com.lulan.shincolle.ai.EntityAIShipPickItem;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.IShipRiderType;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityDestroyerIkazuchi extends BasicEntityShipSmall implements IShipRiderType {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;

    private int riderType;
    public boolean isRaiden;
    private int ridingState;

    public EntityDestroyerIkazuchi(World world) {
        super(world);
        this.setSize(0.5f, 1.5f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, -1);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 53);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 5);
        this.setStateMinor(STATE_MINOR_RARITY, 2);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[0]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[0]);
        this.ModelPos = new float[]{0.0f, 25.0f, 0.0f, 50.0f};
        this.StateFlag[15] = false;
        this.StateFlag[16] = false;
        this.StateFlag[24] = true;
        this.riderType = 0;
        this.isRaiden = false;
        this.ridingState = 0;
        this.postInit();
    }

    @Override
    public EntityAgeable createChild(EntityAgeable ageable) {
        return null;
    }

    @Override
    public float getEyeHeight() {
        return 1.4f;
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
        if (this.world.isRemote) {
            updateClientLogic();
        } else {
            updateServerLogic();
        }
        updateRiderRotation();
    }

    private void updateClientLogic() {
        if (this.ticksExisted % 4 == 0 && EmotionHelper.checkModelState(0, this.getStateEmotion(0)) && !this.isSitting() && !this.getStateFlag(2)) {
            float[] partPos = CalcHelper.rotateXZByAxis(-0.42f, 0.0f, this.renderYawOffset % 360.0f * ((float) Math.PI / 180), 1.0f);
            ParticleHelper.spawnAttackParticleAt(this.posX + partPos[1], this.posY + 1.4, this.posZ + partPos[0], 0.0, 0.0, 0.0, (byte) 20);
        }
        if (this.ticksExisted % 16 == 0) {
            updateState();
        }
    }

    private void updateServerLogic() {
        if (this.ticksExisted % 32 == 0 && !this.isMorph) {
            updateState();
            if (this.riderType == 0 && this.isRaiden && this.getMorale() < 7650) {
                this.addMorale(100);
            }
            if (this.ticksExisted % 128 == 0) {
                applyBuffToPlayer();
                tryRaidenGattai();
            }
        }
    }

    private void updateState() {
        checkRiderType();
        checkIsRaiden();
        checkRidingState();
    }

    private void applyBuffToPlayer() {
        if (this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 0) {
            EntityPlayer player = EntityHelper.getEntityPlayerByUID(this.getPlayerUID());
            if (player != null && this.getDistanceSq(player) < 256.0) {
                player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 80 + this.getStateMinor(0), this.getStateMinor(0) / 50, false, false));
            }
        }
    }

    private void updateRiderRotation() {
        Entity ridingEntity = this.getRidingEntity();
        if (ridingEntity instanceof EntityDestroyerAkatsuki) {
            ((EntityDestroyerAkatsuki) ridingEntity).syncRotateToRider();
        } else if (ridingEntity instanceof EntityDestroyerInazuma) {
            this.renderYawOffset = ((EntityDestroyerInazuma) ridingEntity).renderYawOffset;
            this.prevRenderYawOffset = ((EntityDestroyerInazuma) ridingEntity).prevRenderYawOffset;
            this.rotationYaw = ridingEntity.rotationYaw;
            this.prevRotationYaw = ridingEntity.prevRotationYaw;
        }
    }

    @Override
    protected void updateFuelState(boolean nofuel) {
        if (nofuel) {
            if (this.getRidingEntity() instanceof EntityDestroyerAkatsuki) {
                ((EntityDestroyerAkatsuki) this.getRidingEntity()).dismountAllRider();
            }
            if (this.isRaiden) {
                this.dismountRaiden();
            }
        }
        super.updateFuelState(nofuel);
    }

    @Override
    public boolean attackEntityFrom(DamageSource attacker, float atk) {
        boolean isDamaged = super.attackEntityFrom(attacker, atk);
        if (!this.world.isRemote && isDamaged) {
            if (this.getRidingEntity() instanceof EntityDestroyerAkatsuki) {
                ((EntityDestroyerAkatsuki) this.getRidingEntity()).dismountAllRider();
            }
            if (this.isRaiden) {
                this.dismountRaiden();
            }
        }
        return isDamaged;
    }

    public void tryRaidenGattai() {
        if (!canAttemptGattai()) {
            return;
        }
        this.world.getEntitiesWithinAABB(EntityDestroyerInazuma.class, this.getEntityBoundingBox().expand(4.0, 4.0, 4.0))
                .stream()
                .filter(this::canGattaiWith)
                .findFirst()
                .ifPresent(this::doGattai);
    }

    private boolean canAttemptGattai() {
        if (this.getStateMinor(43) > 0) {
            dismountRaiden();
            dismountRidingEntity();
            return false;
        }
        return !this.isSitting() && !this.isRiding() && !this.getStateFlag(2) &&
                this.riderType <= 0 && !this.isRaiden &&
                this.getHealth() > this.getMaxHealth() * 0.5f;
    }

    private boolean canGattaiWith(EntityDestroyerInazuma partner) {
        return partner != null && TeamHelper.checkSameOwner(this, partner) && partner.isEntityAlive() &&
                partner.getRiderType() == 0 && !partner.isRaiden && !partner.getStateFlag(2) &&
                partner.getStateMinor(43) == 0;
    }

    private void doGattai(EntityDestroyerInazuma partner) {
        this.startRiding(partner);
        this.isRaiden = true;
        partner.isRaiden = true;
    }

    public void checkRiderType() {
        this.riderType = 0;
        if (this.getRidingEntity() instanceof EntityDestroyerAkatsuki) {
            this.riderType = ((EntityDestroyerAkatsuki) this.getRidingEntity()).getRiderType();
        }
    }

    public void checkIsRaiden() {
        this.isRaiden = this.getRidingEntity() instanceof EntityDestroyerInazuma;
    }

    public void checkRidingState() {
        this.ridingState = this.riderType == 7 || this.isRaiden ? 2 : 0;
    }

    public void dismountRaiden() {
        if (this.getRidingEntity() instanceof EntityDestroyerInazuma) {
            this.isRaiden = false;
            ((EntityDestroyerInazuma) this.getRidingEntity()).isRaiden = false;
            this.dismountRidingEntity();
        }
    }

    @Override
    public int getRiderType() {
        return this.riderType;
    }

    @Override
    public void setRiderType(int type) {
        this.riderType = type;
    }

    @Override
    public int getRidingState() {
        return this.ridingState;
    }

    @Override
    public void setRidingState(int state) {
        this.ridingState = state;
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            return this.height * 0.23f;
        }
        return this.height * 0.64f;
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
                return this.shipAttrs.getAttackDamage();
        }
    }
}