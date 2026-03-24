package com.lulan.shincolle.entity;

import com.lulan.shincolle.ShinColle;
import com.lulan.shincolle.ai.*;
import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.capability.CapaShipInventory;
import com.lulan.shincolle.capability.CapaShipSavedValues;
import com.lulan.shincolle.client.render.IShipCustomTexture;
import com.lulan.shincolle.config.ConfigSound;
import com.lulan.shincolle.crafting.EquipCalc;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.entity.other.EntityShipFishingHook;
import com.lulan.shincolle.entity.transport.EntityTransportWa;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.intermod.MetamorphHelper;
import com.lulan.shincolle.item.BasicEntityItem;
import com.lulan.shincolle.network.*;
import com.lulan.shincolle.proxy.ClientProxy;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.proxy.ServerProxy;
import com.lulan.shincolle.reference.Enums;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.reference.unitclass.AttrsAdv;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.server.CacheDataShip;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;

import javax.annotation.Nullable;
import java.util.*;

public abstract class BasicEntityShip extends EntityTameable implements IShipCannonAttack, IShipGuardian, IShipFloating, IShipCustomTexture, IShipMorph {

    protected static final IAttribute MAX_HP = new RangedAttribute(null, "generic.maxHealth", 4.0, 0.0, 30000.0).setDescription("Max Health").setShouldWatch(true);
    private static final Set<Integer> MAJOR_BODY_PARTS = new HashSet<>(Arrays.asList(0, 1, 2, 4));

    protected CapaShipInventory itemHandler;
    protected ShipPathNavigate shipNavigator;
    protected ShipMoveHelper shipMoveHelper;
    protected EntityLivingBase aiTarget;
    protected Entity guardedEntity;
    protected Entity atkTarget;
    protected Entity rvgTarget;
    public EntityShipFishingHook fishHook;
    protected double ShipDepth;
    protected double ShipPrevX;
    protected double ShipPrevY;
    protected double ShipPrevZ;
    protected AttrsAdv shipAttrs;
    protected int[] StateMinor;
    protected int[] StateTimer;
    protected int[] StateEmotion;
    protected boolean[] StateFlag;
    protected byte[] BodyHeightStand;
    protected byte[] BodyHeightSit;
    protected float[] ModelPos;
    protected boolean[] UpdateFlag;
    protected BlockPos[] waypoints;
    public String ownerName;
    public List<String> unitNames;
    protected HashMap<Integer, Integer> BuffMap;
    protected HashMap<Integer, int[]> AttackEffectMap;
    protected MissileData[] MissileData;
    protected float[] rotateAngle;
    private boolean initAI;
    private boolean initWaitAI;
    private boolean isUpdated;
    private int updateTime = 16;
    private ForgeChunkManager.Ticket chunkTicket;
    private Set<ChunkPos> chunks;
    public static boolean stopAI = false;
    protected boolean isMorph = false;
    protected EntityPlayer morphHost;

    protected BasicEntityShip(World world) {
        super(world);
        this.ignoreFrustumCheck = true;
        this.maxHurtResistantTime = 2;
        this.ownerName = "";
        this.unitNames = new ArrayList<>();
        this.itemHandler = new CapaShipInventory(60, this);
        this.isImmuneToFire = true;
        this.StateMinor = new int[]{1, 0, 0, 40, 0, 0, 0, 0, 0, 3, 3, 12, 35, 1, -1, -1, -1, 0, -1, 0, 0, -1, -1, -1, 0, 0, 0, 0, 0, 0, 60, 0, 10, 0, 0, -1, 0, 0, 0, 0, -1, -1, -1, 0, 0, -1};
        this.StateTimer = new int[21];
        this.StateEmotion = new int[8];
        this.StateFlag = new boolean[]{false, false, false, false, true, true, true, true, false, true, true, false, true, true, true, true, true, true, true, false, false, false, true, true, false, true, false, false};
        this.UpdateFlag = new boolean[8];
        this.BodyHeightStand = new byte[]{92, 78, 73, 58, 47, 37};
        this.BodyHeightSit = new byte[]{64, 49, 44, 29, 23, 12};
        this.ModelPos = new float[]{0.0f, 0.0f, 0.0f, 50.0f};
        this.waypoints = new BlockPos[]{BlockPos.ORIGIN};
        this.BuffMap = new HashMap<>();
        this.resetMissileData();
        this.ShipDepth = 0.0;
        this.ShipPrevX = this.posX;
        this.ShipPrevY = this.posY;
        this.ShipPrevZ = this.posZ;
        this.stepHeight = 1.0f;
        this.rotateAngle = new float[3];
        this.initAI = false;
        this.initWaitAI = false;
        this.isUpdated = false;
        this.chunkTicket = null;
        this.chunks = null;
        this.randomSensitiveBody();
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public boolean isEntityInvulnerable(DamageSource source) {
        return this.StateTimer[2] > 0;
    }

    @Override
    public boolean isBurning() {
        return this.getStateEmotion(3) == 3;
    }

    @Override
    public boolean isJumping() {
        return this.isJumping;
    }

    @Override
    public float getEyeHeight() {
        return this.height * 0.8f;
    }

    protected void postInit() {
        this.shipNavigator = new ShipPathNavigate(this);
        this.shipMoveHelper = new ShipMoveHelper(this, 60.0f);
        this.shipAttrs = new AttrsAdv(this.getShipClass());
    }

    @Nullable
    public static SoundEvent getCustomSound(int type, BasicEntityShip ship) {
        int key = ship.getShipClass() + 2;
        float[] rate = ConfigSound.SOUNDRATE.get(key);
        if (rate != null) {
            int soundType = (type >= 10 && type <= 33) ? 8 : type;
            if (rate[soundType] > 0.01f && ship.rand.nextFloat() < rate[soundType]) {
                SoundEvent sound = ModSounds.CUSTOM_SOUND.get(key * 100 + type);
                if (sound != null) return sound;
            }
        }
        switch (type) {
            case 0: return ModSounds.SHIP_IDLE;
            case 1: return ModSounds.SHIP_HIT;
            case 2: return ModSounds.SHIP_HURT;
            case 3: return ModSounds.SHIP_DEATH;
            case 4: return ModSounds.SHIP_MARRY;
            case 5: return ModSounds.SHIP_KNOCKBACK;
            case 6: return ModSounds.SHIP_ITEM;
            case 7: return ModSounds.SHIP_FEED;
            case 10: return ModSounds.SHIP_TIME0;
            case 11: return ModSounds.SHIP_TIME1;
            case 12: return ModSounds.SHIP_TIME2;
            case 13: return ModSounds.SHIP_TIME3;
            case 14: return ModSounds.SHIP_TIME4;
            case 15: return ModSounds.SHIP_TIME5;
            case 16: return ModSounds.SHIP_TIME6;
            case 17: return ModSounds.SHIP_TIME7;
            case 18: return ModSounds.SHIP_TIME8;
            case 19: return ModSounds.SHIP_TIME9;
            case 20: return ModSounds.SHIP_TIME10;
            case 21: return ModSounds.SHIP_TIME11;
            case 22: return ModSounds.SHIP_TIME12;
            case 23: return ModSounds.SHIP_TIME13;
            case 24: return ModSounds.SHIP_TIME14;
            case 25: return ModSounds.SHIP_TIME15;
            case 26: return ModSounds.SHIP_TIME16;
            case 27: return ModSounds.SHIP_TIME17;
            case 28: return ModSounds.SHIP_TIME18;
            case 29: return ModSounds.SHIP_TIME19;
            case 30: return ModSounds.SHIP_TIME20;
            case 31: return ModSounds.SHIP_TIME21;
            case 32: return ModSounds.SHIP_TIME22;
            case 33: return ModSounds.SHIP_TIME23;
            default: return null;
        }
    }

    @Override
    protected float getSoundVolume() {
        return ConfigHandler.volumeShip;
    }

    @Override
    protected float getSoundPitch() {
        return (this.rand.nextFloat() - this.rand.nextFloat()) * 0.1f + 1.0f;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return BasicEntityShip.getCustomSound(0, this);
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return BasicEntityShip.getCustomSound(2, this);
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return BasicEntityShip.getCustomSound(3, this);
    }

    @Override
    public void playLivingSound() {
        if (this.getStateFlag(2) || this.rand.nextInt(10) > 3) {
            return;
        }
        SoundEvent sound = this.getStateFlag(1) && this.rand.nextInt(5) == 0 ?
                BasicEntityShip.getCustomSound(4, this) : this.getAmbientSound();
        if (sound != null) {
            this.playSound(sound, this.getSoundVolume(), this.getSoundPitch());
        }
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        if (this.StateTimer[6] <= 0) {
            this.StateTimer[6] = 20 + this.getRNG().nextInt(30);
            super.playHurtSound(source);
        }
    }

    protected void playTimeSound(int hour) {
        SoundEvent sound = BasicEntityShip.getCustomSound(hour + 10, this);
        if (sound != null) {
            this.playSound(sound, this.getSoundVolume(), (this.rand.nextFloat() - this.rand.nextFloat()) * 0.1f + 1.0f);
        }
    }

    @Override
    public EntityAgeable createChild(EntityAgeable entity) {
        return null;
    }

    protected void setAIList() {
        this.tasks.addTask(1, new EntityAIShipSit(this));
        this.tasks.addTask(2, new EntityAIFollowFleetController(this));
        this.tasks.addTask(3, new EntityAIShipClimbLadder(this));
        this.tasks.addTask(4, new EntityAIShipFlee(this));
        this.tasks.addTask(5, new EntityAIShipGuarding(this));
        this.tasks.addTask(6, new EntityAIShipFollowOwner(this));
        this.tasks.addTask(7, new EntityAIShipOpenDoor(this, true));
        if (this.getStateFlag(3)) {
            this.tasks.addTask(15, new EntityAIShipAttackOnCollide(this, 1.0));
        }
        this.tasks.addTask(23, new EntityAIShipFloating(this));
        this.tasks.addTask(24, new EntityAIShipGetOffChair(this));
        this.tasks.addTask(25, new EntityAIShipSitOnChair(this, 1.0f));
        this.tasks.addTask(26, new EntityAIShipWander(this, 10, 5, 0.8));
        this.tasks.addTask(27, new EntityAIShipWatchClosest(this, EntityPlayer.class, 4.0f, 0.06f));
        this.tasks.addTask(28, new EntityAIShipLookIdle(this));
    }

    public void setAITargetList() {
        this.targetTasks.addTask(1, new EntityAIShipRevengeTarget(this));
        if (!this.getStateFlag(21)) {
            this.targetTasks.addTask(5, new EntityAIShipRangeTarget(this, Entity.class));
        }
    }

    protected void clearAITasks() {
        this.tasks.taskEntries.clear();
    }

    protected void clearAITargetTasks() {
        this.setAttackTarget(null);
        this.setEntityTarget(null);
        this.targetTasks.taskEntries.clear();
    }

    public void onFleetLeaderSpawned(int type) {
        for (EntityAITasks.EntityAITaskEntry taskEntry : this.tasks.taskEntries) {
            if (taskEntry.action instanceof EntityAIFollowFleetController) {
                ((EntityAIFollowFleetController) taskEntry.action).resetFindCooldown(type);
                break;
            }
        }
    }

    @Override
    public boolean writeToNBTOptional(NBTTagCompound nbt) {
        if (this.isRiding() && this.dimension == this.getGuardedPos(3) && this.getGuardedPos(1) > 0) {
            double guardX = this.getGuardedPos(0);
            double guardY = this.getGuardedPos(1);
            double guardZ = this.getGuardedPos(2);
            if (this.getDistanceSq(guardX, guardY, guardZ) > 256.0) {
                this.dismountRidingEntity();
                this.setPosition(guardX + 0.5, guardY + 0.5, guardZ + 0.5);
                if (!this.world.isRemote) {
                    this.sendSyncPacket((byte) 52, true);
                }
            }
        }

        if (this.getRidingEntity() instanceof EntityPlayer) {
            this.dismountRidingEntity();
            this.sendSyncPacketRiders();
        }
        return super.writeToNBTOptional(nbt);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        CapaShipSavedValues.saveNBTData(compound, this);
        compound.setTag("CpInv", this.itemHandler.serializeNBT());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        CapaShipSavedValues.loadNBTData(compound, this);
        if (compound.hasKey("CpInv")) {
            this.itemHandler.deserializeNBT(compound.getCompoundTag("CpInv"));
        }

        if (!this.world.isRemote && this.ticksExisted <= 0 && compound.hasKey("Passengers", Constants.NBT.TAG_LIST)) {
            NBTTagList list = compound.getTagList("Passengers", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound rider = list.getCompoundTagAt(i);
                NBTTagList pos = rider.getTagList("Pos", Constants.NBT.TAG_DOUBLE);
                pos.set(0, new NBTTagDouble(this.posX));
                pos.set(1, new NBTTagDouble(this.posY));
                pos.set(2, new NBTTagDouble(this.posZ));
            }
        }
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
    public EntityLivingBase getAttackTarget() {
        return this.aiTarget;
    }

    public abstract int getEquipType();

    @Override
    public int getLevel() {
        return this.StateMinor[0];
    }

    public byte getShipType() {
        return (byte) this.getStateMinor(19);
    }

    public short getShipClass() {
        return (short) this.getStateMinor(20);
    }

    public int getShipUID() {
        return this.getStateMinor(22);
    }

    public int getMorale() {
        return this.StateMinor[30];
    }

    @Override
    public int getPlayerUID() {
        return this.getStateMinor(21);
    }

    @Override
    public int getAmmoLight() {
        return this.StateMinor[4];
    }

    @Override
    public int getAmmoHeavy() {
        return this.StateMinor[5];
    }

    @Override
    public int getFaceTick() {
        return this.StateTimer[7];
    }

    @Override
    public int getHeadTiltTick() {
        return this.StateTimer[8];
    }

    @Override
    public int getAttackTick() {
        return this.StateTimer[12];
    }

    @Override
    public int getAttackTick2() {
        return this.StateTimer[13];
    }

    public int getEmotesTick() {
        return this.StateTimer[10];
    }

    public int getCombatTick() {
        return this.StateTimer[11];
    }

    public int getHitHeight() {
        return this.StateMinor[33];
    }

    public int getHitAngle() {
        return this.StateMinor[34];
    }

    @Override
    public int getTickExisted() {
        return this.ticksExisted;
    }

    @Override
    public float getSwingTime(float partialTick) {
        return this.getSwingProgress(partialTick);
    }

    @Override
    public boolean getIsRiding() {
        return this.isRiding();
    }

    @Override
    public boolean getIsLeashed() {
        return this.getLeashed();
    }

    @Override
    public boolean getIsSprinting() {
        return this.isSprinting() || this.limbSwingAmount > 0.9f;
    }

    @Override
    public boolean getIsSitting() {
        return this.isSitting();
    }

    @Override
    public boolean getIsSneaking() {
        return this.isSneaking();
    }

    @Override
    public Entity getEntityTarget() {
        return this.atkTarget;
    }

    @Override
    public Entity getEntityRevengeTarget() {
        return this.rvgTarget;
    }

    @Override
    public int getEntityRevengeTime() {
        return this.StateTimer[0];
    }

    @Override
    public boolean getAttackType(int par1) {
        return this.getStateFlag(par1);
    }

    public float getAIMoveSpeed() {
        return this.getMoveSpeed();
    }

    @Override
    public float getMoveSpeed() {
        return this.shipAttrs.getMoveSpeed(false);
    }

    @Override
    public float getJumpSpeed() {
        return 1.0f;
    }

    @Override
    public boolean hasAmmoLight() {
        return this.StateMinor[4] >= this.StateMinor[29];
    }

    @Override
    public boolean hasAmmoHeavy() {
        return this.StateMinor[5] >= this.StateMinor[29];
    }

    @Override
    public boolean useAmmoLight() {
        return this.StateFlag[4];
    }

    @Override
    public boolean useAmmoHeavy() {
        return this.StateFlag[5];
    }

    @Override
    public double getShipDepth() {
        return this.ShipDepth;
    }

    @Override
    public double getShipDepth(int type) {
        if (type == 1) {
            if (this.getRidingEntity() instanceof IShipEmotion) {
                return ((IShipEmotion) this.getRidingEntity()).getShipDepth(0);
            }
            return this.ShipDepth;
        } else if (type == 2) {
            return 0.0;
        }
        return this.ShipDepth;
    }

    @Override
    public boolean getStateFlag(int flag) {
        if (flag == 2 && (this.isDead || this.deathTime > 0)) {
            return true;
        }
        return this.StateFlag[flag];
    }

    public byte getStateFlagI(int flag) {
        return this.StateFlag[flag] ? (byte)1 : (byte)0;
    }

    @Override
    public int getStateMinor(int id) {
        return this.StateMinor[id];
    }

    @Override
    public boolean getUpdateFlag(int id) {
        return this.UpdateFlag[id];
    }

    @Override
    public int getStateTimer(int id) {
        return this.StateTimer[id];
    }

    @Override
    public int getStateEmotion(int id) {
        return this.StateEmotion[id];
    }

    public float[] getModelPos() {
        return this.ModelPos;
    }

    public int getGrudgeConsumption() {
        return this.getStateMinor(28);
    }

    public int getAmmoConsumption() {
        return this.getStateMinor(29);
    }

    public int getFoodSaturation() {
        return this.getStateMinor(31);
    }

    public int getFoodSaturationMax() {
        return this.getStateMinor(32);
    }

    public CapaShipInventory getCapaShipInventory() {
        return this.itemHandler;
    }

    public void calcShipAttributes(int flag, boolean sync) {
        if (this.shipAttrs == null) {
            this.shipAttrs = new AttrsAdv(this.getShipClass());
        }
        if (!this.world.isRemote) {
            if (this.isMorph) {
                if ((flag & 1) == 1) {
                    BuffHelper.updateAttrsRaw(this.shipAttrs, this.getShipClass(), this.getLevel());
                    this.calcShipAttributesAddRaw();
                    this.setUpdateFlag(7, true);
                    this.setUpdateFlag(2, true);
                }
                if ((flag & 2) == 2) {
                    EquipCalc.updateAttrsEquipOfMorph(this);
                    this.calcShipAttributesAddEquip();
                    this.setUpdateFlag(3, true);
                }
                if ((flag & 8) == 8) {
                    BuffHelper.updateBuffPotion(this);
                    this.setUpdateFlag(5, true);
                }
                BuffHelper.applyBuffOnAttrs(this);
                this.calcShipAttributesAdd();
                this.setUpdateFlag(1, true);
            } else {
                if ((flag & 1) == 1) {
                    BuffHelper.updateAttrsRaw(this.shipAttrs, this.getShipClass(), this.getLevel());
                    this.calcShipAttributesAddRaw();
                    this.setUpdateFlag(7, true);
                }
                if ((flag & 2) == 2) {
                    EquipCalc.updateAttrsEquip(this);
                    this.calcShipAttributesAddEquip();
                    this.setUpdateFlag(3, true);
                }
                if ((flag & 4) == 4) {
                    BuffHelper.updateBuffMorale(this.shipAttrs, this.getMorale());
                    this.setUpdateFlag(4, true);
                }
                if ((flag & 8) == 8) {
                    BuffHelper.updateBuffPotion(this);
                    this.setUpdateFlag(5, true);
                }
                if ((flag & 0x10) == 16) {
                    BuffHelper.updateBuffFormation(this);
                    this.setUpdateFlag(6, true);
                }
                BuffHelper.applyBuffOnAttrs(this);
                this.calcShipAttributesAdd();
                this.setUpdateFlag(1, true);
            }
            if (sync) {
                this.sendSyncPacketMinor();
                this.sendSyncPacketAttrs();
            }
        }
        this.getEntityAttribute(MAX_HP).setBaseValue(this.shipAttrs.getAttrsBuffed(0));
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(this.shipAttrs.getAttrsBuffed(7));
        this.getEntityAttribute(SWIM_SPEED).setBaseValue(this.shipAttrs.getAttrsBuffed(7));
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64.0);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(this.shipAttrs.getAttrsBuffed(20));
    }

    public void calcShipAttributesAdd() {}

    public void calcShipAttributesAddRaw() {}

    public void calcShipAttributesAddEquip() {}

    public void calcShipAttributesAddEffect() {
        this.AttackEffectMap = new HashMap<>();
    }

    @Override
    public IAttributeInstance getEntityAttribute(IAttribute attribute) {
        if (attribute == SharedMonsterAttributes.MAX_HEALTH) {
            return this.getAttributeMap().getAttributeInstance(MAX_HP);
        }
        return super.getEntityAttribute(attribute);
    }

    @Override
    protected void applyEntityAttributes() {
        this.getAttributeMap().registerAttribute(MAX_HP);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        this.getAttributeMap().registerAttribute(SWIM_SPEED);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ARMOR);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS);
    }

    public void setExpNext() {
        this.StateMinor[3] = this.StateMinor[0] * ConfigHandler.expMod + ConfigHandler.expMod;
    }

    public void addShipExp(int exp) {
        int capLevel = this.getStateFlag(1) ? 150 : 100;
        if (this.isMorph) {
            exp = (int) (exp * ConfigHandler.expGainPlayerSkill);
        }
        exp = (int) (exp * this.shipAttrs.getAttrsBuffed(16));
        if (this.StateMinor[0] < capLevel && this.StateMinor[0] < 150) {
            this.StateMinor[2] += exp;
            if (this.StateMinor[2] >= this.StateMinor[3]) {
                if (this.rand.nextInt(4) == 0) {
                    this.world.playSound(null, this.posX, this.posY, this.posZ, SoundEvents.ENTITY_PLAYER_LEVELUP, this.getSoundCategory(), 0.75f, 1.0f);
                } else {
                    this.playSound(ModSounds.SHIP_LEVEL, 0.75f, 1.0f);
                }
                this.StateMinor[2] -= this.StateMinor[3];
                this.StateMinor[0] += 1;
                this.setExpNext();
                this.setShipLevel(this.StateMinor[0], true);
            }
        }
    }

    @Override
    public void setShipDepth(double par1) {
        this.ShipDepth = par1;
    }

    public void setShipLevel(int par1, boolean update) {
        if (par1 < 151) {
            this.StateMinor[0] = par1;
        }
        if (update) {
            this.calcShipAttributes(31, true);
            this.setHealth(this.getMaxHealth());
        }
    }

    @Override
    public void setCustomNameTag(String str) {}

    public void setNameTag(String str) {
        super.setCustomNameTag(str);
    }

    public void setAITarget(EntityLivingBase target) {
        this.aiTarget = target;
    }

    public void addKills() {
        this.StateMinor[1]++;
    }

    public void addMorale(int value) {
        this.StateMinor[30] = MathHelper.clamp(this.StateMinor[30] + value, 0, 16000);
    }

    public void addAmmoLight(int value) {
        this.StateMinor[4] = Math.max(0, this.StateMinor[4] + value);
    }

    public void addAmmoHeavy(int value) {
        this.StateMinor[5] = Math.max(0, this.StateMinor[5] + value);
    }

    public void setMorale(int value) {
        this.StateMinor[30] = value;
    }

    @Override
    public void setAmmoLight(int num) {
        this.StateMinor[4] = num;
    }

    @Override
    public void setStateMinor(int id, int par1) {
        if (id == 30 && par1 < 0) {
            par1 = 0;
        } else if (id == 43 && par1 > 0) {
            if (this.getStateTimer(3) > 0) {
                return;
            }
            this.setStateTimer(3, 20);
        }
        this.StateMinor[id] = par1;
    }

    @Override
    public void setUpdateFlag(int id, boolean par1) {
        this.UpdateFlag[id] = par1;
    }

    @Override
    public void setEntitySit(boolean sit) {
        this.setSitting(sit);
        if (sit) {
            this.isJumping = false;
            this.getShipNavigate().clearPathEntity();
            this.getNavigator().clearPath();
            this.setAttackTarget(null);
            this.setEntityTarget(null);
        }
    }

    public void setRiderAndMountSit() {
        if (this.getRidingEntity() instanceof BasicEntityShip) {
            BasicEntityShip mountShip = (BasicEntityShip) this.getRidingEntity();
            mountShip.setEntitySit(this.isSitting());
            if (mountShip.getRidingState() > 0) {
                for (Entity r : mountShip.getPassengers()) {
                    if (r instanceof BasicEntityShip) {
                        ((BasicEntityShip) r).setEntitySit(this.isSitting());
                    }
                }
            }
        }
        for (Entity r : this.getPassengers()) {
            if (r instanceof BasicEntityShip) {
                ((BasicEntityShip) r).setEntitySit(this.isSitting());
            }
        }
    }

    @Override
    public void setStateFlag(int id, boolean par1) {
        this.StateFlag[id] = par1;
        if (!this.world.isRemote) {
            if (id == 3) {
                this.clearAITasks();
                this.setAIList();
                if (this.getRidingEntity() instanceof BasicEntityMount) {
                    ((BasicEntityMount) this.getRidingEntity()).clearAITasks();
                    ((BasicEntityMount) this.getRidingEntity()).setAIList();
                }
            } else if (id == 21) {
                this.clearAITargetTasks();
                this.setAITargetList();
            }
        }
    }

    public void setStateFlagI(int id, int par1) {
        this.setStateFlag(id, par1 > 0);
    }

    @Override
    public void setStateTimer(int id, int value) {
        this.StateTimer[id] = value;
    }

    @Override
    public void setStateEmotion(int id, int value, boolean sync) {
        this.StateEmotion[id] = value;
        if (sync && !this.world.isRemote) {
            this.sendSyncPacketEmotion();
        }
    }

    @Override
    public void setFaceTick(int par1) {
        this.StateTimer[7] = par1;
    }

    @Override
    public void setHeadTiltTick(int par1) {
        this.StateTimer[8] = par1;
    }

    @Override
    public void setAttackTick(int par1) {
        this.StateTimer[12] = par1;
    }

    @Override
    public void setAttackTick2(int par1) {
        this.StateTimer[13] = par1;
    }

    public void setEmotesTick(int par1) {
        this.StateTimer[10] = par1;
    }

    public void setCombatTick(int par1) {
        this.StateTimer[11] = par1;
    }

    public void setHitHeight(int par1) {
        this.StateMinor[33] = par1;
    }

    public void setHitAngle(int par1) {
        this.StateMinor[34] = par1;
    }

    public void setShipUID(int par1) {
        this.setStateMinor(22, par1);
    }

    @Override
    public void setPlayerUID(int par1) {
        this.setStateMinor(21, par1);
    }

    @Override
    public void setEntityTarget(Entity target) {
        this.atkTarget = target;
    }

    @Override
    public void setEntityRevengeTarget(Entity target) {
        this.rvgTarget = target;
    }

    @Override
    public void setEntityRevengeTime() {
        this.StateTimer[0] = this.ticksExisted;
    }

    public void setGrudgeConsumption(int par1) {
        this.setStateMinor(28, par1);
    }

    public void setAmmoConsumption(int par1) {
        this.setStateMinor(29, par1);
    }

    public void setFoodSaturation(int par1) {
        this.setStateMinor(31, par1);
    }

    public void setFoodSaturationMax(int par1) {
        this.setStateMinor(32, par1);
    }

    public void sendSyncPacketAll() {
        for (int i = 1; i <= 7; i++) {
            this.setUpdateFlag(i, true);
        }
        this.sendSyncPacket((byte) 0, false);
        this.sendSyncPacket((byte) 12, false);
    }

    public void sendSyncPacketAttrs() {
        this.sendSyncPacket((byte) 12, false);
    }

    public void sendSyncPacketMinor() {
        this.sendSyncPacket((byte) 3, false);
    }

    public void sendSyncPacketAllMisc() {
        this.sendSyncPacket((byte) 0, false);
    }

    public void sendSyncPacketRiders() {
        this.sendSyncPacket((byte) 4, true);
    }

    public void sendSyncPacketUnitName() {
        this.sendSyncPacket((byte) 11, true);
    }

    public void sendSyncPacketBuffMap() {
        this.sendSyncPacket((byte) 7, false);
    }

    public void sendSyncPacketGUI() {
        if (!this.world.isRemote && this.getPlayerUID() > 0) {
            EntityPlayerMP player = (EntityPlayerMP) EntityHelper.getEntityPlayerByUID(this.getPlayerUID());
            if (player != null && player.dimension == this.dimension && this.getDistance(player) < 64.0f) {
                CommonProxy.channelG.sendTo(new S2CGUIPackets(this), player);
            }
        }
    }

    public void sendSyncPacketTimer(int type) {
        switch (type) {
            case 0:
                this.sendSyncPacket((byte) 8, true);
                break;
            case 1:
                this.sendSyncPacket((byte) 13, false);
                break;
            default:
        }
    }

    public void sendSyncPacketEmotion() {
        this.sendSyncPacket((byte) 1, true);
    }

    public void sendSyncPacket(byte type, boolean sendAll) {
        if (!this.world.isRemote) {
            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
            if (sendAll) {
                CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, type), point);
            } else {
                EntityPlayerMP player = (EntityPlayerMP) EntityHelper.getEntityPlayerByUID(this.getPlayerUID());
                if (player != null && this.getDistance(player) <= 64.0f) {
                    CommonProxy.channelE.sendTo(new S2CEntitySync(this, type), player);
                }
            }
        }
    }

    public void sendSyncRequest(int type) {
        if (this.world.isRemote) {
            int entityID = this.isMorph && this.morphHost != null ? this.morphHost.getEntityId() : this.getEntityId();
            switch (type) {
                case 0:
                    CommonProxy.channelI.sendToServer(new C2SInputPackets((byte) 5, entityID, this.world.provider.getDimension()));
                    break;
                case 1:
                    CommonProxy.channelI.sendToServer(new C2SInputPackets((byte) 10, entityID, this.world.provider.getDimension()));
                    break;
                case 2:
                    CommonProxy.channelI.sendToServer(new C2SInputPackets((byte) 11, entityID, this.world.provider.getDimension()));
                    break;
                default:
            }
        }
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (hand == EnumHand.OFF_HAND || !this.isEntityAlive() || this.isMorph) {
            return false;
        }
        ItemStack stack = player.getHeldItem(hand);
        if (stack.isEmpty()) {
            if (!this.world.isRemote && TeamHelper.checkSameOwner(this, player)) {
                if (player.isSneaking()) {
                    FMLNetworkHandler.openGui(player, ShinColle.instance, 0, this.world, this.getEntityId(), 0, 0);
                } else if (EntityHelper.getPointerInUse(player) == null) {
                    this.sendSyncPacket((byte)53, true);
                    this.setEntitySit(!this.isSitting());
                    this.setRiderAndMountSit();
                }
                return true;
            }
        } else if (!this.world.isRemote) {
            if (TeamHelper.checkSameOwner(this, player)) {
                if (stack.getItem() == Items.NAME_TAG && stack.hasDisplayName()) {
                    this.setNameTag(stack.getDisplayName());
                    return super.processInteract(player, hand);
                }
                if (stack.getItem() == ModItems.BucketRepair && InteractHelper.interactBucket(this, player, stack)) return true;
                if (stack.getItem() == ModItems.OwnerPaper && player.isSneaking() && InteractHelper.interactOwnerPaper(this, player, stack)) return true;
                if (stack.getItem() == ModItems.ModernKit && InteractHelper.interactModernKit(this, player, stack)) return true;
                if (stack.getItem() == ModItems.KaitaiHammer && player.isSneaking() && InteractHelper.interactKaitaiHammer(this, player, stack)) return true;
                if (stack.getItem() == ModItems.MarriageRing && !this.getStateFlag(1) && player.isSneaking() && InteractHelper.interactWeddingRing(this, player, stack)) return true;
                if (stack.getItem() == Items.LEAD) {
                    this.getShipNavigate().clearPathEntity();
                    return super.processInteract(player, hand);
                }
                if (stack.getItem() == ModItems.TrainingBook && this.getLevel() < 150) {
                    int cap = this.getStateFlag(1) ? 150 : 100;
                    int lv = Math.min(this.getLevel() + 5 + this.rand.nextInt(6), cap);
                    this.setShipLevel(lv, true);
                    this.playSound(ModSounds.SHIP_LEVEL, 0.75f, 1.0f);
                    this.world.playSound(null, this.posX, this.posY, this.posZ, SoundEvents.ENTITY_PLAYER_LEVELUP, this.getSoundCategory(), 0.75f, 1.0f);
                    if (!player.capabilities.isCreativeMode) stack.shrink(1);
                    return true;
                }
            }
            if (stack.getItem() == ModItems.PointerItem && stack.getMetadata() > 2 && !player.isSneaking()) {
                InteractHelper.interactPointer(this, player);
                return true;
            }
            if (InteractHelper.interactFeed(this, player, stack)) return true;
            if (TeamHelper.checkSameOwner(this, player)) {
                if (player.isSneaking()) {
                    FMLNetworkHandler.openGui(player, ShinColle.instance, 0, this.world, this.getEntityId(), 0, 0);
                } else if (EntityHelper.getPointerInUse(player) == null) {
                    this.sendSyncPacket((byte)53, true);
                    this.setEntitySit(!this.isSitting());
                    this.setRiderAndMountSit();
                }
                return true;
            }
        } else if (stack.getItem() == ModItems.PointerItem && stack.getMetadata() > 2 && !player.isSneaking()) {
            this.setHitHeight(CalcHelper.getEntityHitHeightByClientPlayer(this));
            this.setHitAngle(CalcHelper.getEntityHitSideByClientPlayer(this));
            this.checkCaressed();
            CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, (byte) 35, this.getEntityId(), this.getHitHeight(), this.getHitAngle()));
        }
        return super.processInteract(player, hand);
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player) {
        return TeamHelper.checkSameOwner(this, player);
    }

    @Override
    public void travel(float strafe, float forward, float vertical) {
        EntityHelper.travel(this, strafe, forward);
    }

    @Override
    public void onUpdate() {
        if (stopAI) return;

        if (this.isMorph && this.morphHost != null) {
            this.world = this.morphHost.world;
            this.dimension = this.morphHost.dimension;
        }

        super.onUpdate();
        EntityHelper.checkDepth(this);
        this.updateArmSwingProgress();
        this.updateBothSideTimer();

        if (this.world.isRemote) {
            this.handleClientUpdate();
        }
    }

    private void handleClientUpdate() {
        if (this.getShipDepth() > 0.0 && !this.isRiding()) {
            double motX = MathHelper.clamp(this.posX - this.prevPosX, -0.25, 0.25);
            double motZ = MathHelper.clamp(this.posZ - this.prevPosZ, -0.25, 0.25);
            if (motX * motX + motZ * motZ > 0.001) {
                ParticleHelper.spawnAttackParticleAt(this.posX + motX * 3.0, this.posY + 0.4, this.posZ + motZ * 3.0, -motX, this.width, -motZ, (byte) 47);
            }
        }

        if (this.ticksExisted == 2) {
            this.sendSyncRequest(0);
        }

        this.updateClientBodyRotate();

        if (this.ticksExisted % ConfigHandler.searchlightCD == 0 && ConfigHandler.canSearchlight && this.isEntityAlive()) {
            this.updateSearchlight();
        }

        if ((this.ticksExisted & 15) == 0) {
            handleClientParticles();
        }
    }

    private void handleClientParticles() {
        if (this.getStateEmotion(3) > 0) {
            byte particleType = (byte) (this.getStateEmotion(3) == 1 ? 4 : (this.getStateEmotion(3) == 2 ? 5 : 7));
            ParticleHelper.spawnAttackParticleAt(this.posX, this.posY + 0.7, this.posZ, this.width, 0.05, 0.0, particleType);
        }
        if ((this.ticksExisted & 31) == 0 && !this.isMorph) {
            this.updateGuardedParticle();
        }
    }

    private void updateGuardedParticle() {
        EntityPlayer player = EntityHelper.getEntityPlayerByID(this.getStateMinor(23), 0, true);
        if (player == null || player.dimension != this.getGuardedPos(3)) {
            this.setStateMinor(23, -1);
            return;
        }

        ItemStack item = EntityHelper.getPointerInUse(player);
        if (item == null || item.getItemDamage() >= 3 && !ConfigHandler.alwaysShowTeamParticle) {
            return;
        }

        ParticleHelper.spawnAttackParticleAtEntity(this, 0.3, 7.0, 0.0, (byte) 2);

        Entity guarded = this.getGuardedEntity();
        if (guarded != null) {
            ParticleHelper.spawnAttackParticleAtEntity(guarded, 0.3, 6.0, 0.0, (byte) 2);
            ParticleHelper.spawnAttackParticleAtEntity(this, guarded, 0.0, 0.0, 0.0, (byte) 3, false);
        } else if (this.getGuardedPos(1) >= 0) {
            double guardX = this.getGuardedPos(0) + 0.5;
            double guardY = this.getGuardedPos(1);
            double guardZ = this.getGuardedPos(2) + 0.5;
            ParticleHelper.spawnAttackParticleAt(guardX, guardY, guardZ, 0.3, 6.0, 0.0, (byte) 25);
            ParticleHelper.spawnAttackParticleAtEntity(this, guardX, guardY + 0.2, guardZ, (byte) 8);
        }
    }

    @Override
    public void onLivingUpdate() {
        if (this.ticksExisted == 5) {
            this.rand.setSeed(((long) this.getShipUID() << 4) + System.currentTimeMillis());
            this.initAI = false;
            this.initWaitAI = false;
        }

        if (this.world.isRemote) {
            this.updateClientSide();
        } else {
            this.updateServerSidePreSuper();
            this.updateServerSidePostSuper();
        }

        super.onLivingUpdate();
        if ((this.ticksExisted & 0x7F) == 0x70) {
            this.setAir(300);
        }
    }

    private void updateServerSidePreSuper() {
        if (!this.isMorph) {
            EntityHelper.updateShipNavigator(this);
            TargetHelper.updateTarget(this);
        }
        this.updateShipCacheData(false);
    }

    private void updateServerSidePostSuper() {
        this.updateServerTimer();

        if (this.StateFlag[26] && !this.isMorph && this.isEntityAlive() && !this.isSitting() && !this.getStateFlag(2)) {
            TaskHelper.onUpdatePumping(this);
        }

        if ((this.ticksExisted & 7) == 0) {
            this.handleServerTicks();
        }

        if (!this.isMorph && ConfigHandler.timeKeeping && this.getStateFlag(22) && this.isEntityAlive()) {
            long worldTime = this.world.getWorldTime();
            if (worldTime % 1000L == 0) {
                this.playTimeSound((int) (worldTime / 1000L % 24L));
            }
        }
    }

    private void handleServerTicks() {
        if (this.ticksExisted == 32 && !this.getPassengers().isEmpty()) {
            this.sendSyncPacket((byte) 53, true);
        }
        if (!this.isMorph) {
            if (this.getUpdateFlag(0)) {
                this.calcShipAttributes(16, true);
            }
            if (!this.initAI && this.ticksExisted > 10) {
                this.initShipAI();
            }
            TaskHelper.onUpdateTask(this);
        }
        if ((this.ticksExisted & 0xF) == 0) {
            this.handleServerTicks16();
        }
    }

    private void handleServerTicks16() {
        if (this.isEntityAlive()) {
            if (!this.isMorph) {
                if (!(this.getRidingEntity() instanceof BasicEntityMount) && EntityHelper.updateWaypointMove(this)) {
                    this.sendSyncPacket((byte) 9, true);
                }
                if (this.hasShipMounts() && !this.canSummonMounts() && this.isRiding() && this.getRidingEntity() instanceof BasicEntityMount) {
                    this.dismountRidingEntity();
                }
            }
            BuffHelper.convertPotionToBuffMap(this);
            this.calcShipAttributes(8, true);
        }
        if ((this.ticksExisted & 0x1F) == 0) {
            this.handleServerTicks32();
        }
    }

    private void handleServerTicks32() {
        if (this.isEntityAlive() && !this.isMorph) {
            BuffHelper.applyBuffOnTicks(this);
            if (this.getHealth() < this.getMaxHealth() * 0.9f && this.decrSupplies(7)) {
                this.heal(this.getMaxHealth() * 0.08f + 15.0f);
                if (this instanceof BasicEntityShipCV) {
                    BasicEntityShipCV ship = (BasicEntityShipCV) this;
                    ship.setNumAircraftLight(ship.getNumAircraftLight() + 1);
                    ship.setNumAircraftHeavy(ship.getNumAircraftHeavy() + 1);
                }
            }
        }
        if ((this.ticksExisted & 0x3F) == 0) {
            this.handleServerTicks64();
        }
    }

    private void handleServerTicks64() {
        this.updateEmotionState();
        if (this.isRiding() && !this.isMorph) {
            this.sendSyncPacketRiders();
        }
        if ((this.ticksExisted & 0x7F) == 0) {
            this.handleServerTicks128();
        }
    }

    private void handleServerTicks128() {
        this.calcShipAttributes(31, true);
        if (!this.isMorph) {
            this.updateChunkLoader();
            if (!this.initWaitAI && this.ticksExisted >= 128) {
                this.setUpdateFlag(0, true);
                this.sendSyncPacketAll();
                this.initWaitAI = true;
            }
            EntityPlayer player = EntityHelper.getEntityPlayerByUID(this.getPlayerUID());
            if (player != null) {
                this.setStateMinor(23, player.getEntityId());
                this.sendSyncPacket((byte) 9, false);
            }
            if (this.isEntityAlive()) {
                if (EntityHelper.getMoraleLevel(this.getMorale()) >= this.getStateMinor(9) && this.getFoodSaturation() < this.getFoodSaturationMax()) {
                    this.useCombatRation();
                }
                this.updateMountSummon();
                this.updateConsumeItem();
                if (!this.getStateFlag(2)) {
                    this.updateMorale();
                }
            }
        }
        if ((this.ticksExisted & 0xFF) == 0) {
            this.handleServerTicks256();
        }
    }

    private void handleServerTicks256() {
        if (!this.isMorph) {
            if (this.isEntityAlive()) {
                if (!this.getStateFlag(2)) {
                    this.applyEmotesReaction(4);
                }
                if (this.getHealth() < this.getMaxHealth()) {
                    this.heal(this.getMaxHealth() * 0.03f + 1.0f);
                }
                this.updateShipCacheDataWithoutNewID();
            }
            EntityHelper.updateNameTag(this);
        }
        int food = this.getFoodSaturation();
        if (food > 0) {
            this.setFoodSaturation(food - 1);
        }
    }

    private void initShipAI() {
        this.setStateFlag(10, true);
        this.clearAITasks();
        this.clearAITargetTasks();
        this.setAIList();
        this.setAITargetList();
        this.decrGrudgeNum(0);
        this.updateChunkLoader();
        this.initAI = true;
    }

    private void updateClientSide() {
        this.updateClientTimer();
        if (this.ticksExisted % 40 == 2) {
            this.sendSyncRequest(0);
        }
        if (this.ticksExisted % 128 == 40) {
            this.sendSyncRequest(1);
        }
        if (this.ticksExisted % 256 == 128) {
            this.sendSyncRequest(2);
        }
        if ((this.ticksExisted & 0x1F) == 0) {
            EntityHelper.showNameTag(this);
        }
    }

    @Override
    public boolean canPassengerSteer() {
        return false;
    }

    protected void useCombatRation() {
        int i = this.findItemInSlot(new ItemStack(ModItems.CombatRation), true);
        if (i >= 0) {
            ItemStack getItem = this.itemHandler.getStackInSlot(i);
            InteractHelper.interactFeed(this, null, getItem);
            getItem.shrink(1);
            if (getItem.isEmpty()) {
                this.itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        float atk = this.getAttackBaseDamage(0, target);
        this.addShipExp(ConfigHandler.expGain[0]);
        this.decrMorale(0);
        this.setCombatTick(this.ticksExisted);
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        this.applySoundAtAttacker(0);
        this.applyParticleAtAttacker(0, distVec);
        boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this), atk);
        if (isTargetHurt) {
            if (!TeamHelper.checkSameOwner(this, target)) {
                BuffHelper.applyBuffOnTarget(target, this.AttackEffectMap);
            }
            this.applySoundAtTarget(0);
            this.applyParticleAtTarget(0, target);
            this.applyEmotesReaction(3);
            if (ConfigHandler.canFlare) {
                this.flareTarget(target);
            }
        }
        this.applyAttackPostMotion(0, target, isTargetHurt, atk);
        return isTargetHurt;
    }

    @Override
    public boolean attackEntityWithAmmo(Entity target) {
        if (!this.decrAmmoNum(0, this.getAmmoConsumption())) {
            return false;
        }
        this.addShipExp(ConfigHandler.expGain[1]);
        this.decrGrudgeNum(ConfigHandler.consumeGrudgeAction[0]);
        this.decrMorale(1);
        this.setCombatTick(this.ticksExisted);
        float atk = this.getAttackBaseDamage(1, target);
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        this.applySoundAtAttacker(1);
        this.applyParticleAtAttacker(1, distVec);
        atk = CombatHelper.applyCombatRateToDamage(this, true, (float) distVec.d, atk);
        atk = CombatHelper.applyDamageReduceOnPlayer(target, atk);
        if (!TeamHelper.doFriendlyFire(this, target)) {
            atk = 0.0f;
        }
        boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this).setProjectile(), atk);
        if (isTargetHurt) {
            if (!TeamHelper.checkSameOwner(this, target)) {
                BuffHelper.applyBuffOnTarget(target, this.AttackEffectMap);
            }
            this.applySoundAtTarget(1);
            this.applyParticleAtTarget(1, target);
            this.applyEmotesReaction(3);
            if (ConfigHandler.canFlare) {
                this.flareTarget(target);
            }
        }
        this.applyAttackPostMotion(1, target, isTargetHurt, atk);
        return isTargetHurt;
    }

    public boolean attackEntityWithHeavyAmmo(BlockPos target) {
        if (!this.decrAmmoNum(1, this.getAmmoConsumption())) {
            return false;
        }
        this.addShipExp(ConfigHandler.expGain[2]);
        this.decrGrudgeNum(ConfigHandler.consumeGrudgeAction[1]);
        this.decrMorale(2);
        this.setCombatTick(this.ticksExisted);
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this.getPosition(), target);
        this.applySoundAtAttacker(2);
        this.applyParticleAtAttacker(2, distVec);
        float tarX = target.getX();
        float tarY = target.getY();
        float tarZ = target.getZ();
        if (this.rand.nextFloat() <= CombatHelper.calcMissRate(this, (float) distVec.d)) {
            tarX += (this.rand.nextFloat() - 0.5f) * 10.0f;
            tarY += this.rand.nextFloat() * 5.0f;
            tarZ += (this.rand.nextFloat() - 0.5f) * 10.0f;
            ParticleHelper.spawnAttackTextParticle(this, 0);
        }
        float atk = this.getAttackBaseDamage(2, null);
        this.summonMissile(2, atk, tarX, tarY, tarZ, 1.0f);
        this.applySoundAtTarget(2);
        this.applyParticleAtTarget(2, this);
        this.applyEmotesReaction(3);
        if (ConfigHandler.canFlare) {
            this.flareTarget(target);
        }
        this.applyAttackPostMotion(2, this, true, atk);
        return true;
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        if (!this.decrAmmoNum(1, this.getAmmoConsumption())) {
            return false;
        }
        this.addShipExp(ConfigHandler.expGain[2]);
        this.decrGrudgeNum(ConfigHandler.consumeGrudgeAction[1]);
        this.decrMorale(2);
        this.setCombatTick(this.ticksExisted);
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        this.applySoundAtAttacker(2);
        this.applyParticleAtAttacker(2, distVec);
        float tarX = (float) target.posX;
        float tarY = (float) target.posY;
        float tarZ = (float) target.posZ;
        if (this.rand.nextFloat() <= CombatHelper.calcMissRate(this, (float) distVec.d)) {
            tarX += (this.rand.nextFloat() - 0.5f) * 10.0f;
            tarY += this.rand.nextFloat() * 5.0f;
            tarZ += (this.rand.nextFloat() - 0.5f) * 10.0f;
            ParticleHelper.spawnAttackTextParticle(this, 0);
        }
        float atk = this.getAttackBaseDamage(2, target);
        this.summonMissile(2, atk, tarX, tarY, tarZ, target.height);
        this.applySoundAtTarget(2);
        this.applyParticleAtTarget(2, target);
        this.applyEmotesReaction(3);
        if (ConfigHandler.canFlare) {
            this.flareTarget(target);
        }
        this.applyAttackPostMotion(2, target, true, atk);
        return true;
    }

    public void summonMissile(int attackType, float atk, float tarX, float tarY, float tarZ, float targetHeight) {
        float launchPos = (float) this.posY + this.height * 0.5f;
        if (this.isMorph) {
            launchPos += 0.5f;
        }
        int moveType = CombatHelper.calcMissileMoveType(this, tarY, attackType);
        if (moveType == 0) {
            launchPos = (float) this.posY + this.height * 0.3f;
        }
        MissileData md = this.getMissileData(attackType);
        float[] data = new float[]{atk, 0.15f, launchPos, tarX, tarY + targetHeight * 0.1f, tarZ, 140.0f, 0.25f, md.vel0, md.accY1, md.accY2};
        EntityAbyssMissile missile = new EntityAbyssMissile(this.world, this, md.type, moveType, data);
        this.world.spawnEntity(missile);
    }

    public void applyAttackPostMotion(int type, Entity target, boolean isTargetHurt, float atk) {}

    @Override
    public boolean updateSkillAttack(Entity target) {
        return false;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float atk) {
        if (this.world.isRemote || this.isMorph) return false;
        if (source.isUnblockable()) {
            if (source == DamageSource.OUT_OF_WORLD) {
                this.setPositionAndUpdate(this.posX, 256, this.posZ);
                return false;
            }
            if (source == DamageSource.IN_WALL || source == DamageSource.STARVE || source == DamageSource.FALL) return false;
            return super.attackEntityFrom(source, atk);
        }

        Entity attacker = source.getTrueSource();
        if (attacker != null && attacker.equals(this)) return false;

        if (this.isEntityInvulnerable(source)) return false;

        if (this.rand.nextInt(10) == 0) this.randomSensitiveBody();

        if (attacker instanceof EntityPlayer && TeamHelper.checkSameOwner(attacker, this)) {
            this.setSitting(false);
            this.setStateEmotion(1, 3, true);
            return super.attackEntityFrom(source, atk);
        }

        float dist = attacker != null ? (float) this.getDistanceSq(attacker) : Float.MAX_VALUE;
        if (CombatHelper.canDodge(this, dist)) return false;

        float reducedAtk = CombatHelper.getFinalDamage(this, source, atk);

        this.setSitting(false);
        if (attacker != null) {
            this.setEntityRevengeTarget(attacker);
            this.setEntityRevengeTime();
        }

        if (reducedAtk >= this.getHealth() && this.decrSupplies(8)) {
            this.setHealth(this.getMaxHealth());
            this.StateTimer[2] = 120;
            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 13, 0.0, 0.03, 0.0), point);
            return false;
        }

        this.decrMorale(5);
        this.setCombatTick(this.ticksExisted);
        if (this.rand.nextInt(5) == 0 && attacker != null) {
            this.setStateMinor(33, CalcHelper.getEntityHitHeight(this, attacker));
            this.setStateMinor(34, CalcHelper.getEntityHitSide(this, attacker));
            this.applyEmotesReaction(2);
        }
        this.setStateEmotion(1, 3, true);

        return super.attackEntityFrom(source, reducedAtk);
    }

    public void decrMorale(int type) {
        if (this.isMorph) return;

        int[] moraleCost = {-2, -4, -6, -6, -8, -5};
        if (type >= 0 && type < moraleCost.length) {
            this.addMorale(moraleCost[type]);
        }
    }

    public boolean decrAmmoNum(int type, int amount) {
        if (this.isMorph) return MetamorphHelper.decrAmmoNum(this, type, amount);

        int ammoType = type == 1 ? 5 : 4;
        boolean hasAmmo = type == 1 ? this.hasAmmoHeavy() : this.hasAmmoLight();

        if (!hasAmmo) {
            boolean supplied = false;
            float modAmmo = this.shipAttrs.getAttrsBuffed(18);
            int addAmmo = 0;

            if (type == 0) {
                if (decrSupplies(0)) { addAmmo = (int) (30f * modAmmo); supplied = true; }
                else if (decrSupplies(2)) { addAmmo = (int) (270f * modAmmo); supplied = true; }
            } else {
                if (decrSupplies(1)) { addAmmo = (int) (15f * modAmmo); supplied = true; }
                else if (decrSupplies(3)) { addAmmo = (int) (135f * modAmmo); supplied = true; }
            }

            if (supplied) {
                if (ConfigHandler.consumptionLevel == 0) addAmmo *= 10;
                this.StateMinor[ammoType] += addAmmo;
                if (this.getEmotesTick() <= 0) {
                    this.setEmotesTick(40);
                    this.applyParticleEmotion(this.rand.nextInt(4) == 0 ? 9 : (this.rand.nextBoolean() ? 29 : 30));
                }
            }
        }

        if (this.StateMinor[ammoType] < amount) {
            if (this.getEmotesTick() <= 0) {
                this.setEmotesTick(20);
                int[] emotes = {0, 2, 5, 20, 32};
                this.applyParticleEmotion(emotes[this.rand.nextInt(emotes.length)]);
            }
            return false;
        }

        this.StateMinor[ammoType] -= amount;
        return true;
    }

    public int getGrudge() {
        return this.StateMinor[6];
    }

    public void addGrudge(int value) {
        this.StateMinor[6] = Math.max(0, this.StateMinor[6] + value);
    }

    public void decrGrudgeNum(int value) {
        if (this.isMorph) {
            MetamorphHelper.decrGrudgeNum(this, value);
            return;
        }

        float modGrudge = this.shipAttrs.getAttrsBuffed(17);
        if (value > 0) {
            int level = BuffHelper.getPotionLevel(this, 17);
            value = (int) (value * (1.0f + level * 2.0f));
        } else if (value < 0) {
            value = (int) (value * modGrudge);
        }

        if (!this.getStateFlag(2)) {
            this.addGrudge(-value);
        }

        if (this.getGrudge() <= 0) {
            int addGrudge = 0;
            if (decrSupplies(4)) addGrudge = (int) (300f * modGrudge);
            else if (decrSupplies(5)) addGrudge = (int) (2700f * modGrudge);
            if (ConfigHandler.consumptionLevel == 0) addGrudge *= 10;
            this.addGrudge(addGrudge);
        }

        boolean outOfFuel = this.getGrudge() <= 0;
        if (this.getStateFlag(2) != outOfFuel) {
            this.setStateFlag(2, outOfFuel);
            this.updateFuelState(outOfFuel);
        }
    }

    public boolean decrSupplies(int type) {
        ItemStack itemType;
        boolean noMeta = false;
        switch (type) {
            case 0: itemType = new ItemStack(ModItems.Ammo, 1, 0); break;
            case 1: itemType = new ItemStack(ModItems.Ammo, 1, 2); break;
            case 2: itemType = new ItemStack(ModItems.Ammo, 1, 1); break;
            case 3: itemType = new ItemStack(ModItems.Ammo, 1, 3); break;
            case 4: itemType = new ItemStack(ModItems.Grudge, 1); noMeta = true; break;
            case 5: itemType = new ItemStack(ModBlocks.BlockGrudge, 1); noMeta = true; break;
            case 6: itemType = new ItemStack(ModBlocks.BlockGrudgeHeavy, 1); noMeta = true; break;
            case 7: itemType = new ItemStack(ModItems.BucketRepair, 1); noMeta = true; break;
            case 8: itemType = new ItemStack(ModItems.RepairGoddess, 1); noMeta = true; break;
            default: return false;
        }
        int i = this.findItemInSlot(itemType, noMeta);
        if (i == -1) {
            return false;
        }
        ItemStack getItem = this.itemHandler.getStackInSlot(i);
        getItem.shrink(1);
        if (getItem.isEmpty()) {
            this.itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        return true;
    }

    protected void updateFuelState(boolean nofuel) {
        this.clearAITasks();
        this.clearAITargetTasks();
        this.sendSyncPacketAllMisc();

        if (nofuel) {
            this.setMorale(0);
            if (this.getRidingEntity() instanceof BasicEntityMount) {
                ((BasicEntityMount) this.getRidingEntity()).clearAITasks();
            }
            if (this.getEmotesTick() <= 0) {
                this.setEmotesTick(20);
                int[] emotes = {0, 32, 2, 12, 5, 20, 10};
                this.applyParticleEmotion(emotes[this.rand.nextInt(emotes.length)]);
            }
        } else {
            this.setAIList();
            this.setAITargetList();
            if (this.getRidingEntity() instanceof BasicEntityMount) {
                ((BasicEntityMount) this.getRidingEntity()).clearAITasks();
                ((BasicEntityMount) this.getRidingEntity()).setAIList();
            }
            if (this.getEmotesTick() <= 0) {
                this.setEmotesTick(40);
                int[] emotes = {31, 32, 7, 1};
                this.applyParticleEmotion(emotes[this.rand.nextInt(emotes.length)]);
            }
        }
    }

    protected int findItemInSlot(ItemStack parItem, boolean noMeta) {
        int endSlot;
        switch (this.getInventoryPageSize()) {
            case 0: endSlot = 24; break;
            case 1: endSlot = 42; break;
            default: endSlot = 60;
        }
        for (int i = 6; i < endSlot; ++i) {
            ItemStack slotitem = this.itemHandler.getStackInSlot(i);
            if (!slotitem.isEmpty() && slotitem.getItem() == parItem.getItem()) {
                if (noMeta || slotitem.getMetadata() == parItem.getMetadata()) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean canFly() {
        return this.isPotionActive(MobEffects.LEVITATION);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    public boolean hasShipMounts() {
        return false;
    }

    public boolean canSummonMounts() {
        return (this.getStateEmotion(0) & 1) == 1 && !this.getStateFlag(2);
    }

    public BasicEntityMount summonMountEntity() {
        return null;
    }

    @Override
    public Entity getGuardedEntity() {
        return this.guardedEntity;
    }

    @Override
    public void setGuardedEntity(Entity entity) {
        if (entity != null && entity.isEntityAlive()) {
            this.guardedEntity = entity;
            this.setStateMinor(18, entity.getEntityId());
        } else {
            this.guardedEntity = null;
            this.setStateMinor(18, -1);
        }
    }

    @Override
    public int getGuardedPos(int vec) {
        switch (vec) {
            case 0: return this.getStateMinor(14);
            case 1: return this.getStateMinor(15);
            case 2: return this.getStateMinor(16);
            case 3: return this.getStateMinor(17);
            case 4: return this.getStateMinor(24);
            default: return 0;
        }
    }

    @Override
    public void setGuardedPos(int x, int y, int z, int dim, int type) {
        this.setStateMinor(14, x);
        this.setStateMinor(15, y);
        this.setStateMinor(16, z);
        this.setStateMinor(17, dim);
        this.setStateMinor(24, type);
    }

    @Override
    public double getMountedYOffset() {
        return this.height;
    }

    @Override
    public Entity getHostEntity() {
        if (this.getPlayerUID() > 0 && !this.world.isRemote) {
            return EntityHelper.getEntityPlayerByUID(this.getPlayerUID());
        }
        return this.getOwner();
    }

    @Override
    public int getDamageType() {
        return this.getStateMinor(25);
    }

    public void updateShipCacheData(boolean forceUpdate) {
        if (!this.isUpdated && this.ticksExisted % this.updateTime == 0 || forceUpdate) {
            if (this.getPlayerUID() <= 0) {
                ServerProxy.updateShipOwnerID(this);
            }
            ServerProxy.updateShipID(this);
            if (this.getPlayerUID() > 0 && this.getShipUID() > 0) {
                this.sendSyncPacketAll();
                this.isUpdated = true;
            }
            this.updateTime = Math.min(this.updateTime * 2, 4096);
        }
    }

    public void updateShipCacheDataWithoutNewID() {
        int uid;
        if (!this.world.isRemote && (uid = this.getShipUID()) > 0) {
            BasicEntityShip ship = ServerProxy.checkShipIsDupe(this, uid);
            if (this.equals(ship)) {
                CacheDataShip sdata = new CacheDataShip(this.getEntityId(), this.world.provider.getDimension(), this.getShipClass(), this.isDead, this.posX, this.posY, this.posZ, this.writeToNBT(new NBTTagCompound()));
                ServerProxy.setShipWorldData(uid, sdata);
            } else {
                this.setDead();
            }
        }
    }

    protected void updateEmotionState() {
        float hpState = this.getHealth() / this.getMaxHealth();
        if (this.isMorph && this.morphHost != null) {
            hpState = this.morphHost.getHealth() / this.morphHost.getMaxHealth();
        }

        if (hpState > 0.75f) this.setStateEmotion(3, 0, false);
        else if (hpState > 0.5f) this.setStateEmotion(3, 1, false);
        else if (hpState > 0.25f) this.setStateEmotion(3, 2, false);
        else this.setStateEmotion(3, 3, false);

        if (this.getStateFlag(2)) {
            if (this.getStateEmotion(1) != 5) this.setStateEmotion(1, 5, false);
        } else if (hpState < 0.35f) {
            if (this.getStateEmotion(1) != 2) this.setStateEmotion(1, 2, false);
        } else {
            if (this.getStateEmotion(1) == 0 && this.rand.nextInt(3) == 0) this.setStateEmotion(1, 4, false);
            else if (this.rand.nextInt(4) == 0) this.setStateEmotion(1, 0, false);
            if (this.getStateEmotion(7) == 0 && this.rand.nextInt(3) == 0) this.setStateEmotion(7, 4, false);
            else if (this.rand.nextInt(3) == 0) this.setStateEmotion(7, 0, false);
        }
        if (!this.world.isRemote) {
            this.sendSyncPacketEmotion();
        }
    }

    protected void updateMountSummon() {
        if (this.hasShipMounts() && this.canSummonMounts() && !this.isRiding()) {
            BasicEntityMount mount = this.summonMountEntity();
            if(mount != null) {
                mount.initAttrs(this);
                this.world.spawnEntity(mount);
                this.getPassengers().forEach(Entity::dismountRidingEntity);
                this.startRiding(mount, true);
                mount.sendSyncPacket((byte) 0);
            }
        }
    }

    protected void updateConsumeItem() {
        if (!this.hasAmmoLight()) this.decrAmmoNum(0, 0);
        if (!this.hasAmmoHeavy()) this.decrAmmoNum(1, 0);

        if (this.ShipPrevY > 0.0) {
            double dist = this.getDistance(this.ShipPrevX, this.ShipPrevY, this.ShipPrevZ);
            int valueConsume = (int) dist;
            int moraleCost = MathHelper.clamp((int) (valueConsume * 0.5f), 5, 50);
            this.addMorale(-moraleCost);
            valueConsume *= ConfigHandler.consumeGrudgeAction[4];
            if (this instanceof EntityTransportWa && this.ticksExisted > 200) {
                this.addShipExp((int)(valueConsume * 0.2f));
            }
            this.decrGrudgeNum(valueConsume + this.getGrudgeConsumption());
        }

        this.ShipPrevX = this.posX;
        this.ShipPrevY = this.posY;
        this.ShipPrevZ = this.posZ;
    }

    protected void updateMorale() {
        int m = this.getMorale();
        int change;
        if (EntityHelper.checkShipOutOfCombat(this)) {
            change = (m < 3100) ? 40 : ((m > 3900) ? -10 : 0);
        } else {
            if (m < 900) change = -11;
            else if (m < 2100) change = -7;
            else if (m < 3900) change = -5;
            else change = -11;
        }
        this.addMorale(change);
    }

    protected void updateSearchlight() {
        if (this.getStateMinor(39) > 0) {
            BlockPos pos = new BlockPos(this);
            if (this.world.getLightFor(EnumSkyBlock.BLOCK, pos) < 10) {
                BlockHelper.placeLightBlock(this.world, pos);
            } else {
                BlockHelper.updateNearbyLightBlock(this.world, pos);
            }
        }
    }

    protected void updateClientTimer() {
        if (this.StateTimer[12] > 0) this.StateTimer[12]--;
        if (this.StateTimer[5] > 0) {
            this.StateTimer[5]--;
            if (this.StateTimer[5] == 0) {
                this.setStateEmotion(6, 0, false);
            }
        }
    }

    protected void updateServerTimer() {
        if (this.StateTimer[2] > 0) this.StateTimer[2]--;
        if (this.StateTimer[3] > 0) this.StateTimer[3]--;
        if (this.getStateMinor(43) > 1) this.StateTimer[1]++;
        if (this.StateTimer[6] > 0) this.StateTimer[6]--;
        if (this.StateTimer[10] > 0) this.StateTimer[10]--;
    }

    protected void updateBothSideTimer() {
        for (int i = 16; i <= 20; i++) {
            if (this.StateTimer[i] > 0) this.StateTimer[i]--;
        }
    }

    protected void updateClientBodyRotate() {
        if (!this.isRiding()) {
            if (this.isMorph && this.morphHost != null) {
                this.rotationYaw = this.morphHost.rotationYaw;
            } else if (this.getDistanceSq(this.prevPosX, this.prevPosY, this.prevPosZ) > 0.01) {
                float[] degree = CalcHelper.getLookDegree(this.posX - this.prevPosX, this.posY - this.prevPosY, this.posZ - this.prevPosZ, true);
                this.rotationYaw = degree[0];
            }
        }
    }

    public void setSensitiveBody(int par1) {
        this.setStateMinor(35, par1);
    }

    public int getSensitiveBody() {
        return this.getStateMinor(35);
    }

    public void randomSensitiveBody() {
        int ran = this.rand.nextInt(100);
        int bodyid;
        if (ran > 80) bodyid = 0;
        else if (ran > 65) bodyid = 1;
        else bodyid = 3 + this.rand.nextInt(8);

        if (bodyid == 8 || bodyid == 5) bodyid = 0;
        if (bodyid == 10 || bodyid == 2) bodyid = 1;

        this.setSensitiveBody(bodyid);
    }

    public void pushAITarget() {
        if (this.aiTarget != null) {
            this.swingArm(EnumHand.MAIN_HAND);
            this.aiTarget.addVelocity(-MathHelper.sin(this.rotationYaw * Values.N.DIV_PI_180) * 0.5f, 0.5, MathHelper.cos(this.rotationYaw * Values.N.DIV_PI_180) * 0.5f);
            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 48.0);
            CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this.aiTarget, 0, (byte) 54), point);
        }
    }

    public void checkCaressed() {
        Enums.BodyHeight hit = this.getBodyIDFromHeight();
        if (hit == Enums.BodyHeight.TOP || hit == Enums.BodyHeight.HEAD || hit == Enums.BodyHeight.NECK || hit == Enums.BodyHeight.CHEST) {
            this.setStateEmotion(6, 1, false);
            this.setStateTimer(5, 80);
        }
    }

    public void reactionNormal() {
        int m = this.getMorale();
        int body = EntityHelper.getHitBodyID(this);
        int baseMorale = (int) (ConfigHandler.baseCaressMorale * 2.5f);
        boolean sensitive = (body == this.getSensitiveBody());

        switch (EntityHelper.getMoraleLevel(m)) {
            case 0:
                this.setStateEmotion(1, sensitive ? 7 : 8, true);
                if (sensitive) {
                    this.applyParticleEmotion(this.rand.nextBoolean() ? 31 : 10);
                    if (m < 7650) this.addMorale(baseMorale * 3 + this.rand.nextInt(baseMorale + 1));
                } else {
                    this.applyParticleEmotion(MAJOR_BODY_PARTS.contains(body) ? (this.getStateFlag(1) ? 15 : 1) : (this.rand.nextBoolean() ? 1 : 7));
                }
                break;
            case 1:
                this.setStateEmotion(1, 7, true);
                if (sensitive) {
                    this.applyParticleEmotion(this.getStateFlag(1) ? (this.rand.nextBoolean() ? 31 : 10) : 10);
                    this.addMorale(baseMorale + this.rand.nextInt(baseMorale + 1));
                } else {
                    this.applyParticleEmotion(MAJOR_BODY_PARTS.contains(body) ? (this.getStateFlag(1) ? 1 : 16) : (this.rand.nextBoolean() ? 1 : 7));
                }
                break;
            case 2:
                if (sensitive) {
                    this.setStateEmotion(1, 7, true);
                    this.applyParticleEmotion(this.getStateFlag(1) ? 19 : 18);
                    this.addMorale(baseMorale + this.rand.nextInt(baseMorale + 1));
                    if (this.rand.nextInt(6) == 0) this.pushAITarget();
                } else {
                    if (MAJOR_BODY_PARTS.contains(body)) {
                        this.applyParticleEmotion(this.getStateFlag(1) ? 1 : 27);
                        if (this.rand.nextInt(8) == 0) this.pushAITarget();
                    } else {
                        int[] emotes = {30, 7, 26, 11, 29};
                        this.applyParticleEmotion(emotes[this.rand.nextInt(emotes.length)]);
                    }
                }
                break;
            case 3:
                if (sensitive) {
                    this.setStateEmotion(1, 7, true);
                    this.applyParticleEmotion(32);
                    this.addMorale(this.rand.nextInt(baseMorale + 1));
                    if (this.rand.nextInt(2) == 0) this.pushAITarget();
                    else if (this.aiTarget != null && this.rand.nextInt(8) == 0) this.attackEntityAsMob(this.aiTarget);
                } else {
                    if (MAJOR_BODY_PARTS.contains(body)) {
                        this.setStateEmotion(1, 3, true);
                        this.applyParticleEmotion(32);
                        if (this.rand.nextInt(4) == 0) this.pushAITarget();
                    } else {
                        int[] emotes = {30, 2, 3, 0};
                        this.applyParticleEmotion(emotes[this.rand.nextInt(emotes.length)]);
                    }
                }
                break;
            default:
                if (sensitive) {
                    this.setStateEmotion(1, 7, true);
                    this.applyParticleEmotion(6);
                    this.addMorale(-(baseMorale * 10 + this.rand.nextInt(baseMorale * 5 + 1)));
                    this.pushAITarget();
                    if (this.aiTarget != null && this.rand.nextInt(3) == 0) this.attackEntityAsMob(this.aiTarget);
                } else {
                    if (MAJOR_BODY_PARTS.contains(body)) {
                        this.setStateEmotion(1, 2, true);
                        this.applyParticleEmotion(this.rand.nextInt(3) == 0 ? 6 : 32);
                        if (this.rand.nextInt(2) == 0) this.pushAITarget();
                        else if (this.aiTarget != null && this.rand.nextInt(5) == 0) this.attackEntityAsMob(this.aiTarget);
                    } else {
                        int[] emotes = {8, 2, 20, 5, 34};
                        this.applyParticleEmotion(emotes[this.rand.nextInt(emotes.length)]);
                    }
                }
                break;
        }
    }

    public void reactionStranger() {
        int body = EntityHelper.getHitBodyID(this);
        if (body == this.getSensitiveBody()) {
            this.setStateEmotion(1, 6, true);
            this.applyParticleEmotion(this.rand.nextBoolean() ? 6 : 22);
            if (this.rand.nextInt(2) == 0) {
                this.pushAITarget();
            } else if (this.aiTarget != null && this.rand.nextInt(4) == 0) {
                this.attackEntityAsMob(this.aiTarget);
            }
        } else {
            this.setStateEmotion(1, 3, true);
            if (MAJOR_BODY_PARTS.contains(body)) {
                this.applyParticleEmotion(this.rand.nextBoolean() ? 6 : 5);
                if (this.rand.nextInt(4) == 0) this.pushAITarget();
                else if (this.aiTarget != null && this.rand.nextInt(8) == 0) this.attackEntityAsMob(this.aiTarget);
            } else {
                int[] emotes = {9, 2, 20, 8, 0, 34};
                this.applyParticleEmotion(emotes[this.rand.nextInt(emotes.length)]);
            }
        }
    }

    public void reactionAttack() {
        if (EntityHelper.getMoraleLevel(this.getMorale()) == 0) {
            this.setStateEmotion(1, 8, true);
            int[] emotes = {33, 17, 19, 16, 7};
            this.applyParticleEmotion(emotes[this.rand.nextInt(emotes.length)]);
        } else {
            int[] emotes = {14, 30, 7, 4, 7, 6};
            this.applyParticleEmotion(emotes[this.rand.nextInt(emotes.length)]);
        }
    }

    public void reactionDamaged() {
        int body = EntityHelper.getHitBodyID(this);
        int morale = EntityHelper.getMoraleLevel(this.getMorale());

        if (morale <= 2) {
            if (MAJOR_BODY_PARTS.contains(body) || body == this.getSensitiveBody()) {
                this.applyParticleEmotion(6);
            } else {
                int[] emotes = {30, 5, 2, 3, 8};
                this.applyParticleEmotion(emotes[this.rand.nextInt(emotes.length)]);
            }
        } else {
            if (MAJOR_BODY_PARTS.contains(body) || body == this.getSensitiveBody()) {
                this.applyParticleEmotion(10);
            } else {
                int[] emotes = {30, 5, 2, 3, 0, 8};
                this.applyParticleEmotion(emotes[this.rand.nextInt(emotes.length)]);
            }
        }
    }

    public void reactionIdle() {
        if (this.getStateFlag(1) && this.rand.nextBoolean()) {
            this.applyParticleEmotion(this.rand.nextInt(3) == 1 ? 31 : 15);
            return;
        }

        switch (EntityHelper.getMoraleLevel(this.getMorale())) {
            case 0: case 1:
                int[] emotesSparkling = {33, 17, 19, 9, 1, 15, 16, 14, 7};
                this.applyParticleEmotion(emotesSparkling[this.rand.nextInt(emotesSparkling.length)]);
                break;
            case 2:
                int[] emotesNormal = {11, 3, 13, 9, 18, 16, 29};
                this.applyParticleEmotion(emotesNormal[this.rand.nextInt(emotesNormal.length)]);
                break;
            default:
                int[] emotesTired = {0, 2, 3, 8, 10, 20, 32};
                this.applyParticleEmotion(emotesTired[this.rand.nextInt(emotesTired.length)]);
                break;
        }
    }

    public void reactionCommand() {
        switch (EntityHelper.getMoraleLevel(this.getMorale())) {
            case 0: case 1: case 2:
                int[] emotesOk = {21, 4, 14, 11, 13};
                this.applyParticleEmotion(emotesOk[this.rand.nextInt(emotesOk.length)]);
                break;
            default:
                int[] emotesTired = {0, 33, 3, 10, 13, 32};
                this.applyParticleEmotion(emotesTired[this.rand.nextInt(emotesTired.length)]);
                break;
        }
    }

    public void reactionShock() {
        int[] emotes = {0, 8, 4, 12};
        this.applyParticleEmotion(emotes[this.rand.nextInt(emotes.length)]);
    }

    public void applyEmotesReaction(int type) {
        if (this.getEmotesTick() > 0) return;

        switch (type) {
            case 0:
                if (this.rand.nextInt(7) == 0) {
                    this.setEmotesTick(50);
                    this.reactionNormal();
                }
                break;
            case 1:
                if (this.rand.nextInt(9) == 0) {
                    this.setEmotesTick(60);
                    this.reactionStranger();
                }
                break;
            case 2:
                if (this.getEmotesTick() <= 10) {
                    this.setEmotesTick(40);
                    this.reactionDamaged();
                }
                break;
            case 3:
                if (this.rand.nextInt(6) == 0) {
                    this.setEmotesTick(60);
                    this.reactionAttack();
                }
                break;
            case 4:
                if (this.rand.nextInt(3) == 0) {
                    this.setEmotesTick(20);
                    this.reactionIdle();
                }
                break;
            case 5:
                if (this.rand.nextInt(3) == 0) {
                    this.setEmotesTick(25);
                    this.reactionCommand();
                }
                break;
            case 6:
                this.reactionShock();
                break;
            default:
        }
    }

    public void applyParticleEmotion(int type) {
        float h = this.isSitting() ? this.height * 0.4f : this.height * 0.45f;
        if (!this.world.isRemote) {
            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 36, h, 0.0, type), point);
        } else {
            ParticleHelper.spawnAttackParticleAtEntity(this, h, 0.0, type, (byte) 36);
        }
    }

    public void applyParticleAtAttacker(int type, Dist4d distance) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        if (type == 1) {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 6, this.posX, this.posY, this.posZ, distance.x, distance.y, distance.z, true), point);
        } else {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
        }
    }

    public void applyParticleAtTarget(int type, Entity target) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        switch (type) {
            case 1:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 9, false), point);
                break;
            case 2: case 3: case 4:
                break;
            default:
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 1, false), point);
        }
    }

    public void applySoundAtAttacker(int type) {
        switch (type) {
            case 1:
                this.playSound(ModSounds.SHIP_FIRELIGHT, ConfigHandler.volumeFire, this.getSoundPitch() * 0.85f);
                if (this.rand.nextInt(8) == 0) this.playSound(BasicEntityShip.getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
                break;
            case 2:
                this.playSound(ModSounds.SHIP_FIREHEAVY, ConfigHandler.volumeFire, this.getSoundPitch() * 0.85f);
                if (this.getRNG().nextInt(8) == 0) this.playSound(BasicEntityShip.getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
                break;
            case 3:
            case 4:
                this.playSound(ModSounds.SHIP_AIRCRAFT, ConfigHandler.volumeFire * 0.5f, this.getSoundPitch() * 0.85f);
                if (this.getRNG().nextInt(8) == 0) this.playSound(BasicEntityShip.getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
                break;
            default:
                if (this.getRNG().nextInt(3) == 0) this.playSound(BasicEntityShip.getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
        }
    }

    public void applySoundAtTarget(int type) {
        switch (type) {
            case 1: case 2: case 3: case 4:
                break;
            default:
        }
    }

    public float getAttackBaseDamage(int type, Entity target) {
        switch (type) {
            case 1: return CombatHelper.modDamageByAdditionAttrs(this, target, this.shipAttrs.getAttackDamage(), 0);
            case 2: return this.shipAttrs.getAttackDamageHeavy();
            case 3: return this.shipAttrs.getAttackDamageAir();
            case 4: return this.shipAttrs.getAttackDamageAirHeavy();
            default: return this.shipAttrs.getAttackDamage() * 0.125f;
        }
    }

    public int getInventoryPageSize() {
        return this.StateMinor[36];
    }

    public void flareTarget(Entity target) {
        if (!this.world.isRemote && this.getStateMinor(38) > 0 && target != null) {
            this.flareTarget(target.getPosition());
        }
    }

    public void flareTarget(BlockPos target) {
        if (!this.world.isRemote && this.getStateMinor(38) > 0 && target != null) {
            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
            CommonProxy.channelI.sendToAllAround(new S2CReactPackets((byte) 20, target.getX(), target.getY(), target.getZ()), point);
        }
    }

    @Override
    public void setDead() {
        this.clearChunkLoader();
        super.setDead();
        this.updateShipCacheDataWithoutNewID();
    }

    public void clearChunkLoader() {
        if (this.chunkTicket != null) {
            if (this.chunks != null) {
                this.chunks.forEach(p -> ForgeChunkManager.unforceChunk(this.chunkTicket, p));
            }
            ForgeChunkManager.releaseTicket(this.chunkTicket);
            this.chunks = null;
            this.chunkTicket = null;
        }
    }

    public void updateChunkLoader() {
        if (!this.world.isRemote) {
            this.setChunkLoader();
            this.applyChunkLoader();
        }
    }

    private void setChunkLoader() {
        if (this.getStateMinor(37) > 0) {
            if (this.chunkTicket == null) {
                EntityPlayer player = (EntityPlayer)this.getOwner();
                if (player != null) {
                    this.chunkTicket = ForgeChunkManager.requestPlayerTicket(ShinColle.instance, player.getName(), this.world, ForgeChunkManager.Type.ENTITY);
                    if (this.chunkTicket != null) {
                        this.chunkTicket.bindEntity(this);
                    } else {
                        LogHelper.debug("DEBUG: Ship failed to get chunk loader ticket.");
                        this.clearChunkLoader();
                    }
                }
            }
        } else {
            this.clearChunkLoader();
        }
    }

    private void applyChunkLoader() {
        if (this.chunkTicket != null) {
            int cx = MathHelper.floor(this.posX) >> 4;
            int cz = MathHelper.floor(this.posZ) >> 4;
            HashSet<ChunkPos> loadChunks = BlockHelper.getChunksWithinRange(cx, cz, ConfigHandler.chunkloaderMode);
            if (this.chunks != null) {
                HashSet<ChunkPos> unloadChunks = new HashSet<>(this.chunks);
                unloadChunks.removeAll(loadChunks);
                unloadChunks.forEach(p -> ForgeChunkManager.unforceChunk(this.chunkTicket, p));
                loadChunks.removeAll(this.chunks);
            }
            loadChunks.forEach(p -> ForgeChunkManager.forceChunk(this.chunkTicket, p));
            if(this.chunks != null) {
                this.chunks.addAll(loadChunks);
            }
        }
    }

    @Override
    public BlockPos getLastWaypoint() {
        return this.waypoints[0];
    }

    @Override
    public void setLastWaypoint(BlockPos pos) {
        this.waypoints[0] = pos;
    }

    public static int wpStayTime2Ticks(int wpstay) {
        if (wpstay >= 1 && wpstay <= 5) return wpstay * 100;
        if (wpstay >= 6 && wpstay <= 10) return (wpstay - 5) * 1200;
        if (wpstay >= 11 && wpstay <= 16) return (wpstay - 10) * 12000;
        return 0;
    }

    @Override
    public int getWpStayTime() {
        return this.getStateTimer(4);
    }

    @Override
    public int getWpStayTimeMax() {
        return BasicEntityShip.wpStayTime2Ticks(this.getStateMinor(44));
    }

    @Override
    public void setWpStayTime(int time) {
        this.setStateTimer(4, time);
    }

    @Override
    protected void collideWithEntity(Entity target) {
        if (!(target instanceof BasicEntityAirplane)) {
            target.applyEntityCollision(this);
        }
    }

    @Override
    public int getPortalCooldown() {
        return 40;
    }

    public int getFieldCount() {
        return 35;
    }

    public int getField(int id) {
        switch (id) {
            case 0: return this.StateMinor[2];
            case 1: return this.StateMinor[4];
            case 2: return this.StateMinor[5];
            case 3: return this.StateMinor[7];
            case 4: return this.StateMinor[8];
            case 5: return this.getStateFlagI(3);
            case 6: return this.getStateFlagI(4);
            case 7: return this.getStateFlagI(5);
            case 8: return this.getStateFlagI(6);
            case 9: return this.getStateFlagI(7);
            case 10: return this.getStateFlagI(1);
            case 11: return this.StateMinor[10];
            case 12: return this.StateMinor[11];
            case 13: return this.StateMinor[12];
            case 14: return this.getStateFlagI(21);
            case 15: return this.getStateFlagI(9);
            case 16: return this.getStateFlagI(12);
            case 17: return this.getStateFlagI(18);
            case 18: return this.getStateFlagI(19);
            case 19: return this.getStateFlagI(20);
            case 20: return this.getStateFlagI(22);
            case 21: return this.getMorale();
            case 22: return this.StateMinor[36];
            case 23: return this.getStateFlagI(23);
            case 24: return this.StateMinor[44];
            case 25: return this.StateMinor[1];
            case 26: return this.StateMinor[6];
            case 27: return this.itemHandler.getInventoryPage();
            case 28: return this.getStateFlagI(25);
            case 29: return this.StateMinor[9];
            case 30: return this.getStateFlagI(26);
            case 31: return this.getStateEmotion(0);
            case 32: return this.StateMinor[40];
            case 33: return this.StateMinor[41];
            case 34: return this.getStateFlagI(2);
            default: return 0;
        }
    }

    public void setField(int id, int value) {
        switch (id) {
            case 0: this.StateMinor[2] = value; break;
            case 1: this.StateMinor[4] = value; break;
            case 2: this.StateMinor[5] = value; break;
            case 3: this.StateMinor[7] = value; break;
            case 4: this.StateMinor[8] = value; break;
            case 5: this.setStateFlagI(3, value); break;
            case 6: this.setStateFlagI(4, value); break;
            case 7: this.setStateFlagI(5, value); break;
            case 8: this.setStateFlagI(6, value); break;
            case 9: this.setStateFlagI(7, value); break;
            case 10: this.setStateFlagI(1, value); break;
            case 11: this.StateMinor[10] = value; break;
            case 12: this.StateMinor[11] = value; break;
            case 13: this.StateMinor[12] = value; break;
            case 14: this.setStateFlagI(21, value); break;
            case 15: this.setStateFlagI(9, value); break;
            case 16: this.setStateFlagI(12, value); break;
            case 17: this.setStateFlagI(18, value); break;
            case 18: this.setStateFlagI(19, value); break;
            case 19: this.setStateFlagI(20, value); break;
            case 20: this.setStateFlagI(22, value); break;
            case 21: this.StateMinor[30] = value; break;
            case 22: this.StateMinor[36] = value; break;
            case 23: this.setStateFlagI(23, value); break;
            case 24: this.StateMinor[44] = value; break;
            case 25: this.StateMinor[1] = value; break;
            case 26: this.StateMinor[6] = value; break;
            case 27: this.itemHandler.setInventoryPage(value); break;
            case 28: this.setStateFlagI(25, value); break;
            case 29: this.StateMinor[9] = value; break;
            case 30: this.setStateFlagI(26, value); break;
            case 31: this.setStateEmotion(0, value, false); break;
            case 32: this.StateMinor[40] = value; break;
            case 33: this.StateMinor[41] = value; break;
            case 34: this.setStateFlagI(2, value); break;
            default:
        }
    }

    @Override
    protected void updateArmSwingProgress() {
        int swingMaxTick = 6;
        if (this.isSwingInProgress) {
            this.swingProgressInt++;
            if (this.swingProgressInt >= swingMaxTick) {
                this.swingProgressInt = 0;
                this.isSwingInProgress = false;
            }
        } else {
            this.swingProgressInt = 0;
        }
        this.swingProgress = (float) this.swingProgressInt / swingMaxTick;
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
    public void setRidingState(int state) {}

    @Override
    public boolean shouldDismountInWater(Entity rider) {
        return false;
    }

    @Override
    public int getScaleLevel() {
        return 0;
    }

    @Override
    public void setScaleLevel(int par1) {}

    @Override
    public Random getRand() {
        return this.rand;
    }

    @Override
    public double getShipFloatingDepth() {
        return 0.32;
    }

    @Override
    public void heal(float healAmount) {
        if (!this.world.isRemote) {
            NetworkRegistry.TargetPoint tp = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 48.0);
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 23, 0.0, 0.1, 0.0), tp);
            healAmount = BuffHelper.applyBuffOnHeal(this, healAmount);
        }
        super.heal(healAmount * this.shipAttrs.getAttrsBuffed(19));
    }

    @Override
    protected void onDeathUpdate() {
        this.deathTime++;
        if (this.world.isRemote && (this.ticksExisted & 3) == 0) {
            int maxpar = (int) ((3 - ClientProxy.getMineraft().gameSettings.particleSetting) * 1.8f);
            double range = this.width * 1.2;
            for (int i = 0; i < maxpar; ++i) {
                ParticleHelper.spawnAttackParticleAt(this.posX - range + this.rand.nextDouble() * range * 2.0, this.posY + 0.1 + this.rand.nextDouble() * 0.3, this.posZ - range + this.rand.nextDouble() * range * 2.0, 1.5, 0.0, 0.0, (byte) 43);
            }
        }
        if (this.deathTime >= ConfigHandler.deathMaxTick) {
            if (!this.world.isRemote && this.getStateFlag(10)) {
                this.setStateFlag(10, false);
                ItemStack egg = new ItemStack(ModItems.ShipSpawnEgg, 1, this.getShipClass() + 2);
                egg.setTagCompound(EntityHelper.saveShipDataToNBT(this, true));
                this.world.spawnEntity(new BasicEntityItem(this.world, this.posX, this.posY + 0.5, this.posZ, egg));
            }
            if (!this.world.isRemote && (this.isPlayer() || (this.recentlyHit > 0 && this.canDropLoot() && this.world.getGameRules().getBoolean("doMobLoot")))) {
                int i = this.getExperiencePoints(this.attackingPlayer);
                i = ForgeEventFactory.getExperienceDrop(this, this.attackingPlayer, i);
                while (i > 0) {
                    int j = EntityXPOrb.getXPSplit(i);
                    i -= j;
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
        }
    }

    @Override
    protected int getExperiencePoints(EntityPlayer player) {
        return 0;
    }

    @Override
    @Nullable
    public ItemStack getHeldItemMainhand() {
        if (this.isMorph && this.morphHost != null) {
            return this.morphHost.getHeldItemMainhand();
        }
        return this.itemHandler != null ? this.itemHandler.getStackInSlot(22) : ItemStack.EMPTY;
    }

    @Override
    @Nullable
    public ItemStack getHeldItemOffhand() {
        if (this.isMorph && this.morphHost != null) {
            return this.morphHost.getHeldItemOffhand();
        }
        return this.itemHandler != null ? this.itemHandler.getStackInSlot(23) : ItemStack.EMPTY;
    }

    public boolean canShowHeldItem() {
        return this.getStateFlag(25) && this.getAttackTick() <= 0 && this.getAttackTick2() <= 0;
    }

    public byte[] getBodyHeightStand() {
        return this.BodyHeightStand;
    }

    public byte[] getBodyHeightSit() {
        return this.BodyHeightSit;
    }

    public Enums.BodyHeight getBodyIDFromHeight() {
        return EntityHelper.getBodyIDFromHeight(this.getHitHeight(), this);
    }

    public Enums.BodySide getHitAngleID() {
        return EntityHelper.getHitAngleID(this.getHitAngle());
    }

    @Override
    public void dismountEntity(Entity mount) {
        if (mount != null) {
            this.setPositionAndUpdate(mount.posX, mount.posY + 1.0, mount.posZ);
        } else {
            super.dismountEntity(this.getRidingEntity());
        }
    }

    @Override
    public HashMap<Integer, Integer> getBuffMap() {
        if (this.BuffMap == null) {
            this.BuffMap = new HashMap<>();
        }
        return this.BuffMap;
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
        if (data instanceof AttrsAdv) {
            this.shipAttrs = (AttrsAdv) data;
        }
    }

    @Override
    public HashMap<Integer, int[]> getAttackEffectMap() {
        if (this.AttackEffectMap == null) {
            this.AttackEffectMap = new HashMap<>();
        }
        return this.AttackEffectMap;
    }

    @Override
    public boolean isGlowing() {
        if (this.world.isRemote && this.isInvisible() && ClientProxy.getClientPlayer() != null && TeamHelper.checkSameOwner(this, ClientProxy.getClientPlayer())) {
            return true;
        }
        return super.isGlowing();
    }

    @Override
    public MissileData getMissileData(int type) {
        return this.MissileData[type];
    }

    public void resetMissileData() {
        this.MissileData = new MissileData[5];
        for (int i = 0; i < 5; ++i) {
            this.MissileData[i] = new MissileData();
        }
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
