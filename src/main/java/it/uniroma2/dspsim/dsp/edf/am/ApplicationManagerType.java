package it.uniroma2.dspsim.dsp.edf.am;

public enum ApplicationManagerType {
    DO_NOTHING,
    CENTRALIZED,
    SPLITQ_BASED;

    public static ApplicationManagerType fromString(String str) throws IllegalArgumentException {
        if (str.equalsIgnoreCase("do-nothing")) {
            return DO_NOTHING;
        } else if (str.equalsIgnoreCase("splitq-based")) {
            return SPLITQ_BASED;
        } else if (str.equalsIgnoreCase("centralized")) {
            return CENTRALIZED;
        } else {
            throw new IllegalArgumentException("Not valid application manager type " + str);
        }
    }
}
