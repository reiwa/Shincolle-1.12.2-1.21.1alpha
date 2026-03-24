package com.lulan.shincolle.entity.carrier;

import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.entity.BasicEntityAirplane;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipCV;
import com.lulan.shincolle.entity.other.EntityAirplaneT;
import com.lulan.shincolle.entity.other.EntityAirplaneZero;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;

public class EntityCarrierAkagi extends BasicEntityShipCV {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;
    private static final int ATTACK_EFFECT_KEY_AURA = 17;

    public EntityCarrierAkagi(World world) {
        super(world);
        this.setSize(0.6f, 1.875f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, 5);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 48);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 1);
        this.setStateMinor(STATE_MINOR_RARITY, 8);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[6]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[6]);
        this.ModelPos = new float[]{0.0f, 20.0f, 0.0f, 40.0f};
        this.launchHeight = this.height * 0.65f;
        this.StateFlag[13] = false;
        this.StateFlag[14] = false;
        this.setFoodSaturationMax(35);
        this.postInit();
    }

    @Override
    public EntityAgeable createChild(EntityAgeable ageable) {
        return null;
    }

    @Override
    public int getEquipType() {
        return 3;
    }

    @Override
    public void calcShipAttributesAddRaw() {
        super.calcShipAttributesAddRaw();
        this.maxAircraftLight += (int) (this.getLevel() * 0.28f);
        this.maxAircraftHeavy += (int) (this.getLevel() * 0.18f);
    }

    @Override
    public void calcShipAttributesAddEffect() {
        super.calcShipAttributesAddEffect();
        this.AttackEffectMap.put(ATTACK_EFFECT_KEY_AURA, new int[]{this.getLevel() / 120, 100 + this.getLevel(), this.getLevel()});
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(11, new EntityAIShipCarrierAttack(this));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.world.isRemote) {
            updateClientEffects();
        } else {
            updateServerLogic();
        }
    }

    private void updateClientEffects() {
        if (this.ticksExisted % 128 == 0 && this.rand.nextInt(4) == 0 && !this.getStateFlag(2)) {
            this.applyParticleEmotion(9);
        }
    }

    private void updateServerLogic() {
        if (this.ticksExisted % 128 == 0) {
            applyBuffToNearbyAllies();
        }
    }

    private void applyBuffToNearbyAllies() {
        if (!(this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 0)) {
            return;
        }
        List<BasicEntityShip> nearbyShips = this.world.getEntitiesWithinAABB(BasicEntityShip.class, this.getEntityBoundingBox().expand(16.0, 16.0, 16.0));
        if (nearbyShips.isEmpty()) {
            return;
        }
        nearbyShips.stream()
                .filter(ship -> TeamHelper.checkSameOwner(this, ship))
                .forEach(ship -> ship.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 50 + this.getStateMinor(0), this.getStateMinor(0) / 85, false, false)));
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            return this.getStateEmotion(1) == 4 ? this.height * 0.35f : this.height * 0.45f;
        }
        return this.height * 0.72f;
    }

    @Override
    public BasicEntityAirplane getAttackAirplane(boolean isLightAirplane) {
        if (isLightAirplane) {
            return new EntityAirplaneZero(this.world);
        }
        return new EntityAirplaneT(this.world);
    }

    public void applySoundAtAttacker(int type) {
        switch (type) {
            case 1:
            case 2:
                break;
            case 3:
            case 4:
                this.playSound(SoundEvents.ENTITY_ARROW_SHOOT, ConfigHandler.volumeFire + 0.2f, 1.0f / (this.rand.nextFloat() * 0.4f + 1.2f) + 0.5f);
                if (this.getRNG().nextInt(10) > 7) {
                    this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
                }
                break;
            default:
                if (this.getRNG().nextInt(2) == 0) {
                    this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
                }
                break;
        }
    }
}