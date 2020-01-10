import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.RLQLearningOM;
import it.uniroma2.dspsim.dsp.edf.om.RewardBasedOM;
import it.uniroma2.dspsim.dsp.edf.om.fa.FunctionApproximationManager;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.Tiling;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.TilingBuilder;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.TilingType;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.shape.RectangleTilingShape;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.shape.StripeTilingShape;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.concrete.KLambdaState;
import it.uniroma2.dspsim.dsp.queueing.MG1OperatorQueueModel;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.utils.Coordinate3D;
import it.uniroma2.dspsim.utils.Tuple2;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TestsTiling {

    @Test
    public void rectangleShapeTilingTest() {
        ComputingInfrastructure.initDefaultInfrastructure(2);
        RewardBasedOM om = new RLQLearningOM(new Operator("rank", new MG1OperatorQueueModel(1.0, 0.0),5));

        Tiling tiling = new TilingBuilder()
                .setShape(new RectangleTilingShape(6, 4, 9))
                .setXRange(-3.2, 4.7)
                .setYRange(-2.6, 30.6)
                .setZRange(-1, 6.3)
                .build(TilingType.K_LAMBDA_RES_TYPE);

        Action doNothingAction = new Action(0, 0, 0);

        State s1 = new KLambdaState(0, new int[] {1, 2, 0}, 6, 100, 100);
        State s2 = new KLambdaState(0, new int[] {9, 6, 0}, 53, 100, 100);
        State s3 = new KLambdaState(0, new int[] {1, 1, 1, 1, 0, 2}, 3, 100, 100);
        State s4 = new KLambdaState(0, new int[] {2, 0, 2}, 19, 100, 100);

        Assert.assertTrue(tiling.isActive(s1, doNothingAction, om));
        Assert.assertEquals(0.0, tiling.evaluate(s1, doNothingAction, om), 0.0);
        Assert.assertFalse(tiling.isActive(s2, doNothingAction, om));
        Assert.assertEquals(0.0, tiling.evaluate(s2, doNothingAction, om), 0.0);
        Assert.assertFalse(tiling.isActive(s3, doNothingAction, om));
        Assert.assertEquals(0.0, tiling.evaluate(s3, doNothingAction, om), 0.0);
        Assert.assertTrue(tiling.isActive(s4, doNothingAction, om));
        Assert.assertEquals(0.0, tiling.evaluate(s4, doNothingAction, om), 0.0);


        tiling.update(2.3, s1, doNothingAction, om);
        tiling.update(5.6, s2, doNothingAction, om);
        tiling.update(3.7, s4, doNothingAction, om);

        List<Tuple2<Object, Double>> weights = tiling.getWeights();

        for (Tuple2<Object, Double> weight : weights) {
            Assert.assertTrue(weight.getK() instanceof Coordinate3D);
            double x = ((Coordinate3D) weight.getK()).getX();
            double y = ((Coordinate3D) weight.getK()).getY();
            double z = ((Coordinate3D) weight.getK()).getZ();
            System.out.println(String.format("(%f, %f, %f) -> %f", x, y, z, weight.getV()));
        }

        Assert.assertArrayEquals(new double[] {4, 1, 4},
                new double[] {((Coordinate3D) weights.get(0).getK()).getX(),
                        ((Coordinate3D) weights.get(0).getK()).getY(),
                        ((Coordinate3D) weights.get(0).getK()).getZ()}, 0.0);

        Assert.assertArrayEquals(new double[] {5, 2, 7},
                new double[] {((Coordinate3D) weights.get(1).getK()).getX(),
                        ((Coordinate3D) weights.get(1).getK()).getY(),
                        ((Coordinate3D) weights.get(1).getK()).getZ()}, 0.0);
    }

    @Test
    public void stripeShapeTilingTest() {
        ComputingInfrastructure.initDefaultInfrastructure(2);
        RewardBasedOM om = new RLQLearningOM(new Operator("rank", new MG1OperatorQueueModel(1.0, 0.0),5));

        Tiling tiling = new TilingBuilder()
                .setShape(new StripeTilingShape(7, 1.0, 9))
                .setXRange(-3.2, 4.7)
                .setYRange(-2.6, 30.6)
                .setZRange(-1, 6.3)
                .build(TilingType.K_LAMBDA_RES_TYPE);

        Action doNothingAction = new Action(0, 0, 0);

        State s1 = new KLambdaState(0, new int[] {1, 2, 0}, 6, 100, 100);
        State s2 = new KLambdaState(0, new int[] {9, 6, 0}, 53, 100, 100);
        State s3 = new KLambdaState(0, new int[] {1, 1, 1, 1, 0, 2}, 3, 100, 100);
        State s4 = new KLambdaState(0, new int[] {2, 0, 2}, 19, 100, 100);

        Assert.assertTrue(tiling.isActive(s1, doNothingAction, om));
        Assert.assertEquals(0.0, tiling.evaluate(s1, doNothingAction, om), 0.0);
        Assert.assertFalse(tiling.isActive(s2, doNothingAction, om));
        Assert.assertEquals(0.0, tiling.evaluate(s2, doNothingAction, om), 0.0);
        Assert.assertFalse(tiling.isActive(s3, doNothingAction, om));
        Assert.assertEquals(0.0, tiling.evaluate(s3, doNothingAction, om), 0.0);
        Assert.assertTrue(tiling.isActive(s4, doNothingAction, om));
        Assert.assertEquals(0.0, tiling.evaluate(s4, doNothingAction, om), 0.0);


        tiling.update(2.3, s1, doNothingAction, om);
        tiling.update(5.6, s2, doNothingAction, om);
        tiling.update(3.7, s4, doNothingAction, om);

        List<Tuple2<Object, Double>> weights = tiling.getWeights();

        for (Tuple2<Object, Double> weight : weights) {
            Assert.assertTrue(weight.getK() instanceof Coordinate3D);
            double x = ((Coordinate3D) weight.getK()).getX();
            double y = ((Coordinate3D) weight.getK()).getY();
            double z = ((Coordinate3D) weight.getK()).getZ();
            System.out.println(String.format("(%f, %f, %f) -> %f", x, y, z, weight.getV()));
        }

        Assert.assertArrayEquals(new double[] {5, 2, 4},
                new double[] {((Coordinate3D) weights.get(0).getK()).getX(),
                        ((Coordinate3D) weights.get(0).getK()).getY(),
                        ((Coordinate3D) weights.get(0).getK()).getZ()}, 0.0);

        Assert.assertArrayEquals(new double[] {5, 4, 7},
                new double[] {((Coordinate3D) weights.get(1).getK()).getX(),
                        ((Coordinate3D) weights.get(1).getK()).getY(),
                        ((Coordinate3D) weights.get(1).getK()).getZ()}, 0.0);
    }
}
