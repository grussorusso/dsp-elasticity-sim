import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.RLQLearningOM;
import it.uniroma2.dspsim.dsp.edf.om.RewardBasedOM;
import it.uniroma2.dspsim.dsp.edf.om.fa.FunctionApproximationManager;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.Tiling;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.TilingBuilder;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.TilingType;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.shape.RectangleTilingShape;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.concrete.KLambdaState;
import it.uniroma2.dspsim.dsp.queueing.MG1OperatorQueueModel;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import org.junit.Assert;
import org.junit.Test;

public class TilingTests {

    @Test
    public void rectangleShapeTilingTest() {
        ComputingInfrastructure.initDefaultInfrastructure(2);
        RewardBasedOM om = new RLQLearningOM(new Operator("rank", new MG1OperatorQueueModel(1.0, 0.0),5));

        Tiling tiling = new TilingBuilder()
                .setShape(new RectangleTilingShape(5, 5, 3))
                .setXRange(0.0, 5.0)
                .setYRange(0.0, 30)
                .setZRange(0.0, 20.0)
                .build(TilingType.K_LAMBDA_RES_TYPE);


        State s1 = new KLambdaState(0, new int[] {2, 0, 0}, 5, 30, 5);
        Action a1 = new Action(0, 0, 0);

        Assert.assertTrue(tiling.isActive(s1, a1, om));

        tiling.evaluate(s1, a1, om);
        tiling.updateWeight(2.3, s1, a1, om);
        tiling.evaluate(s1, a1, om);
        tiling.updateWeight(9.5, s1, a1, om);

        Assert.assertEquals(0.0 + 9.5 + 2.3, tiling.evaluate(s1, a1, om), 0.0);
    }
}
