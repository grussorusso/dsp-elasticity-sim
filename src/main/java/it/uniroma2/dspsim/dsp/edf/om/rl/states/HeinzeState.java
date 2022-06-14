package it.uniroma2.dspsim.dsp.edf.om.rl.states;

public class HeinzeState {

    static public final int UTIL_STEP_SIZE = 5;
    static public final int UTIL_LEVELS = 100/UTIL_STEP_SIZE + 1;
    private int utilLevel;

    public HeinzeState(double util) {
        this.utilLevel = (int)util*100/UTIL_STEP_SIZE;
    }

    public double getUtil() {
        return 1.0*this.utilLevel*UTIL_STEP_SIZE/100.0;
    }


}
