package it.uniroma2.dspsim.dsp.edf.am;

public enum ApplicationManagerType {
    DO_NOTHING,
    PDS_BASED;

    public static ApplicationManagerType fromString(String str) throws IllegalArgumentException {
        if (str.equalsIgnoreCase("do-nothing")) {
            return DO_NOTHING;
        } else if (str.equalsIgnoreCase("pds-based")) {
            return PDS_BASED;
        } else {
            throw new IllegalArgumentException("Not valid application manager type " + str);
        }
    }
}
