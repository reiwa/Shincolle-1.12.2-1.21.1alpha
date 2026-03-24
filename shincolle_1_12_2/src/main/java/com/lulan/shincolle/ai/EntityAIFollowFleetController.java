package com.lulan.shincolle.ai;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.other.EntityFleetController;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.FormationHelper;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class EntityAIFollowFleetController extends EntityAIBase {

    private final BasicEntityShip ship;
    private EntityFleetController targetController;
    private final ShipPathNavigate shipNavigator;
    private int findCoolDown;
    private int updateCoolDown;

    private static final double FOLLOW_DISTANCE_SQ = 3.0D;
    private static final double STOP_DISTANCE_SQ = 1.0D;

    public EntityAIFollowFleetController(BasicEntityShip ship) {
        this.ship = ship;
        this.shipNavigator = ship.getShipNavigate();
        this.findCoolDown = 0;
        this.updateCoolDown = 0;
        this.setMutexBits(7);
    }

    private Optional<Vec3d> getFormationTargetPosition() {
        if (this.targetController == null) {
            return Optional.empty();
        }
        EntityPlayer owner = EntityHelper.getEntityPlayerByUID(this.ship.getPlayerUID());
        if (owner == null) return Optional.empty();
        CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(owner);
        if (capa == null) return Optional.empty();
        int formationID = capa.getFormatIDCurrentTeam();
        int formationPosID = this.ship.getStateMinor(27);
        if (formationID <= 0 || formationPosID == 0) {
            return Optional.empty();
        }
        double[] controllerPos = {this.targetController.posX, this.targetController.posY, this.targetController.posZ};
        boolean[] formationDir = getFormationDirectionFromYaw(this.targetController.rotationYaw);
        int[] targetPos = FormationHelper.calcFormationPos(formationID, formationPosID, controllerPos, formationDir);
        return Optional.of(new Vec3d(targetPos[0] + 0.5D, targetPos[1], targetPos[2] + 0.5D));
    }

    @Override
    public boolean shouldExecute() {
        if (this.ship.isAIDisabled() || !this.ship.getStateFlag(27)) {
            return false;
        }
        if (--this.findCoolDown <= 0) {
            this.findCoolDown = 20;
            this.targetController = TeamHelper.getActiveFleetController(this.ship).orElse(null);
        }
        if (this.targetController == null) {
            return false;
        }
        Optional<Vec3d> targetPosOpt = getFormationTargetPosition();
        if (targetPosOpt.isPresent()) {
            return this.ship.getPositionVector().squareDistanceTo(targetPosOpt.get()) > FOLLOW_DISTANCE_SQ;
        } else {
            return this.ship.getDistanceSq(this.targetController) > FOLLOW_DISTANCE_SQ;
        }
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (!this.ship.getStateFlag(27) || this.targetController == null || this.targetController.isDead) {
            return false;
        }
        if (this.shipNavigator.noPath()) {
            return false;
        }
        Optional<Vec3d> targetPosOpt = getFormationTargetPosition();
        if (targetPosOpt.isPresent()) {
            return this.ship.getPositionVector().squareDistanceTo(targetPosOpt.get()) > STOP_DISTANCE_SQ;
        } else {
            return this.ship.getDistanceSq(this.targetController) > STOP_DISTANCE_SQ;
        }
    }

    @Override
    public void startExecuting() {
        this.updateCoolDown = 0;
    }

    @Override
    public void resetTask() {
        this.targetController = null;
        this.shipNavigator.clearPathEntity();
        //this.ship.setStateFlag(27, false);
    }

    @Override
    public void updateTask() {
        if (--this.updateCoolDown <= 0) {
            this.updateCoolDown = 4;
            Optional<Vec3d> targetPosOpt = getFormationTargetPosition();
            if (targetPosOpt.isPresent()) {
                Vec3d target = targetPosOpt.get();
                this.shipNavigator.tryMoveToXYZ(target.x, target.y, target.z, 1.0D);
                ship.setGuardedPos((int)target.x, (int)target.y, (int)target.z, ship.world.provider.getDimension(), 1);
            } else if (this.targetController != null) {
                this.shipNavigator.tryMoveToEntityLiving(this.targetController, 1.0D);
                ship.setGuardedPos((int)this.targetController.posX, (int)this.targetController.posY, (int)this.targetController.posZ, ship.world.provider.getDimension(), 1);
            }
        }
    }

    private boolean[] getFormationDirectionFromYaw(float yaw) {
        boolean[] dir = new boolean[2];
        double rad = Math.toRadians(yaw);
        double dx = -Math.sin(rad);
        double dz = Math.cos(rad);
        dir[0] = CalcHelper.isAbsGreater(dx, dz);
        dir[1] = dir[0] ? (dx >= 0.0D) : (dz >= 0.0D);
        return dir;
    }

    public void resetFindCooldown(int type) {
        this.findCoolDown = 10 - type*5;
    }
}