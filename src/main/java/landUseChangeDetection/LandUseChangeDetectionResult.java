package landUseChangeDetection;

import java.io.Serializable;

public class LandUseChangeDetectionResult implements Serializable {

    private double area;
    private int before;
    private int after;

    public LandUseChangeDetectionResult(double area, int before, int after) {
        this.area = area;
        this.before = before;
        this.after = after;
    }

    public double getArea() {
        return area;
    }

    public int getBefore() {
        return before;
    }

    public int getAfter() {
        return after;
    }
}
