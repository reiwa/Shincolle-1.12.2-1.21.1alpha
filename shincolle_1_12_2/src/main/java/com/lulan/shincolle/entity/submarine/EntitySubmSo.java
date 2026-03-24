package com.lulan.shincolle.entity.submarine;

import com.lulan.shincolle.ai.EntityAIShipPickItem;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.IShipInvisible;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.utility.EntityHelper;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

public class EntitySubmSo
extends BasicEntityShipSmall
implements IShipInvisible {
    public EntitySubmSo(World world) {
        super(world);
        this.setSize(0.6f, 1.8f);
        this.setStateMinor(19, 8);
        this.setStateMinor(20, 19);
        this.setStateMinor(25, 6);
        this.setStateMinor(13, 4);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[9]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[9]);
        this.ModelPos = new float[]{0.0f, 25.0f, 0.0f, 45.0f};
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
        this.tasks.addTask(11, new EntityAIShipRangeAttack(this));
        this.tasks.addTask(20, new EntityAIShipPickItem(this, 4.0f));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote && this.ticksExisted % 128 == 0 && this.getStateFlag(9) && this.getStateMinor(6) > 0) {
            EntityPlayerMP player;
            if (this.getStateFlag(1) && (player = (EntityPlayerMP)EntityHelper.getEntityPlayerByUID(this.getPlayerUID())) != null && this.getDistanceSq(player) < 256.0) {
                player.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 40 + this.getLevel(), 0, false, false));
            }
            this.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 40 + this.getLevel(), 0, false, false));
        }
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            if (this.getStateEmotion(1) == 4) {
                return this.height * 0.48f;
            }
            return 0.0;
        }
        return this.height * 0.69f;
    }

    @Override
    public float getInvisibleLevel() {
        return 0.25f;
    }

    @Override
    public double getShipFloatingDepth() {
        return 1.0;
    }
}
