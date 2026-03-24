package com.lulan.shincolle.entity.other;

import com.lulan.shincolle.client.render.IShipCustomTexture;
import com.lulan.shincolle.entity.*;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Map;

public class EntityAbyssMissile
        extends Entity
        implements IShipOwner, IShipAttrs, IShipFlyable, IShipCustomTexture, IShipProjectile {

    private static final int DATA_ATK = 0;
    private static final int DATA_ARMOR_PEN = 1;
    private static final int DATA_POS_Y = 2;
    private static final int DATA_TARGET_X = 3;
    private static final int DATA_TARGET_Y = 4;
    private static final int DATA_TARGET_Z = 5;
    private static final int DATA_LIFE = 6;
    private static final int DATA_ARC_FACTOR = 7;
    private static final int DATA_VELOCITY_INITIAL = 8;
    private static final int DATA_ACCEL_Y1 = 9;
    private static final int DATA_ACCEL_Y2 = 10;
    private static final int DATA_START_X = 11;
    private static final int DATA_START_Y = 12;
    private static final int DATA_START_Z = 13;
    private static final int DATA_INVISIBLE_TICKS = 14;

    private static final double MIN_DIST_FOR_ARC = 4.0;
    private static final double ARC_ACCEL_LIMIT = 0.15;
    private static final float SUBMUNITION_ATK_MULTI = 0.5f;
    private static final float SUBMUNITION_POS_Y_OFFSET = -0.75f;
    private static final float SUBMUNITION_START_POS_Y_OFFSET = 0.65f;
    private static final int SUBMUNITION_LIFE = 140;
    private static final float SUBMUNITION_ARC_FACTOR = 0.0f;
    private static final float SUBMUNITION_VEL_INITIAL = 0.5f;
    private static final float SUBMUNITION_ACCEL_Y = -0.06f;
    private static final int SUBMUNITION_INVIS_TICKS = 4;
    private static final double TORPEDO_VEL_MULTIPLIER = 0.85;
    private static final double TORPEDO_ACCEL_MULTIPLIER = 1.05;
    private static final double SUBMUNITION_VEL_MULTIPLIER = 0.95;
    private static final double EXPLOSION_RADIUS = 3.5;
    private static final int MIN_TICKS_FOR_COLLISION = 5;
    private static final int SUBMUNITION_SPAWN_START_TICK = 6;
    private static final int SUBMUNITION_SPAWN_END_TICK = 41;
    private static final int SUBMUNITION_SPAWN_INTERVAL = 8;
    private static final float HIGH_SPEED_THRESHOLD = 0.55f;
    private static final float LOW_SPEED_THRESHOLD = 0.45f;
    private static final int TORPEDO_START_DELAY = 3;
    private static final float MIN_DMG_TO_DESTROY = 8.0f;

    public enum MoveType {
        DIRECT, ARC, TORPEDO, ARC_HOMING, PRESET_VELOCITY;
        public static MoveType fromId(int id) {
            return id >= 0 && id < values().length ? values()[id] : DIRECT;
        }
    }

    protected IShipAttackBase host;
    protected EntityLiving host2;
    protected int playerUID;
    protected Attrs attrs;
    protected int type;
    public MoveType moveType;
    protected float[] data;
    public int life;
    public Map<Integer, int[]> effectMap;
    public boolean startMove;
    public int startMoveDelay;
    public double vel0;
    public double velX;
    public double velY;
    public double velZ;
    public double accY1;
    public double accY2;
    public double t0;
    public double t1;
    public int invisibleTicks;

    public EntityAbyssMissile(World world) {
        super(world);
        this.setSize(1.0f, 1.0f);
        this.data = new float[11];
    }

    public EntityAbyssMissile(World world, IShipAttackBase host, int type, int moveType, float[] data) {
        this(world);
        this.host = host;
        this.host2 = (EntityLiving) host;
        this.effectMap = host.getAttackEffectMap();
        this.setPlayerUID(host.getPlayerUID());
        this.type = type;
        this.moveType = MoveType.fromId(moveType);
        this.data = data;
        this.life = (int) data[DATA_LIFE];
        this.attrs = new Attrs();
        this.startMove = false;
        this.startMoveDelay = 0;
        initializeAttributes();
        initializePosition();
        initializeMovement();
    }

    private void initializeAttributes() {
        this.attrs.copyRaw2Buffed();
        this.attrs.setAttrsBuffed(1, data[DATA_ATK]);
        this.attrs.setAttrsBuffed(2, data[DATA_ATK]);
        this.attrs.setAttrsBuffed(3, data[DATA_ATK]);
        this.attrs.setAttrsBuffed(4, data[DATA_ATK]);
        this.attrs.setAttrsBuffed(15, 0.5f);
        this.attrs.setAttrsBuffed(20, data[DATA_ARMOR_PEN]);
        if (this.data.length > DATA_INVISIBLE_TICKS) {
            this.invisibleTicks = (int) this.data[DATA_INVISIBLE_TICKS];
        }
    }

    private void initializePosition() {
        if (this.data.length > DATA_START_Z) {
            data[DATA_POS_Y] = data[DATA_START_Y];
            this.posX = data[DATA_START_X];
            this.posY = data[DATA_START_Y];
            this.posZ = data[DATA_START_Z];
        } else {
            this.posX = this.host2.posX;
            this.posY = data[DATA_POS_Y];
            this.posZ = this.host2.posZ;
        }

        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.setPosition(this.posX, this.posY, this.posZ);
    }

    private void initializeMovement() {
        this.vel0 = data[DATA_VELOCITY_INITIAL];
        Vec3d targetVec = new Vec3d(data[DATA_TARGET_X], data[DATA_TARGET_Y], data[DATA_TARGET_Z]);
        Dist4d dist = CalcHelper.getDistanceFromA2B(this.getPositionVector(), targetVec);
        if (dist.d < MIN_DIST_FOR_ARC) {
            this.moveType = MoveType.DIRECT;
        }
        switch (this.moveType) {
            case DIRECT:
                setDirectMovement(dist, this.vel0);
                break;
            case ARC:
                initializeArcMovement(dist, this.vel0, targetVec.y);
                break;
            case TORPEDO:
                this.velX = dist.x * 0.6;
                this.velY = 0.1;
                this.velZ = dist.z * 0.6;
                this.accY1 = -0.035;
                break;
            case ARC_HOMING:
                setDirectMovement(dist, this.vel0);
                this.accY1 = -0.035;
                this.accY2 = -0.035;
                break;
            case PRESET_VELOCITY:
                this.velX = data[DATA_TARGET_X];
                this.velY = data[DATA_TARGET_Y];
                this.velZ = data[DATA_TARGET_Z];
                this.accY1 = data.length > DATA_ACCEL_Y1 ? data[DATA_ACCEL_Y1] : 0;
                break;
        }
    }

    private void setDirectMovement(Dist4d dist, double velocity) {
        this.velX = dist.x * velocity;
        this.velY = dist.y * velocity;
        this.velZ = dist.z * velocity;
        this.accY1 = 0.0;
        this.accY2 = 0.0;
    }

    private void initializeArcMovement(Dist4d dist, double initialVelocity, double targetY) {
        float arcFactor = data[DATA_ARC_FACTOR];
        if (arcFactor <= 0.0f) {
            setDirectMovement(dist, initialVelocity);
            return;
        }
        double dx = data[DATA_TARGET_X] - this.posX;
        double dz = data[DATA_TARGET_Z] - this.posZ;
        double dxz = MathHelper.sqrt(dx * dx + dz * dz);
        if (dxz <= MIN_DIST_FOR_ARC) {
            setDirectMovement(dist, initialVelocity);
            return;
        }
        dx /= dxz;
        dz /= dxz;
        double t = dxz / initialVelocity;
        double addHeight = dist.d * arcFactor;
        double dy = Math.abs(this.posY - targetY);
        this.velX = dx * initialVelocity;
        this.velZ = dz * initialVelocity;
        if (this.posY - targetY < 1.0) {
            double hy = MathHelper.sqrt(addHeight / (addHeight + dy));
            this.t0 = Math.floor(t / (1.0 + hy));
            this.t1 = Math.floor(t * hy / (1.0 + hy));
            this.velY = 2.0 * (addHeight + dy) / this.t0;
            this.accY1 = -this.velY / this.t0;
            this.accY2 = -2.0 * addHeight / (this.t1 * this.t1);
        } else {
            double hy = MathHelper.sqrt(addHeight / (addHeight + dy));
            this.t0 = Math.floor(t * hy / (1.0 + hy));
            this.t1 = Math.floor(t / (1.0 + hy));
            this.accY1 = -2.0 * addHeight / (this.t0 * this.t0);
            this.velY = -this.accY1 * this.t0;
            this.accY2 = -2.0 * (addHeight + dy) / (this.t1 * this.t1);
        }
        if (Math.abs(this.accY1) > ARC_ACCEL_LIMIT || Math.abs(this.accY2) > ARC_ACCEL_LIMIT) {
            setDirectMovement(dist, initialVelocity);
        }
    }

    @Override
    protected void entityInit() {}

    @Override
    public void onUpdate() {
        super.onUpdate();
        this.motionX = this.velX;
        this.motionY = this.velY;
        this.motionZ = this.velZ;
        this.posX += this.motionX;
        this.posY += this.motionY;
        this.posZ += this.motionZ;
        this.setPosition(this.posX, this.posY, this.posZ);
        handleMissileMovement();
        if (this.invisibleTicks > 0) {
            --this.invisibleTicks;
        }
        if (this.world.isRemote) {
            updateClientLogic();
        } else {
            updateServerLogic();
        }
    }

    private void updateServerLogic() {
        if (this.host == null) {
            this.setDead();
            return;
        }

        if (this.ticksExisted > this.life) {
            onImpact(null);
            return;
        }
        if (this.ticksExisted == 1) {
            sendSyncPacket();
        }
        if (this.type == 3) {
            spawnSubmunitions();
        }
        if (this.ticksExisted > MIN_TICKS_FOR_COLLISION) {
            checkCollisions();
        }
    }

    private void updateClientLogic() {
        updateClientRotation();
        spawnClientParticles();
    }

    private void sendSyncPacket() {
        float[] syncData = new float[]{0.0f, this.type, this.moveType.ordinal(), (float) this.velX, (float) this.velY, (float) this.velZ, (float) this.vel0, (float) this.accY1, (float) this.accY2, this.invisibleTicks};
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, (byte) 56, syncData), point);
    }

    private void spawnSubmunitions() {
        boolean canSpawn = this.ticksExisted > SUBMUNITION_SPAWN_START_TICK && this.ticksExisted < SUBMUNITION_SPAWN_END_TICK && (this.ticksExisted & (SUBMUNITION_SPAWN_INTERVAL - 1)) == 0;
        if (!canSpawn) {
            return;
        }
        float[] subData = new float[]{this.data[DATA_ATK] * SUBMUNITION_ATK_MULTI, this.data[DATA_ARMOR_PEN], (float) this.posY + SUBMUNITION_POS_Y_OFFSET, (float) this.motionX, (float) this.motionY, (float) this.motionZ, SUBMUNITION_LIFE, SUBMUNITION_ARC_FACTOR, SUBMUNITION_VEL_INITIAL, SUBMUNITION_ACCEL_Y, SUBMUNITION_ACCEL_Y, (float) this.posX, (float) this.posY - SUBMUNITION_START_POS_Y_OFFSET - MathHelper.abs((float) this.motionY), (float) this.posZ, SUBMUNITION_INVIS_TICKS};
        EntityAbyssMissile subMissile = new EntityAbyssMissile(this.world, this.host, 4, MoveType.PRESET_VELOCITY.ordinal(), subData);
        this.world.spawnEntity(subMissile);
    }

    private void checkCollisions() {
        if (this.world.getBlockState(new BlockPos(this)).getMaterial().isSolid()) {
            this.onImpact(null);
            return;
        }
        Vec3d start = new Vec3d(this.posX, this.posY, this.posZ);
        Vec3d end = new Vec3d(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
        RayTraceResult raytrace = this.world.rayTraceBlocks(start, end, false, true, false);
        if (raytrace != null && raytrace.typeOfHit != RayTraceResult.Type.MISS) {
            if (raytrace.typeOfHit == RayTraceResult.Type.BLOCK) {
                this.onImpact(null);
                return;
            }
            if (raytrace.typeOfHit == RayTraceResult.Type.ENTITY && isValidCollisionTarget(raytrace.entityHit)) {
                this.onImpact(raytrace.entityHit);
                return;
            }
        }
        this.world.getEntitiesWithinAABB(Entity.class, this.getEntityBoundingBox().expand(1.0, 1.5, 1.0)).stream().filter(this::isValidCollisionTarget).findFirst().ifPresent(this::onImpact);
    }

    private boolean isValidCollisionTarget(Entity entity) {
        return entity != null && entity.canBeCollidedWith() && EntityHelper.isNotHost(this, entity) && !TeamHelper.checkSameOwner(this.host2, entity);
    }

    private void updateClientRotation() {
        if (this.moveType == MoveType.TORPEDO && !this.startMove) {
            this.rotationPitch = 0.0f;
            this.rotationYaw = (float) Math.atan2(this.motionX, this.motionZ);
        } else {
            float f1 = MathHelper.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
            this.rotationPitch = (float) Math.atan2(this.motionY, f1);
            this.rotationYaw = (float) Math.atan2(this.motionX, this.motionZ);
        }
        this.rotationYaw = this.motionX > 0.0 ? (float) (this.rotationYaw - Math.PI) : (float) (this.rotationYaw + Math.PI);
    }

    private void spawnClientParticles() {
        if (this.invisibleTicks > 0) return;
        if (this.type == 2) {
            ParticleHelper.spawnAttackParticleAtEntity(this, 0.0, 10.0, 4.0, (byte) 9);
            return;
        }
        boolean canSpawn = (this.moveType == MoveType.TORPEDO && this.startMove) || (this.moveType != MoveType.TORPEDO && this.ticksExisted > 2);
        if (!canSpawn) return;
        double initialVelocity = data[DATA_VELOCITY_INITIAL];
        if (initialVelocity > HIGH_SPEED_THRESHOLD || initialVelocity < LOW_SPEED_THRESHOLD) {
            byte particleType = (byte) (initialVelocity > HIGH_SPEED_THRESHOLD ? 1 : 2);
            for (int j = 0; j < 4; ++j) {
                ParticleHelper.spawnAttackParticleAtEntity(this, particleType, new double[]{initialVelocity, j});
            }
        } else {
            byte smokeType = (this.type == 3 || this.type == 4) ? (byte) 18 : (byte) 15;
            for (int j = 0; j < 4; ++j) {
                ParticleHelper.spawnAttackParticleAt(this.posX + this.motionX * 2.0 - this.motionX * 1.5 * j, this.posY + this.motionY * 2.0 + 0.5 - this.motionY * 1.5 * j, this.posZ + this.motionZ * 2.0 - this.motionZ * 1.5 * j, -this.motionX * 0.1, -this.motionY * 0.1, -this.motionZ * 0.1, smokeType);
            }
        }
    }

    protected void handleMissileMovement() {
        if (this.type == 4) {
            this.velX *= SUBMUNITION_VEL_MULTIPLIER;
            this.velY += this.accY1;
            this.velZ *= SUBMUNITION_VEL_MULTIPLIER;
            return;
        }
        if(this.moveType == MoveType.ARC){
            handleArcMovement();
        } else if(this.moveType == MoveType.TORPEDO){
            handleTorpedoMovement();
        }
    }

    private void handleArcMovement() {
        if (this.ticksExisted <= this.t0) {
            this.velY += this.accY1;
        } else {
            this.velY += this.accY2;
        }
    }

    private void handleTorpedoMovement() {
        if (this.world.isRemote) {
            if (this.startMove) {
                accelerateTorpedo();
            } else {
                descendTorpedo();
            }
        } else {
            if (this.startMove) {
                if (this.startMoveDelay > 0) {
                    --this.startMoveDelay;
                } else if (this.startMoveDelay == 0) {
                    launchTorpedo();
                    --this.startMoveDelay;
                } else {
                    accelerateTorpedo();
                }
            } else {
                descendTorpedo();
                if (BlockHelper.checkBlockIsLiquid(this.world.getBlockState(new BlockPos(this)))) {
                    this.startMove = true;
                    this.startMoveDelay = TORPEDO_START_DELAY;
                }
            }
        }
    }

    private void descendTorpedo() {
        this.velX *= TORPEDO_VEL_MULTIPLIER;
        this.velY += this.accY1;
        this.velZ *= TORPEDO_VEL_MULTIPLIER;
    }

    private void accelerateTorpedo() {
        if ((velX * velX + velY * velY + velZ * velZ) < 2.0) {
            double accel = data.length > DATA_ACCEL_Y2 ? data[DATA_ACCEL_Y2] : TORPEDO_ACCEL_MULTIPLIER;
            this.velX *= accel;
            this.velY *= accel;
            this.velZ *= accel;
        }
    }

    private void launchTorpedo() {
        Dist4d dist = CalcHelper.getDistanceFromA2B(this.getPositionVector(), new Vec3d(this.data[DATA_TARGET_X], this.data[DATA_TARGET_Y], this.data[DATA_TARGET_Z]));
        this.velX = dist.x * this.vel0 * 0.25;
        this.velY = dist.y * this.vel0 * 0.25;
        this.velZ = dist.z * this.vel0 * 0.25;
        if (this.velY > 0.003) this.velY = 0.003;
        float[] syncData = new float[]{2.0f, this.type, this.moveType.ordinal(), (float) this.velX, (float) this.velY, (float) this.velZ, 1.0f, (float) this.vel0, (float) this.accY1, (float) this.accY2};
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, (byte) 56, syncData), point);
    }

    protected void onImpact(Entity target) {
        this.playSound(ModSounds.SHIP_EXPLODE, ConfigHandler.volumeFire * 1.5f, 0.7f / (this.rand.nextFloat() * 0.4f + 0.8f));
        if (this.world.isRemote || this.host == null) {
            this.setDead();
            return;
        }
        handleExplosion();
        this.setDead();
    }

    private void handleExplosion() {
        CombatHelper.specialAttackEffect(this.host, this.type, new float[]{(float) this.posX, (float) this.posY, (float) this.posZ});
        List<Entity> hitList = this.world.getEntitiesWithinAABB(Entity.class, this.getEntityBoundingBox().expand(EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS));
        hitList.stream().filter(ent -> ent.canBeCollidedWith() && EntityHelper.isNotHost(this, ent) && !TargetHelper.isEntityInvulnerable(ent)).forEach(this::applyDamageAndEffects);
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 2, false), point);
    }

    private void applyDamageAndEffects(Entity target) {
        float missileAtk = this.attrs.getAttackDamage();
        missileAtk = CombatHelper.modDamageByAdditionAttrs(this, target, missileAtk, 0);
        if (TeamHelper.checkSameOwner(this.host2, target)) {
            return;
        }
        missileAtk = CombatHelper.applyCombatRateToDamage(this.host, false, 1.0f, missileAtk);
        missileAtk = CombatHelper.applyDamageReduceOnPlayer(target, missileAtk);
        if (!TeamHelper.doFriendlyFire(this.host, target)) {
            missileAtk = 0.0f;
        }
        if (missileAtk > 0 && target.attackEntityFrom(DamageSource.causeMobDamage(this.host2).setExplosion(), missileAtk)) {
            BuffHelper.applyBuffOnTarget(target, this.effectMap);
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float atk) {
        if (this.world.isRemote) return false;
        if (this.host == null) {
            this.setDead();
            return false;
        }
        if (source == DamageSource.IN_WALL || source == DamageSource.STARVE || source == DamageSource.CACTUS || source == DamageSource.FALL || source == DamageSource.LAVA || source == DamageSource.ON_FIRE || source == DamageSource.HOT_FLOOR || source == DamageSource.ANVIL || source == DamageSource.FALLING_BLOCK) {
            return false;
        }
        if (source == DamageSource.MAGIC || source == DamageSource.DRAGON_BREATH || source == DamageSource.WITHER || source == DamageSource.OUT_OF_WORLD) {
            this.onImpact(null);
            return true;
        }
        if (CombatHelper.canDodge(this, 0.0f) || this.isEntityInvulnerable(source)) {
            return false;
        }
        if (this.isEntityAlive() && atk > MIN_DMG_TO_DESTROY) {
            this.onImpact(null);
            return true;
        }
        return super.attackEntityFrom(source, atk);
    }

    @Override public void writeEntityToNBT(NBTTagCompound nbt) {}
    @Override public void readEntityFromNBT(NBTTagCompound nbt) {}
    @Override public boolean canBeCollidedWith() { return true; }
    @Override public float getCollisionBorderSize() { return 1.0f; }
    @Override public int getPlayerUID() { return this.playerUID; }
    @Override public void setPlayerUID(int uid) { this.playerUID = uid; }
    @Override public Entity getHostEntity() { return this.host2; }
    @Override public int getTextureID() { return 0; }
    @Override public Attrs getAttrs() { return this.attrs; }
    @Override public void setAttrs(Attrs data) { this.attrs = data; }

    @Override public void setProjectileType(int type) { this.type = type; }
    @Override public boolean isInvisible() { return this.invisibleTicks > 0 || this.getFlag(5); }
    @Override @SideOnly(value = Side.CLIENT) public boolean isInRangeToRenderDist(double distanceSq) { double d1 = this.getEntityBoundingBox().getAverageEdgeLength() * 256.0; return distanceSq < d1 * d1; }
}