package com.lulan.shincolle.entity.cruiser;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.proxy.CommonProxy;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.List;
import java.util.Objects;

public class EntityCANe extends BasicEntityShipSmall {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;
    private static final int PUSH_MAX_TICKS = 200;
    private static final float PUSH_ENGAGE_DISTANCE = 2.5f;

    private boolean isPushing = false;
    private int tickPush = 0;
    private EntityLivingBase targetPush = null;

    public EntityCANe(World world) {
        super(world);
        this.setSize(0.6f, 1.3f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, 2);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 10);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 4);
        this.setStateMinor(STATE_MINOR_RARITY, 0);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[2]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[2]);
        this.ModelPos = new float[]{0.0f, 10.0f, 0.0f, 40.0f};
        this.StateFlag[15] = false;
        this.StateFlag[16] = false;
        this.setFoodSaturationMax(14);
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
    public void calcShipAttributesAddRaw() {
        super.calcShipAttributesAddRaw();
        if (!this.world.isDaytime()) {
            this.getAttrs().setAttrsRaw(9, this.getAttrs().getAttrsRaw(9) + 0.3f);
        }
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote) {
            updateServerSideLogic();
        }
    }

    private void updateServerSideLogic() {
        if (this.ticksExisted % 128 == 0) {
            handlePeriodicChecks();
        }
        if (this.isPushing) {
            updatePushingState();
        }
    }

    private void handlePeriodicChecks() {
        if (!this.world.isDaytime() && this.getStateFlag(9)) {
            this.addPotionEffect(new PotionEffect(MobEffects.SPEED, 150, this.getStateMinor(0) / 70, false, false));
            this.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 150, this.getStateMinor(0) / 60, false, false));
        }
        boolean canFindTarget = (this.ticksExisted % 256 == 0) && (this.getRNG().nextInt(5) != 0);
        boolean isActionBlocked = this.isMorph || this.isSitting() || this.isRiding() || this.getStateFlag(2) || this.getIsLeashed();
        if (canFindTarget && !isActionBlocked) {
            this.findTargetPush();
        }
    }

    private void updatePushingState() {
        if (this.isMorph) {
            cancelPush();
            return;
        }
        this.tickPush++;
        if (this.tickPush > PUSH_MAX_TICKS || this.targetPush == null || !this.targetPush.isEntityAlive()) {
            cancelPush();
            return;
        }
        if (this.getDistance(this.targetPush) <= PUSH_ENGAGE_DISTANCE) {
            executePushAttack();
        } else if (this.ticksExisted % 32 == 0) {
            this.getShipNavigate().tryMoveToEntityLiving(this.targetPush, 1.0);
        }
    }

    private void executePushAttack() {
        this.targetPush.addVelocity(-MathHelper.sin(this.rotationYaw * ((float) Math.PI / 180f)) * 0.5f, 0.5, MathHelper.cos(this.rotationYaw * ((float) Math.PI / 180f)) * 0.5f);
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 48.0);
        CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this.targetPush, 0, (byte) 54), point);
        this.swingArm(EnumHand.MAIN_HAND);
        this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
        this.cancelPush();
    }

    private void cancelPush() {
        this.isPushing = false;
        this.tickPush = 0;
        this.targetPush = null;
    }

    private void findTargetPush() {
        AxisAlignedBB impactBox = this.getEntityBoundingBox().expand(12.0, 6.0, 12.0);
        List<EntityLivingBase> list = this.world.getEntitiesWithinAABB(EntityLivingBase.class, impactBox);
        list.removeIf(ent -> this.equals(ent) || !ent.canBePushed() || !ent.canBeCollidedWith());
        if (!list.isEmpty()) {
            this.targetPush = list.get(this.rand.nextInt(list.size()));
            this.tickPush = 0;
            this.isPushing = true;
        }
    }

    @Override
    public double getMountedYOffset() {
        return this.isSitting() ? 0.0 : this.height * 0.24f;
    }

    @Override
    public double getYOffset() {
        return 0.3;
    }
}