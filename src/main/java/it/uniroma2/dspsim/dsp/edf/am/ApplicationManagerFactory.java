package it.uniroma2.dspsim.dsp.edf.am;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.*;

public class ApplicationManagerFactory {

    // avoid instantiation
    private ApplicationManagerFactory() {}

    public static ApplicationManager createApplicationManager(
            ApplicationManagerType appManagerType, Application app) throws IllegalArgumentException {
        switch (appManagerType) {
            case DO_NOTHING:
                return new DoNothingAM(app);
            case PDS_BASED:
                return new PDSBasedAM(app);
            default:
                // throw "Not valid om type" exception
                throw new IllegalArgumentException("Not supported application manager type: " + appManagerType.toString());
        }
    }
}
