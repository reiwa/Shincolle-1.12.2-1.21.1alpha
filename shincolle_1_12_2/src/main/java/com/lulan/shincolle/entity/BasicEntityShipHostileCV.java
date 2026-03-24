package com.lulan.shincolle.entity;

import com.lulan.shincolle.entity.other.EntityAirplaneTMob;
import com.lulan.shincolle.entity.other.EntityAirplaneZeroMob;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.BlockHelper;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.Objects;

public abstract class BasicEntityShipHostileCV
extends BasicEntityShipHostile
implements IShipAircraftAttack {
    protected double launchHeight;

    protected BasicEntityShipHostileCV(World world) {
        super(world);
    }

    @Override
    public int getNumAircraftLight() {
        return 10;
    }

    @Override
    public int getNumAircraftHeavy() {
        return 10;
    }

    @Override
    public boolean hasAirLight() {
        return true;
    }

    @Override
    public boolean hasAirHeavy() {
        return true;
    }

    @Override
    public void setNumAircraftLight(int par1) {
    }

    @Override
    public void setNumAircraftHeavy(int par1) {
    }

    @Override
    public boolean attackEntityWithAircraft(Entity target) {
        this.applySoundAtAttacker(3);
        this.applyParticleAtAttacker(3, Dist4d.ONE);
        float summonHeight = (float)(this.posY + this.launchHeight);
        if (!BlockHelper.checkBlockSafe(this.world, (int)this.posX, (int)(this.posY + this.launchHeight), (int)this.posZ)) {
            summonHeight = (float)this.posY + this.height * 0.75f;
        }
        EntityAirplaneZeroMob plane = new EntityAirplaneZeroMob(this.world);
        plane.initAttrs(this, target, this.scaleLevel, summonHeight);
        this.world.spawnEntity(plane);
        this.applyEmotesReaction(3);
        return true;
    }

    @Override
    public boolean attackEntityWithHeavyAircraft(Entity target) {
        this.applySoundAtAttacker(4);
        this.applyParticleAtAttacker(4, Dist4d.ONE);
        float summonHeight = (float)(this.posY + this.launchHeight);
        if (!BlockHelper.checkBlockSafe(this.world, (int)this.posX, (int)(this.posY + this.launchHeight), (int)this.posZ)) {
            summonHeight = (float)this.posY + this.height * 0.75f;
        }
        EntityAirplaneTMob plane = new EntityAirplaneTMob(this.world);
        plane.initAttrs(this, target, this.scaleLevel, summonHeight);
        this.world.spawnEntity(plane);
        this.applyEmotesReaction(3);
        return true;
    }
}
