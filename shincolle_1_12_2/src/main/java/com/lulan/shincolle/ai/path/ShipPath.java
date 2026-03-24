package com.lulan.shincolle.ai.path;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class ShipPath {
    private final ShipPathPoint[] points;
    private int currentPathIndex;
    private final int pathLength;

    public ShipPath(ShipPathPoint[] pathpoint) {
        this.points = pathpoint;
        this.pathLength = pathpoint.length;
    }

    public boolean isFinished() {
        return this.currentPathIndex >= this.pathLength;
    }

    public ShipPathPoint getFinalPathPoint() {
        return this.pathLength > 0 ? this.points[this.pathLength - 1] : null;
    }

    public ShipPathPoint getPathPointFromIndex(int i) {
        return this.points[i];
    }

    public ShipPathPoint[] getPathPoints(){return this.points;}

    public int getCurrentPathLength() {
        return this.pathLength;
    }

    public int getCurrentPathIndex() {
        return this.currentPathIndex;
    }

    public void setCurrentPathIndex(int i) {
        this.currentPathIndex = i;
    }

    public Vec3d getVectorFromIndex(Entity entity, int i) {
        if (i >= this.points.length) {
            i = this.points.length - 1;
        }
        double d0 = this.points[i].xCoord + ((int)(entity.width + 1.0f)) * 0.5;
        double d1 = this.points[i].yCoord;
        double d2 = this.points[i].zCoord + ((int)(entity.width + 1.0f)) * 0.5;
        return new Vec3d(d0, d1, d2);
    }

    public Vec3d getPosition(Entity entity) {
        return this.getVectorFromIndex(entity, this.currentPathIndex);
    }

    public Vec3d getCurrentPos() {
        ShipPathPoint pathpoint = this.points[this.currentPathIndex];
        return new Vec3d(pathpoint.xCoord, pathpoint.yCoord, pathpoint.zCoord);
    }
}
