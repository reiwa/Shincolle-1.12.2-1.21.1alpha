package com.lulan.shincolle.entity.destroyer;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.entity.IShipRiderType;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.EmotionHelper;
import com.lulan.shincolle.utility.ParticleHelper;
import net.minecraft.entity.Entity;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;

public class EntityDestroyerInazumaMob
extends BasicEntityShipHostile
implements IShipRiderType {
    public boolean isRaiden;
    private int ridingState;
    private float smokeX;
    private float smokeY;

    public EntityDestroyerInazumaMob(World world) {
        super(world);
        this.setStateMinor(20, 54);
        this.ridingState = 0;
        this.smokeX = 0.0f;
        this.smokeY = 0.0f;
        this.setStateEmotion(0, this.rand.nextInt(2), false);
    }

    @Override
    protected void setSizeWithScaleLevel() {
        switch (this.getScaleLevel()) {
            case 3: {
                this.setSize(1.7f, 6.0f);
                this.smokeX = -1.65f;
                this.smokeY = 5.3f;
                break;
            }
            case 2: {
                this.setSize(1.3f, 4.5f);
                this.smokeX = -1.2f;
                this.smokeY = 4.1f;
                break;
            }
            case 1: {
                this.setSize(0.9f, 3.0f);
                this.smokeX = -0.8f;
                this.smokeY = 2.7f;
                break;
            }
            default: {
                this.setSize(0.5f, 1.5f);
                this.smokeX = -0.42f;
                this.smokeY = 1.4f;
            }
        }
    }

    @Override
    protected void setBossInfo() {
        this.bossInfo = new BossInfoServer(this.getDisplayName(), BossInfo.Color.PINK, BossInfo.Overlay.NOTCHED_10);
    }

    @Override
    protected void setAIList() {
        super.setAIList();
        this.tasks.addTask(1, new EntityAIShipRangeAttack(this));
    }

    @Override
    public int getDamageType() {
        return 5;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.world.isRemote) {
            if (EmotionHelper.checkModelState(0, this.getStateEmotion(0)) && this.ticksExisted % 4 == 0) {
                float[] partPos = CalcHelper.rotateXZByAxis(this.smokeX, 0.0f, this.renderYawOffset % 360.0f * ((float)Math.PI / 180), 1.0f);
                ParticleHelper.spawnAttackParticleAt(this.posX + partPos[1], this.posY + this.smokeY, this.posZ + partPos[0], 1.0 + this.scaleLevel * 0.8, 0.0, 0.0, (byte)43);
            }
            if (this.ticksExisted % 16 == 0) {
                this.checkIsRaiden();
                this.checkRidingState();
            }
        } else if (this.ticksExisted % 128 == 0 && !this.isMorph) {
            this.checkIsRaiden();
        }
    }

    public void updatePassenger(Entity passenger) {
        if (this.isPassenger(passenger)) {
            passenger.setPosition(this.posX, this.posY + this.getMountedYOffset() + passenger.getYOffset(), this.posZ);
        }
    }

    public double getMountedYOffset() {
        return this.height * 0.47f;
    }

    public void checkRidingState() {
        this.ridingState = this.isRaiden ? 2 : 0;
    }

    public void checkIsRaiden() {
        this.isRaiden = false;
        List<Entity> riders = this.getPassengers();
        for (Entity rider : riders) {
            if (!(rider instanceof EntityDestroyerIkazuchiMob)) continue;
            this.isRaiden = true;
            break;
        }
    }

    @Override
    public int getRiderType() {
        return 0;
    }

    @Override
    public void setRiderType(int type) {
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
