package com.lulan.shincolle.entity.hime;

import com.lulan.shincolle.ai.EntityAIShipPickItem;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.utility.CombatHelper;
import com.lulan.shincolle.utility.EntityHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

public class EntityDestroyerHime
extends BasicEntityShipSmall {
    public EntityDestroyerHime(World world) {
        super(world);
        this.setSize(0.6f, 1.55f);
        this.setStateMinor(19, 10);
        this.setStateMinor(20, 27);
        this.setStateMinor(25, 5);
        this.setStateMinor(13, 6);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[0]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[0]);
        this.ModelPos = new float[]{0.0f, 25.0f, 0.0f, 50.0f};
        this.StateFlag[15] = false;
        this.StateFlag[16] = false;
        this.StateFlag[24] = true;
        this.postInit();
    }

    @Override
    public EntityAgeable createChild(EntityAgeable ageable) {
        return null;
    }

    @Override
    public int getEquipType() {
        return 1;
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(12, new EntityAIShipRangeAttack(this));
        this.tasks.addTask(20, new EntityAIShipPickItem(this, 4.0f));
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            if (this.getStateEmotion(1) == 4) {
                return 0.0;
            }
            return this.height * 0.62f;
        }
        return this.height * 0.76f;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote && (this.ticksExisted & 0x7F) == 0 && !this.isMorph) {
            EntityPlayer player = EntityHelper.getEntityPlayerByUID(this.getPlayerUID());
            if (this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 0 && player != null && this.getDistanceSq(player) < 256.0) {
                player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 80 + this.getStateMinor(0), this.getStateMinor(0) / 45 + 1, false, false));
                player.addPotionEffect(new PotionEffect(MobEffects.HASTE, 80 + this.getStateMinor(0), this.getStateMinor(0) / 30, false, false));
            }
        }
    }

    @Override
    public float getAttackBaseDamage(int type, Entity target) {
        switch (type) {
            case 1: {
                return CombatHelper.modDamageByAdditionAttrs(this, target, this.shipAttrs.getAttackDamage(), 0);
            }
            case 2: {
                return this.shipAttrs.getAttackDamageHeavy();
            }
            case 3: {
                return this.shipAttrs.getAttackDamageAir();
            }
            case 4: {
                return this.shipAttrs.getAttackDamageAirHeavy();
            }
            default:
        }
        return this.shipAttrs.getAttackDamage();
    }
}
