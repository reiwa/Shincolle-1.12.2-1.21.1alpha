package com.lulan.shincolle.ai.path;

import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.IShipNavigator;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.EntityHelper;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class ShipPathNavigate {
    private final EntityLiving host;
    private final IShipNavigator hostShip;
    private final World world;
    @Nullable
    private ShipPath currentPath;
    private double speed;
    private int pathTicks;
    private int ticksAtLastPos;
    private Vec3d lastPosCheck = Vec3d.ZERO;
    private Vec3d lastPosStuck = Vec3d.ZERO;
    private long timeoutTimer = 0L;
    private long lastTimeoutCheck = 0L;
    private double timeoutLimit;
    private final float maxDistanceToWaypoint;
    private final int hostCeilWeight;
    private final int hostCeilHight;
    private BlockPos targetPos;
    private static final int CHECK_INTERVAL = 32;
    private static final int MAX_STUCK_TICKS = 100;
    private static final double STUCK_DISTANCE = 1.0;
    private static final float  MOVE_DIR_THRESH = 0.1f;
    private static final float  JUMP_SPEED_FACT = 0.35f;
    private static final float  UNSTUCK_FACTOR = 0.5f;

    public ShipPathNavigate(EntityLiving entity) {
        this.host = entity;
        this.hostShip = (IShipNavigator)entity;
        this.world = entity.world;
        this.maxDistanceToWaypoint = (float)MathHelper.absMax(this.host.width * 0.5, 0.5);
        this.hostCeilWeight = MathHelper.ceil(this.host.width);
        this.hostCeilHight = MathHelper.ceil(this.host.height);
    }

    public void setSpeed(double par1) {
        this.speed = par1;
    }

    public float getPathSearchRange() {
        if (this.host instanceof IShipAttackBase) {
            return 70.0f;
        }
        return 48.0f;
    }

    public boolean tryMoveToXYZ(double x, double y, double z, double speed) {
        ShipPath path = this.getPathToXYZ(MathHelper.floor(x), (int)y, MathHelper.floor(z));
        return this.setPath(path, speed);
    }

    public ShipPath getPathToXYZ(double x, double y, double z) {
        return !this.canNavigate() ? null : this.getShipPathToXYZ(this.host, MathHelper.floor(x), (int)y, MathHelper.floor(z), this.getPathSearchRange(), this.hostShip.canFly());
    }

    public ShipPath getShipPathToXYZ(Entity entity, int x, int y, int z, float range, boolean canFly) {
        BlockPos pos = new BlockPos(x, y, z);
        if (this.currentPath != null && !this.currentPath.isFinished() && pos.equals(this.targetPos)) {
            return this.currentPath;
        }
        this.targetPos = pos;
        BlockPos hostPos = new BlockPos(entity);
        int i = (int) (range + 8.0f);
        ChunkCache chunkcache = new ChunkCache(this.world, hostPos.add(-i, -i, -i), hostPos.add(i, i, i), 0);
        return new ShipPathFinder(chunkcache, canFly).findPath(entity, x, y, z, range);
    }

    public ShipPath getPathToEntityLiving(Entity entity) {
        return !this.canNavigate() ? null : this.getPathEntityToEntity(this.host, entity, this.getPathSearchRange(), this.hostShip.canFly());
    }

    public ShipPath getPathEntityToEntity(Entity entity, Entity targetEntity, float range, boolean canFly) {
        BlockPos pos = new BlockPos(targetEntity);
        if (this.currentPath != null && !this.currentPath.isFinished() && pos.equals(this.targetPos)) {
            return this.currentPath;
        }
        this.targetPos = pos;
        BlockPos hostPos = new BlockPos(entity).add(1, 1, 1);
        int i = (int) (range + 16.0f);
        ChunkCache chunkcache = new ChunkCache(this.world, hostPos.add(-i, -i, -i), hostPos.add(i, i, i), 0);
        return new ShipPathFinder(chunkcache, canFly).findPath(entity, targetEntity, range);
    }

    public boolean tryMoveToEntityLiving(Entity entity, double speed) {
        ShipPath pathentity = this.getPathToEntityLiving(entity);
        return pathentity != null && this.setPath(pathentity, speed);
    }

    public boolean setPath(ShipPath pathEntity, double speed) {
        if (pathEntity == null) {
            this.currentPath = null;
            return false;
        }
        if (pathEntity.getCurrentPathLength() == 0) {
            return false;
        }
        this.currentPath = pathEntity;
        this.speed = speed;
        Vec3d vec3 = this.getEntityPosition();
        this.ticksAtLastPos = this.pathTicks;
        this.lastPosCheck = vec3;
        return true;
    }

    public ShipPath getPath() {
        return this.currentPath;
    }

    public void onUpdateNavigation() {
        if (this.host.ticksExisted > 40) {
            ++this.pathTicks;
            if (!this.noPath()) {
                Vec3d vec3;
                if (this.canNavigate()) {
                    this.pathFollow();
                }
                if (!this.noPath() && (vec3 = this.currentPath.getPosition(this.host)) != null) {
                    BlockPos blockPos = new BlockPos(vec3).down();
                    AxisAlignedBB blockAABB = this.world.getBlockState(blockPos).getBoundingBox(this.world, blockPos);
                    vec3 = vec3.subtract(0.0, 1.0 - blockAABB.maxY, 0.0);
                    this.hostShip.getShipMoveHelper().setMoveTo(vec3.x, vec3.y + 0.1, vec3.z, this.speed);
                }
            }
        }
    }

    private void pathFollow() {
        Vec3d hostPos = this.getEntityPosition();
        int i = this.currentPath.getCurrentPathLength();
        for (int j = this.currentPath.getCurrentPathIndex(); j < this.currentPath.getCurrentPathLength(); ++j) {
            if (this.currentPath.getPathPointFromIndex(j).yCoord == Math.floor(hostPos.y) || EntityHelper.checkEntityIsInLiquid(this.host)) continue;
            i = j;
            break;
        }
        Vec3d nowPos = this.currentPath.getCurrentPos();
        if (MathHelper.abs((float)(this.host.posX - nowPos.x - 0.5)) < this.maxDistanceToWaypoint && MathHelper.abs((float)(this.host.posZ - nowPos.z - 0.5)) < this.maxDistanceToWaypoint) {
            this.currentPath.setCurrentPathIndex(this.currentPath.getCurrentPathIndex() + 1);
        }
        for (int j1 = i - 1; j1 >= this.currentPath.getCurrentPathIndex(); --j1) {
            if (!this.isDirectPathBetweenPoints(hostPos, this.currentPath.getVectorFromIndex(this.host, j1), this.hostCeilWeight, this.hostCeilHight, this.hostCeilWeight)) continue;
            this.currentPath.setCurrentPathIndex(j1);
            break;
        }
        this.checkForStuck(hostPos);
    }

    protected void checkForStuck(Vec3d pos) {
        if (!isTimeToCheck()) return;
        boolean stuck = isStuckAt(pos);
        if (stuck && !currentPath.isFinished()) {
            applyUnstuckMotion();
        }
        if (hasExceededMaxTicks()) {
            if (stuck) clearPathEntity();
            ticksAtLastPos = pathTicks;
        }
        lastPosCheck = pos;
        checkPathTimeout(pos);
    }

    private boolean isTimeToCheck() {
        int delta = pathTicks - ticksAtLastPos;
        return delta % CHECK_INTERVAL == 0;
    }

    private boolean isStuckAt(Vec3d pos) {
        return pos.squareDistanceTo(lastPosCheck) < STUCK_DISTANCE;
    }

    private boolean hasExceededMaxTicks() {
        return pathTicks - ticksAtLastPos > MAX_STUCK_TICKS;
    }

    private void applyUnstuckMotion() {
        Vec3d target = currentPath.getVectorFromIndex(host, currentPath.getCurrentPathIndex());
        float dx = (float)(target.x - host.posX);
        float dz = (float)(target.z - host.posZ);
        double dirX = Math.signum(dx) * UNSTUCK_FACTOR;
        double dirZ = Math.signum(dz) * UNSTUCK_FACTOR;
        host.motionX = speed * dirX;
        host.motionZ = speed * dirZ;
        if (host.getRNG().nextInt(2) == 0) {
            host.getJumpHelper().setJumping();
            float extra = host.getAIMoveSpeed() * JUMP_SPEED_FACT;
            if (Math.abs(dx) > MOVE_DIR_THRESH) host.motionX += Math.signum(dx) * extra;
            if (Math.abs(dz) > MOVE_DIR_THRESH) host.motionZ += Math.signum(dz) * extra;
        }
    }

    private void checkPathTimeout(Vec3d pos) {
        if (currentPath == null || currentPath.isFinished()) return;
        Vec3d curr = currentPath.getCurrentPos();
        long now  = System.currentTimeMillis();
        if (!curr.equals(lastPosStuck)) {
            lastPosStuck = curr;
            timeoutTimer = 0;
            double dist = pos.distanceTo(curr);
            timeoutLimit = host.getAIMoveSpeed() > 0 ? dist / host.getAIMoveSpeed() * 1000 : 0;
        } else {
            timeoutTimer += now - lastTimeoutCheck;
        }
        lastTimeoutCheck = now;
        if (timeoutLimit > 0 && timeoutTimer > timeoutLimit * 3) {
            lastPosStuck = Vec3d.ZERO;
            timeoutTimer = 0;
            timeoutLimit = 0;
            clearPathEntity();
        }
    }

    public boolean noPath() {
        return this.currentPath == null || this.currentPath.isFinished();
    }

    public void clearPathEntity() {
        this.currentPath = null;
    }

    private Vec3d getEntityPosition() {
        return new Vec3d(this.host.posX, this.host.posY + 0.5, this.host.posZ);
    }

    private boolean canNavigate() {
        return !this.host.isRiding() && (this.hostShip.canFly() || this.host.onGround || EntityHelper.checkEntityIsFree(this.host));
    }

    private boolean isDirectPathBetweenPoints(Vec3d pos1, Vec3d pos2, int sizeX, int sizeY, int sizeZ) {
        int x1 = MathHelper.floor(pos1.x);
        int y1 = (int)pos1.y;
        int z1 = MathHelper.floor(pos1.z);
        double dx = pos2.x - pos1.x;
        double dy = pos2.y - pos1.y;
        double dz = pos2.z - pos1.z;
        double offsetSq = dx * dx + dy * dy + dz * dz;
        if (offsetSq < 1.0E-8) {
            return false;
        }
        double invDist = 1.0 / Math.sqrt(offsetSq);
        dx *= invDist;
        dy *= invDist;
        dz *= invDist;
        if (!this.isSafeToStandAt(x1, y1, z1, sizeX + 2, sizeY + 1, sizeZ + 2, pos1, dx, dz)) {
            return false;
        }
        double unitX = 1.0 / Math.abs(dx);
        double unitY = 1.0 / Math.abs(dy);
        double unitZ = 1.0 / Math.abs(dz);
        double proX = x1 - pos1.x;
        double proY = y1 - pos1.y;
        double proZ = z1 - pos1.z;
        if (dx >= 0.0) proX += 1.0;
        if (dy >= 0.0) proY += 1.0;
        if (dz >= 0.0) proZ += 1.0;
        proX /= dx;
        proY /= dy;
        proZ /= dz;
        int dirX = dx < 0.0 ? -1 : 1;
        int dirY = dy < 0.0 ? -1 : 1;
        int dirZ = dz < 0.0 ? -1 : 1;
        int x2 = MathHelper.floor(pos2.x);
        int y2 = MathHelper.floor(pos2.y);
        int z2 = MathHelper.floor(pos2.z);
        int xIntOffset = x2 - x1;
        int yIntOffset = y2 - y1;
        int zIntOffset = z2 - z1;
        while (xIntOffset * dirX > 0 || yIntOffset * dirY > 0 || zIntOffset * dirZ > 0) {
            switch (CalcHelper.min(proX, proY, proZ)) {
                case 1:
                    proX += unitX;
                    x1 += dirX;
                    xIntOffset = x2 - x1;
                    break;
                case 2:
                    proY += unitY;
                    y1 += dirY;
                    yIntOffset = y2 - y1;
                    break;
                case 3:
                    proZ += unitZ;
                    z1 += dirZ;
                    zIntOffset = z2 - z1;
                    break;
                default:
            }
            if (!this.isSafeToStandAt(x1, y1, z1, sizeX, sizeY, sizeZ, pos1, dx, dz)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSafeToStandAt(int xOffset, int yOffset, int zOffset, int xSize, int ySize, int zSize, Vec3d orgPos, double vecX, double vecZ) {
        if (this.hostShip.canFly()) {
            return true;
        }
        int xSize2 = xOffset - xSize / 2;
        int zSize2 = zOffset - zSize / 2;
        if (!this.isPositionClear(xSize2, yOffset, zSize2, xSize, ySize, zSize, orgPos, vecX, vecZ)) {
            return false;
        }
        if (EntityHelper.checkEntityIsInLiquid(this.host)) return true;
        for (int x1 = xSize2; x1 < xSize2 + xSize; ++x1) {
            for (int z1 = zSize2; z1 < zSize2 + zSize; ++z1) {
                double x2 = x1 + 0.5 - orgPos.x;
                double z2 = z1 + 0.5 - orgPos.z;
                if (x2 * vecX + z2 * vecZ < 0.0 || this.world.getBlockState(new BlockPos(x1, yOffset - 1, z1)).getMaterial() != Material.AIR) continue;
                return false;
            }
        }
        return true;
    }

    private boolean isPositionClear(int xOffset, int yOffset, int zOffset, int xSize, int ySize, int zSize, Vec3d orgPos, double vecX, double vecZ) {
        for (BlockPos blockpos : BlockPos.getAllInBox(new BlockPos(xOffset, yOffset, zOffset), new BlockPos(xOffset + xSize - 1, yOffset + ySize - 1, zOffset + zSize - 1))) {
            double d0 = blockpos.getX() + 0.5 - orgPos.x;
            if (d0 * vecX + (blockpos.getZ() + 0.5 - orgPos.z) * vecZ < 0.0 || this.world.getBlockState(blockpos).getBlock().isPassable(this.world, blockpos)) continue;
            return false;
        }
        return true;
    }
}
