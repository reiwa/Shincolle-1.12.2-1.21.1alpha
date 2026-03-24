package com.lulan.shincolle.ai.path;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipNavigator;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.FormationHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.util.math.MathHelper;

public class ShipMoveHelper {
    private final EntityLiving entity;
    private final IShipNavigator entityN;
    private double posX;
    private double posY;
    private double posZ;
    private double speed;
    private final float rotateLimit;
    protected Action action = Action.WAIT;

    public ShipMoveHelper(EntityLiving entity, float rotlimit) {
        this.entity = entity;
        this.entityN = (IShipNavigator)entity;
        this.posX = entity.posX;
        this.posY = entity.posY;
        this.posZ = entity.posZ;
        this.rotateLimit = rotlimit;
    }

    public double getSpeed() {
        return this.speed;
    }

    public void setMoveTo(double x, double y, double z, double speed) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.speed = speed;
        this.action = Action.MOVE_TO;
    }

    public void onUpdateMoveHelper() {
        this.entity.setMoveForward(0.0f);
        if (this.action != Action.MOVE_TO) {
            this.entity.setMoveForward(0.0f);
            return;
        }
        this.action = Action.WAIT;
        double x1 = this.posX - this.entity.posX;
        double y1 = this.posY - this.entity.posY;
        double z1 = this.posZ - this.entity.posZ;
        double moveSq = x1 * x1 + y1 * y1 + z1 * z1;
        if (moveSq <= 0.001) {
            this.entity.setMoveForward(0.0f);
            return;
        }
        float targetYaw = (float)(MathHelper.atan2(z1, x1) * 57.29578f) - 90.0f;
        float moveSpeed = (float)this.entity
                .getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED)
                .getAttributeValue();
        if (this.entity instanceof BasicEntityShip) {
            BasicEntityShip ship = (BasicEntityShip)this.entity;
            if (ship.getStateMinor(26) > 0) {
                moveSpeed = FormationHelper.getFormationMOV(ship);
            }
        } else if (this.entity instanceof BasicEntityMount) {
            BasicEntityShip host = (BasicEntityShip)((BasicEntityMount)this.entity).getHostEntity();
            if (host != null && host.getStateMinor(26) > 0) {
                moveSpeed = FormationHelper.getFormationMOV(host);
            }
        }
        moveSpeed *= (float)this.speed;
        this.entity.rotationYaw = this.limitAngle(this.entity.rotationYaw, targetYaw, this.rotateLimit);
        if (this.entityN.canFly()) {
            if (y1 > 0.5) {
                this.entity.motionY += moveSpeed * 0.12;
                moveSpeed *= 0.8f;
            } else if (y1 < -0.5) {
                this.entity.motionY -= moveSpeed * 0.16;
                moveSpeed *= 0.92f;
            }
            this.entity.setAIMoveSpeed(moveSpeed);
            return;
        }
        if (EntityHelper.checkEntityIsInLiquid(this.entity)) {
            if (y1 > 1.5) {
                this.entity.motionY += moveSpeed * 0.15;
                moveSpeed *= 0.5f;
            } else if (y1 > -0.5 && y1 < 0) {
                this.entity.motionY += moveSpeed * 0.1;
                moveSpeed *= 0.5f;
            } else if (y1 < -1.0) {
                this.entity.motionY -= moveSpeed * 0.5;
                moveSpeed *= 0.82f;
            }
            this.entity.setAIMoveSpeed(moveSpeed);
            return;
        }
        if (y1 > this.entity.stepHeight && (x1 * x1 + z1 * z1) < 1.0) {
            this.entity.getJumpHelper().setJumping();
        }
        this.entity.setAIMoveSpeed(moveSpeed);
    }
    private float limitAngle(float yaw, float degree, float limit) {
        float f1;
        float f = MathHelper.wrapDegrees(degree - yaw);
        if (f > limit) {
            f = limit;
        }
        if (f < -limit) {
            f = -limit;
        }
        if ((f1 = yaw + f) < 0.0f) {
            f1 += 360.0f;
        } else if (f1 > 360.0f) {
            f1 -= 360.0f;
        }
        return f1;
    }

    public enum Action {
        WAIT,
        MOVE_TO
    }
}
