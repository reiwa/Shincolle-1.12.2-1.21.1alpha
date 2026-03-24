package com.lulan.shincolle.entity.cruiser;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;

import java.util.HashMap;

public class EntityCAAtagoMob
extends BasicEntityShipHostile {
    public EntityCAAtagoMob(World world) {
        super(world);
        this.setStateMinor(20, 58);
        this.setStateEmotion(0, this.rand.nextInt(16), false);
    }

    @Override
    protected void setSizeWithScaleLevel() {
        switch (this.getScaleLevel()) {
            case 3: {
                this.setSize(1.7f, 7.0f);
                break;
            }
            case 2: {
                this.setSize(1.3f, 5.25f);
                break;
            }
            case 1: {
                this.setSize(0.9f, 3.5f);
                break;
            }
            default: {
                this.setSize(0.75f, 1.75f);
            }
        }
    }

    @Override
    protected void setBossInfo() {
        this.bossInfo = new BossInfoServer(this.getDisplayName(), BossInfo.Color.YELLOW, BossInfo.Overlay.NOTCHED_10);
    }

    @Override
    protected void setAIList() {
        super.setAIList();
        this.tasks.addTask(1, new EntityAIShipRangeAttack(this));
    }

    @Override
    public int getDamageType() {
        return 4;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float atk) {
        boolean attack = super.attackEntityFrom(source, atk);
        if (attack && source.getTrueSource() instanceof EntityLivingBase) {
            ((EntityLivingBase)source.getTrueSource()).addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100 + this.getScaleLevel() * 50, this.getScaleLevel() / 3, false, false));
        }
        return attack;
    }

    @Override
    public void initAttrsServerPost() {
        super.initAttrsServerPost();
        if (this.AttackEffectMap == null) {
            this.AttackEffectMap = new HashMap<>();
        }
        this.AttackEffectMap.put(2, new int[]{this.getScaleLevel() / 2, 100 + this.getScaleLevel() * 50, 25 + this.getScaleLevel() * 25});
        if (this.getScaleLevel() >= 2) {
            this.getMissileData(2).type = 5;
        }
    }
}
