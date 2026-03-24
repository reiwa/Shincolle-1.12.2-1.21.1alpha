package com.lulan.shincolle.entity;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public interface IShipGuardian
extends IShipAttackBase {
    Entity getGuardedEntity();

    void setGuardedEntity(Entity var1);

    int getGuardedPos(int var1);

    void setGuardedPos(int var1, int var2, int var3, int var4, int var5);

    BlockPos getLastWaypoint();

    void setLastWaypoint(BlockPos var1);

    int getWpStayTime();

    int getWpStayTimeMax();

    void setWpStayTime(int var1);
}
