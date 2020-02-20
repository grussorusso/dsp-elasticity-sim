package centralized;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.ApplicationBuilder;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.am.ApplicationManager;
import it.uniroma2.dspsim.dsp.edf.am.ApplicationManagerFactory;
import it.uniroma2.dspsim.dsp.edf.am.ApplicationManagerType;
import it.uniroma2.dspsim.dsp.edf.am.centralized.*;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManagerType;
import it.uniroma2.dspsim.dsp.edf.om.ValueIterationOM;
import it.uniroma2.dspsim.dsp.edf.om.factory.OperatorManagerFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;

import java.io.*;
import java.util.List;

public class CompareAMPolicies {

	public static void compareAMPolicies (Application application)
	{
		Configuration conf = Configuration.getInstance();
		double sloLatency = conf.getDouble(ConfigurationKeys.SLO_LATENCY_KEY, 0.100);

		/* Create OMs */
		final List<Operator> operators = application.getOperators();
		final int numOperators = operators.size();
		ValueIterationOM[] operatorManagers = new ValueIterationOM[numOperators];
		ActionSelectionPolicy[] omPolicy = new ActionSelectionPolicy[numOperators];

		CentralizedAM am = (CentralizedAM)newApplicationManager(ApplicationManagerType.CENTRALIZED, application, sloLatency);

		for (int i = 0; i<numOperators; i++) {
			ValueIterationOM om = (ValueIterationOM) newOperatorManager(operators.get(i), OperatorManagerType.VALUE_ITERATION);
			operatorManagers[i] = om;
			omPolicy[i] = ActionSelectionPolicyFactory.getPolicy(ActionSelectionPolicyType.GREEDY, om);
		}


		/*
		 *
		 */
		int maxInputRate = conf.getInteger(ConfigurationKeys.RL_OM_MAX_INPUT_RATE_KEY, 600);
		int inputRateLevels = conf.getInteger(ConfigurationKeys.RL_OM_INPUT_RATE_LEVELS_KEY, 20);

		int[] maxParallelism = new int[numOperators];
		for (int i = 0; i<numOperators; i++) {
			maxParallelism[i] = application.getOperators().get(i).getMaxParallelism();
		}

		long actionsEq = 0;
		long actionsDiff = 0;

		JointStateIterator sit = new JointStateIterator(numOperators, maxParallelism,
				ComputingInfrastructure.getInfrastructure(), inputRateLevels);
		while (sit.hasNext()) {
			JointState s = sit.next();
			JointAction a = am.greedyAction(s);

			/* Ignore states where lambdas differ */
			boolean lambdaEq = true;
			for (int i = 1; i<numOperators; i++) {
				if (s.getStates()[i].getLambda() != s.getStates()[i-1].getLambda()) {
					lambdaEq = false;
					break;
				}
			}
			if (!lambdaEq)
				continue;

			/*
			 * Talk to the OMs.
			 */
			Action[] actions = new Action[numOperators];
			for (int i = 0; i<operatorManagers.length; i++) {
				actions[i]= omPolicy[i].selectAction(s.getStates()[i]);
			}
			JointAction omsA = new JointAction(actions);

			if (!omsA.equals(a)) {
				actionsDiff++;
			} else {
				actionsEq++;
				continue; // do not print
			}

			StringBuilder sb = new StringBuilder();
			sb.append(omsA.equals(a)? "    " : "+++ ");
			sb.append(s.toString());
			sb.append("-> ");
			sb.append(a.toString());
			sb.append("--\t");
			sb.append(omsA.toString());
			sb.append("\tRcf: ");
			sb.append(a.isReconfiguration()? 'Y' : 'N');
			sb.append("--");
			sb.append(omsA.isReconfiguration()? 'Y' : 'N');
			sb.append("\tViol: ");
			sb.append(String.format("%.4f", am.computeSLOViolationProbability(s, a)));
			sb.append("--");
			sb.append(String.format("%.4f", am.computeSLOViolationProbability(s, omsA)));

			sb.append(" (");
			for (int i = 0; i<numOperators; i++) {
				double sloProb = operatorManagers[i].computeSLOViolationProbability(s.getStates()[i], actions[i]);
				sb.append(String.format("%.4f ",sloProb));
			}
			sb.append(')');

			System.out.println(sb.toString());
		}

		System.out.println(String.format("Eq: %d, diff: %d", actionsEq, actionsDiff));
	}

	private static ApplicationManager newApplicationManager(ApplicationManagerType appManagerType, Application application, double sloLatency) {
		return ApplicationManagerFactory.createApplicationManager(appManagerType, application, sloLatency);
	}

	protected static OperatorManager newOperatorManager (Operator op, OperatorManagerType operatorManagerType) {
		return OperatorManagerFactory.createOperatorManager(operatorManagerType, op);
	}

	public static void main (String args[]) {

		Configuration conf = Configuration.getInstance();
		conf.parseDefaultConfigurationFile();
		String otherConfFile = "/home/gabriele/simulator.properties";
				try {
					InputStream is = new FileInputStream(new File(otherConfFile));
					conf.parseConfigurationFile(is);
					is.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

		ComputingInfrastructure.initCustomInfrastructure(
				new double[]{1.0, 0.7, 1.3, 0.9, 1.7, 0.8, 1.8, 2.0, 1.65, 1.5},
				conf.getInteger(ConfigurationKeys.NODE_TYPES_NUMBER_KEY, 3));

			Application app = ApplicationBuilder.buildApplication();
			compareAMPolicies(app);

	}
}
