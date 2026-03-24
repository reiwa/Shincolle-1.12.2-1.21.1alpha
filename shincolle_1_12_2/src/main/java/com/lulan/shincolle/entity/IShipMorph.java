package com.lulan.shincolle.entity;

import net.minecraft.entity.player.EntityPlayer;

public interface IShipMorph {
    boolean isMorph();

    void setIsMorph(boolean var1);

    EntityPlayer getMorphHost();

    void setMorphHost(EntityPlayer var1);
}
