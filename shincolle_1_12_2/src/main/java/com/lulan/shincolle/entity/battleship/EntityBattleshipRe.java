package com.lulan.shincolle.entity.battleship;

import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipCV;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.CombatHelper;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.TargetHelper;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.List;
import java.util.Objects;

public class EntityBattleshipRe extends BasicEntityShipCV {

    private static final int STATE_MINOR_FACTION_ID = 19;
    private static final int STATE_MINOR_SHIP_CLASS = 20;
    private static final int STATE_MINOR_SPECIAL_EQUIP = 25;
    private static final int STATE_MINOR_RARITY = 13;
    private static final int PUSH_MAX_TICKS = 200;
    private static final float PUSH_ENGAGE_DISTANCE = 2.5f;

    private boolean isPushing = false;
    private int tickPush = 0;
    private EntityLivingBase targetPush = null;

    public EntityBattleshipRe(World world) {
        super(world);
        this.setSize(0.6f, 1.55f);
        this.setStateMinor(STATE_MINOR_FACTION_ID, 6);
        this.setStateMinor(STATE_MINOR_SHIP_CLASS, 15);
        this.setStateMinor(STATE_MINOR_SPECIAL_EQUIP, 2);
        this.setStateMinor(STATE_MINOR_RARITY, 3);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[8]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[8]);
        this.ModelPos = new float[]{-6.0f, 25.0f, 0.0f, 40.0f};
        this.launchHeight = this.height * 0.8f;
        this.postInit();
    }

    @Override
    public EntityAgeable createChild(EntityAgeable ageable) {
        return null;
    }

    @Override
    public int getEquipType() {
        return 2;
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(11, new EntityAIShipCarrierAttack(this));
        this.tasks.addTask(12, new EntityAIShipRangeAttack(this));
    }

    @Override
    public void calcShipAttributesAddRaw() {
        super.calcShipAttributesAddRaw();
        this.maxAircraftLight += (int) (this.getLevel() * 0.1f);
        this.maxAircraftHeavy += (int) (this.getLevel() * 0.05f);
        this.getAttrs().setAttrsRaw(10, this.getAttrs().getAttrsRaw(10) + 0.1f);
        this.getAttrs().setAttrsRaw(11, this.getAttrs().getAttrsRaw(11) + 0.1f);
    }

    @Override
    public void calcShipAttributesAddEffect() {
        super.calcShipAttributesAddEffect();
        this.AttackEffectMap.put(4, new int[]{this.getLevel() / 100, 100 + this.getLevel(), this.getLevel()});
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote) {
            updateServerSideLogic();
        }
    }

    private void updateServerSideLogic() {
        if ((this.ticksExisted & 0x7F) == 0) {
            handlePeriodicChecks();
        }
        if (this.isPushing) {
            updatePushingState();
        }
    }

    private void handlePeriodicChecks() {
        if (this.isMorph) return;
        if (this.getStateFlag(1) && this.getStateFlag(9) && this.getStateMinor(6) > 0) {
            EntityPlayer player = EntityHelper.getEntityPlayerByUID(this.getPlayerUID());
            if (player != null && this.getDistanceSq(player) < 256.0) {
                player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 50 + this.getStateMinor(0), this.getStateMinor(0) / 50, false, false));
            }
        }
        boolean canFindTarget = (this.ticksExisted & 0xFF) == 0 && this.getRNG().nextInt(5) != 0;
        boolean isActionBlocked = this.isSitting() || this.isRiding() || this.getStateFlag(2) || this.getIsLeashed();
        if (canFindTarget && !isActionBlocked) {
            this.findTargetPush();
        }
    }

    private void updatePushingState() {
        if (this.isMorph) {
            cancelPush();
            return;
        }
        this.tickPush++;
        if (this.tickPush > PUSH_MAX_TICKS || this.targetPush == null || !this.targetPush.isEntityAlive()) {
            cancelPush();
            return;
        }
        if (this.getDistance(this.targetPush) <= PUSH_ENGAGE_DISTANCE) {
            executePushAttack();
        } else if (this.ticksExisted % 32 == 0) {
            this.getShipNavigate().tryMoveToEntityLiving(this.targetPush, 1.0);
        }
    }

    private void executePushAttack() {
        this.targetPush.addVelocity(-MathHelper.sin(this.rotationYaw * ((float) Math.PI / 180f)) * 0.5f, 0.5, MathHelper.cos(this.rotationYaw * ((float) Math.PI / 180f)) * 0.5f);
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 48.0);
        CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this.targetPush, 0, (byte) 54), point);
        this.swingArm(EnumHand.MAIN_HAND);
        this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
        this.cancelPush();
    }

    private void cancelPush() {
        this.isPushing = false;
        this.tickPush = 0;
        this.targetPush = null;
    }

    private void findTargetPush() {
        AxisAlignedBB impactBox = this.getEntityBoundingBox().expand(12.0, 6.0, 12.0);
        List<EntityLivingBase> list = this.world.getEntitiesWithinAABB(EntityLivingBase.class, impactBox);
        list.removeIf(ent -> this.equals(ent) || !ent.canBePushed() || !ent.canBeCollidedWith());
        if (!list.isEmpty()) {
            this.targetPush = list.get(this.rand.nextInt(list.size()));
            this.tickPush = 0;
            this.isPushing = true;
        }
    }

    @Override
    public void applyAttackPostMotion(int type, Entity target, boolean isTargetHurt, float atk) {
        if (type == 1 && isTargetHurt) {
            applyChainedLightningAttack(target, atk);
        }
    }

    private void applyChainedLightningAttack(Entity primaryTarget, float baseAttack) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        int maxTargets = (int) (this.getLevel() * 0.05f);
        float damage = baseAttack * 0.2f;
        AxisAlignedBB impactBox = primaryTarget.getEntityBoundingBox().expand(3.5, 3.5, 3.5);
        List<Entity> potentialTargets = this.world.getEntitiesWithinAABB(Entity.class, impactBox);
        if (potentialTargets.isEmpty()) return;
        potentialTargets.stream()
                .filter(entity -> isValidAoeTarget(entity, primaryTarget))
                .limit(maxTargets)
                .forEach(secondaryTarget -> {
                    float finalDamage = CombatHelper.applyCombatRateToDamage(this, true, 3.0f, damage);
                    finalDamage = CombatHelper.applyDamageReduceOnPlayer(secondaryTarget, finalDamage);
                    if (!TeamHelper.doFriendlyFire(this, secondaryTarget)) {
                        finalDamage = 0.0f;
                    }
                    secondaryTarget.attackEntityFrom(DamageSource.causeMobDamage(this), finalDamage);
                    CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(primaryTarget, secondaryTarget, primaryTarget.height * 0.5, 0.025, 0.008, 6, true), point);
                    this.applySoundAtTarget(1);
                    this.applyParticleAtTarget(1, secondaryTarget);
                });
    }

    private boolean isValidAoeTarget(Entity entityToTest, Entity primaryTarget) {
        if (entityToTest.equals(this) || entityToTest.equals(primaryTarget)) return false;
        if (TargetHelper.isEntityInvulnerable(entityToTest) || !entityToTest.canBeCollidedWith()) return false;
        return !TeamHelper.checkSameOwner(this, entityToTest);
    }

    public void applyParticleAtAttacker(int type, Entity target) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        if (type == 1) {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, target, this.isRiding() ? 1.45 : 1.7, 0.08, 0.02, 6, true), point);
        } else {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
        }
    }

    public void applySoundAtAttacker(int type) {
        switch (type) {
            case 1:
                this.playSound(ModSounds.SHIP_LASER, ConfigHandler.volumeFire * 0.25f, this.getSoundPitch() * 0.85f);
                if (this.rand.nextInt(10) > 7) {
                    this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
                }
                break;
            case 2:
                this.playSound(ModSounds.SHIP_FIREHEAVY, ConfigHandler.volumeFire, this.getSoundPitch() * 0.85f);
                if (this.getRNG().nextInt(10) > 7) {
                    this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
                }
                break;
            case 3:
            case 4:
                this.playSound(ModSounds.SHIP_AIRCRAFT, ConfigHandler.volumeFire * 0.5f, this.getSoundPitch() * 0.85f);
                break;
            default:
                if (this.getRNG().nextInt(2) == 0) {
                    this.playSound(getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
                }
                break;
        }
    }

    @Override
    public double getMountedYOffset() {
        if (this.isSitting()) {
            return this.getStateEmotion(1) == 4 ? this.height * 0.35f : 0.0;
        }
        return this.height * 0.55f;
    }
}