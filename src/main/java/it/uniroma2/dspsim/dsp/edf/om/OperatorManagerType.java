package it.uniroma2.dspsim.dsp.edf.om;

public enum OperatorManagerType {
    DO_NOTHING,
    THRESHOLD_BASED,
    Q_LEARNING,
    RL_Q_LEARNING,
    DEEP_Q_LEARNING,
    DEEP_V_LEARNING,
    VALUE_ITERATION,
    TRAJECTORY_BASED_VALUE_ITERATION;

    public static OperatorManagerType fromString(String str) throws IllegalArgumentException {
        if (str.equalsIgnoreCase("do-nothing")) {
            return DO_NOTHING;
        } else if (str.equalsIgnoreCase("threshold")) {
            return THRESHOLD_BASED;
        } else if (str.equalsIgnoreCase("q-learning")) {
            return Q_LEARNING;
        } else if (str.equalsIgnoreCase("rl-q-learning")) {
            return RL_Q_LEARNING;
        } else if (str.equalsIgnoreCase("deep-q-learning")) {
            return DEEP_Q_LEARNING;
        } else if (str.equalsIgnoreCase("deep-v-learning")) {
            return DEEP_V_LEARNING;
        } else if (str.equalsIgnoreCase("vi")) {
            return VALUE_ITERATION;
        } else if (str.equalsIgnoreCase("tb_vi")) {
            return TRAJECTORY_BASED_VALUE_ITERATION;
        } else {
            throw new IllegalArgumentException("Not valid operator manager type " + str);
        }
    }
}
