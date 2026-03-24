package com.lulan.shincolle.entity.battleship;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipSummonAttack;
import com.lulan.shincolle.entity.other.EntityRensouhou;
import com.lulan.shincolle.entity.other.EntityRensouhouS;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.utility.EmotionHelper;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.List;
import java.util.Objects;

public class EntityBattleshipTa extends BasicEntityShip implements IShipSummonAttack {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;
    private static final int MAX_RENSOUHOU = 6;

    public int numRensouhou;

    public EntityBattleshipTa(World world) {
        super(world);
        this.setSize(0.7f, 1.8f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, 6);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 14);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 3);
        this.setStateMinor(STATE_MINOR_RARITY, 3);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[7]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[7]);
        this.ModelPos = new float[]{0.0f, 25.0f, 0.0f, 40.0f};
        this.StateFlag[15] = false;
        this.StateFlag[16] = false;
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
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote && this.ticksExisted % 128 == 0) {
            updateServerLogic();
        }
    }

    private void updateServerLogic() {
        if (this.numRensouhou < MAX_RENSOUHOU) {
            this.numRensouhou++;
        }
        if (this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 0) {
            applyBuffToNearbyAllies();
        }
    }

    private void applyBuffToNearbyAllies() {
        List<BasicEntityShip> nearbyShips = this.world.getEntitiesWithinAABB(BasicEntityShip.class, this.getEntityBoundingBox().expand(16.0, 16.0, 16.0));
        if (nearbyShips.isEmpty()) return;
        nearbyShips.stream()
                .filter(ship -> TeamHelper.checkSameOwner(this, ship))
                .forEach(ship -> ship.addPotionEffect(new PotionEffect(MobEffects.SPEED, 50 + this.getStateMinor(0), this.getStateMinor(0) / 80, false, false)));
    }

    @Override
    public void calcShipAttributesAddRaw() {
        super.calcShipAttributesAddRaw();
        this.getAttrs().setAttrsRaw(9, this.getAttrs().getAttrsRaw(9) + 0.1f);
        this.getAttrs().setAttrsRaw(12, this.getAttrs().getAttrsRaw(12) + 0.1f);
    }

    @Override
    public boolean attackEntityWithAmmo(Entity target) {
        if (this.numRensouhou <= 0) return false;
        if (!this.decrAmmoNum(0, 4 * this.getAmmoConsumption())) return false;

        this.numRensouhou--;
        this.addShipExp(ConfigHandler.expGain[1] * 2);
        this.decrGrudgeNum(ConfigHandler.consumeGrudgeAction[0] * 4);
        this.decrMorale(1);
        this.setCombatTick(this.ticksExisted);

        spawnAttackEffects();
        summonRensouhou(target);

        this.applyEmotesReaction(3);
        if (ConfigHandler.canFlare) {
            this.flareTarget(target);
        }
        return true;
    }

    private void spawnAttackEffects() {
        if (this.rand.nextInt(10) > 7) {
            this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
        }
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32.0);
        CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
    }

    private void summonRensouhou(Entity target) {
        if (EmotionHelper.checkModelState(0, this.getStateEmotion(0))) {
            EntityRensouhou rensouhou = new EntityRensouhou(this.world);
            rensouhou.initAttrs(this, target, 0);
            this.world.spawnEntity(rensouhou);
        } else {
            EntityRensouhouS rensouhouS = new EntityRensouhouS(this.world);
            rensouhouS.initAttrs(this, target, 0);
            this.world.spawnEntity(rensouhouS);
        }
    }

    @Override
    public int getNumServant() {
        return this.numRensouhou;
    }

    @Override
    public void setNumServant(int num) {
        this.numRensouhou = num;
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            return this.getStateEmotion(1) == 4 ? 0.0 : this.height * 0.47f;
        }
        return this.height * 0.76f;
    }
}