package it.uniroma2.dspsim.dsp.edf.am;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;

public enum ApplicationManagerType {
    DO_NOTHING,
    CENTRALIZED,
    SECOND_OPPORTUNITY,
    LOHRMANN,
    DRS,
    SPLITQ_BASED;

    public static ApplicationManagerType fromString(String str) throws IllegalArgumentException {
        if (str.equalsIgnoreCase("do-nothing")) {
            return DO_NOTHING;
        } else if (str.equalsIgnoreCase("splitq-based")) {
            return SPLITQ_BASED;
        } else if (str.equalsIgnoreCase("centralized")) {
            return CENTRALIZED;
        } else if (str.equalsIgnoreCase("second-opportunity")) {
            return SECOND_OPPORTUNITY;
        } else if (str.equalsIgnoreCase("lohrmann")) {
            return LOHRMANN;
        } else if (str.equalsIgnoreCase("lohrmann-cost")) {
            Configuration.getInstance().setString(ConfigurationKeys.OM_BASIC_RESOURCE_SELECTION, "cost");
            return LOHRMANN;
        } else if (str.equalsIgnoreCase("lohrmann-speedup")) {
            Configuration.getInstance().setString(ConfigurationKeys.OM_BASIC_RESOURCE_SELECTION, "speedup");
            return LOHRMANN;
        } else if (str.equalsIgnoreCase("drs")) {
            return DRS;
        } else if (str.equalsIgnoreCase("drs-cost")) {
            Configuration.getInstance().setString(ConfigurationKeys.OM_BASIC_RESOURCE_SELECTION, "cost");
            return DRS;
        } else if (str.equalsIgnoreCase("drs-speedup")) {
            Configuration.getInstance().setString(ConfigurationKeys.OM_BASIC_RESOURCE_SELECTION, "speedup");
            return DRS;
        } else {
            throw new IllegalArgumentException("Not valid application manager type " + str);
        }
    }
}
