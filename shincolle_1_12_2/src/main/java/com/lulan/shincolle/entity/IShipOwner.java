package com.lulan.shincolle.entity;

import net.minecraft.entity.Entity;

public interface IShipOwner {
    int getPlayerUID();

    void setPlayerUID(int var1);

    Entity getHostEntity();
}
