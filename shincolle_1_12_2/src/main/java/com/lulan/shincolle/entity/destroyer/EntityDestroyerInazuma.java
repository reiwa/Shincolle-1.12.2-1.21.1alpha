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

import java.util.List;
import java.util.Objects;

public class EntityDestroyerInazuma extends BasicEntityShipSmall implements IShipRiderType {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;

    private int riderType;
    public boolean isRaiden;
    private int ridingState;

    public EntityDestroyerInazuma(World world) {
        super(world);
        this.setSize(0.5f, 1.5f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, -1);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 54);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 5);
        this.setStateMinor(STATE_MINOR_RARITY, 1);
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
        if (this.ticksExisted % 4 == 0 && EmotionHelper.checkModelState(0, this.getStateEmotion(0)) && !this.isSitting() && !this.getStateFlag(2) && this.riderType < 4) {
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
                player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 80 + this.getStateMinor(0), this.getStateMinor(0) / 45, false, false));
            }
        }
    }

    private void updateRiderRotation() {
        if (this.getRidingEntity() instanceof EntityDestroyerAkatsuki) {
            ((EntityDestroyerAkatsuki) this.getRidingEntity()).syncRotateToRider();
        } else if (this.isRaiden) {
            this.getPassengers().stream()
                    .filter(EntityDestroyerIkazuchi.class::isInstance)
                    .forEach(rider -> {
                        ((EntityDestroyerIkazuchi) rider).renderYawOffset = this.renderYawOffset;
                        ((EntityDestroyerIkazuchi) rider).prevRenderYawOffset = this.prevRenderYawOffset;
                        rider.rotationYaw = this.rotationYaw;
                        rider.prevRotationYaw = this.prevRotationYaw;
                    });
        }
    }

    @Override
    protected void updateFuelState(boolean nofuel) {
        if (nofuel) {
            if (this.getRidingEntity() instanceof EntityDestroyerAkatsuki) {
                ((EntityDestroyerAkatsuki) this.getRidingEntity()).dismountAllRider();
                this.dismountRidingEntity();
            }
            if (this.isRaiden) {
                this.dismountRaiden();
            }
        }
        super.updateFuelState(nofuel);
    }

    public void updatePassenger(Entity rider) {
        if (!this.isPassenger(rider)) return;

        if (rider instanceof EntityDestroyerIkazuchi) {
            updateIkazuchiPosition((EntityDestroyerIkazuchi) rider);
        } else {
            updateDefaultPassengerPosition(rider);
        }
    }

    private void updateIkazuchiPosition(EntityDestroyerIkazuchi rider) {
        double yOffsetBase = this.posY + rider.getYOffset();
        double yOffsetEmotion = this.getStateEmotion(1) == 4 ? -0.6 : -0.45;
        double finalY = yOffsetBase + (this.isSitting() ? 0.26 : 0.68) + yOffsetEmotion;
        float[] partPos = CalcHelper.rotateXZByAxis(-0.2f, 0.0f, this.renderYawOffset % 360.0f * ((float) Math.PI / 180), 1.0f);
        rider.setPosition(this.posX + partPos[1], finalY, this.posZ + partPos[0]);
    }

    private void updateDefaultPassengerPosition(Entity rider) {
        double finalY = this.posY + rider.getYOffset() + this.getMountedYOffset();
        rider.setPosition(this.posX, finalY, this.posZ);
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            return this.getStateEmotion(1) == 4 ? this.height * 0.23f : this.height * 0.44f;
        }
        return this.height * 0.64f;
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
        if (this.getStateMinor(43) > 0) {
            this.dismountRaiden();
            this.dismountRidingEntity();
            return;
        }
        boolean isActionBlocked = this.isSitting() || this.getStateFlag(2) || this.riderType > 0 || this.isRaiden || this.isRiding();
        if (isActionBlocked || this.getHealth() <= this.getMaxHealth() * 0.5f) {
            return;
        }

        this.world.getEntitiesWithinAABB(EntityDestroyerIkazuchi.class, this.getEntityBoundingBox().expand(4.0, 4.0, 4.0))
                .stream()
                .filter(this::canGattaiWith)
                .findFirst()
                .ifPresent(this::doGattai);
    }

    private boolean canGattaiWith(EntityDestroyerIkazuchi ikazuchi) {
        if (ikazuchi == null || !ikazuchi.isEntityAlive() || !TeamHelper.checkSameOwner(this, ikazuchi)) return false;
        return ikazuchi.getRiderType() == 0 && !ikazuchi.getStateFlag(2) && !ikazuchi.isRaiden && ikazuchi.getStateMinor(43) == 0;
    }

    private void doGattai(EntityDestroyerIkazuchi ikazuchi) {
        ikazuchi.startRiding(this);
        this.isRaiden = true;
        ikazuchi.isRaiden = true;
    }

    public void checkRiderType() {
        this.riderType = 0;
        if (this.getRidingEntity() instanceof EntityDestroyerAkatsuki) {
            this.riderType = ((EntityDestroyerAkatsuki) this.getRidingEntity()).getRiderType();
        }
    }

    public void checkRidingState() {
        if (this.riderType == 7) {
            this.ridingState = 3;
        } else if (this.isRaiden) {
            this.ridingState = 2;
        } else if (this.riderType == 3) {
            this.ridingState = 1;
        } else {
            this.ridingState = 0;
        }
    }

    public void checkIsRaiden() {
        this.isRaiden = this.getPassengers().stream().anyMatch(EntityDestroyerIkazuchi.class::isInstance);
    }

    public void dismountRaiden() {
        this.getPassengers().stream()
                .filter(EntityDestroyerIkazuchi.class::isInstance)
                .forEach(rider -> {
                    ((EntityDestroyerIkazuchi) rider).isRaiden = false;
                    rider.dismountRidingEntity();
                });
        this.isRaiden = false;
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
}