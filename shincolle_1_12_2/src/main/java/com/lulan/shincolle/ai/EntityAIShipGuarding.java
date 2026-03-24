package com.lulan.shincolle.ai;

import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.*;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.FormationHelper;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.TargetHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class EntityAIShipGuarding
extends EntityAIBase {
    private final IShipGuardian host;
    private final EntityLiving host2;
    private EntityPlayer owner;
    private Entity guarded;
    private final ShipPathNavigate shipnavigator;
    private final TargetHelper.Sorter targetSorter;
    private final TargetHelper.Selector targetSelector;
    private int checkTP_T;
    private int checkTP_D;
    private int findCooldown;
    private double minDistSq;
    private double distSq;
    private double[] pos;
    private final double[] guardPosOld;
    private IShipCannonAttack ship;
    private IShipAircraftAttack ship2;
    private EntityLivingBase target;
    private final int[] delayTime;
    private final int[] maxDelayTime;
    private int onSightTime;
    private int aimTime;
    private float range;
    private float rangeSq;
    private boolean launchType;
    private boolean isMoving;

    public EntityAIShipGuarding(IShipGuardian entity) {
        this.host = entity;
        this.host2 = (EntityLiving)entity;
        this.shipnavigator = entity.getShipNavigate();
        this.targetSorter = new TargetHelper.Sorter(this.host2);
        this.targetSelector = new TargetHelper.Selector(this.host2);
        this.distSq = 1.0;
        this.isMoving = false;
        this.setMutexBits(7);
        if (entity instanceof IShipCannonAttack) {
            this.ship = (IShipCannonAttack) entity;
            if (entity instanceof IShipAircraftAttack) {
                this.ship2 = (IShipAircraftAttack) entity;
            }
        }
        if (entity instanceof BasicEntityShip || entity instanceof BasicEntityMount) {
            this.owner = EntityHelper.getEntityPlayerByUID(entity.getPlayerUID());
        }
        this.pos = new double[]{-1.0, -1.0, -1.0};
        this.guardPosOld = new double[]{-1.0, -100.0, -1.0};
        this.delayTime = new int[]{20, 20, 20};
        this.maxDelayTime = new int[]{20, 40, 40};
        this.onSightTime = 0;
        this.aimTime = 20;
        this.range = 1.0f;
        this.rangeSq = 1.0f;
    }

    public boolean shouldExecute() {
        if (this.ship.getStateFlag(27)) {
            return false;
        }
        if (!(this.host == null || this.host.getIsRiding() || this.host.getIsSitting() || this.host.getStateFlag(11) || this.host.getStateMinor(43) >= 1 || this.host.getStateMinor(6) <= 0)) {
            return this.checkGuardTarget();
        }
        return false;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (this.ship.getStateFlag(27)) {
            return false;
        }
        if (this.host != null) {
            if (!(this.host.getIsRiding() || this.host.getIsSitting() || this.host.getStateFlag(11) || this.host.getStateMinor(43) >= 1 || this.host.getStateMinor(6) <= 0)) {
                if (this.distSq > this.minDistSq) {
                    return true;
                }
                return !this.shipnavigator.noPath() || this.shouldExecute();
            }
            this.resetTask();
            return false;
        }
        return false;
    }

    @Override
    public void startExecuting() {
        this.findCooldown = 10;
        this.checkTP_T = 0;
        this.checkTP_D = 0;
    }

    @Override
    public void resetTask() {
        this.guarded = null;
        this.isMoving = false;
        this.findCooldown = 10;
        this.shipnavigator.clearPathEntity();
    }

    @Override
    public void updateTask() {
        if (this.isMoving && this.ship != null && !this.ship.getStateFlag(21) && this.ship.getStateMinor(24) > 0) {
            if (this.host2.ticksExisted % 64 == 0) {
                this.updateAttackParms();
            }
            this.delayTime[0] = this.delayTime[0] - 1;
            this.delayTime[1] = this.delayTime[1] - 1;
            this.delayTime[2] = this.delayTime[2] - 1;
            if (this.host2.ticksExisted % 32 == 0) {
                this.findTarget();
                if (this.target != null && !this.target.isEntityAlive()) {
                    this.target = null;
                }
            }
            if (this.target != null && this.host2.canEntityBeSeen(this.target)) {
                ++this.onSightTime;
                double tarDistX = this.target.posX - this.host2.posX;
                double tarDistY = this.target.posY - this.host2.posY;
                double tarDistZ = this.target.posZ - this.host2.posZ;
                double tarDistSqrt = tarDistX * tarDistX + tarDistY * tarDistY + tarDistZ * tarDistZ;
                if (tarDistSqrt <= this.rangeSq && this.onSightTime >= this.aimTime) {
                    this.attackTarget();
                }
            } else {
                this.onSightTime = 0;
            }
        }
        if (this.host != null) {
            --this.findCooldown;
            if (this.host2.motionX * this.host2.motionX < 3.0E-4 && this.host2.motionZ * this.host2.motionZ < 3.0E-4) {
                ++this.checkTP_T;
            }
            if (this.host2.ticksExisted % 8 == 0 && !this.checkGuardTarget()) {
                return;
            }
            if (this.distSq <= this.minDistSq) {
                this.isMoving = false;
                this.shipnavigator.clearPathEntity();
            }
            if (this.findCooldown <= 0) {
                this.findCooldown = 32;
                this.isMoving = this.shipnavigator.tryMoveToXYZ(this.pos[0], this.pos[1], this.pos[2], 1.0);
            }
            this.host2.getLookHelper().setLookPosition(this.pos[0], this.pos[1], this.pos[2], 30.0f, this.host2.getVerticalFaceSpeed());
            if (this.host2.dimension == this.host.getGuardedPos(3)) {
                if (!ConfigHandler.canTeleport) {
                    return;
                }
                if (this.distSq > ConfigHandler.shipTeleport[1]) {
                    ++this.checkTP_D;
                    if (this.checkTP_D > ConfigHandler.shipTeleport[0]) {
                        this.checkTP_D = 0;
                        if (this.owner != null) {
                            LogHelper.debug("DEBUG: guard AI: distSQ > " + ConfigHandler.shipTeleport[1] + " , teleport to target. dim: " + this.host2.dimension + " " + this.owner.dimension);
                        }
                        EntityHelper.applyTeleport(this.host, this.distSq, new Vec3d(this.pos[0], this.pos[1] + 0.75, this.pos[2]));
                        return;
                    }
                }
                if (this.checkTP_T > ConfigHandler.shipTeleport[0]) {
                    this.checkTP_T = 0;
                    if (this.owner != null) {
                        LogHelper.debug("DEBUG: guard AI: teleport entity: dimension check: " + this.host2.dimension + " " + this.owner.dimension);
                    }
                    EntityHelper.applyTeleport(this.host, this.distSq, new Vec3d(this.pos[0], this.pos[1] + 0.75, this.pos[2]));
                }
            } else {
                this.host.setGuardedPos(-1, -1, -1, 0, 0);
                this.host.setGuardedEntity(null);
                this.host.setStateFlag(11, true);
            }
        }
    }

    private void updateAttackParms() {
        if (this.ship != null) {
            this.range = (int)this.ship.getAttrs().getAttackRange();
            if (this.range < 1.0f) {
                this.range = 1.0f;
            }
            this.rangeSq = this.range * this.range;
            this.maxDelayTime[0] = (int)(ConfigHandler.baseAttackSpeed[1] / this.ship.getAttrs().getAttackSpeed()) + ConfigHandler.fixedAttackDelay[1];
            this.maxDelayTime[1] = (int)(ConfigHandler.baseAttackSpeed[2] / this.ship.getAttrs().getAttackSpeed()) + ConfigHandler.fixedAttackDelay[2];
            this.maxDelayTime[2] = (int)(ConfigHandler.baseAttackSpeed[3] / this.ship.getAttrs().getAttackSpeed()) + ConfigHandler.fixedAttackDelay[3];
            this.aimTime = (int)(20.0f * (150 - this.host.getLevel()) / 150.0f) + 10;
        }
    }

    private void findTarget() {
        List<Entity> list1 = this.host2.world.getEntitiesWithinAABB(EntityLivingBase.class, this.host2.getEntityBoundingBox().expand(this.range * 0.9, this.range * 0.6, this.range * 0.9), this.targetSelector);
        list1.sort(this.targetSorter);
        if (list1.size() > 2) {
            this.target = (EntityLivingBase)list1.get(this.host2.world.rand.nextInt(3));
        } else if (!list1.isEmpty()) {
            this.target = (EntityLivingBase)list1.get(0);
        }
    }

    private void attackTarget() {
        if (this.ship.getStateFlag(13) && this.delayTime[0] <= 0 && this.ship.useAmmoLight() && this.ship.hasAmmoLight()) {
            this.ship.attackEntityWithAmmo(this.target);
            this.delayTime[0] = this.maxDelayTime[0];
        }
        if (this.ship.getStateFlag(14) && this.delayTime[1] <= 0 && this.ship.useAmmoHeavy() && this.ship.hasAmmoHeavy()) {
            this.ship.attackEntityWithHeavyAmmo(this.target);
            this.delayTime[1] = this.maxDelayTime[1];
        }
        if (this.ship2 != null && this.delayTime[2] <= 0 && (this.ship2.getStateFlag(6) || this.ship2.getStateFlag(7))) {
            if (!this.ship2.getStateFlag(6)) {
                this.launchType = false;
            }
            if (!this.ship2.getStateFlag(7)) {
                this.launchType = true;
            }
            if (this.launchType && this.ship2.hasAmmoLight() && this.ship2.hasAirLight()) {
                this.ship2.attackEntityWithAircraft(this.target);
                this.delayTime[2] = this.maxDelayTime[2];
            }
            if (!this.launchType && this.ship2.hasAmmoHeavy() && this.ship2.hasAirHeavy()) {
                this.ship2.attackEntityWithHeavyAircraft(this.target);
                this.delayTime[2] = this.maxDelayTime[2];
            }
            this.launchType = !this.launchType;
        }
    }

    private boolean checkGuardTarget() {
        this.guarded = this.host.getGuardedEntity();
        if (this.guarded != null) {
            if (!this.guarded.isEntityAlive() || this.guarded.world.provider.getDimension() != this.host2.world.provider.getDimension()) {
                this.host.setGuardedPos(-1, -1, -1, 0, 0);
                this.host.setGuardedEntity(null);
                this.host.setStateFlag(11, true);
                this.resetTask();
                return false;
            }
            if (this.host.getStateMinor(26) > 0) {
                double dx = this.guardPosOld[0] - this.guarded.posX;
                double dy = this.guardPosOld[1] - this.guarded.posY;
                double dz = this.guardPosOld[2] - this.guarded.posZ;
                boolean canSendParticle = this.owner != null && (ConfigHandler.alwaysShowTeamParticle || EntityHelper.getPointerInUse(this.owner) != null) && this.owner.dimension == this.host2.dimension;
                if (dx * dx + dy * dy + dz * dz > 6.0) {
                    this.pos = FormationHelper.getFormationGuardingPos(this.host, this.guarded, this.guardPosOld[0], this.guardPosOld[2]);
                    this.guardPosOld[0] = this.guarded.posX;
                    this.guardPosOld[1] = this.guarded.posY;
                    this.guardPosOld[2] = this.guarded.posZ;
                    if (canSendParticle) {
                        CommonProxy.channelP.sendTo(new S2CSpawnParticle(25, this.pos[0], this.pos[1], this.pos[2], 0.3, 4.0, 0.0), (EntityPlayerMP)this.owner);
                    }
                }
                if (this.host2.ticksExisted % 16 == 0 && canSendParticle) {
                    CommonProxy.channelP.sendTo(new S2CSpawnParticle(25, this.pos[0], this.pos[1], this.pos[2], 0.3, 6.0, 0.0), (EntityPlayerMP)this.owner);
                }
            } else {
                this.pos[0] = this.guarded.posX;
                this.pos[1] = this.guarded.posY;
                this.pos[2] = this.guarded.posZ;
            }
        } else {
            this.pos[0] = this.host.getStateMinor(14) + 0.5;
            this.pos[1] = this.host.getStateMinor(15) + 0.5;
            this.pos[2] = this.host.getStateMinor(16) + 0.5;
        }
        if (this.pos[1] <= 0.0 || this.host2.dimension != this.host.getGuardedPos(3)) {
            this.host.setGuardedPos(-1, -1, -1, 0, 0);
            this.host.setGuardedEntity(null);
            this.host.setStateFlag(11, true);
            this.resetTask();
            return false;
        }
        double maxDistSq;
        if (this.ship != null && this.ship.getStateMinor(26) > 0) {
            if (this.ship.getStateMinor(24) == 2) {
                this.minDistSq = 5.0;
                maxDistSq = 9.0;
            } else {
                this.minDistSq = 4.0;
                maxDistSq = 7.0;
            }
            if (this.host.getStateFlag(23)) {
                maxDistSq = 64.0;
            }
        } else {
            float fMin = this.host.getStateMinor(10) + this.host2.width * 0.75f;
            float fMax = this.host.getStateMinor(11) + this.host2.width * 0.75f;
            if (this.host.getStateFlag(23)) {
                fMax += 5.0f;
            }
            this.minDistSq = fMin * fMin;
            maxDistSq = fMax * fMax;
        }
        double distX = this.pos[0] - this.host2.posX;
        double distY = this.pos[1] - this.host2.posY;
        double distZ = this.pos[2] - this.host2.posZ;
        this.distSq = distX * distX + distY * distY + distZ * distZ;

        return this.distSq > maxDistSq && this.host2.dimension == this.host.getStateMinor(17);
    }
}
