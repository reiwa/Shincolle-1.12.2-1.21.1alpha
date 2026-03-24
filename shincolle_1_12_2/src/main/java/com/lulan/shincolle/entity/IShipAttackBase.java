package com.lulan.shincolle.entity;

import com.lulan.shincolle.reference.unitclass.MissileData;
import net.minecraft.entity.Entity;

import java.util.HashMap;

public interface IShipAttackBase
extends IShipNavigator,
IShipEmotion,
IShipOwner,
IShipAttrs {
    Entity getEntityTarget();

    void setEntityTarget(Entity var1);

    Entity getEntityRevengeTarget();

    void setEntityRevengeTarget(Entity var1);

    int getEntityRevengeTime();

    void setEntityRevengeTime();

    int getDamageType();

    boolean getAttackType(int var1);

    int getAmmoLight();

    int getAmmoHeavy();

    void setAmmoLight(int var1);

    boolean hasAmmoLight();

    boolean hasAmmoHeavy();

    int getLevel();

    boolean updateSkillAttack(Entity var1);

    HashMap<Integer, Integer> getBuffMap();

    void setBuffMap(HashMap<Integer, Integer> var1);

    HashMap<Integer, int[]> getAttackEffectMap();

    MissileData getMissileData(int var1);
}
