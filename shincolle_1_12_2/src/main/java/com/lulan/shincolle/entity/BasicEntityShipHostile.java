package com.lulan.shincolle.entity;

import com.lulan.shincolle.ai.*;
import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.client.render.IShipCustomTexture;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.item.BasicEntityItem;
import com.lulan.shincolle.network.C2SInputPackets;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CReactPackets;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.ClientProxy;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.reference.unitclass.AttrsAdv;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Random;

public abstract class BasicEntityShipHostile
extends EntityMob
implements IShipCannonAttack,
IShipFloating,
IShipCustomTexture,
IShipMorph {
    protected static final IAttribute MAX_HP = new RangedAttribute(null, "generic.maxHealth", 4.0, 0.0, 30000.0).setDescription("Max Health").setShouldWatch(true);
    protected double ShipDepth;
    protected HashMap<Integer, Integer> BuffMap;
    protected HashMap<Integer, int[]> AttackEffectMap;
    protected MissileData MissileData;
    protected Attrs shipAttrs;
    protected int[] stateEmotion;
    protected int startEmotion;
    protected int startEmotion2;
    protected int attackTime;
    protected int attackTime2;
    protected int attackTime3;
    protected int emoteDelay;
    protected boolean headTilt;
    protected float[] rotateAngle;
    protected int soundHurtDelay;
    protected short shipClass;
    public byte scaleLevel;
    protected ItemStack dropItem;
    protected BossInfoServer bossInfo;
    public boolean canDrop;
    public boolean initScale;
    protected ShipPathNavigate shipNavigator;
    protected ShipMoveHelper shipMoveHelper;
    protected Entity atkTarget;
    protected Entity rvgTarget;
    protected int revengeTime;
    public static boolean stopAI = false;
    protected boolean isMorph = false;
    protected EntityPlayer morphHost;

    protected BasicEntityShipHostile(World world) {
        super(world);
        this.isImmuneToFire = true;
        this.ignoreFrustumCheck = true;
        this.maxHurtResistantTime = 2;
        this.canDrop = true;
        this.rotateAngle = new float[]{0.0f, 0.0f, 0.0f};
        this.scaleLevel = 0;
        this.startEmotion = 0;
        this.startEmotion2 = 0;
        this.headTilt = false;
        this.initScale = false;
        this.BuffMap = new HashMap<>();
        this.shipAttrs = new Attrs();
        this.MissileData = new MissileData();
        this.soundHurtDelay = 0;
        this.stateEmotion = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
    }

    protected void applyEntityAttributes() {
        this.getAttributeMap().registerAttribute(MAX_HP);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        this.getAttributeMap().registerAttribute(SWIM_SPEED);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ARMOR);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
    }

    public boolean isBurning() {
        return this.getStateEmotion(3) == 3;
    }

    @Override
    public boolean isJumping() {
        return this.isJumping;
    }

    protected boolean canDespawn() {
        if (this.scaleLevel > 1) {
            if (ConfigHandler.despawnBoss > -1) {
                return this.ticksExisted > ConfigHandler.despawnBoss;
            }
            return false;
        }
        if (ConfigHandler.despawnMinion > -1) {
            return this.ticksExisted > ConfigHandler.despawnMinion;
        }
        return false;
    }

    protected void setAIList() {
        this.clearAITasks();
        this.clearAITargetTasks();
        this.tasks.addTask(0, new EntityAIShipFloating(this));
        this.tasks.addTask(21, new EntityAIShipOpenDoor(this, true));
        this.tasks.addTask(23, new EntityAIShipWander(this, 12, 1, 0.8));
        this.tasks.addTask(24, new EntityAIShipWatchClosest(this, EntityPlayer.class, 8.0f, 0.1f));
        this.tasks.addTask(25, new EntityAILookIdle(this));
    }

    public void setAITargetList() {
        this.targetTasks.addTask(1, new EntityAIShipRevengeTarget(this));
        this.targetTasks.addTask(3, new EntityAIShipRangeTarget(this, Entity.class));
    }

    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.scaleLevel = nbt.getByte("scaleLV");
        float hp = this.getHealth();
        this.initAttrs(this.scaleLevel);
        this.setHealth(hp);
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setByte("scaleLV", this.scaleLevel);
        return nbt;
    }

    protected void clearAITasks() {
        this.tasks.taskEntries.clear();
    }

    protected void clearAITargetTasks() {
        this.setEntityTarget(null);
        this.targetTasks.taskEntries.clear();
    }

    public ItemStack getDropEgg() {
        switch (this.getScaleLevel()) {
            case 0:
                return this.rand.nextInt(5) == 0 ? this.dropItem : ItemStack.EMPTY;
            case 1:
                return this.rand.nextInt(3) == 0 ? this.dropItem : ItemStack.EMPTY;
            default:
                return this.rand.nextInt(10) > 0 ? this.dropItem : ItemStack.EMPTY;
        }
    }

    public IAttributeInstance getEntityAttribute(IAttribute attribute) {
        if (attribute == SharedMonsterAttributes.MAX_HEALTH) {
            this.getAttributeMap().getAttributeInstance(MAX_HP);
        }
        return this.getAttributeMap().getAttributeInstance(attribute);
    }

    protected void setAttrsWithScaleLevel() {
        this.shipAttrs = new Attrs(this.getShipClass());
        this.calcShipAttributes(9);
        if (this.getHealth() < this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    public void calcShipAttributes(int flag) {
        if (this.shipAttrs == null) {
            this.shipAttrs = new AttrsAdv(this.getShipClass());
        }
        this.stepHeight = 1.0f + this.getScaleLevel();
        if ((flag & 1) == 1) {
            BuffHelper.updateAttrsRawHostile(this.shipAttrs, this.getScaleLevel(), this.getShipClass());
        }
        if ((flag & 8) == 8) {
            BuffHelper.updateBuffPotion(this);
        }
        BuffHelper.applyBuffOnAttrsHostile(this);
        this.getEntityAttribute(MAX_HP).setBaseValue(this.shipAttrs.getAttrsRaw(0));
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(this.shipAttrs.getAttrsRaw(7));
        this.getEntityAttribute(SWIM_SPEED).setBaseValue(this.shipAttrs.getAttrsRaw(7));
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64.0);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(this.shipAttrs.getAttrsRaw(20));
    }

    protected abstract void setSizeWithScaleLevel();

    protected abstract void setBossInfo();

    public void initAttrs(int scaleLevel) {
        this.setScaleLevel(scaleLevel);
        if (this.scaleLevel > 1) {
            this.setBossInfo();
        }
        if (!this.world.isRemote) {
            this.shipNavigator = new ShipPathNavigate(this);
            this.shipMoveHelper = new ShipMoveHelper(this, 60.0f);
            this.setAIList();
            this.setAITargetList();
            this.initAttrsServerPost();
            this.dropItem = new ItemStack(ModItems.ShipSpawnEgg, 1, this.getStateMinor(20) + 2);
        }
    }

    protected void initAttrsServerPost() {
    }

    @Override
    public void setScaleLevel(int par1) {
        this.scaleLevel = (byte)par1;
        this.setSizeWithScaleLevel();
        this.setAttrsWithScaleLevel();
        this.initScale = true;
        if (!this.world.isRemote) {
            this.sendSyncPacket(0);
        }
    }

    @Nullable
    protected SoundEvent getAmbientSound() {
        if (this.rand.nextInt(2) == 0) {
            return ModSounds.SHIP_IDLE;
        }
        return null;
    }

    @Nullable
    protected SoundEvent getHurtSound(DamageSource source) {
        if (this.rand.nextInt(2) == 0 && this.soundHurtDelay <= 0) {
            this.soundHurtDelay = 20 + this.getRNG().nextInt(40);
            return ModSounds.SHIP_HURT;
        }
        return null;
    }

    @Nullable
    protected SoundEvent getDeathSound() {
        return ModSounds.SHIP_DEATH;
    }

    protected float getSoundVolume() {
        return ConfigHandler.volumeShip;
    }

    @Override
    public int getStateEmotion(int id) {
        return this.stateEmotion[id];
    }

    @Override
    public void setStateEmotion(int id, int value, boolean sync) {
        this.stateEmotion[id] = value;
        if (sync && !this.world.isRemote) {
            this.sendSyncPacket(4);
        }
    }

    @Override
    public boolean getStateFlag(int flag) {
        switch (flag) {
            case 3:
            case 12: {
                return false;
            }
            case 8: {
                return this.headTilt;
            }
            case 2: {
                return this.isDead || this.deathTime > 0;
            }
            default:
        }
        return true;
    }

    @Override
    public void setStateFlag(int id, boolean flag) {
        switch (id) {
            case 8: {
                this.headTilt = flag;
                break;
            }
            case 2: {
                this.deathTime = flag ? 1 : 0;
                break;
            }
            default:
        }
    }

    @Override
    public int getFaceTick() {
        return this.startEmotion;
    }

    @Override
    public int getHeadTiltTick() {
        return this.startEmotion2;
    }

    @Override
    public void setFaceTick(int par1) {
        this.startEmotion = par1;
    }

    @Override
    public void setHeadTiltTick(int par1) {
        this.startEmotion2 = par1;
    }

    @Override
    public int getTickExisted() {
        return this.ticksExisted;
    }

    public boolean attackEntityFrom(DamageSource source, float atk) {
        if (this.world.isRemote) {
            return false;
        }
        this.setStateEmotion(1, 3, true);
        boolean checkDEF = true;
        if (source == DamageSource.IN_WALL || source == DamageSource.STARVE || source == DamageSource.CACTUS || source == DamageSource.FALL) {
            return false;
        }
        if (source == DamageSource.MAGIC || source == DamageSource.DRAGON_BREATH) {
            if (atk < this.getMaxHealth() * 0.01f) {
                return false;
            }
            return super.attackEntityFrom(source, atk);
        }
        if (source == DamageSource.OUT_OF_WORLD) {
            this.setDead();
            return false;
        }
        float patk = BuffHelper.getPotionDamage(this, source, atk);
        if (patk > 0.0f) {
            atk = patk;
            checkDEF = false;
        }
        if (this.isEntityInvulnerable(source)) {
            return false;
        }
        if (source.getTrueSource() != null) {
            Entity attacker = source.getTrueSource();
            if (attacker.equals(this)) {
                return false;
            }
            float dist = (float)this.getDistanceSq(attacker);
            if (CombatHelper.canDodge(this, dist)) {
                return false;
            }
            float reducedAtk = atk;
            if (checkDEF) {
                reducedAtk = CombatHelper.applyDamageReduceByDEF(this.shipAttrs, reducedAtk);
            }
            reducedAtk = BuffHelper.applyBuffOnDamageByResist(this, source, reducedAtk);
            if ((reducedAtk = BuffHelper.applyBuffOnDamageByLight(this, source, reducedAtk)) < 1.0f && reducedAtk > 0.0f) {
                reducedAtk = 1.0f;
            } else if (reducedAtk <= 0.0f) {
                reducedAtk = 0.0f;
            }
            this.setEntityRevengeTarget(attacker);
            this.setEntityRevengeTime();
            if (this.rand.nextInt(5) == 0) {
                this.applyEmotesReaction(2);
            }
            return super.attackEntityFrom(source, reducedAtk);
        }
        return false;
    }

    @Override
    public boolean attackEntityWithAmmo(Entity target) {
        boolean isTargetHurt;
        float atk = this.getAttackBaseDamage(1, target);
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        this.applySoundAtAttacker(1);
        this.applyParticleAtAttacker(1, distVec);
        atk = CombatHelper.applyCombatRateToDamage(this, true, (float)distVec.d, atk);
        atk = CombatHelper.applyDamageReduceOnPlayer(target, atk);
        if (!TeamHelper.doFriendlyFire(this, target)) {
            atk = 0.0f;
        }
        if (isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this).setProjectile(), atk)) {
            if (!TeamHelper.checkSameOwner(this, target)) {
                BuffHelper.applyBuffOnTarget(target, this.getAttackEffectMap());
            }
            this.applySoundAtTarget(1);
            this.applyParticleAtTarget(1, target);
            this.applyEmotesReaction(3);
        }
        return isTargetHurt;
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        float atk = this.getAttackBaseDamage(2, target);
        float kbValue = 0.15f;
        float launchPos = (float)this.posY + this.height * 0.5f;
        int moveType = CombatHelper.calcMissileMoveType(this, target.posY, 2);
        if (moveType == 0) {
            launchPos = (float)this.posY + this.height * 0.3f;
        }
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        this.applySoundAtAttacker(2);
        this.applyParticleAtAttacker(2, distVec);
        float tarX = (float)target.posX;
        float tarY = (float)target.posY;
        float tarZ = (float)target.posZ;
        if (this.rand.nextFloat() <= CombatHelper.calcMissRate(this, (float)distVec.d)) {
            tarX = tarX - 5.0f + this.rand.nextFloat() * 10.0f;
            tarY += this.rand.nextFloat() * 5.0f;
            tarZ = tarZ - 5.0f + this.rand.nextFloat() * 10.0f;
            ParticleHelper.spawnAttackTextParticle(this, 0);
        }
        MissileData md = this.getMissileData(2);
        float[] data = new float[]{atk, kbValue, launchPos, tarX, tarY + target.height * 0.1f, tarZ, 160.0f, 0.25f, md.vel0, md.accY1, md.accY2};
        EntityAbyssMissile missile = new EntityAbyssMissile(this.world, this, md.type, moveType, data);
        this.world.spawnEntity(missile);
        this.applySoundAtTarget(2);
        this.applyParticleAtTarget(2, target);
        this.applyEmotesReaction(3);
        return true;
    }

    @Override
    public boolean updateSkillAttack(Entity target) {
        return false;
    }

    @Override
    public float getMoveSpeed() {
        return this.shipAttrs.getMoveSpeed();
    }

    @Override
    public float getJumpSpeed() {
        return this.isNonBoss() ? 1.0f : 2.0f;
    }

    @Override
    public Entity getEntityTarget() {
        return this.atkTarget;
    }

    @Override
    public void setEntityTarget(Entity target) {
        this.atkTarget = target;
    }

    @Override
    public boolean getIsRiding() {
        return false;
    }

    @Override
    public boolean getIsSprinting() {
        return false;
    }

    @Override
    public boolean getIsSitting() {
        return false;
    }

    @Override
    public boolean getIsSneaking() {
        return false;
    }

    @Override
    public boolean hasAmmoLight() {
        return true;
    }

    @Override
    public boolean hasAmmoHeavy() {
        return true;
    }

    @Override
    public int getAmmoLight() {
        return 30000;
    }

    @Override
    public int getAmmoHeavy() {
        return 30000;
    }

    @Override
    public void setAmmoLight(int num) {
    }

    @Override
    public double getShipDepth() {
        return this.ShipDepth;
    }

    @Override
    public double getShipDepth(int type) {
        return this.ShipDepth;
    }

    public void sendSyncPacket(int type) {
        if (!this.world.isRemote) {
            byte pid;
            switch (type) {
                case 0: {
                    pid = 5;
                    break;
                }
                case 1: {
                    pid = 54;
                    break;
                }
                case 2: {
                    pid = 52;
                    break;
                }
                case 3: {
                    pid = 53;
                    break;
                }
                case 4: {
                    pid = 50;
                    break;
                }
                default: {
                    return;
                }
            }
            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
            CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, pid), point);
        }
    }

    public void onUpdate() {
        if (stopAI) {
            return;
        }
        super.onUpdate();
        this.updateArmSwingProgress();
        EntityHelper.checkDepth(this);
        if (this.soundHurtDelay > 0) {
            --this.soundHurtDelay;
        }
        if (this.emoteDelay > 0) {
            --this.emoteDelay;
        }
        if (this.attackTime > 0) {
            --this.attackTime;
        }
        if (this.world.isRemote) {
            if (!this.initScale) {
                CommonProxy.channelI.sendToServer(new C2SInputPackets((byte)5, this.getEntityId(), this.world.provider.getDimension()));
            }
            if (EntityHelper.checkEntityIsInLiquid(this)) {
                double motX = this.posX - this.prevPosX;
                double motZ = this.posZ - this.prevPosZ;
                if (motX != 0.0 || motZ != 0.0) {
                    ParticleHelper.spawnAttackParticleAt(this.posX + motX * 3.0, this.posY + 0.4, this.posZ + motZ * 3.0, -motX, this.width, -motZ, (byte)47);
                }
            }
        } else if (this.ticksExisted == 1) {
            this.sendSyncPacket(0);
        }
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.world.isRemote) {
            this.updateClientSide();
        } else {
            this.updateServerSide();
        }
    }

    private void updateServerSide() {
        if (!this.isMorph) {
            EntityHelper.updateShipNavigator(this);
        }
        if ((this.ticksExisted & 0xF) == 0) {
            this.handleServerTicks16();
        }
    }

    private void handleServerTicks16() {
        if (!this.isMorph) {
            TargetHelper.updateTarget(this);
            if (this.getAttackTarget() != null) {
                this.setEntityTarget(this.getAttackTarget());
            }
            if (this.isEntityAlive()) {
                BuffHelper.convertPotionToBuffMap(this);
                this.calcShipAttributes(8);
            }
        }
        if ((this.ticksExisted & 0x1F) == 0) {
            this.handleServerTicks32();
        }
    }

    private void handleServerTicks32() {
        if (this.isEntityAlive() && !this.isMorph) {
            BuffHelper.applyBuffOnTicks(this);
        }
        if ((this.ticksExisted & 0x3F) == 0) {
            this.handleServerTicks64();
        }
    }

    private void handleServerTicks64() {
        this.updateEmotionState();
        if ((this.ticksExisted & 0x7F) == 0) {
            this.handleServerTicks128();
        }
    }

    private void handleServerTicks128() {
        if (this.isEntityAlive() && !this.isMorph) {
            this.calcShipAttributes(31);
        }
        if ((this.ticksExisted & 0xFF) == 0) {
            this.handleServerTicks256();
        }
    }

    private void handleServerTicks256() {
        if (this.getAir() < 300) {
            this.setAir(300);
        }
        if (this.isEntityAlive()) {
            this.applyEmotesReaction(4);
        }
    }

    private void updateClientSide() {
        if ((this.ticksExisted & 0xF) == 0) {
            switch (this.getStateEmotion(3)) {
                case 1:
                    ParticleHelper.spawnAttackParticleAt(this.posX, this.posY + 0.7, this.posZ, this.width * 1.5, 0.05, 0.0, (byte)4);
                    break;
                case 2:
                    ParticleHelper.spawnAttackParticleAt(this.posX, this.posY + 0.7, this.posZ, this.width * 1.5, 0.05, 0.0, (byte)5);
                    break;
                case 3:
                    ParticleHelper.spawnAttackParticleAt(this.posX, this.posY + 0.7, this.posZ, this.width * 1.5, 0.05, 0.0, (byte)7);
                    break;
                default:
            }
        }
    }

    protected void updateEmotionState() {
        float hpState;
        hpState = this.morphHost != null ? this.morphHost.getHealth() / this.morphHost.getMaxHealth() : this.getHealth() / this.getMaxHealth();
        if (hpState > 0.75f) {
            this.setStateEmotion(3, 0, false);
        } else if (hpState > 0.5f) {
            this.setStateEmotion(3, 1, false);
        } else if (hpState > 0.25f) {
            this.setStateEmotion(3, 2, false);
        } else {
            this.setStateEmotion(3, 3, false);
        }
        if (this.getStateFlag(2)) {
            if (this.getStateEmotion(1) != 5) {
                this.setStateEmotion(1, 5, false);
            }
        } else if (hpState < 0.35f) {
            if (this.getStateEmotion(1) != 2) {
                this.setStateEmotion(1, 2, false);
            }
        } else {
            if(this.getStateEmotion(1) == 0 && this.getRNG().nextInt(4) == 0){
                this.setStateEmotion(1, 4, false);
            } else if(this.getRNG().nextInt(2) == 0){
                this.setStateEmotion(1, 0, false);
            }
            if(this.getStateEmotion(7) == 0 && this.getRNG().nextInt(3) == 0){
                this.setStateEmotion(7, 4, false);
            } else if(this.getRNG().nextInt(2) == 0){
                this.setStateEmotion(7, 0, false);
            }
        }
        this.sendSyncPacket(4);
    }

    @Override
    public ShipPathNavigate getShipNavigate() {
        return this.shipNavigator;
    }

    @Override
    public ShipMoveHelper getShipMoveHelper() {
        return this.shipMoveHelper;
    }

    @Override
    public boolean canFly() {
        return false;
    }

    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public void setShipDepth(double par1) {
        this.ShipDepth = par1;
    }

    @Override
    public boolean getIsLeashed() {
        return this.getLeashed();
    }

    @Override
    public int getLevel() {
        return 150;
    }

    @Override
    public boolean useAmmoLight() {
        return true;
    }

    @Override
    public boolean useAmmoHeavy() {
        return true;
    }

    @Override
    public int getStateMinor(int id) {
        if(id == 20){
            return this.shipClass;
        }
        return 0;
    }

    @Override
    public void setStateMinor(int id, int value) {
        if(id == 20){
            this.shipClass = (short)value;
        }
    }

    @Override
    public void setEntitySit(boolean sit) {
    }

    @Override
    public boolean getAttackType(int par1) {
        return true;
    }

    @Override
    public int getPlayerUID() {
        return -100;
    }

    @Override
    public void setPlayerUID(int uid) {
    }

    @Override
    public Entity getHostEntity() {
        return this;
    }

    @Override
    public Entity getEntityRevengeTarget() {
        return this.rvgTarget;
    }

    @Override
    public int getEntityRevengeTime() {
        return this.revengeTime;
    }

    @Override
    public void setEntityRevengeTarget(Entity target) {
        this.rvgTarget = target;
    }

    @Override
    public void setEntityRevengeTime() {
        this.revengeTime = this.ticksExisted;
    }

    @Override
    public EnumActionResult applyPlayerInteraction(EntityPlayer player, Vec3d vec, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!this.world.isRemote
                && player.capabilities.isCreativeMode
                && !stack.isEmpty()
                && stack.getItem() == ModItems.KaitaiHammer) {
            this.setDead();
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.PASS;
    }

    @Override
    public int getAttackTick() {
        return this.attackTime;
    }

    @Override
    public int getAttackTick2() {
        return this.attackTime2;
    }

    @Override
    public void setAttackTick(int par1) {
        this.attackTime = par1;
    }

    @Override
    public void setAttackTick2(int par1) {
        this.attackTime2 = par1;
    }

    public void applyParticleEmotion(int type) {
        float h = this.height * 0.6f;
        if (!this.world.isRemote) {
            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 36, h, 0.0, type), point);
        } else {
            ParticleHelper.spawnAttackParticleAtEntity(this, h, 0.0, type, (byte)36);
        }
    }

    public void applyEmotesReaction(int type) {
        switch (type) {
            case 2: {
                if (this.emoteDelay > 0) break;
                this.emoteDelay = 40;
                this.reactionDamaged();
                break;
            }
            case 3: {
                if (this.rand.nextInt(7) != 0 || this.emoteDelay > 0) break;
                this.emoteDelay = 60;
                this.reactionAttack();
                break;
            }
            case 6: {
                this.reactionShock();
                break;
            }
            default: {
                if (this.rand.nextInt(3) != 0 || this.emoteDelay > 0) break;
                this.emoteDelay = 20;
                this.reactionIdle();
            }
        }
    }

    protected void reactionShock() {
        switch (this.rand.nextInt(6)) {
            case 1: {
                this.applyParticleEmotion(0);
                break;
            }
            case 2: {
                this.applyParticleEmotion(8);
                break;
            }
            case 3: {
                this.applyParticleEmotion(4);
                break;
            }
            default: {
                this.applyParticleEmotion(12);
            }
        }
    }

    protected void reactionIdle() {
        switch (this.rand.nextInt(15)) {
            case 3: {
                this.applyParticleEmotion(7);
                break;
            }
            case 6: {
                this.applyParticleEmotion(3);
                break;
            }
            case 7: {
                this.applyParticleEmotion(16);
                break;
            }
            case 9: {
                this.applyParticleEmotion(29);
                break;
            }
            case 10: {
                this.applyParticleEmotion(18);
                break;
            }
            default: {
                this.applyParticleEmotion(11);
            }
        }
    }

    protected void reactionAttack() {
        switch (this.rand.nextInt(15)) {
            case 1: {
                this.applyParticleEmotion(33);
                break;
            }
            case 2: {
                this.applyParticleEmotion(17);
                break;
            }
            case 3: {
                this.applyParticleEmotion(7);
                break;
            }
            case 4: {
                this.applyParticleEmotion(9);
                break;
            }
            case 5: {
                this.applyParticleEmotion(1);
                break;
            }
            case 7: {
                this.applyParticleEmotion(16);
                break;
            }
            case 8: {
                this.applyParticleEmotion(14);
                break;
            }
            case 10: {
                this.applyParticleEmotion(18);
                break;
            }
            default: {
                this.applyParticleEmotion(4);
            }
        }
    }

    protected void reactionDamaged() {
        switch (this.rand.nextInt(15)) {
            case 1: {
                this.applyParticleEmotion(4);
                break;
            }
            case 2: {
                this.applyParticleEmotion(5);
                break;
            }
            case 3: {
                this.applyParticleEmotion(2);
                break;
            }
            case 4: {
                this.applyParticleEmotion(3);
                break;
            }
            case 5: {
                this.applyParticleEmotion(8);
                break;
            }
            case 7: {
                this.applyParticleEmotion(10);
                break;
            }
            case 8: {
                this.applyParticleEmotion(0);
                break;
            }
            default: {
                this.applyParticleEmotion(6);
            }
        }
    }

    @Override
    public float getSwingTime(float partialTick) {
        return this.getSwingProgress(partialTick);
    }

    protected void updateArmSwingProgress() {
        int swingMaxTick = 6;
        if (this.isSwingInProgress) {
            ++this.swingProgressInt;
            if (this.swingProgressInt >= swingMaxTick) {
                this.swingProgressInt = 0;
                this.isSwingInProgress = false;
            }
        } else {
            this.swingProgressInt = 0;
        }
        this.swingProgress = (float)this.swingProgressInt / (float)swingMaxTick;
    }

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
        return this.shipAttrs.getAttackDamage() * 0.125f;
    }

    public void applySoundAtAttacker(int type) {
        switch (type) {
            case 1: {
                this.playSound(ModSounds.SHIP_FIRELIGHT, ConfigHandler.volumeFire, this.getSoundPitch() * 0.85f);
                if (this.rand.nextInt(10) <= 7) break;
                this.playSound(ModSounds.SHIP_HIT, this.getSoundVolume(), this.getSoundPitch());
                break;
            }
            case 2: {
                this.playSound(ModSounds.SHIP_FIREHEAVY, ConfigHandler.volumeFire, this.getSoundPitch() * 0.85f);
                if (this.rand.nextInt(10) <= 7) break;
                this.playSound(ModSounds.SHIP_HIT, this.getSoundVolume(), this.getSoundPitch());
                break;
            }
            case 3:
            case 4: {
                this.playSound(SoundEvents.ENTITY_ARROW_SHOOT, ConfigHandler.volumeFire + 0.2f, 1.0f / (this.rand.nextFloat() * 0.4f + 1.2f) + 0.5f);
                break;
            }
            default: {
                if (this.getRNG().nextInt(2) != 0) break;
                this.playSound(ModSounds.SHIP_HIT, this.getSoundVolume(), this.getSoundPitch());
            }
        }
    }

    public void applyParticleAtAttacker(int type, Dist4d distVec) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        if (type == 1) {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 6, this.posX, this.posY, this.posZ, distVec.x, distVec.y, distVec.z, true), point);
        } else {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
        }
    }

    public void applySoundAtTarget(int type) {
        switch (type) {
            case 1:
            case 2:
            case 3:
            case 4: {
                break;
            }
            default:
        }
    }

    public void applyParticleAtTarget(int type, Entity target) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        switch (type) {
            case 1: {
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 9, false), point);
                break;
            }
            case 2:
            case 3:
            case 4: {
                break;
            }
            default: {
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 1, false), point);
            }
        }
    }

    public void flareTarget(Entity target) {
        if (!this.world.isRemote && this.getStateMinor(38) > 0 && target != null) {
            BlockPos pos = new BlockPos(target);
            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
            CommonProxy.channelI.sendToAllAround(new S2CReactPackets((byte)20, pos.getX(), pos.getY(), pos.getZ()), point);
        }
    }

    public short getShipClass() {
        return (short)this.getStateMinor(20);
    }

    @Override
    public int getTextureID() {
        return this.getShipClass();
    }

    @Override
    public int getRidingState() {
        return 0;
    }

    @Override
    public void setRidingState(int state) {
    }

    @Override
    public int getScaleLevel() {
        return this.scaleLevel;
    }

    public boolean isNonBoss() {
        return this.scaleLevel < 2;
    }

    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        if (this.bossInfo != null) {
            this.bossInfo.addPlayer(player);
        }
    }

    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        if (this.bossInfo != null) {
            this.bossInfo.removePlayer(player);
        }
    }

    protected void updateAITasks() {
        if (this.bossInfo != null) {
            this.bossInfo.setPercent(this.getHealth() / this.getMaxHealth());
        }
    }

    @Override
    public Random getRand() {
        return this.rand;
    }

    @Override
    public double getShipFloatingDepth() {
        return 0.3 + this.scaleLevel * 0.05;
    }

    protected void onDeathUpdate() {
        ++this.deathTime;
        if (this.world.isRemote && (this.ticksExisted & 3) == 0) {
            int maxpar = (int)((3 - ClientProxy.getMineraft().gameSettings.particleSetting) * 1.8f);
            double range = this.width * 1.2;
            for (int i = 0; i < maxpar; ++i) {
                ParticleHelper.spawnAttackParticleAt(this.posX - range + this.rand.nextDouble() * range * 2.0, this.posY + 0.3 + this.rand.nextDouble() * 0.3, this.posZ - range + this.rand.nextDouble() * range * 2.0, 1.0 + this.scaleLevel, 0.0, 0.0, (byte)43);
            }
        }
        if (this.deathTime == ConfigHandler.deathMaxTick) {
            if (!this.world.isRemote && this.canDrop && this.world.getGameRules().getBoolean("doMobLoot")) {
                this.canDrop = false;
                ItemStack bossEgg = this.getDropEgg();
                if (!bossEgg.isEmpty()) {
                    BasicEntityItem entityItem1 = new BasicEntityItem(this.world, this.posX, this.posY + 0.5, this.posZ, bossEgg);
                    this.world.spawnEntity(entityItem1);
                }
            }
            if (!this.world.isRemote && (this.isPlayer() || this.recentlyHit > 0 && this.canDropLoot() && this.world.getGameRules().getBoolean("doMobLoot"))) {
                int j;
                int i = this.getExperiencePoints(this.attackingPlayer);
                for (i = ForgeEventFactory.getExperienceDrop(this, this.attackingPlayer, i); i > 0; i -= j) {
                    j = EntityXPOrb.getXPSplit(i);
                    this.world.spawnEntity(new EntityXPOrb(this.world, this.posX, this.posY, this.posZ, j));
                }
            }
            this.setDead();
            for (int k = 0; k < 20; ++k) {
                double d2 = this.rand.nextGaussian() * 0.02;
                double d0 = this.rand.nextGaussian() * 0.02;
                double d1 = this.rand.nextGaussian() * 0.02;
                this.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, this.posX + (this.rand.nextFloat() * this.width * 2.0f) - this.width, this.posY + (this.rand.nextFloat() * this.height), this.posZ + (this.rand.nextFloat() * this.width * 2.0f) - this.width, d2, d0, d1);
            }
        } else if (this.deathTime > ConfigHandler.deathMaxTick && !this.isDead) {
            this.setDead();
        }
    }

    protected int getExperiencePoints(EntityPlayer player) {
        return 10 + this.scaleLevel * 30 + this.rand.nextInt(1 + this.scaleLevel * 30);
    }

    @Override
    public int getStateTimer(int id) {
        return 0;
    }

    @Override
    public void setStateTimer(int id, int value) {
    }

    @Override
    public HashMap<Integer, Integer> getBuffMap() {
        if (this.BuffMap == null) {
            this.BuffMap = new HashMap<>();
        }
        return this.BuffMap;
    }

    @Override
    public HashMap<Integer, int[]> getAttackEffectMap() {
        if (this.AttackEffectMap == null) {
            this.AttackEffectMap = new HashMap<>();
        }
        return this.AttackEffectMap;
    }

    @Override
    public void setBuffMap(HashMap<Integer, Integer> map) {
        if (map != null) {
            this.BuffMap = map;
        }
    }

    @Override
    public Attrs getAttrs() {
        return this.shipAttrs;
    }

    @Override
    public void setAttrs(Attrs data) {
        this.shipAttrs = data;
    }

    public void heal(float healAmount) {
        if (!this.world.isRemote) {
            NetworkRegistry.TargetPoint tp = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 48.0);
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 23, 0.0, 0.1, 0.0), tp);
            healAmount = BuffHelper.applyBuffOnHeal(this, healAmount);
        }
        super.heal(healAmount * this.getAttrs().getAttrsBuffed(19));
    }

    @Override
    public void setUpdateFlag(int id, boolean value) {
    }

    @Override
    public boolean getUpdateFlag(int id) {
        return false;
    }

    @Override
    public MissileData getMissileData(int type) {
        return this.MissileData;
    }

    @Override
    public boolean isMorph() {
        return this.isMorph;
    }

    @Override
    public void setIsMorph(boolean par1) {
        this.isMorph = par1;
    }

    @Override
    public EntityPlayer getMorphHost() {
        return this.morphHost;
    }

    @Override
    public void setMorphHost(EntityPlayer player) {
        this.morphHost = player;
    }
}
