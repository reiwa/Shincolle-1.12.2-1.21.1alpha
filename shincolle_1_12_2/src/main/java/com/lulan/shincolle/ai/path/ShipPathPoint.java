package com.lulan.shincolle.ai.path;

import net.minecraft.util.math.MathHelper;

public class ShipPathPoint {
    public final int xCoord;
    public final int yCoord;
    public final int zCoord;
    private final int hash;
    private int index = -1;
    private float totalPathDistance;
    private float distanceToNext;
    private float distanceToTarget;
    private ShipPathPoint previous;
    private boolean visited;
    private float distanceFromOrigin;

    public ShipPathPoint(int x, int y, int z) {
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
        this.hash   = makeHash(x, y, z);
    }

    public static int makeHash(int x, int y, int z) {
        return (y & 0xFF)
                | ((x & Short.MAX_VALUE) << 8)
                | ((z & Short.MAX_VALUE) << 24)
                | (x < 0 ? Integer.MIN_VALUE : 0)
                | (z < 0 ? 32768 : 0);
    }

    public void initForSearch(ShipPathPoint target) {
        setTotalPathDistance(0f);
        float d = distanceManhattan(target);
        setDistanceToNext(d);
        setDistanceToTarget(d);
        setDistanceFromOrigin(0f);
        setPrevious(null);
        setVisited(false);
        setIndex(-1);
    }

    public boolean initPathParameters(ShipPathPoint parent, ShipPathPoint target, float range) {
        float dist = parent.distanceManhattan(this);
        float newFrom = parent.getDistanceFromOrigin() + dist;
        float dist2 = parent.getTotalPathDistance() + dist;
        if (newFrom >= range || (isAssigned() && dist2 >= getTotalPathDistance())) {
            return false;
        }
        setDistanceFromOrigin(newFrom);
        setPrevious(parent);
        setTotalPathDistance(dist2);
        setDistanceToNext(distanceManhattan(target));
        return true;
    }

    public float distanceTo(ShipPathPoint point) {
        float dx = ((float)point.xCoord) - this.xCoord;
        float dy = ((float)point.yCoord) - this.yCoord;
        float dz = ((float)point.zCoord) - this.zCoord;
        return MathHelper.sqrt(dx*dx + dy*dy + dz*dz);
    }

    public float distanceToSquared(ShipPathPoint point) {
        float dx = ((float)point.xCoord) - this.xCoord;
        float dy = ((float)point.yCoord) - this.yCoord;
        float dz = ((float)point.zCoord) - this.zCoord;
        return dx*dx + dy*dy + dz*dz;
    }

    public float distanceManhattan(ShipPathPoint point) {
        return Math.abs(point.getX() - this.xCoord)
               + Math.abs(point.getZ() - this.zCoord)
               + Math.abs(point.getY() - this.yCoord);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ShipPathPoint)) return false;
        ShipPathPoint p = (ShipPathPoint) obj;
        return this.hash == p.hash
                && this.xCoord == p.xCoord
                && this.yCoord == p.yCoord
                && this.zCoord == p.zCoord;
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    @Override
    public String toString() {
        return xCoord + ", " + yCoord + ", " + zCoord;
    }

    public int getX()                   { return xCoord; }
    public int getY()                   { return yCoord; }
    public int getZ()                   { return zCoord; }
    public int getIndex()               { return index; }
    public void setIndex(int index)     { this.index = index; }
    public float getTotalPathDistance() { return totalPathDistance; }
    public void setTotalPathDistance(float d) { this.totalPathDistance = d; }
    public float getDistanceToNext()    { return distanceToNext; }
    public void setDistanceToNext(float d) { this.distanceToNext = d; }
    public float getDistanceToTarget()  { return distanceToTarget; }
    public void setDistanceToTarget(float d) { this.distanceToTarget = d; }
    public ShipPathPoint getPrevious()  { return previous; }
    public void setPrevious(ShipPathPoint p) { this.previous = p; }
    public boolean isVisited()          { return visited; }
    public void setVisited(boolean v)   { this.visited = v; }
    public float getDistanceFromOrigin() { return distanceFromOrigin; }
    public void setDistanceFromOrigin(float d) { this.distanceFromOrigin = d; }

    public boolean isAssigned() {
        return this.index >= 0;
    }
}