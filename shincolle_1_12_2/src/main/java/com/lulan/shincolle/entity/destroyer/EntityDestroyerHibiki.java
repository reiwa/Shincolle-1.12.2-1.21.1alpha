package com.lulan.shincolle.entity.destroyer;

import com.lulan.shincolle.ai.EntityAIShipPickItem;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.IShipRiderType;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.EmotionHelper;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.ParticleHelper;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Objects;

public class EntityDestroyerHibiki extends BasicEntityShipSmall implements IShipRiderType {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;
    private static final int STATE_FLAG_CAN_RIDE = 24;

    private int riderType;
    private int ridingState;

    public EntityDestroyerHibiki(World world) {
        super(world);
        this.setSize(0.5f, 1.5f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, -1);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 52);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 5);
        this.setStateMinor(STATE_MINOR_RARITY, 5);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[0]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[0]);
        this.ModelPos = new float[]{0.0f, 25.0f, 0.0f, 50.0f};
        this.StateFlag[15] = false;
        this.StateFlag[16] = false;
        this.StateFlag[STATE_FLAG_CAN_RIDE] = true;
        this.riderType = 0;
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
        EntityDestroyerAkatsuki akatsuki = getAkatsukiRiding();
        if (akatsuki != null) {
            akatsuki.syncRotateToRider();
        }
    }

    private void updateServerLogic() {
        if (this.ticksExisted % 32 == 0 && !this.isMorph) {
            this.checkRiderType();
            this.checkRidingState();
            if (this.ticksExisted % 128 == 0) {
                applyBuffToOwner();
            }
        }
    }

    private void updateClientLogic() {
        if (this.ticksExisted % 4 == 0) {
            spawnEngineParticles();
        }
        if (this.ticksExisted % 16 == 0) {
            this.checkRiderType();
            this.checkRidingState();
        }
    }

    private void applyBuffToOwner() {
        boolean canApplyBuff = this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 0;
        if (canApplyBuff) {
            EntityPlayer player = EntityHelper.getEntityPlayerByUID(this.getPlayerUID());
            if (player != null && this.getDistanceSq(player) < 256.0) {
                player.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 80 + this.getStateMinor(0), this.getStateMinor(0) / 45 + 1, false, false));
            }
        }
    }

    private void spawnEngineParticles() {
        boolean canSpawn = EmotionHelper.checkModelState(0, this.getStateEmotion(0)) && !this.isSitting() && !this.getStateFlag(2) && this.riderType < 2;
        if (canSpawn) {
            double smokeY = this.posY + 1.4;
            float[] partPos = CalcHelper.rotateXZByAxis(-0.42f, 0.0f, this.renderYawOffset % 360.0f * ((float) Math.PI / 180), 1.0f);
            ParticleHelper.spawnAttackParticleAt(this.posX + partPos[1], smokeY, this.posZ + partPos[0], 0.0, 0.0, 0.0, (byte) 20);
        }
    }

    public void checkRiderType() {
        this.riderType = 0;
        EntityDestroyerAkatsuki akatsuki = getAkatsukiRiding();
        if (akatsuki != null) {
            this.riderType = akatsuki.getRiderType();
        }
    }

    public void checkRidingState() {
        this.ridingState = this.riderType > 1 ? 2 : (this.riderType == 1 ? 1 : 0);
    }

    @Nullable
    private EntityDestroyerAkatsuki getAkatsukiRiding() {
        if (this.getRidingEntity() instanceof EntityDestroyerAkatsuki) {
            return (EntityDestroyerAkatsuki) this.getRidingEntity();
        }
        return null;
    }

    @Override
    protected void updateFuelState(boolean nofuel) {
        EntityDestroyerAkatsuki akatsuki = getAkatsukiRiding();
        if (nofuel && akatsuki != null) {
            akatsuki.dismountAllRider();
        }
        super.updateFuelState(nofuel);
    }

    @Override
    public void calcShipAttributesAddRaw() {
        super.calcShipAttributesAddRaw();
        this.getAttrs().setAttrsRaw(15, this.getAttrs().getAttrsRaw(15) + 0.3f);
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            return this.getStateEmotion(1) == 4 ? this.height * -0.07f : this.height * 0.26f;
        }
        return this.height * 0.64f;
    }

    @Override
    public boolean attackEntityFrom(DamageSource attacker, float atk) {
        if (this.world.isRemote) {
            return false;
        }
        boolean isDamaged = super.attackEntityFrom(attacker, atk);
        if (isDamaged) {
            EntityDestroyerAkatsuki akatsuki = getAkatsukiRiding();
            if (akatsuki != null) {
                akatsuki.dismountAllRider();
            }
        }
        return isDamaged;
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