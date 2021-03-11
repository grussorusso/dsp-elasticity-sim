import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.KLambdaState;
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
        int inputRateLevels = 20;
        String operatorName = "count";
        int maxParallelism = 3;

        OperatorQueueModel queueModel = new MG1OperatorQueueModel(serviceTimeMean, serviceTimeVariance);
        Operator operator = new Operator(operatorName, queueModel, maxParallelism);
        StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, operator.getMaxParallelism(), ComputingInfrastructure.getInfrastructure(), inputRateLevels);

        int count = 0;
        while (stateIterator.hasNext()) {
            State state = stateIterator.next();
            StringBuilder str = new StringBuilder(String.format("State %d \t -> \t[", count));
            for (int i = 0; i < state.getActualDeployment().length; i++) {
                str.append(String.format("%d", state.getActualDeployment()[i]));
                if (i < state.getActualDeployment().length - 1)
                    str.append(", ");
            }
            str.append(String.format("]\t%d\t%d", state.getLambda(), state.getMaxLambda()));
            if (state instanceof KLambdaState) {
                // print k lambda state info
                KLambdaState s = (KLambdaState) state;
                str.append("\t[");
                for (int i = 0; i < s.getActualDeployment().length; i++) {
                    str.append(String.format("%d", s.getActualDeployment()[i]));
                    if (i < s.getActualDeployment().length - 1)
                        str.append(", ");
                }
                str.append("]");
            }
            System.out.println(str);
            count++;
        }
        System.out.println(String.format("Total states: %d", count));
    }
}
