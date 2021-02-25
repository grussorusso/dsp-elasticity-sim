package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.Transition;
import org.apache.commons.lang3.tuple.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Collection;


public class DeepQLearningOM extends DeepLearningOM {

    public DeepQLearningOM(Operator operator) {
        super(operator);
    }

    @Override
    protected void learningStep(State oldState, Action action, State currentState, double reward) {
        this.expReplay.add(new Transition(oldState, action, currentState, reward));

        // training step
        this.learn();
    }

    private INDArray getQ(State state) {
        if (hasNetworkCache() && networkCache.containsKey(state))
            return (INDArray)networkCache.get(state);

        INDArray input = buildInput(state);
        INDArray output = this.network.output(input);

        if (hasNetworkCache())
            networkCache.put(state, output.dup());

        return output;
    }

    private INDArray buildInput(State state) {
        //State indexedState = getIndexedState(state);
        return state.arrayRepresentation(this.stateFeatures);
    }

    /**
     * DEEP LEARNING OM
     */

    @Override
    protected int computeOutputLayerNodesNumber() {
        return this.numActions;
    }

    @Override
    protected int computeInputLayerNodesNumber() {
        return this.stateFeatures;
    }

    @Override
    protected Pair<INDArray, INDArray> getTargets(Collection<Transition> batch) {
        INDArray inputs = null;
        INDArray labels = null;

        for (Transition t : batch) {
            // get old state network output
            INDArray qs = getQ(t.getS());
            // get current state network output
            INDArray qns = getQ(t.getNextS());

            //System.out.println("qs has shape: " + qs.shapeInfoToString());

            // update Q(s,a)with new estimation
            // we get min(qns) because we want to minimize cost
            // reward = cost -> minimize Q equals minimize cost
            qs = qs.dup();
            qs.put(0, t.getA().getIndex(), t.getReward() + gamma * (double) qns.minNumber());

            INDArray trainingInput = buildInput(t.getS());

            if (inputs == null) {
                inputs = trainingInput;
                labels = qs;
            } else {
                inputs = Nd4j.concat(0, inputs, trainingInput);
                labels = Nd4j.concat(0, labels, qs);
            }
        }
        return Pair.of(inputs, labels);
    }

    /**
     * ACTION SELECTION POLICY CALLBACK INTERFACE
     */

    @Override
    public double evaluateAction(State s, Action a) {
        // return network Q-function prediction associated to action a in state s
        INDArray networkOutput = getQ(s);
        return networkOutput.getDouble(a.getIndex());
    }
}
