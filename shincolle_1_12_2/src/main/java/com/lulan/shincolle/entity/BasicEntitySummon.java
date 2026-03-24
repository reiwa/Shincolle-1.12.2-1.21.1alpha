package com.lulan.shincolle.entity;

import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.client.render.IShipCustomTexture;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.C2SInputPackets;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.utility.BuffHelper;
import com.lulan.shincolle.utility.CombatHelper;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.HashMap;
import java.util.Random;

public abstract class BasicEntitySummon
extends EntityLiving
implements IShipCannonAttack,
IShipCustomTexture,
IShipMorph {
    protected static final IAttribute MAX_HP = new RangedAttribute(null, "generic.maxHealth", 4.0, 0.0, 30000.0).setDescription("Max Health").setShouldWatch(true);
    protected Attrs shipAttrs;
    protected IShipAttackBase host;
    protected ShipPathNavigate shipNavigator;
    protected ShipMoveHelper shipMoveHelper;
    protected Entity atkTarget;
    protected Entity rvgTarget;
    protected int numAmmoLight;
    protected int numAmmoHeavy;
    protected int revengeTime;
    protected int stateEmotion;
    protected int stateEmotion2;
    protected int scaleLevel;
    protected int startEmotion;
    protected int startEmotion2;
    protected int attackTime;
    protected int attackTime2;
    protected boolean headTilt;
    public boolean initScale;
    protected boolean isMorph = false;
    protected EntityPlayer morphHost;

    protected BasicEntitySummon(World world) {
        super(world);
        this.maxHurtResistantTime = 2;
        this.numAmmoLight = 6;
        this.numAmmoHeavy = 0;
        this.stateEmotion = 0;
        this.stateEmotion2 = 0;
        this.startEmotion = 0;
        this.startEmotion2 = 0;
        this.headTilt = false;
        this.isImmuneToFire = true;
        this.initScale = false;
        this.shipAttrs = new Attrs();
        this.scaleLevel = 0;
    }

    public abstract void initAttrs(IShipAttackBase var1, Entity var2, int var3, float ... var4);

    protected abstract void setSizeWithScaleLevel();

    protected abstract void setAttrsWithScaleLevel();

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

    public IAttributeInstance getEntityAttribute(IAttribute attribute) {
        if (attribute == SharedMonsterAttributes.MAX_HEALTH) {
            this.getAttributeMap().getAttributeInstance(MAX_HP);
        }
        return this.getAttributeMap().getAttributeInstance(attribute);
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

    protected abstract void setAIList();

    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.scaleLevel = nbt.getByte("scaleLV");
        float hp = this.getHealth();
        this.setScaleLevel(this.scaleLevel);
        this.setHealth(hp);
    }

    public void sendSyncPacket(int type) {
        if (!this.world.isRemote) {
            byte pid;
            if(type == 0){
                pid = 5;
            } else {
                return;
            }
            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
            CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, pid), point);
        }
    }

    public void onUpdate() {
        super.onUpdate();
        if (this.attackTime > 0) {
            --this.attackTime;
        }
        if (!this.world.isRemote) {
            if (this.ticksExisted == 1) {
                this.sendSyncPacket(0);
            }
            boolean hostIsDead = this.host == null || (this.host instanceof EntityLivingBase && !((EntityLivingBase)this.host).isEntityAlive());
            if (hostIsDead || this.ticksExisted > this.getLifeLength()) {
                if (this.host != null) this.returnSummonResource();
                this.setDead();
                return;
            }
            if (!this.canFindTarget() && (this.getEntityTarget() == null || this.getEntityTarget().isDead)) {
                Entity hostTarget = this.host.getEntityTarget();
                if (hostTarget != null && hostTarget.isEntityAlive()) {
                    this.setEntityTarget(hostTarget);
                } else {
                    this.returnSummonResource();
                    this.setDead();
                    return;
                }
            }
        } else if (!this.initScale) {
            CommonProxy.channelI.sendToServer(new C2SInputPackets((byte)5, this.getEntityId(), this.world.provider.getDimension()));
        }
        if ((this.ticksExisted & 0x7F) == 0) {
            this.setAir(300);
        }
    }

    public void onLivingUpdate() {
        if (!this.world.isRemote) {
            EntityHelper.updateShipNavigator(this);
            super.onLivingUpdate();
        } else {
            super.onLivingUpdate();
        }
    }

    protected abstract void returnSummonResource();

    @Override
    public int getStateEmotion(int id) {
        return id == 1 ? this.stateEmotion : this.stateEmotion2;
    }

    @Override
    public void setStateEmotion(int id, int value, boolean sync) {
        if(id == 1){
            this.stateEmotion = value;
        } else if(id == 2){
            this.stateEmotion2 = value;
        }
        if (sync && !this.world.isRemote) {
            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32.0);
            CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, (byte)50), point);
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

    @Override
    public int getAttackTick() {
        return this.attackTime;
    }

    @Override
    public int getAttackTick2() {
        return this.attackTime2;
    }

    protected void clearAITasks() {
        this.tasks.taskEntries.clear();
    }

    protected void clearAITargetTasks() {
        this.targetTasks.taskEntries.clear();
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
    public float getMoveSpeed() {
        return this.shipAttrs.getMoveSpeed();
    }

    @Override
    public float getJumpSpeed() {
        return 1.0f;
    }

    @Override
    public int getAmmoLight() {
        return this.numAmmoLight;
    }

    @Override
    public int getAmmoHeavy() {
        return this.numAmmoHeavy;
    }

    @Override
    public boolean hasAmmoLight() {
        return this.numAmmoLight > 0;
    }

    @Override
    public boolean hasAmmoHeavy() {
        return this.numAmmoHeavy > 0;
    }

    @Override
    public void setAmmoLight(int num) {
        this.numAmmoLight = num;
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
    public boolean getIsLeashed() {
        return false;
    }

    @Override
    public int getLevel() {
        if (this.host != null) {
            return this.host.getLevel();
        }
        return 150;
    }

    @Override
    public int getStateMinor(int id) {
        return 0;
    }

    @Override
    public void setStateMinor(int state, int par1) {
    }

    @Override
    public void setEntitySit(boolean sit) {
    }

    @Override
    public boolean getAttackType(int par1) {
        return true;
    }

    @Override
    public Entity getHostEntity() {
        return (Entity)this.host;
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
    public void setAttackTick(int par1) {
        this.attackTime = par1;
    }

    @Override
    public void setAttackTick2(int par1) {
        this.attackTime2 = par1;
    }

    @Override
    public float getSwingTime(float partialTick) {
        return 0.0f;
    }

    @Override
    public boolean isJumping() {
        return this.isJumping;
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

    public abstract float getAttackBaseDamage(int var1, Entity var2);

    public boolean attackEntityFrom(DamageSource source, float atk) {
        if (this.world.isRemote) {
            return false;
        }
        if (this.host == null) {
            this.setDead();
            return false;
        }
        boolean checkDEF = true;
        if (source == DamageSource.IN_WALL || source == DamageSource.STARVE || source == DamageSource.CACTUS || source == DamageSource.FALL) {
            return false;
        }
        if (source == DamageSource.MAGIC || source == DamageSource.WITHER || source == DamageSource.DRAGON_BREATH) {
            checkDEF = false;
        } else if (source == DamageSource.OUT_OF_WORLD) {
            this.returnSummonResource();
            this.setDead();
            return false;
        }
        if (this.getStateEmotion(1) != 3) {
            this.setStateEmotion(1, 3, true);
        }
        if (source.getTrueSource() instanceof EntityPlayer && TeamHelper.checkSameOwner(source.getTrueSource(), this)) {
            return super.attackEntityFrom(source, atk);
        }
        if (this.isEntityInvulnerable(source)) {
            return false;
        }
        if (source.getTrueSource() != null) {
            Entity attacker = source.getTrueSource();
            if (attacker.equals(this)) {
                return false;
            }
            if (this.getPlayerUID() > 0 && attacker instanceof EntityPlayer && !ConfigHandler.friendlyFire) {
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
            if (this.getPlayerUID() > 0 && attacker instanceof IShipOwner && ((IShipOwner)attacker).getPlayerUID() > 0 && (attacker instanceof BasicEntityShip || attacker instanceof BasicEntitySummon || attacker instanceof BasicEntityMount)) {
                reducedAtk = reducedAtk * ConfigHandler.dmgSvS * 0.01f;
            }
            if ((reducedAtk = BuffHelper.applyBuffOnDamageByLight(this, source, reducedAtk)) < 1.0f && reducedAtk > 0.0f) {
                reducedAtk = 1.0f;
            } else if (reducedAtk <= 0.0f) {
                reducedAtk = 0.0f;
            }
            return super.attackEntityFrom(source, reducedAtk);
        }
        return false;
    }

    @Override
    public boolean updateSkillAttack(Entity target) {
        return false;
    }

    public void applySoundAtAttacker(int type) {
        switch (type) {
            case 1: {
                this.playSound(ModSounds.SHIP_FIRELIGHT, ConfigHandler.volumeFire, this.getSoundPitch() * 0.85f);
                break;
            }
            case 2: {
                this.playSound(ModSounds.SHIP_FIREHEAVY, ConfigHandler.volumeFire, this.getSoundPitch() * 0.85f);
                break;
            }
            case 3: 
            case 4: {
                this.playSound(ModSounds.SHIP_AIRCRAFT, ConfigHandler.volumeFire * 0.5f, this.getSoundPitch() * 0.85f);
                break;
            }
            default:
        }
    }

    public void applyParticleAtAttacker(int type, Dist4d distVec) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        if(type == 1){
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
            case 2: {
                break;
            }
            case 3: {
                break;
            }
            case 4: {
                break;
            }
            default: {
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 1, false), point);
            }
        }
    }

    protected int getLifeLength() {
        return 1200;
    }

    protected boolean canFindTarget() {
        return false;
    }

    @Override
    public Random getRand() {
        return this.rand;
    }

    @Override
    public double getShipDepth(int type) {
        return 0.0;
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
        if (this.host != null) {
            return this.host.getBuffMap();
        }
        return new HashMap<Integer, Integer>();
    }

    @Override
    public void setBuffMap(HashMap<Integer, Integer> map) {
    }

    public void heal(float healAmount) {
        if (!this.world.isRemote) {
            NetworkRegistry.TargetPoint tp = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 48.0);
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 23, 0.0, 0.1, 0.0), tp);
            healAmount = BuffHelper.applyBuffOnHeal(this, healAmount);
        }
        super.heal(healAmount * this.shipAttrs.getAttrsBuffed(19));
    }

    @Override
    public Attrs getAttrs() {
        return this.shipAttrs;
    }

    @Override
    public void setAttrs(Attrs data) {
        this.shipAttrs = data;
    }

    @Override
    public void setUpdateFlag(int id, boolean value) {
    }

    @Override
    public boolean getUpdateFlag(int id) {
        return false;
    }

    @Override
    public HashMap<Integer, int[]> getAttackEffectMap() {
        if (this.host != null) {
            return this.host.getAttackEffectMap();
        }
        return new HashMap<Integer, int[]>();
    }

    @Override
    public MissileData getMissileData(int type) {
        if (this.host != null) {
            return this.host.getMissileData(type);
        }
        return new MissileData();
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
