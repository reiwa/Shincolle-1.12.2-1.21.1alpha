package com.lulan.shincolle.entity.other;

import com.lulan.shincolle.utility.EntityHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

public class EntityFleetController extends Entity implements IEntityAdditionalSpawnData {

    private static final DataParameter<Float> DIR_YAW = EntityDataManager.createKey(EntityFleetController.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> TARGET_X = EntityDataManager.createKey(EntityFleetController.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> TARGET_Y = EntityDataManager.createKey(EntityFleetController.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> TARGET_Z = EntityDataManager.createKey(EntityFleetController.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> MOVE_SPEED = EntityDataManager.createKey(EntityFleetController.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> TURN_PARAM = EntityDataManager.createKey(EntityFleetController.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> OWNER_UID = EntityDataManager.createKey(EntityFleetController.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> TEAM_MODE = EntityDataManager.createKey(EntityFleetController.class, DataSerializers.VARINT);

    private static final float ARRIVAL_RADIUS = 4.0F;
    private static final float STOP_RADIUS = 0.5F;
    private static final int NUM_WHISKERS = 9;
    private static final float SCAN_ANGLE_DEG = 120.0f;
    private static final float SCAN_DISTANCE_BASE = 4.0f;
    private static final float SCAN_DISTANCE_PER_SPEED = 15.0f;
    private static final float IMMINENT_COLLISION_DISTANCE = 2.0f;
    private static final float OBSTACLE_PENALTY_SCALE = 1000.0f;
    private static final float TARGET_ANGLE_PENALTY_SCALE = 0.8f;
    private static final float TURN_SPEED_OBSTACLE_MULTIPLIER = 2.0f;
    private static final float PREFERRED_SHORE_DISTANCE = 3.0f;
    private static final float SHORE_PENALTY = 250.0f;

    public EntityFleetController(World worldIn) {
        super(worldIn);
        this.noClip = true;
        setInvisible(true);
    }

    public EntityFleetController(World worldIn, EntityPlayer owner, int teamMode, double x, double y, double z, float yaw,
                                 double tx, double ty, double tz,
                                 float speed, float turnParam) {
        this(worldIn);
        setPosition(x, y, z);
        dataManager.set(OWNER_UID, EntityHelper.getPlayerUID(owner));
        dataManager.set(TEAM_MODE, teamMode);
        dataManager.set(TARGET_X, (float)tx);
        dataManager.set(TARGET_Y, (float)ty);
        dataManager.set(TARGET_Z, (float)tz);
        dataManager.set(MOVE_SPEED, speed);
        dataManager.set(TURN_PARAM, turnParam);
        dataManager.set(DIR_YAW, yaw);
    }

    @Override
    protected void entityInit() {
        dataManager.register(DIR_YAW, 0.0F);
        dataManager.register(OWNER_UID, 0);
        dataManager.register(TEAM_MODE, 0);
        dataManager.register(TARGET_X, 0.0F);
        dataManager.register(TARGET_Y, 0.0F);
        dataManager.register(TARGET_Z, 0.0F);
        dataManager.register(MOVE_SPEED, 0.1F);
        dataManager.register(TURN_PARAM, 1.0F);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.onGround) {
            Vec3d pos = new Vec3d(posX, posY, posZ);
            Vec3d target = new Vec3d(
                    dataManager.get(TARGET_X),
                    dataManager.get(TARGET_Y),
                    dataManager.get(TARGET_Z)
            );
            Vec3d toTarget = target.subtract(pos);
            double distSq = toTarget.lengthSquared();
            if (distSq > 0.0001) {
                float speed = dataManager.get(MOVE_SPEED);
                Vec3d dir = toTarget.normalize();
                motionX = dir.x * speed;
                motionY = dir.y * speed;
                motionZ = dir.z * speed;
                move(MoverType.SELF, motionX, motionY, motionZ);
            } else {
                motionX = motionY = motionZ = 0;
            }
            return;
        }
        prevRotationYaw = rotationYaw;
        float currentYaw = dataManager.get(DIR_YAW);
        Vec3d pos = new Vec3d(posX, posY, posZ);
        Vec3d target = new Vec3d(
                dataManager.get(TARGET_X),
                dataManager.get(TARGET_Y),
                dataManager.get(TARGET_Z)
        );
        Vec3d toTarget = target.subtract(pos);
        double distSq = toTarget.lengthSquared();
        float speed;
        float dist = (float)Math.sqrt(distSq);
        if (dist < STOP_RADIUS) {
            speed = 0f;
        } else if (dist < ARRIVAL_RADIUS) {
            speed = dataManager.get(MOVE_SPEED)
                    * (dist - STOP_RADIUS) / (ARRIVAL_RADIUS - STOP_RADIUS);
        } else {
            speed = dataManager.get(MOVE_SPEED);
        }
        if (speed < 0.01f) {
            motionX = motionY = motionZ = 0;
            return;
        }
        boolean obstacleDetected = false;
        boolean imminentCollision = false;
        float bestYaw = currentYaw;
        float leastPenalty = Float.MAX_VALUE;
        float desiredYaw = (float) Math.toDegrees(
                Math.atan2(-toTarget.x, toTarget.z)
        );
        float scanDist = SCAN_DISTANCE_BASE + speed * SCAN_DISTANCE_PER_SPEED;
        float angleStep = SCAN_ANGLE_DEG / (NUM_WHISKERS - 1);
        float startAngle = -SCAN_ANGLE_DEG / 2f;
        Vec3d eye = pos.add(new Vec3d(0, height * 0.5f, 0));
        for (int i = 0; i < NUM_WHISKERS; i++) {
            float checkAngle = wrapDegrees(currentYaw + startAngle + i * angleStep);
            double rad = Math.toRadians(checkAngle);
            Vec3d dir = new Vec3d(Math.sin(rad), 0, Math.cos(rad));
            Vec3d end = eye.add(dir.scale(scanDist));
            RayTraceResult hit = world.rayTraceBlocks(eye, end, false, true, false);
            float penalty = 0f;
            if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
                double dObs = pos.distanceTo(hit.hitVec);
                penalty += OBSTACLE_PENALTY_SCALE / (dObs * dObs + 0.01f);
                obstacleDetected = true;
                if (dObs < IMMINENT_COLLISION_DISTANCE) {
                    imminentCollision = true;
                }
            }
            Vec3d shorePoint = eye.add(dir.scale(PREFERRED_SHORE_DISTANCE));
            if (!isSafeWaterPoint(new BlockPos(shorePoint))) {
                penalty += SHORE_PENALTY;
            }
            float angleDiff = Math.abs(wrapDegrees(checkAngle - desiredYaw));
            penalty += angleDiff * TARGET_ANGLE_PENALTY_SCALE;
            if (penalty < leastPenalty) {
                leastPenalty = penalty;
                bestYaw = checkAngle;
            }
        }
        float finalYaw = obstacleDetected ? bestYaw : desiredYaw;
        if (imminentCollision) {
            speed = 0f;
        }
        float yawDiff = wrapDegrees(finalYaw - currentYaw);
        float turnSpeed = dataManager.get(TURN_PARAM)
                * (obstacleDetected || imminentCollision
                ? TURN_SPEED_OBSTACLE_MULTIPLIER : 1f);
        float rotation = Math.signum(yawDiff) * Math.min(Math.abs(yawDiff), turnSpeed);
        currentYaw = wrapDegrees(currentYaw + rotation);
        dataManager.set(DIR_YAW, currentYaw);
        rotationYaw = currentYaw;
        if (speed > 0f) {
            float moveRad = (float) Math.toRadians(currentYaw);
            motionX = -Math.sin(moveRad) * speed;
            motionZ = Math.cos(moveRad) * speed;
            motionY = (float)(toTarget.y / dist) * speed;
        } else {
            motionX = motionZ = motionY = 0;
        }
        move(MoverType.SELF, motionX, motionY, motionZ);
    }

    private boolean isSafeWaterPoint(BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = center.add(dx, 0, dz);
                IBlockState state = world.getBlockState(p);
                if (!state.getMaterial().isLiquid()) {
                    return false;
                }
                if (!world.getBlockState(p.down()).isSideSolid(world, p.down(), EnumFacing.UP)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        dataManager.set(TARGET_X, compound.getFloat("targetX"));
        dataManager.set(TARGET_Y, compound.getFloat("targetY"));
        dataManager.set(TARGET_Z, compound.getFloat("targetZ"));
        dataManager.set(MOVE_SPEED, compound.getFloat("moveSpeed"));
        dataManager.set(TURN_PARAM, compound.getFloat("turnParam"));
        dataManager.set(DIR_YAW, compound.getFloat("dirYaw"));
        if (compound.hasKey("ownerUID")) {
            dataManager.set(OWNER_UID, compound.getInteger("ownerUID"));
        }
        if (compound.hasKey("teamMode")) {
            dataManager.set(TEAM_MODE, compound.getInteger("teamMode"));
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setFloat("targetX", dataManager.get(TARGET_X));
        compound.setFloat("targetY", dataManager.get(TARGET_Y));
        compound.setFloat("targetZ", dataManager.get(TARGET_Z));
        compound.setFloat("moveSpeed", dataManager.get(MOVE_SPEED));
        compound.setFloat("turnParam", dataManager.get(TURN_PARAM));
        compound.setFloat("dirYaw", dataManager.get(DIR_YAW));
        compound.setInteger("ownerUID", dataManager.get(OWNER_UID));
        compound.setInteger("teamMode", dataManager.get(TEAM_MODE));
    }

    public int getOwnerUID() {
        return dataManager.get(OWNER_UID);
    }

    public int getTeamMode() {
        return dataManager.get(TEAM_MODE);
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public AxisAlignedBB getCollisionBox(Entity entityIn) {
        return null;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeFloat(dataManager.get(TARGET_X));
        buffer.writeFloat(dataManager.get(TARGET_Y));
        buffer.writeFloat(dataManager.get(TARGET_Z));
        buffer.writeFloat(dataManager.get(MOVE_SPEED));
        buffer.writeFloat(dataManager.get(TURN_PARAM));
        buffer.writeInt(dataManager.get(OWNER_UID));
        buffer.writeInt(dataManager.get(TEAM_MODE));
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        dataManager.set(TARGET_X, additionalData.readFloat());
        dataManager.set(TARGET_Y, additionalData.readFloat());
        dataManager.set(TARGET_Z, additionalData.readFloat());
        dataManager.set(MOVE_SPEED, additionalData.readFloat());
        dataManager.set(TURN_PARAM, additionalData.readFloat());
        dataManager.set(OWNER_UID, additionalData.readInt());
        dataManager.set(TEAM_MODE, additionalData.readInt());
    }

    private float wrapDegrees(float diff) {
        diff %= 360F;
        if (diff >= 180F) diff -= 360F;
        if (diff < -180F) diff += 360F;
        return diff;
    }
}