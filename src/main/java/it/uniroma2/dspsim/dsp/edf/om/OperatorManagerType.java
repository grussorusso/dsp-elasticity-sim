package it.uniroma2.dspsim.dsp.edf.om;

public enum OperatorManagerType {
    DO_NOTHING,
    THRESHOLD_BASED,
    Q_LEARNING,
    DEEP_Q_LEARNING,
    DEEP_V_LEARNING,
    VALUE_ITERATION,
    VALUE_ITERATION_SPLITQ,
    FA_TRAJECTORY_BASED_VALUE_ITERATION,
    DEEP_TRAJECTORY_BASED_VALUE_ITERATION,
    HYBRID;

    public static OperatorManagerType fromString(String str) throws IllegalArgumentException {
        if (str.equalsIgnoreCase("do-nothing")) {
            return DO_NOTHING;
        } else if (str.equalsIgnoreCase("threshold")) {
            return THRESHOLD_BASED;
        } else if (str.equalsIgnoreCase("q-learning")) {
            return Q_LEARNING;
        } else if (str.equalsIgnoreCase("deep-q-learning")) {
            return DEEP_Q_LEARNING;
        } else if (str.equalsIgnoreCase("deep-v-learning")) {
            return DEEP_V_LEARNING;
        } else if (str.equalsIgnoreCase("vi")) {
            return VALUE_ITERATION;
        } else if (str.equalsIgnoreCase("vi-splitq")) {
            return VALUE_ITERATION_SPLITQ;
        } else if (str.equalsIgnoreCase("fa-tb-vi")) {
            return FA_TRAJECTORY_BASED_VALUE_ITERATION;
        } else if (str.equalsIgnoreCase("deep-tb-vi")) {
            return DEEP_TRAJECTORY_BASED_VALUE_ITERATION;
        } else if (str.equalsIgnoreCase("hybrid")){
            return HYBRID;
        } else {
            throw new IllegalArgumentException("Not valid operator manager type " + str);
        }
    }
}
