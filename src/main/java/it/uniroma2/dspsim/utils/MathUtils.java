package it.uniroma2.dspsim.utils;

import java.util.ArrayList;
import java.util.List;

public class MathUtils {

    // avoid init
    private MathUtils() {}

    public static double normalizeValue(double value, double max) {
        return value/max;
    }

    public static double normalizeValue(int value, int max) {
        return normalizeValue((double) value, (double) max);
    }

    public static double normalizeValue(long value, long max) {
        return normalizeValue((double) value, (double) max);
    }

    public static int discretizeValue(double max, double value, int levels) {
        final double quantum = max / levels;
        final int level = (int) Math.floor(value/quantum);
        return level < levels? level : levels-1;
    }

    public static double remapDiscretizedValue(double max, int level, int levels) {
        final double quantum = max / levels;
        final double floorMapping = level * quantum;
        final double ceilMapping = floorMapping + quantum;
        return (floorMapping + ceilMapping) / 2;
    }

    public static int toBase10(int[] number, int fromBase) {
        int sum = 0;
        for (int i = number.length - 1; i >= 0; i--) {
            sum += number[i] * Math.pow(fromBase, number.length - 1 - i);
        }

        return sum;
    }

    public static int[] fromBase10(int number, int toBase) {
        KeyValueStorage<Integer, Integer> num = new KeyValueStorage<>();

        int seqNumber = 0;
        int q = number;
        while (q != 0) {
            q = Math.floorDiv(number, toBase);
            int r = number - (q * toBase);
            num.addKeyValue(seqNumber, r);
            seqNumber++;
        }

        int[] result = new int[seqNumber];
        int i = 1;
        while (i <= seqNumber) {
            result[i] = (num.getValue(seqNumber - i));
            i++;
        }
        return result;
    }
}
