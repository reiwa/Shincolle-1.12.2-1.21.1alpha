package com.lulan.shincolle.ai.path;

public class ShipPathHeap {
    private ShipPathPoint[] pathPoints = new ShipPathPoint[128];
    private int count;

    public int getCount() {
        return this.count;
    }

    public ShipPathPoint addPoint(ShipPathPoint point) {
        if (point.getIndex() >= 0) {
            throw new IllegalStateException("OW KNOWS!");
        }
        if (this.count == this.pathPoints.length) {
            ShipPathPoint[] apathpoint = new ShipPathPoint[this.count << 1];
            System.arraycopy(this.pathPoints, 0, apathpoint, 0, this.count);
            this.pathPoints = apathpoint;
        }
        this.pathPoints[this.count] = point;
        point.setIndex(this.count);
        this.sortToRoot(this.count);
        this.count++;
        return point;
    }

    public void clearPath() {
        this.count = 0;
    }

    public ShipPathPoint dequeue() {
        ShipPathPoint pathpoint = this.pathPoints[0];
        this.pathPoints[0] = this.pathPoints[--this.count];
        this.pathPoints[this.count] = null;
        if (this.count > 0) {
            this.sortToLeaf(0);
        }
        pathpoint.setIndex(-1);
        return pathpoint;
    }

    public void changeDistance(ShipPathPoint point, float dist) {
        float old = point.getDistanceToTarget();
        point.setDistanceToTarget(dist);
        if (dist < old) {
            this.sortToRoot(point.getIndex());
        } else {
            this.sortToLeaf(point.getIndex());
        }
    }

    private void sortToRoot(int id) {
        ShipPathPoint fromNode = this.pathPoints[id];
        float f = fromNode.getDistanceToTarget();
        while (id > 0) {
            int parentId = (id - 1) >> 1;
            ShipPathPoint parentNode = this.pathPoints[parentId];
            if (f >= parentNode.getDistanceToTarget()) break;
            this.pathPoints[id] = parentNode;
            parentNode.setIndex(id);
            id = parentId;
        }
        this.pathPoints[id] = fromNode;
        fromNode.setIndex(id);
    }

    private void sortToLeaf(int id) {
        ShipPathPoint node = this.pathPoints[id];
        float nodeDist = node.getDistanceToTarget();
        while (true) {
            int left  = 2 * id + 1;
            if (left >= this.count) break;
            int right = left + 1;
            int swap  = left;
            float best = this.pathPoints[left].getDistanceToTarget();
            if (right < this.count) {
                float rightDist = this.pathPoints[right].getDistanceToTarget();
                if (rightDist < best) {
                    swap = right;
                    best = rightDist;
                }
            }
            if (best >= nodeDist) break;
            ShipPathPoint swapNode = this.pathPoints[swap];
            this.pathPoints[id] = swapNode;
            swapNode.setIndex(id);
            id = swap;
        }
        this.pathPoints[id] = node;
        node.setIndex(id);
    }

    public boolean isPathEmpty() {
        return this.count == 0;
    }
}
