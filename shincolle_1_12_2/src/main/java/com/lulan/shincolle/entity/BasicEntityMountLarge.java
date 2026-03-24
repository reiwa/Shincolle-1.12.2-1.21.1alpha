package com.lulan.shincolle.entity;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public abstract class BasicEntityMountLarge
extends BasicEntityMount
implements IShipAircraftAttack {
    protected BasicEntityMountLarge(World world) {
        super(world);
    }

    @Override
    public int getNumAircraftLight() {
        if (this.host != null) {
            return this.host.getStateMinor(7);
        }
        return 0;
    }

    @Override
    public int getNumAircraftHeavy() {
        if (this.host != null) {
            return this.host.getStateMinor(8);
        }
        return 0;
    }

    @Override
    public boolean hasAirLight() {
        if (this.host != null) {
            return this.host.getStateMinor(7) > 0;
        }
        return false;
    }

    @Override
    public boolean hasAirHeavy() {
        if (this.host != null) {
            return this.host.getStateMinor(8) > 0;
        }
        return false;
    }

    @Override
    public void setNumAircraftLight(int par1) {
    }

    @Override
    public void setNumAircraftHeavy(int par1) {
    }

    @Override
    public boolean attackEntityWithAircraft(Entity target) {
        return ((IShipAircraftAttack) this.host).attackEntityWithAircraft(target);
    }

    @Override
    public boolean attackEntityWithHeavyAircraft(Entity target) {
        return ((IShipAircraftAttack) this.host).attackEntityWithHeavyAircraft(target);
    }
}
