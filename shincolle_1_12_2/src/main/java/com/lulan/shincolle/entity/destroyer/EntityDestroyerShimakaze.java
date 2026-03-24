package com.lulan.shincolle.entity.destroyer;

import com.lulan.shincolle.ai.EntityAIShipPickItem;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.IShipSummonAttack;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.entity.other.EntityRensouhou;
import com.lulan.shincolle.entity.other.EntityRensouhouS;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.Objects;

public class EntityDestroyerShimakaze extends BasicEntityShipSmall implements IShipSummonAttack {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;
    private static final int MAX_RENSOUHOU = 6;
    private static final float[][] TORPEDO_OFFSETS = {{0f, 0f}, {3.5f, 3.5f}, {3.5f, -3.5f}, {-3.5f, 3.5f}, {-3.5f, -3.5f}};

    public int numRensouhou;

    public EntityDestroyerShimakaze(World world) {
        super(world);
        this.setSize(0.5f, 1.6f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, -1);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 36);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 5);
        this.setStateMinor(STATE_MINOR_RARITY, 6);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[0]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[0]);
        this.ModelPos = new float[]{0.0f, 25.0f, 0.0f, 45.0f};
        this.numRensouhou = MAX_RENSOUHOU;
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
    public float getEyeHeight() {
        return 1.5f;
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
        if (!this.world.isRemote && this.ticksExisted % 128 == 0) {
            updateServerLogic();
        }
    }

    private void updateServerLogic() {
        if (this.isMorph) return;
        if (this.numRensouhou < MAX_RENSOUHOU) {
            this.numRensouhou++;
        }
        if (this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 0) {
            EntityPlayer player = EntityHelper.getEntityPlayerByUID(this.getPlayerUID());
            if (player != null && this.getDistanceSq(player) < 256.0) {
                player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 80 + this.getStateMinor(0), this.getStateMinor(0) / 35 + 1, false, false));
            }
        }
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
            EntityRensouhouS rensouhouS = new EntityRensouhouS(this.world);
            rensouhouS.initAttrs(this, target, 0);
            this.world.spawnEntity(rensouhouS);
        } else {
            EntityRensouhou rensouhou = new EntityRensouhou(this.world);
            rensouhou.initAttrs(this, target, 0);
            this.world.spawnEntity(rensouhou);
        }
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(BlockPos target) {
        return launchTorpedoSalvo(target, null);
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        return launchTorpedoSalvo(new BlockPos(target), target);
    }

    private boolean launchTorpedoSalvo(BlockPos targetPos, Entity targetEntity) {
        if (!this.decrAmmoNum(1, this.getAmmoConsumption())) {
            return false;
        }
        this.addShipExp(ConfigHandler.expGain[2]);
        this.decrGrudgeNum(ConfigHandler.consumeGrudgeAction[1]);
        this.decrMorale(2);
        this.setCombatTick(this.ticksExisted);

        this.applySoundAtAttacker(2);
        this.playSound(ModSounds.SHIP_FIREHEAVY, ConfigHandler.volumeFire, this.getSoundPitch() * 0.8f);
        this.playSound(ModSounds.SHIP_FIREHEAVY, ConfigHandler.volumeFire, this.getSoundPitch() * 0.9f);

        BlockPos finalTargetPos = calculateFinalTargetPos(targetPos);

        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32.0);
        CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);

        spawnTorpedoes(finalTargetPos, targetEntity);

        this.applyEmotesReaction(3);
        if (ConfigHandler.canFlare) {
            if (targetEntity != null) this.flareTarget(targetEntity);
            else this.flareTarget(finalTargetPos);
        }
        return true;
    }

    private BlockPos calculateFinalTargetPos(BlockPos initialPos) {
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this.getPosition(), initialPos);
        double tarX = initialPos.getX();
        double tarY = initialPos.getY();
        double tarZ = initialPos.getZ();
        if (distVec.d < 6.0) {
            tarX += distVec.x * (6.0 - distVec.d);
            tarY += distVec.y * (6.0 - distVec.d);
            tarZ += distVec.z * (6.0 - distVec.d);
        }
        if (this.rand.nextFloat() <= CombatHelper.calcMissRate(this, (float) distVec.d)) {
            tarX += -5.0f + this.rand.nextFloat() * 10.0f;
            tarY += this.rand.nextFloat() * 5.0f;
            tarZ += -5.0f + this.rand.nextFloat() * 10.0f;
            ParticleHelper.spawnAttackTextParticle(this, 0);
        }
        return new BlockPos(tarX, tarY, tarZ);
    }

    private void spawnTorpedoes(BlockPos centerTarget, Entity targetEntity) {
        float atk = this.getAttackBaseDamage(2, targetEntity) * 0.3f;
        float kbValue = 0.15f;
        float launchY = (float) this.posY + this.height * 0.7f;
        double targetY = (targetEntity != null) ? centerTarget.getY() + targetEntity.height * 0.1f : centerTarget.getY() + 0.2f;
        int moveType = CombatHelper.calcMissileMoveType(this, targetY, 2);
        if (moveType == 1) moveType = 0;

        MissileData md = this.getMissileData(2);
        for (float[] offset : TORPEDO_OFFSETS) {
            float tarX = centerTarget.getX() + offset[0];
            float tarZ = centerTarget.getZ() + offset[1];
            float[] data = {atk, kbValue, launchY, tarX, (float) targetY, tarZ, 160.0f, 0.25f, md.vel0, md.accY1, md.accY2};
            EntityAbyssMissile missile = new EntityAbyssMissile(this.world, this, md.type, moveType, data);
            this.world.spawnEntity(missile);
        }
    }

    @Override
    public void calcShipAttributesAddEquip() {
        super.calcShipAttributesAddEquip();
        MissileData md = this.getMissileData(2);
        md.vel0 += 0.2f;
        md.accY1 += 0.025f;
        md.accY2 += 0.025f;
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
            return this.getStateEmotion(1) == 4 ? this.height * -0.04f : this.height * 0.16f;
        }
        return this.height * 0.67f;
    }
}