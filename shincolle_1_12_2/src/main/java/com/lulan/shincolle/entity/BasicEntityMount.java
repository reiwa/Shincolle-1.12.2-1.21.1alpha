package com.lulan.shincolle.entity;

import com.lulan.shincolle.ShinColle;
import com.lulan.shincolle.ai.*;
import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.client.render.IShipCustomTexture;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.reference.unitclass.AttrsAdv;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public abstract class BasicEntityMount extends EntityCreature
        implements IShipMount, IShipCannonAttack, IShipGuardian, IShipCustomTexture, IShipMorph {

    protected static final IAttribute MAX_HP = new RangedAttribute(null, "generic.maxHealth", 4.0, 0.0, 30000.0).setDescription("Max Health").setShouldWatch(true);

    private static final int KEY_FORWARD = 1;
    private static final int KEY_BACK = 2;
    private static final int KEY_LEFT = 4;
    private static final int KEY_RIGHT = 8;
    private static final int KEY_JUMP = 16;

    private static final int AI_TASK_PRIO_GUARD = 1;
    private static final int AI_TASK_PRIO_FOLLOW = 2;
    private static final int AI_TASK_PRIO_MELEE = 12;
    private static final int AI_TASK_PRIO_DOOR = 21;
    private static final int AI_TASK_PRIO_FLOAT = 22;
    private static final int AI_TASK_PRIO_WANDER = 25;

    private enum AttackType {
        MELEE(0), LIGHT(1), HEAVY(2), AIR_LIGHT(3), AIR_HEAVY(4);
        final int id;
        AttackType(int id) { this.id = id; }
    }

    protected Attrs shipAttrs;
    public BasicEntityShip host;
    protected ShipPathNavigate shipNavigator;
    protected ShipMoveHelper shipMoveHelper;
    protected int revengeTime;
    protected int soundHurtDelay;
    protected int stateEmotion;
    protected int stateEmotion2;
    protected int attackTime;
    protected int attackTime2;
    protected int startEmotion;
    protected int startEmotion2;
    protected float[] seatPos;
    protected float[] seatPos2;
    protected double shipDepth;
    public int keyPressed;
    public int keyTick;
    public static boolean stopAI = false;
    protected boolean isMorph = false;
    protected EntityPlayer morphHost;

    protected BasicEntityMount(World world) {
        super(world);
        this.isImmuneToFire = true;
        this.ignoreFrustumCheck = true;
        this.stepHeight = 3.0f;
        this.shipAttrs = new Attrs();
    }

    public abstract void initAttrs(BasicEntityShip host);

    @Override
    public void onUpdate() {
        if (stopAI) return;
        super.onUpdate();
        if (this.soundHurtDelay > 0) --this.soundHurtDelay;
        EntityHelper.checkDepth(this);
        if (world.isRemote) {
            updateClientLogic();
        } else {
            updateServerLogic();
        }
        handleMovement();
        if ((this.ticksExisted & 0x7F) == 0) this.setAir(300);
    }

    private void updateClientLogic() {
        if (this.shipDepth > 0.0) spawnMovingParticle();
        if (this.ticksExisted % 32 == 0) {
            this.setStateEmotion(1, this.getPassengers().size() > 1 ? 1 : 0, false);
            if ((this.ticksExisted & 0x7F) == 0 && this.ticksExisted > 250 && this.host != null) {
                this.ticksExisted = this.host.ticksExisted;
            }
        }
    }

    private void updateServerLogic() {
        if (!checkHostExistence()) return;
        if (this.keyTick > 0) this.getShipNavigate().clearPathEntity();
        EntityHelper.updateShipNavigator(this);
        updateTargetingAndNavigation();
    }

    private boolean checkHostExistence() {
        if (this.host == null || this.host.getRidingEntity() != this) {
            this.setDead();
            return false;
        }
        return true;
    }

    private void updateTargetingAndNavigation() {
        if (this.ticksExisted % 8 == 0) {
            this.setEntityTarget(this.host.getEntityTarget());
            if ((this.ticksExisted & 0xF) == 0) {
                updateWaypointMovement();
                if ((this.ticksExisted & 0x1F) == 0) {
                    setupAttrs();
                    if ((this.ticksExisted & 0x7F) == 0) sendSyncPacket(S2CEntitySync.PID.SyncEntity_Emo);
                }
            }
        }
    }

    private void updateWaypointMovement() {
        if (EntityHelper.updateWaypointMove(this) && this.host.getStateMinor(6) > 0) {
            this.shipNavigator.tryMoveToXYZ(this.getGuardedPos(0), this.getGuardedPos(1), this.getGuardedPos(2), 1.0);
            this.host.sendSyncPacket((byte) 9, true);
        }
    }

    private void spawnMovingParticle() {
        double motX = this.posX - this.prevPosX;
        double motZ = this.posZ - this.prevPosZ;
        double limit = 0.25;
        motX = MathHelper.clamp(motX, -limit, limit);
        motZ = MathHelper.clamp(motZ, -limit, limit);
        if (motX != 0.0 || motZ != 0.0) {
            ParticleHelper.spawnAttackParticleAt(this.posX + motX * 3.0, this.posY + 0.6, this.posZ + motZ * 3.0, -motX, this.width, -motZ, (byte) 47);
        }
    }

    private void handleMovement() {
        if (this.keyTick > 0) {
            --this.keyTick;
            EntityPlayer rider = (EntityPlayer) this.getControllingPassenger();
            if (this.host != null && !this.host.getStateFlag(2) && rider != null) {
                float yaw = rider.rotationYawHead * ((float) Math.PI / 180);
                float pitch = rider.rotationPitch * ((float) Math.PI / 180);
                this.applyMovement(pitch, yaw);
                this.rotationYaw = rider.rotationYaw;
                this.renderYawOffset = rider.renderYawOffset;
            }
        } else if (Math.abs(this.posX - this.prevPosX) > 0.001 || Math.abs(this.posZ - this.prevPosZ) > 0.001) {
            handleAIMovementRotation();
        } else {
            setRotationByRider();
        }
    }

    private void handleAIMovementRotation() {
        float[] degree = CalcHelper.getLookDegree(this.posX - this.prevPosX, this.posY - this.prevPosY, this.posZ - this.prevPosZ, true);
        this.rotationYaw = degree[0];
        getPassengers().stream()
                .filter(BasicEntityShip.class::isInstance)
                .forEach(r -> {
                    ((EntityLivingBase) r).prevRotationYawHead = this.prevRotationYawHead;
                    ((EntityLivingBase) r).rotationYawHead = this.rotationYawHead;
                    ((EntityLivingBase) r).prevRenderYawOffset = this.prevRenderYawOffset;
                    ((EntityLivingBase) r).renderYawOffset = this.renderYawOffset;
                    r.prevRotationYaw = this.prevRenderYawOffset;
                    r.rotationYaw = this.renderYawOffset;
                });
    }

    private void applyMovement(float pitch, float yaw) {
        boolean canMoveFreely = onGround || EntityHelper.checkEntityIsInLiquid(this);
        handleJump();
        handleForwardBackward(pitch, yaw, canMoveFreely);
        handleLeftRight(yaw, canMoveFreely);
        if (this.collidedHorizontally) this.motionY += 0.4;
    }

    private void handleJump() {
        if ((this.keyPressed & KEY_JUMP) > 0) {
            this.jumpHelper.setJumping();
            if (this.getShipDepth() > 0.0) {
                this.motionY = Math.min(this.motionY + getMoveSpeed() * 0.1f, 1.0);
            }
        }
    }

    private void handleForwardBackward(float pitch, float yaw, boolean canMoveFreely) {
        float movSpeed = getMoveSpeed();
        float[] moveVec = CalcHelper.rotateXZByAxis(movSpeed, 0.0f, yaw, 1.0f);
        float verticalMotion = 0.0f;
        if (pitch > 1.0f) verticalMotion = -0.1f;
        if (pitch < -1.0f) verticalMotion = 0.1f;
        if ((this.keyPressed & KEY_FORWARD) > 0) {
            this.motionX = MathHelper.clamp(this.motionX + moveVec[1] * 0.25f, -Math.abs(moveVec[1]), Math.abs(moveVec[1]));
            this.motionZ = MathHelper.clamp(this.motionZ + moveVec[0] * 0.25f, -Math.abs(moveVec[0]), Math.abs(moveVec[0]));
            if (canMoveFreely) this.motionY = MathHelper.clamp(this.motionY + verticalMotion, -movSpeed * 0.5f, movSpeed * 0.5f);
        }
        if ((this.keyPressed & KEY_BACK) > 0) {
            this.motionX = MathHelper.clamp(this.motionX - moveVec[1] * 0.25f, -Math.abs(moveVec[1]), Math.abs(moveVec[1]));
            this.motionZ = MathHelper.clamp(this.motionZ - moveVec[0] * 0.25f, -Math.abs(moveVec[0]), Math.abs(moveVec[0]));
            if (canMoveFreely) this.motionY = MathHelper.clamp(this.motionY - verticalMotion, -movSpeed * 0.5f, movSpeed * 0.5f);
        }
    }

    private void handleLeftRight(float yaw, boolean canMoveFreely) {
        float movSpeed = getMoveSpeed();
        float[] moveVec = CalcHelper.rotateXZByAxis(0.0f, movSpeed, yaw, 1.0f);
        float multiplier = canMoveFreely ? 0.25f : 0.03125f;
        if ((this.keyPressed & KEY_LEFT) > 0) {
            this.motionX = MathHelper.clamp(this.motionX + moveVec[1] * multiplier, -Math.abs(moveVec[1]), Math.abs(moveVec[1]));
            this.motionZ = MathHelper.clamp(this.motionZ + moveVec[0] * multiplier, -Math.abs(moveVec[0]), Math.abs(moveVec[0]));
        }
        if ((this.keyPressed & KEY_RIGHT) > 0) {
            this.motionX = MathHelper.clamp(this.motionX - moveVec[1] * multiplier, -Math.abs(moveVec[1]), Math.abs(moveVec[1]));
            this.motionZ = MathHelper.clamp(this.motionZ - moveVec[0] * multiplier, -Math.abs(moveVec[0]), Math.abs(moveVec[0]));
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float atk) {
        if (world.isRemote || host == null) {
            if (host == null && !world.isRemote) setDead();
            return false;
        }
        if (isEntityInvulnerable(source) || source == DamageSource.OUT_OF_WORLD) {
            if (source == DamageSource.OUT_OF_WORLD) setDead();
            return false;
        }
        if (source == DamageSource.IN_WALL || source == DamageSource.STARVE || source == DamageSource.CACTUS || source == DamageSource.FALL) return false;
        Entity attacker = source.getTrueSource();
        if (attacker == null) return false;
        if (attacker.equals(this)) {
            host.setSitting(false);
            return false;
        }
        if (attacker instanceof EntityPlayer) {
            if (TeamHelper.checkSameOwner(attacker, this)) {
                host.setSitting(false);
                return super.attackEntityFrom(source, atk);
            }
            if (!ConfigHandler.friendlyFire) return false;
        }
        if (CombatHelper.canDodge(this, (float) this.getDistanceSq(attacker))) return false;
        this.setStateEmotion(1, 3, true);
        if (rand.nextInt(10) == 0) host.randomSensitiveBody();
        float reducedAtk = calculateDamageTaken(source, attacker, atk);
        if (reducedAtk <= 0.0f) return false;
        host.setSitting(false);
        if (rand.nextInt(5) == 0) host.applyEmotesReaction(2);
        return super.attackEntityFrom(source, reducedAtk);
    }

    private float calculateDamageTaken(DamageSource source, Entity attacker, float rawDamage) {
        float reducedAtk = rawDamage;
        if (source != DamageSource.MAGIC && source != DamageSource.WITHER && source != DamageSource.DRAGON_BREATH) {
            reducedAtk = CombatHelper.applyDamageReduceByDEF(this.shipAttrs, reducedAtk);
        }
        if (attacker instanceof IShipOwner && ((IShipOwner) attacker).getPlayerUID() > 0 && (attacker instanceof BasicEntityShip || attacker instanceof BasicEntitySummon || attacker instanceof BasicEntityMount)) {
            reducedAtk *= ConfigHandler.dmgSvS * 0.01f;
        }
        reducedAtk = BuffHelper.applyBuffOnDamageByResist(this, source, reducedAtk);
        reducedAtk = BuffHelper.applyBuffOnDamageByLight(this, source, reducedAtk);
        return Math.max(0f, reducedAtk < 1f && reducedAtk > 0f ? 1f : reducedAtk);
    }

    @Override
    public EnumActionResult applyPlayerInteraction(EntityPlayer player, Vec3d vec, EnumHand hand) {
        if (hand == EnumHand.OFF_HAND || world.isRemote) {
            return world.isRemote ? EnumActionResult.PASS : EnumActionResult.FAIL;
        }
        ItemStack stack = player.getHeldItem(hand);
        if (!stack.isEmpty() && handleItemInteraction(stack)) {
            return EnumActionResult.SUCCESS;
        }
        if (!player.isSneaking()) {
            return handleRiding(player);
        } else if (TeamHelper.checkSameOwner(player, this.host)) {
            return handleGui(player);
        }
        return EnumActionResult.PASS;
    }

    private boolean handleItemInteraction(ItemStack stack) {
        if (this.host != null) {
            if (stack.getItem() == ModItems.PointerItem && stack.getItemDamage() > 2) {
                if (this.host.getMorale() < 6630) this.host.addMorale(ConfigHandler.baseCaressMorale);
                return true;
            }
            if (stack.getItem() == Items.LEAD) {
                this.getShipNavigate().clearPathEntity();
                return true;
            }
        }
        return false;
    }

    private EnumActionResult handleRiding(EntityPlayer player) {
        if (!TeamHelper.checkIsBanned(this, player) && this.getDistanceSq(player) < 16.0) {
            player.startRiding(this, true);
            this.stateEmotion = 1;
            this.sendSyncPacket(S2CEntitySync.PID.SyncShip_Riders);
            return EnumActionResult.SUCCESS;
        }
        if (TeamHelper.checkSameOwner(player, this.host)) {
            this.host.setEntitySit(!this.host.isSitting());
            this.isJumping = false;
            this.getShipNavigate().clearPathEntity();
            this.getNavigator().clearPath();
            this.setAttackTarget(null);
            this.setEntityTarget(null);
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.PASS;
    }

    private EnumActionResult handleGui(EntityPlayer player) {
        int eid = this.host.getEntityId();
        FMLNetworkHandler.openGui(player, ShinColle.instance, 0, this.world, eid, 0, 0);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        float atk = this.getAttackBaseDamage(AttackType.MELEE.id, target);
        prepareAttack(0, -1);
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        this.applySoundAtAttacker(AttackType.MELEE.id);
        this.applyParticleAtAttacker(AttackType.MELEE.id, distVec);
        boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this), atk);
        if (isTargetHurt) {
            applyPostAttackEffects(target);
        }
        return isTargetHurt;
    }

    @Override
    public boolean attackEntityWithAmmo(Entity target) {
        if (!this.host.decrAmmoNum(0, this.host.getAmmoConsumption())) return false;
        prepareAttack(1, 0);
        float atk = this.getAttackBaseDamage(AttackType.LIGHT.id, target);
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        this.applySoundAtAttacker(AttackType.LIGHT.id);
        this.applyParticleAtAttacker(AttackType.LIGHT.id, distVec);
        atk = CombatHelper.applyCombatRateToDamage(this, true, (float) distVec.d, atk);
        atk = CombatHelper.applyDamageReduceOnPlayer(target, atk);
        if (!TeamHelper.doFriendlyFire(this, target)) atk = 0.0f;
        boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this).setProjectile(), atk);
        if (isTargetHurt) {
            applyPostAttackEffects(target);
        }
        return isTargetHurt;
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        if (!this.host.decrAmmoNum(1, this.host.getAmmoConsumption())) return false;
        prepareAttack(2, 1);
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        this.applySoundAtAttacker(AttackType.HEAVY.id);
        this.applyParticleAtAttacker(AttackType.HEAVY.id, distVec);
        launchMissile(target);
        this.applySoundAtTarget();
        this.applyParticleAtTarget(AttackType.HEAVY.id, target);
        this.host.applyEmotesReaction(3);
        if (ConfigHandler.canFlare) this.host.flareTarget(target);
        return true;
    }

    private void prepareAttack(int expGainType, int grudgeConsumeType) {
        host.addShipExp(ConfigHandler.expGain[expGainType]);
        if (grudgeConsumeType >= 0) host.decrGrudgeNum(ConfigHandler.consumeGrudgeAction[grudgeConsumeType]);
        host.setCombatTick(this.ticksExisted);
    }

    private void applyPostAttackEffects(Entity target) {
        if (!TeamHelper.checkSameOwner(this.getHostEntity(), target)) {
            BuffHelper.applyBuffOnTarget(target, this.getAttackEffectMap());
        }
        this.applySoundAtTarget();
        this.applyParticleAtTarget(AttackType.MELEE.id, target);
        this.host.applyEmotesReaction(3);
        if (ConfigHandler.canFlare) this.host.flareTarget(target);
    }

    private void launchMissile(Entity target) {
        float atk = this.getAttackBaseDamage(AttackType.HEAVY.id, target);
        float launchPos = (float) this.posY + this.height * 0.5f;
        int moveType = CombatHelper.calcMissileMoveType(this, target.posY, 2);
        if (moveType == 0) launchPos = (float) this.posY + this.height * 0.3f;
        float tarX = (float) target.posX;
        float tarY = (float) target.posY;
        float tarZ = (float) target.posZ;
        if (this.rand.nextFloat() <= CombatHelper.calcMissRate(this, getDistance(target))) {
            tarX += (this.rand.nextFloat() - 0.5f) * 10.0f;
            tarY += this.rand.nextFloat() * 5.0f;
            tarZ += (this.rand.nextFloat() - 0.5f) * 10.0f;
            ParticleHelper.spawnAttackTextParticle(this, 0);
        }
        MissileData md = this.getMissileData(2);
        float[] data = new float[]{atk, 0.15f, launchPos, tarX, tarY + target.height * 0.2f, tarZ, 160.0f, 0.25f, md.vel0, md.accY1, md.accY2};
        EntityAbyssMissile missile = new EntityAbyssMissile(this.world, this, md.type, moveType, data);
        this.world.spawnEntity(missile);
    }

    @Override public boolean updateSkillAttack(Entity target) { return false; }

    @Override
    public void travel(float strafe, float forward, float vertical) {
        EntityHelper.travel(this, strafe, forward);
    }

    protected void setRotationByRider() { getPassengers().stream().filter(BasicEntityShip.class::isInstance).forEach(r -> r.rotationYaw = ((BasicEntityShip) r).renderYawOffset); }
    public void setAIList() {
        if (world.isRemote) return;
        this.clearAITasks();
        this.clearAITargetTasks();
        this.tasks.addTask(AI_TASK_PRIO_GUARD, new EntityAIShipGuarding(this));
        this.tasks.addTask(AI_TASK_PRIO_FOLLOW, new EntityAIShipFollowOwner(this));
        if (this.getStateFlag(3)) this.tasks.addTask(AI_TASK_PRIO_MELEE, new EntityAIShipAttackOnCollide(this, 1.0));
        this.tasks.addTask(AI_TASK_PRIO_DOOR, new EntityAIShipOpenDoor(this, true));
        this.tasks.addTask(AI_TASK_PRIO_FLOAT, new EntityAIShipFloating(this));
        this.tasks.addTask(AI_TASK_PRIO_WANDER, new EntityAIShipWander(this, 12, 7, 0.8));
    }
    @Nullable @Override protected SoundEvent getAmbientSound() { return null; }
    @Nullable @Override protected SoundEvent getHurtSound(DamageSource source) { return null; }
    @Nullable @Override protected SoundEvent getDeathSound() { return null; }
    @Override protected float getSoundVolume() { return ConfigHandler.volumeShip * 0.4f; }
    @Override protected float getSoundPitch() { return 1.0f; }
    @Override protected void collideWithEntity(Entity target) { if (!target.equals(this.getRidingEntity()) && !this.isPassenger(target) && !(target instanceof BasicEntityAirplane)) target.applyEntityCollision(this); }
    @Override public boolean canBeLeashedTo(EntityPlayer player) { return player.world.isRemote || TeamHelper.checkSameOwner(this, player); }
    @Override public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if ((this.world == null || !this.world.isRemote && this.ticksExisted <= 0) && nbt.hasKey("Passengers", 9)) {
            NBTTagList list = nbt.getTagList("Passengers", 10);
            for (int i = 0; i < list.tagCount(); ++i) {
                NBTTagCompound rider = list.getCompoundTagAt(i);
                NBTTagList pos = rider.getTagList("Pos", 6);
                pos.set(0, new NBTTagDouble(this.posX));
                pos.set(2, new NBTTagDouble(this.posZ));
            }
        }
    }
    @Override public boolean writeToNBTOptional(NBTTagCompound nbt) { getPassengers().forEach(r -> r.setPosition(this.posX, r.posY, this.posZ)); return super.writeToNBTOptional(nbt); }
    @Override public void setStateEmotion(int id, int value, boolean sync) { if(id == 1) this.stateEmotion = value; else this.stateEmotion2 = value; if (sync && !this.world.isRemote) sendSyncPacket(S2CEntitySync.PID.SyncEntity_Emo); }
    public void setupAttrs() {
        if (this.host == null) return;
        this.shipAttrs = AttrsAdv.copyAttrsAdv((AttrsAdv) this.host.getAttrs());
        float newMaxHP = this.host.getMaxHealth() * 0.5f;
        this.shipAttrs.setAttrsBuffed(0, newMaxHP);
        this.shipAttrs.setAttrsBuffed(5, this.host.getAttrs().getDefense() * 0.5f);
        this.getEntityAttribute(MAX_HP).setBaseValue(newMaxHP);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(this.host.getAttrs().getMoveSpeed());
        this.getEntityAttribute(SWIM_SPEED).setBaseValue(this.host.getAttrs().getMoveSpeed());
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64.0);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(this.host.getAttrs().getAttrsBuffed(20));
    }
    @Override public IAttributeInstance getEntityAttribute(IAttribute attribute) { if (attribute == SharedMonsterAttributes.MAX_HEALTH) this.getAttributeMap().getAttributeInstance(MAX_HP); return super.getEntityAttribute(attribute); }
    @Override protected void applyEntityAttributes() { this.getAttributeMap().registerAttribute(MAX_HP); this.getAttributeMap().registerAttribute(SharedMonsterAttributes.FOLLOW_RANGE); this.getAttributeMap().registerAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE); this.getAttributeMap().registerAttribute(SharedMonsterAttributes.MOVEMENT_SPEED); this.getAttributeMap().registerAttribute(SWIM_SPEED); this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ARMOR); this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS); }
    public void sendSyncPacket(byte pid) { if (!this.world.isRemote) { NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0); CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, pid), point); }}
    @Nullable @Override public Entity getControllingPassenger() { return getPassengers().stream().filter(EntityPlayer.class::isInstance).findFirst().orElse(null); }
    @Override public void updatePassenger(Entity passenger) {
        if (!this.isDead && this.isPassenger(passenger)) {
            double pX = this.posX;
            double pY;
            double pZ = this.posZ;
            float[] ridePos;
            if (passenger instanceof BasicEntityShip) ridePos = CalcHelper.rotateXZByAxis(this.seatPos[0], this.seatPos[2], this.renderYawOffset * 0.017453292F, 1.0F);
            else ridePos = CalcHelper.rotateXZByAxis(this.seatPos2[0], this.seatPos2[2], this.renderYawOffset * 0.017453292F, 1.0F);
            pX += ridePos[1];
            pY = this.posY + (passenger instanceof BasicEntityShip ? this.seatPos[1] : this.seatPos2[1]) + passenger.getYOffset();
            pZ += ridePos[0];
            passenger.setPosition(pX, pY, pZ);
        }
    }

    public void clearRider() {
        List<Entity> riders = this.getPassengers();
        for (int i = 0; i < riders.size(); ++i) {
            Entity rider = riders.get(i);
            if (rider == null) continue;
            rider.dismountRidingEntity();
        }
        this.setDead();
    }

    @Override public void heal(float healAmount) { if (!this.world.isRemote) { NetworkRegistry.TargetPoint tp = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 48.0); CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 23, 0.0, 0.1, 0.0), tp); healAmount = BuffHelper.applyBuffOnHeal(this, healAmount); } super.heal(healAmount); }
    @Override public int getStateEmotion(int id) { return id == 1 ? this.stateEmotion : this.stateEmotion2; }
    @Override public void setShipDepth(double par1) { this.shipDepth = par1; }
    @Override public boolean getStateFlag(int flag) { return this.host != null && this.host.getStateFlag(flag); }
    @Override public void setStateFlag(int id, boolean flag) { if (this.host != null) this.host.setStateFlag(id, flag); }
    @Override public int getFaceTick() { return this.startEmotion; }
    @Override public int getHeadTiltTick() { return this.startEmotion2; }
    @Override public void setFaceTick(int par1) { this.startEmotion = par1; }
    @Override public void setHeadTiltTick(int par1) { this.startEmotion2 = par1; }
    @Override public int getTickExisted() { return this.ticksExisted; }
    @Override public int getAttackTick() { return this.attackTime; }
    protected void clearAITasks() { this.tasks.taskEntries.clear(); }
    protected void clearAITargetTasks() { this.targetTasks.taskEntries.clear(); }
    @Override public Entity getEntityTarget() { return this.host != null ? this.host.getEntityTarget() : null; }
    @Override public void setEntityTarget(Entity target) { if (this.host != null) this.host.setEntityTarget(target); }
    public float getAttackBaseDamage(int type, Entity target) { if (this.getAttrs() == null) return 0.0f; switch (type) { case 1: return CombatHelper.modDamageByAdditionAttrs(this.host, target, this.getAttrs().getAttrsBuffed(1), 0); case 2: return this.getAttrs().getAttrsBuffed(2); case 3: return this.getAttrs().getAttrsBuffed(3); case 4: return this.getAttrs().getAttrsBuffed(4); default: return this.getAttrs().getAttrsBuffed(1); }}
    @Override public int getLevel() { return this.host != null ? this.host.getLevel() : 150; }
    @Override public float getMoveSpeed() { return !this.getIsSitting() && this.getAttrs() != null ? this.getAttrs().getMoveSpeed() : 0.0f; }
    @Override public float getJumpSpeed() { return 2.0f; }
    @Override public boolean getIsLeashed() { return this.host != null && (this.host.getIsLeashed() || this.getLeashed()); }
    @Override public int getStateMinor(int id) { return this.host != null ? this.host.getStateMinor(id) : 0; }
    @Override public void setStateMinor(int state, int par1) { if (this.host != null) this.host.setStateMinor(state, par1); }
    @Override public int getAmmoLight() { return this.host != null ? this.host.getAmmoLight() : 0; }
    @Override public int getAmmoHeavy() { return this.host != null ? this.host.getAmmoHeavy() : 0; }
    @Override public boolean useAmmoLight() { return this.host != null && this.host.useAmmoLight(); }
    @Override public boolean useAmmoHeavy() { return this.host != null && this.host.useAmmoHeavy(); }
    @Override public boolean hasAmmoLight() { return this.getAmmoLight() > 0; }
    @Override public boolean hasAmmoHeavy() { return this.getAmmoHeavy() > 0; }
    @Override public void setAmmoLight(int num) {}

    @Override public boolean getAttackType(int par1) { return this.host == null || this.host.getAttackType(par1); }
    @Override public boolean getIsRiding() { return false; }
    @Override public boolean getIsSprinting() { return false; }
    @Override public boolean getIsSitting() { return this.host != null && this.host.getIsSitting(); }
    @Override public boolean getIsSneaking() { return false; }
    @Override public double getShipDepth() { return this.shipDepth; }
    @Override public double getShipDepth(int type) { return type == 2 && this.host != null ? this.host.getShipDepth() : this.shipDepth; }
    @Override public boolean shouldDismountInWater(Entity rider) { return false; }
    @Override public ShipPathNavigate getShipNavigate() { return this.shipNavigator; }
    @Override public ShipMoveHelper getShipMoveHelper() { return this.shipMoveHelper; }
    @Override public boolean canFly() { return this.isPotionActive(MobEffects.LEVITATION); }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public void setEntitySit(boolean sit) { if (this.host != null) this.host.setEntitySit(sit); }
    @Override public Entity getGuardedEntity() { return this.host != null ? this.host.getGuardedEntity() : null; }
    @Override public void setGuardedEntity(Entity entity) { if (this.host != null) this.host.setGuardedEntity(entity); }
    @Override public int getGuardedPos(int vec) { return this.host != null ? this.host.getGuardedPos(vec) : -1; }
    @Override public void setGuardedPos(int x, int y, int z, int dim, int type) { if (this.host != null) this.host.setGuardedPos(x, y, z, dim, type); }

    @Override public int getPlayerUID() { return this.host != null ? this.host.getPlayerUID() : -1; }
    @Override public void setPlayerUID(int uid) {}
    @Override public Entity getHostEntity() { return this.host; }
    @Override public Entity getEntityRevengeTarget() { return this.host != null ? this.host.getEntityRevengeTarget() : null; }
    @Override public int getEntityRevengeTime() { return this.host != null ? this.host.getEntityRevengeTime() : 0; }
    @Override public void setEntityRevengeTarget(Entity target) { if (this.host != null) this.host.setEntityRevengeTarget(target); }
    @Override public void setEntityRevengeTime() { this.revengeTime = this.ticksExisted; }
    @Override public double getMountedYOffset() { return this.height; }
    @Override public boolean canPassengerSteer() { return false; }
    @Override public int getAttackTick2() { return this.attackTime2; }
    @Override public void setAttackTick(int par1) { this.attackTime = par1; }
    @Override public void setAttackTick2(int par1) { this.attackTime2 = par1; }
    @Override public float getSwingTime(float partialTick) { return this.getSwingProgress(partialTick); }
    @Override public BlockPos getLastWaypoint() { return this.host != null ? this.host.getLastWaypoint() : BlockPos.ORIGIN; }
    @Override public void setLastWaypoint(BlockPos pos) { if (this.host != null) this.host.setLastWaypoint(pos); }
    @Override public int getWpStayTime() { return this.host != null ? this.host.getStateTimer(4) : 0; }
    @Override public int getWpStayTimeMax() { return this.host != null ? this.host.getWpStayTimeMax() : 0; }
    @Override public void setWpStayTime(int time) { if (this.host != null) this.host.setStateTimer(4, time); }
    @Override public int getRidingState() { return 0; }
    @Override public void setRidingState(int state) {}
    @Override public int getDamageType() { return this.host != null ? this.host.getDamageType() : 0; }
    @Override public boolean isJumping() { return this.isJumping; }
    @Override public int getScaleLevel() { return 0; }
    @Override public void setScaleLevel(int par1) {}
    @Override public Random getRand() { return this.rand; }
    @Override public double getShipFloatingDepth() { return 0.3; }

    @Override public float[] getSeatPos() { return this.seatPos; }
    @Override public void setSeatPos(float[] pos) { this.seatPos = pos; }

    @Override public int getStateTimer(int id) { if (this.host != null) return this.host.getStateTimer(id); return 0; }
    @Override public void setStateTimer(int id, int value) { if (this.host != null) this.host.setStateTimer(id, value); }
    @Override public HashMap<Integer, Integer> getBuffMap() { return this.host != null ? this.host.getBuffMap() : new HashMap<>(); }
    @Override public void setBuffMap(HashMap<Integer, Integer> map) {}
    @Override public Attrs getAttrs() { return this.host != null ? this.host.getAttrs() : null; }
    @Override public void setAttrs(Attrs data) {}
    @Override public void setUpdateFlag(int id, boolean value) {}
    @Override public boolean getUpdateFlag(int id) { return false; }
    @Override public HashMap<Integer, int[]> getAttackEffectMap() { return this.host != null ? this.host.getAttackEffectMap() : new HashMap<>(); }

    @Override public MissileData getMissileData(int type) { return this.host != null ? this.host.getMissileData(type) : new MissileData(); }

    @Override public boolean isMorph() { return this.isMorph; }
    @Override public void setIsMorph(boolean par1) { this.isMorph = par1; }
    @Override public EntityPlayer getMorphHost() { return this.morphHost; }
    @Override public void setMorphHost(EntityPlayer player) { this.morphHost = player; }
    public void applyParticleAtAttacker(int type, Dist4d distVec) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        if(AttackType.values()[type] == AttackType.LIGHT){
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 19, this.posX, this.posY + 1.5, this.posZ, distVec.x, 1.2f, distVec.z, true), point);
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this.host, 0, true), point);
        } else {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this.host, 0, true), point);
        }
    }
    public void applyParticleAtTarget(int type, Entity target) { NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0); if(AttackType.values()[type] == AttackType.LIGHT) CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 9, false), point); else CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 1, false), point); }
    public void applySoundAtAttacker(int type) {
        switch (AttackType.values()[type]) {
            case LIGHT:
                this.playSound(ModSounds.SHIP_FIRELIGHT, ConfigHandler.volumeFire, this.getSoundPitch() * 0.85f);
                if (this.rand.nextInt(20) == 0) break;
            case HEAVY: this.playSound(ModSounds.SHIP_FIREHEAVY, ConfigHandler.volumeFire, this.getSoundPitch() * 0.85f);
                if (this.rand.nextInt(20) == 0) break;
            case AIR_LIGHT:
            case AIR_HEAVY:
                this.playSound(ModSounds.SHIP_AIRCRAFT, ConfigHandler.volumeFire * 0.5f, this.getSoundPitch() * 0.85f); break;
            default:
                break;
        }
    }
    public void applySoundAtTarget() {}
}