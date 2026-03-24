package com.lulan.shincolle.entity.carrier;

import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.entity.BasicEntityShipHostileCV;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;

import java.util.HashMap;

public class EntityCarrierAkagiMob
extends BasicEntityShipHostileCV {
    public EntityCarrierAkagiMob(World world) {
        super(world);
        this.setStateMinor(20, 48);
        this.launchHeight = this.height * 0.65f;
        this.setStateEmotion(0, this.rand.nextInt(128), false);
    }

    @Override
    protected void setSizeWithScaleLevel() {
        switch (this.getScaleLevel()) {
            case 3: {
                this.setSize(2.4f, 7.5f);
                break;
            }
            case 2: {
                this.setSize(1.8f, 5.625f);
                break;
            }
            case 1: {
                this.setSize(1.2f, 3.75f);
                break;
            }
            default: {
                this.setSize(0.6f, 1.875f);
            }
        }
    }

    @Override
    public void initAttrsServerPost() {
        super.initAttrsServerPost();
        if (this.AttackEffectMap == null) {
            this.AttackEffectMap = new HashMap<>();
        }
        this.AttackEffectMap.put(17, new int[]{this.getScaleLevel() / 3, 100 + this.getScaleLevel() * 50, 25 + this.getScaleLevel() * 25});
    }

    @Override
    protected void setBossInfo() {
        this.bossInfo = new BossInfoServer(this.getDisplayName(), BossInfo.Color.RED, BossInfo.Overlay.NOTCHED_10);
    }

    @Override
    protected void setAIList() {
        super.setAIList();
        this.tasks.addTask(11, new EntityAIShipCarrierAttack(this));
    }

    @Override
    public int getDamageType() {
        return 1;
    }
}
