package it.uniroma2.dspsim.dsp.edf.om.rl.states;

import java.util.Objects;

public class HeinzeState {

    static public final int UTIL_STEP_SIZE = 5;
    static public final double MAX_UTIL = 10.0;
    private int utilLevel;

    public HeinzeState(double util) {
        util = Math.min(MAX_UTIL, util);
        this.utilLevel = (int)(util*100/UTIL_STEP_SIZE);
    }

    public double getUtil() {
        return 1.0*this.utilLevel*UTIL_STEP_SIZE/100.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeinzeState that = (HeinzeState) o;
        return utilLevel == that.utilLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(utilLevel);
    }
}
