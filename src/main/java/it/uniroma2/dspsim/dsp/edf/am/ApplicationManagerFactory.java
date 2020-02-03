package it.uniroma2.dspsim.dsp.edf.am;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.edf.am.centralized.CentralizedAM;

public class ApplicationManagerFactory {

    // avoid instantiation
    private ApplicationManagerFactory() {}

    public static ApplicationManager createApplicationManager(
            ApplicationManagerType appManagerType, Application app) throws IllegalArgumentException {
        switch (appManagerType) {
            case DO_NOTHING:
                return new DoNothingAM(app);
            case SPLITQ_BASED:
                return new SplitQBasedAM(app);
            case CENTRALIZED:
                return new CentralizedAM(app);
            default:
                // throw "Not valid om type" exception
                throw new IllegalArgumentException("Not supported application manager type: " + appManagerType.toString());
        }
    }
}
