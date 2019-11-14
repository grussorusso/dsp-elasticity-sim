package it.uniroma2.dspsim.dsp.edf.om.factory;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.*;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;

import java.util.HashMap;

public class OperatorManagerFactory {

    // avoid instantiation
    private OperatorManagerFactory() {}

    public static OperatorManager createOperatorManager(
            OperatorManagerType operatorManagerType, Operator operator) throws IllegalArgumentException {
        switch (operatorManagerType) {
            case DO_NOTHING:
                return new DoNothingOM(operator);
            case THRESHOLD_BASED:
                return new ThresholdBasedOM(operator);
            case Q_LEARNING:
                return new QLearningOM(operator);
            case RL_Q_LEARNING:
                return new RLQLearningOM(operator);
            case DEEP_Q_LEARNING:
                return new DeepQLearningOM(operator);
            case DEEP_V_LEARNING:
                return new DeepVLearningOM(operator);
            default:
                // throw "Not valid om type" exception
                throw new IllegalArgumentException("Not valid operator manager type: " + operatorManagerType.toString());
        }
    }
}
