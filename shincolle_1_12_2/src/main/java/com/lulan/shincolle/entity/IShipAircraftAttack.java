package com.lulan.shincolle.entity;

import net.minecraft.entity.Entity;

public interface IShipAircraftAttack
extends IShipAttackBase {
    int getNumAircraftLight();

    int getNumAircraftHeavy();

    boolean hasAirLight();

    boolean hasAirHeavy();

    void setNumAircraftLight(int var1);

    void setNumAircraftHeavy(int var1);

    boolean attackEntityWithAircraft(Entity var1);

    boolean attackEntityWithHeavyAircraft(Entity var1);
}
