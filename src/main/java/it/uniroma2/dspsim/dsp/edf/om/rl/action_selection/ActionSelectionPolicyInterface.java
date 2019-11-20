package it.uniroma2.dspsim.dsp.edf.om.rl.action_selection;

import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

public interface ActionSelectionPolicyInterface {
    Action selectAction(State s);
}
