package it.uniroma2.dspsim.dsp.edf.am;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.edf.am.centralized.CentralizedAM;

public class ApplicationManagerFactory {

    // avoid instantiation
    private ApplicationManagerFactory() {}

    public static ApplicationManager createApplicationManager(
            ApplicationManagerType appManagerType, Application app, double sloLatency) throws IllegalArgumentException {
        switch (appManagerType) {
            case DO_NOTHING:
                return new DoNothingAM(app, sloLatency);
            case SPLITQ_BASED:
                return new SplitQBasedAM(app, sloLatency);
            case CENTRALIZED:
                return new CentralizedAM(app, sloLatency);
            case SECOND_OPPORTUNITY:
                return new SecondOpportunityAM(app, sloLatency);
            default:
                // throw "Not valid om type" exception
                throw new IllegalArgumentException("Not supported application manager type: " + appManagerType.toString());
        }
    }
}
