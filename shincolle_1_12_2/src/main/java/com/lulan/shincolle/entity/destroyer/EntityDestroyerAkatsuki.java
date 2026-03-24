package com.lulan.shincolle.entity.destroyer;

import com.lulan.shincolle.ai.EntityAIShipPickItem;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.IShipEmotion;
import com.lulan.shincolle.entity.IShipRiderType;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;

public class EntityDestroyerAkatsuki extends BasicEntityShipSmall implements IShipRiderType {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;
    private static final int RIDER_TYPE_NONE = 0;
    private static final int RIDER_TYPE_HIBIKI = 1;
    private static final int RIDER_TYPE_INAZUMA = 2;
    private static final int RIDER_TYPE_IKAZUCHI = 4;
    private static final int RIDER_TYPE_ALL = 7;

    private int riderType;
    private int ridingState;

    public EntityDestroyerAkatsuki(World world) {
        super(world);
        this.setSize(0.5f, 1.5f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, -1);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 51);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 5);
        this.setStateMinor(STATE_MINOR_RARITY, 5);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[0]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[0]);
        this.ModelPos = new float[]{0.0f, 25.0f, 0.0f, 50.0f};
        this.StateFlag[15] = false;
        this.StateFlag[16] = false;
        this.StateFlag[24] = true;
        this.riderType = RIDER_TYPE_NONE;
        this.ridingState = 0;
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
        if (this.world.isRemote) {
            updateClientEffects();
        } else {
            updateServerLogic();
        }
        if (!this.isMorph) {
            this.syncRotateToRider();
        }
    }

    private void updateClientEffects() {
        if (this.ticksExisted % 4 == 0) {
            if (EmotionHelper.checkModelState(0, this.getStateEmotion(0)) && !this.isSitting() && !this.getStateFlag(2) && this.riderType < 1) {
                float addz = this.isRiding() ? -0.2f : 0.0f;
                float[] partPos = CalcHelper.rotateXZByAxis(-0.42f + addz, 0.0f, this.renderYawOffset % 360.0f * ((float) Math.PI / 180), 1.0f);
                ParticleHelper.spawnAttackParticleAt(this.posX + partPos[1], this.posY + 1.4, this.posZ + partPos[0], 0.0, 0.0, 0.0, (byte) 20);
            }
            if (this.ticksExisted % 16 == 0) {
                this.checkRiderType();
            }
        }
    }

    private void updateServerLogic() {
        if (this.ticksExisted % 32 == 0 && !this.isMorph) {
            this.checkRiderType();
            if (this.riderType == 2 || this.riderType == 4 || this.riderType == 5 || this.riderType == 6) {
                this.dismountAllRider();
            }
            if (this.riderType > 0) {
                this.addMoraleToRider();
                if (this.getMorale() < 7650) {
                    this.addMorale(100);
                }
            }
            if (this.ticksExisted % 128 == 0) {
                applyPlayerBuff();
                tryGattai();
                if (this.ridingState > 0) {
                    this.sendSyncPacketRiders();
                }
            }
        }
    }

    private void applyPlayerBuff() {
        if (this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 0) {
            EntityPlayer player = EntityHelper.getEntityPlayerByUID(this.getPlayerUID());
            if (player != null && this.getDistanceSq(player) < 256.0) {
                player.addPotionEffect(new PotionEffect(MobEffects.HASTE, 80 + this.getStateMinor(0), this.getStateMinor(0) / 30, false, false));
            }
        }
    }

    @Override
    protected void updateFuelState(boolean nofuel) {
        if (nofuel) {
            this.dismountAllRider();
        }
        super.updateFuelState(nofuel);
    }

    @Override
    public boolean attackEntityFrom(DamageSource attacker, float atk) {
        if (this.world.isRemote) return false;
        boolean isDamaged = super.attackEntityFrom(attacker, atk);
        if (isDamaged) {
            this.dismountAllRider();
        }
        return isDamaged;
    }

    @Override
    public void updatePassenger(Entity rider) {
        if (!this.isPassenger(rider)) return;
        if (rider instanceof EntityDestroyerHibiki) {
            double d = this.isSitting() ? 0.22 : 0.68;
            float[] partPos = CalcHelper.rotateXZByAxis(-0.2f, 0.0f, this.renderYawOffset % 360.0f * ((float) Math.PI / 180), 1.0f);
            rider.setPosition(this.posX + partPos[1], d - 0.45, this.posZ + partPos[0]);
        } else if (rider instanceof EntityDestroyerInazuma) {
            ((EntityDestroyerInazuma) rider).setStateEmotion(1, this.getStateEmotion(1), false);
            double d = this.isSitting() ? -0.08 : 0.68;
            float[] partPos = CalcHelper.rotateXZByAxis(-0.48f, 0.0f, this.renderYawOffset % 360.0f * ((float) Math.PI / 180), 1.0f);
            rider.setPosition(this.posX + partPos[1], d + 0.1, this.posZ + partPos[0]);
        } else if (rider instanceof EntityDestroyerIkazuchi) {
            this.getPassengers().stream()
                    .filter(EntityDestroyerInazuma.class::isInstance)
                    .findFirst()
                    .ifPresent(inazuma -> {
                        double d = ((EntityDestroyerInazuma) inazuma).getStateEmotion(1) == 4 ? 0.5 : 0.6;
                        float[] partPos = CalcHelper.rotateXZByAxis(-0.68f, 0.0f, this.renderYawOffset % 360.0f * ((float) Math.PI / 180), 1.0f);
                        rider.setPosition(this.posX + partPos[1], d, this.posZ + partPos[0]);
                    });
        } else {
            rider.setPosition(this.posX, this.getMountedYOffset(), this.posZ);
        }
    }

    private boolean isGattaiCandidate(BasicEntityShip ship) {
        if (ship == null || !ship.isEntityAlive() || !TeamHelper.checkSameOwner(this, ship)) return false;
        return !ship.isRiding() && !ship.isSitting() && !ship.getStateFlag(2) && ship.getStateMinor(43) == 0 && ship.getStateMinor(26) == 1;
    }

    public void tryGattai() {
        if (this.getStateMinor(43) > 0) {
            this.dismountAllRider();
            this.dismountRidingEntity();
            return;
        }
        if (this.isSitting() || this.getStateFlag(2) || this.riderType == RIDER_TYPE_ALL) return;

        List<BasicEntityShip> slist = this.world.getEntitiesWithinAABB(BasicEntityShip.class, this.getEntityBoundingBox().expand(6.0, 5.0, 6.0));
        if (slist.isEmpty()) return;

        EntityDestroyerHibiki getHibiki = null;
        EntityDestroyerInazuma getInazuma = null;
        EntityDestroyerIkazuchi getIkazuchi = null;

        for (BasicEntityShip s : slist) {
            if (s instanceof EntityDestroyerHibiki && isGattaiCandidate(s)) getHibiki = (EntityDestroyerHibiki) s;
            if (s instanceof EntityDestroyerInazuma && isGattaiCandidate(s)) getInazuma = (EntityDestroyerInazuma) s;
            if (s instanceof EntityDestroyerIkazuchi && isGattaiCandidate(s)) getIkazuchi = (EntityDestroyerIkazuchi) s;
        }

        if ((this.riderType & RIDER_TYPE_HIBIKI) == 0 && getHibiki != null) getHibiki.startRiding(this, true);
        if ((this.riderType & RIDER_TYPE_INAZUMA) == 0 && getInazuma != null) getInazuma.startRiding(this, true);
        if ((this.riderType & RIDER_TYPE_IKAZUCHI) == 0 && getIkazuchi != null) getIkazuchi.startRiding(this, true);
    }

    public void addMoraleToRider() {
        for (Entity rider : this.getPassengers()) {
            if (rider instanceof BasicEntityShip && ((BasicEntityShip) rider).getMorale() < 7650) {
                ((BasicEntityShip) rider).addMorale(100);
            }
            if (rider instanceof IShipRiderType) {
                ((IShipRiderType) rider).setRiderType(this.riderType);
            }
        }
    }

    public void checkRiderType() {
        this.riderType = RIDER_TYPE_NONE;
        boolean hasHibiki = false;
        for (Entity rider : this.getPassengers()) {
            if (rider instanceof EntityDestroyerHibiki) {
                this.riderType |= RIDER_TYPE_HIBIKI;
                hasHibiki = true;
            } else if (rider instanceof EntityDestroyerInazuma) {
                this.riderType |= RIDER_TYPE_INAZUMA;
            } else if (rider instanceof EntityDestroyerIkazuchi) {
                this.riderType |= RIDER_TYPE_IKAZUCHI;
            }
        }
        this.ridingState = hasHibiki ? 1 : 0;
    }

    public void dismountAllRider() {
        this.riderType = RIDER_TYPE_NONE;
        this.ridingState = 0;
        for (Entity rider : this.getPassengers()) {
            if (rider instanceof IShipRiderType) ((IShipRiderType) rider).setRiderType(RIDER_TYPE_NONE);
            if (rider instanceof IShipEmotion) ((IShipEmotion) rider).setRidingState(0);
        }
        this.removePassengers();
    }

    public void syncRotateToRider() {
        for (Entity rider : this.getPassengers()) {
            if (rider instanceof EntityLivingBase) {
                ((EntityLivingBase) rider).renderYawOffset = this.renderYawOffset;
                ((EntityLivingBase) rider).prevRenderYawOffset = this.prevRenderYawOffset;
                rider.rotationYaw = this.rotationYaw;
                rider.prevRotationYaw = this.prevRotationYaw;
            }
        }
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            return this.getStateEmotion(1) == 4 ? this.height * -0.07f : this.height * 0.26f;
        }
        return this.height * 0.64f;
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
    public void calcShipAttributesAddEquip() {
        super.calcShipAttributesAddEquip();
        if (this.getStateFlag(1) && this.getStateFlag(9)) {
            this.StateMinor[39] = 1;
        }
    }
}