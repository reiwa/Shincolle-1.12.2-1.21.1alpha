package com.lulan.shincolle.entity;

public interface IShipFlags {
    int getStateMinor(int var1);

    void setStateMinor(int var1, int var2);

    boolean getStateFlag(int var1);

    void setStateFlag(int var1, boolean var2);

    void setUpdateFlag(int var1, boolean var2);

    boolean getUpdateFlag(int var1);
}
