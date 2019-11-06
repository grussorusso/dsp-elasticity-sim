package it.uniroma2.dspsim.dsp.edf.om.rl.action_selection;

import java.util.ArrayList;
import java.util.List;

public abstract class ActionSelectionPolicyComposition extends ActionSelectionPolicy {

    protected List<ActionSelectionPolicy> policies;

    public ActionSelectionPolicyComposition(ActionSelectionPolicyCallback aSCallback) {
        super(aSCallback);
        this.policies = new ArrayList<>();
    }
}
