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

            double[] mus = {350.0, 270.0, 180.0, 300.0, 250.0};
            double[] serviceTimeMeans = new double[mus.length];
            double[] serviceTimeVariances = new double[mus.length];

            for (int i = 0; i < mus.length; i++) {
                serviceTimeMeans[i] = 1/mus[i];
                serviceTimeVariances[i] = 1.0/mus[i]*1.0/mus[i]/2.0;
            }
            final int maxParallelism = 5;
            Operator op1 = new Operator("filter-1",
                    new MG1OperatorQueueModel(serviceTimeMeans[0], serviceTimeVariances[0]), maxParallelism);
            app.addOperator(op1);
            Operator op2 = new Operator("map",
                    new MG1OperatorQueueModel(serviceTimeMeans[1], serviceTimeVariances[1]), maxParallelism);
            app.addOperator(op2);
            Operator op3 = new Operator("reduce",
                    new MG1OperatorQueueModel(serviceTimeMeans[2], serviceTimeVariances[2]), maxParallelism);
            app.addOperator(op3);
            Operator op4 = new Operator("filter-2",
                    new MG1OperatorQueueModel(serviceTimeMeans[3], serviceTimeVariances[3]), maxParallelism);
            app.addOperator(op4);
            Operator op5 = new Operator("rank",
                    new MG1OperatorQueueModel(serviceTimeMeans[4], serviceTimeVariances[4]), maxParallelism);
            app.addOperator(op5);

            app.addEdge(op1, op2);
            app.addEdge(op2, op3);
            app.addEdge(op3, op4);
            app.addEdge(op3, op5);

            if (balanced) computeHeuristicOperatorSLO(app); else computeOperatorsSLO(app);

            return app;
        }
    }

    @Test
    public void computeOperatorSLOTest() {
        ComputingInfrastructure.initDefaultInfrastructure(3);
        double SLO = 0.100;
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
