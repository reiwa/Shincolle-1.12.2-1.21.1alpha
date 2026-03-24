package com.lulan.shincolle.entity.other;

import net.minecraft.world.World;

public class EntityRensouhouS
extends EntityRensouhou {
    public EntityRensouhouS(World world) {
        super(world);
        this.setSize(0.5f, 1.4f);
    }

    @Override
    public int getDamageType() {
        return 3;
    }

    @Override
    public int getTextureID() {
        return 7;
    }
}
