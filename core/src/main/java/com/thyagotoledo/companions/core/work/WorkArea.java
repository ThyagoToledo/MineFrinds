package com.thyagotoledo.companions.core.work;

public class WorkArea {
    private final String dimension;
    private final int centerX;
    private final int centerY;
    private final int centerZ;
    private final int radius;

    public WorkArea(String dimension, int centerX, int centerY, int centerZ, int radius) {
        this.dimension = dimension != null ? dimension : "overworld";
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = Math.max(1, radius);
    }

    public boolean contains(String dim, int x, int y, int z) {
        if (!this.dimension.equals(dim)) return false;
        int dx = Math.abs(this.centerX - x);
        int dy = Math.abs(this.centerY - y);
        int dz = Math.abs(this.centerZ - z);
        return dx <= radius && dy <= radius && dz <= radius;
    }

    public String getDimension() {
        return dimension;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public int getRadius() {
        return radius;
    }
}
