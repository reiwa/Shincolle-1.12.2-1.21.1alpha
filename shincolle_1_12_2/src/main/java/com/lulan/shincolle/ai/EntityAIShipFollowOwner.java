package com.lulan.shincolle.ai;

import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.FormationHelper;
import com.lulan.shincolle.utility.LogHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;

public class EntityAIShipFollowOwner
extends EntityAIBase {
    private final IShipAttackBase host;
    private final EntityLiving host2;
    private EntityLivingBase owner;
    private EntityPlayer player;
    private int checkTP_T;
    private int checkTP_D;
    private int findCooldown;
    private final ShipPathNavigate ShipNavigator;
    private double maxDistSq;
    private double minDistSq;
    private double distSq;
    private double distX;
    private double distY;
    private double distZ;
    private double[] pos;
    private final double[] ownerPosOld;
    private final int BASE_COOLDOWN = 64;
    private final int MIN_COOLDOWN = BASE_COOLDOWN/4;
    private final double MIN_RANGE = 2.0 * 2.0;
    private final double MAX_RANGE = 32.0 * 32.0;

    public EntityAIShipFollowOwner(IShipAttackBase entity) {
        this.host = entity;
        this.host2 = (EntityLiving)entity;
        this.ShipNavigator = entity.getShipNavigate();
        this.distSq = 1.0;
        this.setMutexBits(7);
        this.pos = new double[]{this.host2.posX, this.host2.posY, this.host2.posZ};
        this.ownerPosOld = new double[]{this.host2.posX, this.host2.posY, this.host2.posZ};
        if (entity instanceof BasicEntityShip || entity instanceof BasicEntityMount) {
            this.player = EntityHelper.getEntityPlayerByUID(entity.getPlayerUID());
        }
    }

    private int calcCooldown() {
        double clamped = this.maxDistSq;
        if (clamped < MIN_RANGE) clamped = MIN_RANGE;
        if (clamped > MAX_RANGE) clamped = MAX_RANGE;
        double ratio = (clamped - MIN_RANGE) / (MAX_RANGE - MIN_RANGE);
        return (int)(MIN_COOLDOWN + ratio * (BASE_COOLDOWN - MIN_COOLDOWN));
    }

    public boolean shouldExecute() {
        if (!host.getIsSitting()
                && !host.getIsRiding()
                && !host.getIsLeashed()
                && host.getStateFlag(11)
                && host.getStateMinor(43) < 1
                && host.getStateMinor(6) > 0
                && !host.getStateFlag(27)) {
            EntityPlayer ownerEntity = EntityHelper.getEntityPlayerByUID(host.getPlayerUID());
            if (ownerEntity == null) {
                return false;
            }
            if (ownerEntity.dimension != host2.dimension) {
                return false;
            }
            owner = ownerEntity;
            updateDistance();
            return distSq > maxDistSq;
        }
        return false;
    }

    public boolean shouldContinueExecuting() {
        if (host != null && owner != null
                && !host.getIsSitting()
                && !host.getIsRiding()
                && !host.getIsLeashed()
                && host.getStateFlag(11)
                && host.getStateMinor(43) < 1
                && host.getStateMinor(6) > 0) {
            updateDistance();
            return distSq > minDistSq;
        }
        return false;
    }

    public void startExecuting() {
        this.findCooldown = calcCooldown();
        this.checkTP_T = 0;
        this.checkTP_D = 0;
    }

    public void resetTask() {
        this.owner = null;
        this.ShipNavigator.clearPathEntity();
    }

    public void updateTask() {
        if (this.host != null) {
            --this.findCooldown;
            ++this.checkTP_T;
            if (this.host2.ticksExisted % 32 == 0) {
                EntityPlayer OwnerEntity = EntityHelper.getEntityPlayerByUID(this.host.getPlayerUID());
                if (OwnerEntity != null) {
                    this.owner = OwnerEntity;
                    if (this.owner.dimension != this.host2.dimension) {
                        this.resetTask();
                        return;
                    }
                    this.updateDistance();
                } else {
                    this.resetTask();
                    return;
                }
            }
            if (this.distSq <= this.minDistSq) {
                this.ShipNavigator.clearPathEntity();
            }
            if (this.findCooldown <= 0) {
                this.findCooldown = calcCooldown();
                this.ShipNavigator.tryMoveToXYZ(this.pos[0], this.pos[1], this.pos[2], 1.0);
            }
            this.host2.getLookHelper().setLookPositionWithEntity(this.owner, 20.0f, 40.0f);
            if (this.host2.dimension == this.owner.dimension) {
                if (!ConfigHandler.canTeleport) {
                    return;
                }
                if (this.distSq > ConfigHandler.shipTeleport[1]) {
                    ++this.checkTP_D;
                    if (this.checkTP_D > ConfigHandler.shipTeleport[0]) {
                        this.checkTP_D = 0;
                        LogHelper.debug("DEBUG: follow AI: distSQ > " + ConfigHandler.shipTeleport[1] + " , teleport to target. dim: " + this.host2.dimension + " " + this.owner.dimension);
                        EntityHelper.applyTeleport(this.host, this.distSq, new Vec3d(this.owner.posX, this.owner.posY + 0.75, this.owner.posZ));
                        return;
                    }
                }
                if (this.checkTP_T > ConfigHandler.shipTeleport[0]) {
                    this.checkTP_T = 0;
                    LogHelper.debug("DEBUG: follow AI: teleport entity: dimension check: " + this.host2.dimension + " " + this.owner.dimension);
                    EntityHelper.applyTeleport(this.host, this.distSq, new Vec3d(this.owner.posX, this.owner.posY + 0.75, this.owner.posZ));
                }
            }
        }
    }

    private void updateDistance() {
        if (this.host.getStateMinor(26) > 0) {
            this.minDistSq = 4.0;
            this.maxDistSq = 7.0;
            double dx = this.ownerPosOld[0] - this.owner.posX;
            double dy = this.ownerPosOld[1] - this.owner.posY;
            double dz = this.ownerPosOld[2] - this.owner.posZ;
            double dsq = dx * dx + dy * dy + dz * dz;
            if (dsq > 7.0) {
                this.pos = FormationHelper.getFormationGuardingPos(this.host, this.owner, this.ownerPosOld[0], this.ownerPosOld[2]);
                this.ownerPosOld[0] = this.owner.posX;
                this.ownerPosOld[1] = this.owner.posY;
                this.ownerPosOld[2] = this.owner.posZ;
                if (this.player != null && (ConfigHandler.alwaysShowTeamParticle || EntityHelper.getPointerInUse(this.player) != null) && this.player.dimension == this.host2.dimension) {
                    CommonProxy.channelP.sendTo(new S2CSpawnParticle(25, this.pos[0], this.pos[1], this.pos[2], 0.3, 4.0, 0.0), (EntityPlayerMP)this.player);
                }
            }
            if (this.host2.ticksExisted % 16 == 0 && this.player != null && (ConfigHandler.alwaysShowTeamParticle || EntityHelper.getPointerInUse(this.player) != null) && this.player.dimension == this.host2.dimension) {
                CommonProxy.channelP.sendTo(new S2CSpawnParticle(25, this.pos[0], this.pos[1], this.pos[2], 0.3, 6.0, 0.0), (EntityPlayerMP)this.player);
            }
            if (this.host.getStateFlag(23)) {
                this.maxDistSq = 64.0;
            }
        } else {
            float fMin = this.host.getStateMinor(10);
            float fMax = this.host.getStateMinor(11);
            this.minDistSq = fMin * fMin + this.host2.width * 0.75f;
            this.maxDistSq = fMax * fMax + this.host2.width * 0.75f;
            this.pos[0] = this.owner.posX;
            this.pos[1] = this.owner.posY;
            this.pos[2] = this.owner.posZ;
        }
        this.distX = this.pos[0] - this.host2.posX;
        this.distY = this.pos[1] - this.host2.posY;
        this.distZ = this.pos[2] - this.host2.posZ;
        this.distSq = this.distX * this.distX + this.distY * this.distY  + this.distZ * this.distZ;
    }
}
