import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.concrete.KLambdaState;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.dsp.queueing.MG1OperatorQueueModel;
import it.uniroma2.dspsim.dsp.queueing.OperatorQueueModel;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import org.junit.Before;
import org.junit.Test;

public class TestStateIterator {

    @Before
    public void setup()
    {
        int numResTypes = 3;
        ComputingInfrastructure.initDefaultInfrastructure(numResTypes);
    }

    @Test
    public void stateIterator() {
        double serviceTimeMean = 0.0;
        double serviceTimeVariance = 1.0;
        int maxLambda = 20;
        String operatorName = "count";
        int maxParallelism = 3;

        OperatorQueueModel queueModel = new MG1OperatorQueueModel(serviceTimeMean, serviceTimeVariance);
        Operator operator = new Operator(operatorName, queueModel, maxParallelism);
        StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, operator.getMaxParallelism(), ComputingInfrastructure.getInfrastructure(), maxLambda);

        int count = 0;
        while (stateIterator.hasNext()) {
            State state = stateIterator.next();
            if (state instanceof KLambdaState) {
                KLambdaState s = (KLambdaState) state;
                StringBuilder str = new StringBuilder(String.format("State %d \t -> \t[", count));
                for (int i = 0; i < s.getK().length; i++) {
                    str.append(String.format("%d", s.getK()[i]));
                    if (i < s.getK().length - 1)
                        str.append(", ");
                }

                str.append(String.format("]\t%d", s.getLambda()));

                System.out.println(str);

                count++;
            }
        }
    }
}
