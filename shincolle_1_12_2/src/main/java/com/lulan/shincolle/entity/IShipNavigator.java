package com.lulan.shincolle.entity;

import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;

public interface IShipNavigator {
    ShipPathNavigate getShipNavigate();

    ShipMoveHelper getShipMoveHelper();

    boolean canFly();

    boolean isJumping();

    float getMoveSpeed();

    float getJumpSpeed();
}
