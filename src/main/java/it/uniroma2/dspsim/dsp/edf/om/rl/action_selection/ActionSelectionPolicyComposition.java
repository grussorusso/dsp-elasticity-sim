package it.uniroma2.dspsim.dsp.edf.om.rl.action_selection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class ActionSelectionPolicyComposition extends ActionSelectionPolicy {

    protected List<ActionSelectionPolicy> policies;

    public ActionSelectionPolicyComposition(ActionSelectionPolicyCallback aSCallback) {
        super(aSCallback);
        this.init();
    }

    public ActionSelectionPolicyComposition(HashMap<String, Object> metadata, ActionSelectionPolicyCallback aSCallback) {
        super(metadata, aSCallback);
        this.init();
    }

    private void init() {
        this.policies = new ArrayList<>();
    }
}
