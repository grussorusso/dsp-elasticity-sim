package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.InputRateFileReader;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.utils.MathUtils;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;
import it.uniroma2.dspsim.utils.matrix.IntegerMatrix;

import java.io.IOException;

public abstract class DynamicProgrammingOM extends RewardBasedOM {

    private String trainingInputRateFilePath;

    private double gamma;

    // p matrix
    private DoubleMatrix<Integer, Integer> pMatrix;

    public DynamicProgrammingOM(Operator operator) {
        super(operator);

        this.trainingInputRateFilePath = Configuration.getInstance()
                .getString(ConfigurationKeys.TRAINING_INPUT_FILE_PATH_KEY, "/Users/simone/Documents/Tesi/profile_last_month.dat");

        try {
            this.pMatrix = buildPMatrix(this.trainingInputRateFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO configure
        this.gamma = 0.99;

        buildQ();
    }

    private DoubleMatrix<Integer, Integer> buildPMatrix(String inputRateFilePath) throws IOException {
        InputRateFileReader inputRateFileReader = new InputRateFileReader(inputRateFilePath);

        IntegerMatrix<Integer, Integer> transitionMatrix = computeTransitionMatrix(inputRateFileReader);

        DoubleMatrix<Integer, Integer> pMatrix = new DoubleMatrix<>(0.0);

        for (Integer x : transitionMatrix.getRowLabels()) {
            Integer total = transitionMatrix.rowSum(x);
            for (Integer y : transitionMatrix.getColLabels(x)) {
                pMatrix.setValue(x, y, transitionMatrix.getValue(x, y).doubleValue() / total.doubleValue());
            }
        }

        return pMatrix;
    }

    private IntegerMatrix<Integer, Integer> computeTransitionMatrix(InputRateFileReader inputRateFileReader) throws IOException {
        IntegerMatrix<Integer, Integer> transitionMatrix = new IntegerMatrix<>(0);
        if (inputRateFileReader.hasNext()) {
            int prevInputRateLevel = MathUtils.discretizeValue(this.getMaxInputRate(), inputRateFileReader.next(), this.getInputRateLevels());
            while (inputRateFileReader.hasNext()) {
                int inputRateLevel = MathUtils.discretizeValue(this.getMaxInputRate(), inputRateFileReader.next(), this.getInputRateLevels());
                transitionMatrix.add(prevInputRateLevel, inputRateLevel, 1);
                prevInputRateLevel = inputRateLevel;
            }
        }

        return transitionMatrix;
    }

    @Override
    protected final void useReward(double reward, State lastState, Action lastChosenAction, State currentState, OMMonitoringInfo monitoringInfo) {}

    /**
     * ABSTRACT METHODS
     */

    protected abstract void buildQ();
    protected abstract void dumpQOnFile(String filename);

    /**
     * GETTERS
     */
    public String getTrainingInputRateFilePath() {
        return trainingInputRateFilePath;
    }

    public double getGamma() {
        return gamma;
    }

    public DoubleMatrix<Integer, Integer> getpMatrix() {
        return pMatrix;
    }
}
