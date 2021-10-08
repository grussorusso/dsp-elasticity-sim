package it.uniroma2.dspsim.utils;

import it.uniroma2.dspsim.dsp.edf.om.RewardBasedOM;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.KLambdaState;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

import java.io.*;

public class PolicyDumper {

	public static void dumpPolicy (RewardBasedOM om, File outputFile, ActionSelectionPolicy asp)
	{
		final int maxP = om.getOperator().getMaxParallelism();
		final int lambdaLevels = om.getInputRateLevels();

		try {
			FileWriter fileOut = new FileWriter(outputFile);
			BufferedWriter bw = new BufferedWriter(fileOut);

			int lambda = 0;

			while (lambda < lambdaLevels) {
				// print column
				for (int k = 1; k <= maxP; ++k) {
					State s = new KLambdaState(-1, new int[]{k}, lambda, lambdaLevels-1, maxP);
					Action a = asp.selectAction(s) ;
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
}
