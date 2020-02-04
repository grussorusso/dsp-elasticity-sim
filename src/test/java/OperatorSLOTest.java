import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.ApplicationBuilder;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.queueing.MG1OperatorQueueModel;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class OperatorSLOTest {

    private static class ApplicationBuilderTest extends ApplicationBuilder {
        public static Application buildTestApplication(boolean balanced) {
            Application app = new Application();

            double[] mus = new double[]{250.0, 300.0, 190.0, 120.0, 90.0, 1200.0};
            double[] sigmas = new double[] {1,1,1,1,1,1};

            final int maxParallelism = 5;
            Operator op1 = new Operator("splitter",
                    new MG1OperatorQueueModel(1/mus[0], sigmas[0]), maxParallelism);
            app.addOperator(op1);
            Operator op2 = new Operator("parallel1",
                    new MG1OperatorQueueModel(1/mus[1], sigmas[1]), maxParallelism);
            app.addOperator(op2);
            Operator op3 = new Operator("parallel2",
                    new MG1OperatorQueueModel(1/mus[2], sigmas[2]), maxParallelism);
            app.addOperator(op3);
            Operator op4 = new Operator("parallel3-1",
                    new MG1OperatorQueueModel(1/mus[3], sigmas[3]), maxParallelism);
            app.addOperator(op4);
            Operator op5 = new Operator("parallel3-2",
                    new MG1OperatorQueueModel(1/mus[4], sigmas[4]), maxParallelism);
            app.addOperator(op5);
            Operator op6 = new Operator("join\t",
                    new MG1OperatorQueueModel(1/mus[5], sigmas[5]), maxParallelism);
            app.addOperator(op6);

            app.addEdge(op1, op2);
            app.addEdge(op1, op3);
            app.addEdge(op1, op4);
            app.addEdge(op4, op5);
            app.addEdge(op2, op6);
            app.addEdge(op3, op6);
            app.addEdge(op5, op6);

            if (balanced) computeBalancedOperatorSLO(app); else computeOperatorsSlo(app);

            return app;
        }
    }

    @Test
    public void computeOperatorSLOTest() {
        ComputingInfrastructure.initDefaultInfrastructure(3);
        double SLO = 0.065;
        Application balancedApplication = ApplicationBuilderTest.buildTestApplication(true);
        Application notBalancedApplication = ApplicationBuilderTest.buildTestApplication(false);

        Map<String, Double> balancedSLOMap = getSLOMap(balancedApplication);
        Map<String, Double> notBalancedSLOMap = getSLOMap(notBalancedApplication);

        for (String opName : balancedSLOMap.keySet()) {
            System.out.println(String.format("%s\t->\tBalanced: %f * SLO\t Not Balanced: %f * SLO",
                    opName, balancedSLOMap.get(opName) / SLO, notBalancedSLOMap.get(opName) / SLO));
        }
    }

    private Map<String, Double> getSLOMap(Application app) {
        Map<String, Double> sloMap = new HashMap<>();
        for (Operator op : app.getOperators()) {
            sloMap.put(op.getName(), op.getSloRespTime());
        }
        return sloMap;
    }

}
