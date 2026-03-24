package com.lulan.shincolle.entity;

import net.minecraft.entity.Entity;

import java.util.Random;

public interface IShipEmotion
extends IShipFlags {
    int getStateEmotion(int var1);

    void setStateEmotion(int var1, int var2, boolean var3);

    int getStateTimer(int var1);

    void setStateTimer(int var1, int var2);

    int getFaceTick();

    int getHeadTiltTick();

    int getAttackTick();

    int getAttackTick2();

    void setFaceTick(int var1);

    void setHeadTiltTick(int var1);

    void setAttackTick(int var1);

    void setAttackTick2(int var1);

    int getTickExisted();

    float getSwingTime(float var1);

    boolean getIsRiding();

    Entity getRidingEntity();

    boolean getIsSprinting();

    boolean getIsSitting();

    boolean getIsSneaking();

    boolean getIsLeashed();

    void setEntitySit(boolean var1);

    int getRidingState();

    void setRidingState(int var1);

    int getScaleLevel();

    void setScaleLevel(int var1);

    Random getRand();

    double getShipDepth(int var1);
}
