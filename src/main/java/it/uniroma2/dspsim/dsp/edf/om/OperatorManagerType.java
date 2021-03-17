package it.uniroma2.dspsim.dsp.edf.om;

public enum OperatorManagerType {
    DO_NOTHING,
    THRESHOLD_BASED,
    OPTIMAL_ALLOCATION,
    MODEL_BASED,
    Q_LEARNING,
    Q_LEARNING_PDS,
    FA_Q_LEARNING,
    VALUE_ITERATION,
    VALUE_ITERATION_SPLITQ,
    FA_TRAJECTORY_BASED_VALUE_ITERATION,
    FA_HYBRID;

    public static OperatorManagerType fromString(String str) throws IllegalArgumentException {
        if (str.equalsIgnoreCase("do-nothing")) {
            return DO_NOTHING;
        } else if (str.equalsIgnoreCase("threshold")) {
            return THRESHOLD_BASED;
        } else if (str.equalsIgnoreCase("optimal-allocation")) {
            return OPTIMAL_ALLOCATION;
        } else if (str.equalsIgnoreCase("model-based")) {
            return MODEL_BASED;
        } else if (str.equalsIgnoreCase("q-learning")) {
            return Q_LEARNING;
        } else if (str.equalsIgnoreCase("q-learning-pds")) {
            return Q_LEARNING_PDS;
        } else if (str.equalsIgnoreCase("fa-q-learning")) {
            return FA_Q_LEARNING;
        } else if (str.equalsIgnoreCase("vi")) {
            return VALUE_ITERATION;
        } else if (str.equalsIgnoreCase("vi-splitq")) {
            return VALUE_ITERATION_SPLITQ;
        } else if (str.equalsIgnoreCase("fa-tb-vi")) {
            return FA_TRAJECTORY_BASED_VALUE_ITERATION;
        } else if (str.equalsIgnoreCase("fa-hybrid")) {
            return FA_HYBRID;
        } else {
            throw new IllegalArgumentException("Not valid operator manager type " + str);
        }
    }
}
