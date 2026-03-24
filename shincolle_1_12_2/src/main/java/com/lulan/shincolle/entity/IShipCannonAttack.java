package com.lulan.shincolle.entity;

import net.minecraft.entity.Entity;

public interface IShipCannonAttack
extends IShipAttackBase {
    boolean attackEntityWithAmmo(Entity var1);

    boolean attackEntityWithHeavyAmmo(Entity var1);

    boolean useAmmoLight();

    boolean useAmmoHeavy();
}
