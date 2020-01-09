package it.uniroma2.dspsim.utils;

public class Geometry2DUtils {

    //avoid init
    private Geometry2DUtils() { }

    /**
     * Get a value and map it in a interval system
     * Compute interval length and detect witch interval contains value
     * If value is less than min value or greater than max value it will be put in first or last interval
     * @param intervalsNum number of intervals
     * @param minValue min possible value
     * @param maxValue max possible value
     * @param value current value
     * @return interval number
     */
    public static int mapInIntervals(int intervalsNum, double minValue, double maxValue, double value) {
        if (value < minValue) return 0;
        if (value >= maxValue) return intervalsNum - 1;

        // subtract 1 to start counting from 0;
        return (int) Math.ceil((intervalsNum / (maxValue - minValue)) * (value - minValue)) - 1;
    }
}
