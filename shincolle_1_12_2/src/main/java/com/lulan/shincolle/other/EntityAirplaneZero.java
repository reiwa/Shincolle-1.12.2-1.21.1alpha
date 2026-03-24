package com.lulan.shincolle.entity.other;

import com.lulan.shincolle.init.ModSounds;
import net.minecraft.world.World;

public class EntityAirplaneZero
extends EntityAirplane {
    public EntityAirplaneZero(World world) {
        super(world);
        this.setSize(0.5f, 0.5f);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.ticksExisted == 6) {
            this.playSound(ModSounds.SHIP_AIRCRAFT, 0.4f, 0.7f / (this.getRNG().nextFloat() * 0.4f + 0.8f));
        }
    }

    @Override
    protected void applyFlyParticle() {
    }

    @Override
    public int getTextureID() {
        return 4;
    }
}
