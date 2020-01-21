package it.uniroma2.dspsim.dsp.edf.am;

public enum ApplicationManagerType {
    DO_NOTHING;

    public static ApplicationManagerType fromString(String str) throws IllegalArgumentException {
        if (str.equalsIgnoreCase("do-nothing")) {
            return DO_NOTHING;
        } else {
            throw new IllegalArgumentException("Not valid application manager type " + str);
        }
    }
}
