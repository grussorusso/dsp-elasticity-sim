package it.uniroma2.dspsim.utils;

public class StringUtils {
    // avoid init
    private StringUtils() {}

    public static int charSum(String str) {
        int sum = 0;
        for (char c : str.toCharArray()) {
            sum += c;
        }
        return sum;
    }
}
