package it.uniroma2.dspsim.dsp.edf.om.rl.action_selection;

import java.util.HashMap;

public abstract class ActionSelectionPolicy implements ActionSelectionPolicyInterface {
    protected HashMap<String, Object> metadata;

    protected ActionSelectionPolicyCallback aSCallback;

    public ActionSelectionPolicy(ActionSelectionPolicyCallback aSCallback) {
        this.init(aSCallback);
    }

    public ActionSelectionPolicy(HashMap<String, Object> metadata, ActionSelectionPolicyCallback aSCallback) {
        this.init(metadata, aSCallback);
    }

    private void init(HashMap<String, Object> metadata, ActionSelectionPolicyCallback aSCallback) {
        if (metadata == null) {
            this.init(aSCallback);
        } else {
            this.metadata = metadata;
            this.aSCallback = aSCallback;
        }
    }

    private void init(ActionSelectionPolicyCallback aSCallback) {
        this.metadata = new HashMap<>();
        this.aSCallback = aSCallback;
    }

    protected  <T> T getMetadata(String key, String className) {
        if (this.metadata.containsKey(key) && this.metadata.get(key) != null)
            try {
                return (T) this.metadata.get(key);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException(key + " must be of type " + className);
            }
        else
            throw new IllegalArgumentException(key + " must be not null and of type " + className);
    }
}
