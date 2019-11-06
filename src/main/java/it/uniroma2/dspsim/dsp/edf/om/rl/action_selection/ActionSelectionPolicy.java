package it.uniroma2.dspsim.dsp.edf.om.rl.action_selection;

public abstract class ActionSelectionPolicy implements ActionSelectionPolicyInterface {
    protected ActionSelectionPolicyCallback aspCallback;

    public ActionSelectionPolicy(ActionSelectionPolicyCallback aspCallback) {
        this.aspCallback = aspCallback;
    }
}
