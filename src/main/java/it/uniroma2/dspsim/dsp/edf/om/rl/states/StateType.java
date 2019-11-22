package it.uniroma2.dspsim.dsp.edf.om.rl.states;

public enum  StateType {
    K_LAMBDA,
    REDUCED_K_LAMBDA,
    GENERAL_RESOURCES;

    public static StateType fromString(String str) throws IllegalArgumentException {
        if (str.equalsIgnoreCase("k_lambda")) {
            return K_LAMBDA;
        } else if (str.equalsIgnoreCase("reduced_k_lambda")) {
            return REDUCED_K_LAMBDA;
        } else if (str.equalsIgnoreCase("general_resources")) {
            return GENERAL_RESOURCES;
        } else {
            throw new IllegalArgumentException("Not valid State type: " + str);
        }
    }
}
