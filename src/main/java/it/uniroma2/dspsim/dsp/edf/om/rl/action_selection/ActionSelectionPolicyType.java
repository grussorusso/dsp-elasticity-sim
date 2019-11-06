package it.uniroma2.dspsim.dsp.edf.om.rl.action_selection;

public enum ActionSelectionPolicyType {
    RANDOM,
    GREEDY,
    EPSILON_GREEDY;

    public static ActionSelectionPolicyType fromString(String str) throws IllegalArgumentException {
        if (str.equalsIgnoreCase("random")) {
            return RANDOM;
        } else if (str.equalsIgnoreCase("greedy")) {
            return GREEDY;
        } else if (str.equalsIgnoreCase("e-greedy")) {
            return EPSILON_GREEDY;
        } else {
            throw new IllegalArgumentException("Not valid ASP type: " + str);
        }
    }
}
