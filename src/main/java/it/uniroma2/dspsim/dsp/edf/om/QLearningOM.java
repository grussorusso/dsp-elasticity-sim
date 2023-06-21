package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.MapBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTableFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.KLambdaState;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.PolicyIOUtils;
import it.uniroma2.dspsim.stats.Statistics;
import it.uniroma2.dspsim.utils.PolicyDumper;
import it.uniroma2.dspsim.utils.parameter.VariableParameter;

import java.io.*;

public class QLearningOM extends ReinforcementLearningOM {
    private QTable qTable;
    private long time = 1;

    private VariableParameter alpha;
    private int alphaDecaySteps;
    private int alphaDecayStepsCounter;

    private long iters = 0;

    private double gamma;

    private ActionSelectionPolicy greedyActionSelection;


    public QLearningOM(Operator operator) {
        super(operator);

        // get configuration instance
        Configuration configuration = Configuration.getInstance();

        this.qTable = QTableFactory.newQTable(operator.getMaxParallelism(), getInputRateLevels());

        if (PolicyIOUtils.shouldLoadPolicy(configuration)) {
            this.qTable.load(PolicyIOUtils.getFileForLoading(this.operator, "qTable"));
        }

        double alphaInitValue = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_KEY, 1.0);
        double alphaDecay = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_DECAY_KEY, 0.98);
        double alphaMinValue = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_MIN_VALUE_KEY, 0.1);

        this.alpha = new VariableParameter(alphaInitValue, alphaMinValue, 1.0, alphaDecay);

        this.alphaDecaySteps = configuration.getInteger(ConfigurationKeys.QL_OM_ALPHA_DECAY_STEPS_KEY, -1);
        this.alphaDecayStepsCounter = 0;

        this.gamma = configuration.getDouble(ConfigurationKeys.DP_GAMMA_KEY,0.99);

        this.greedyActionSelection = ActionSelectionPolicyFactory.getPolicy(
                ActionSelectionPolicyType.GREEDY,
                this
        );
    }

    @Override
    protected void registerMetrics(Statistics statistics) {
        super.registerMetrics(statistics);
    }

    @Override
    protected void learningStep(State oldState, Action action, State currentState, double reward) {
        final double oldQ  = qTable.getQ(oldState, action);
        final double newQ = (1.0 - alpha.getValue()) * oldQ + alpha.getValue() * (reward +
                        this.gamma * qTable.getQ(currentState, greedyActionSelection.selectAction(currentState)));

        final double bellmanError = Math.abs(newQ - oldQ);

        qTable.setQ(oldState, action, newQ);

        decrementAlpha();

        // XXX: Tutorial stuff
	if (this.time % 100 == 0) {
		File f = PolicyIOUtils.getFileForDumping(this.operator, String.format("coloredPolicy-%d", this.time));
		System.err.println(f.getAbsolutePath());
		PolicyDumper.dumpPolicy(this, f, this.greedyActionSelection);
	}
	this.time++;
    }

    private void decrementAlpha() {
        if (this.alphaDecaySteps > 0) {
            this.alphaDecayStepsCounter++;
            if (this.alphaDecayStepsCounter >= this.alphaDecaySteps) {
                this.alpha.update();
                this.alphaDecayStepsCounter = 0;
            }
        }
    }

    @Override
    public void savePolicy()
    {
        this.qTable.dump(PolicyIOUtils.getFileForDumping(this.operator, "qTable"));

        // XXX: Tutorial stuff
        File f = PolicyIOUtils.getFileForDumping(this.operator, "coloredPolicy");
        try {
            FileWriter fileOut = new FileWriter(f.getAbsolutePath());
            BufferedWriter bw = new BufferedWriter(fileOut);

            int lambda = 0;

            while (lambda < this.getInputRateLevels()) {
                // print column
                for (int k = 1; k <= this.operator.getMaxParallelism(); ++k) {
                    State s = new KLambdaState(-1, new int[]{k}, lambda, this.getInputRateLevels()-1, this.operator.getMaxParallelism() );
                    Action a = this.greedyActionSelection.selectAction(s) ;
                    String line;
                    if (a.getDelta() == 0) {
                       line = "255 255 255\n";
                    } else if (a.getDelta() == 1) {
                        line = "0 215 0\n";
                    } else {
                        line = "230 0 0\n";
                    }
                    bw.write(line);
                }
                bw.write("\n\n");
                ++lambda;
            }

            bw.close();
            fileOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * ACTION SELECTION POLICY CALLBACK INTERFACE
     */

    @Override
    public double evaluateAction(State s, Action a) {
        return qTable.getQ(s, a);
    }
}
