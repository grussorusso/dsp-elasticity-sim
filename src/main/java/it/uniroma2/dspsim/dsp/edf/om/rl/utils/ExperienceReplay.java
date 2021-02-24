package it.uniroma2.dspsim.dsp.edf.om.rl.utils;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.*;

public class ExperienceReplay {

	private CircularFifoQueue<Transition> transitions;
	private Random r;

	public ExperienceReplay (int maxSize) {
		this.transitions = new CircularFifoQueue<>(maxSize);

		final int seed = Configuration.getInstance().getInteger(ConfigurationKeys.DL_OM_ND4j_RANDOM_SEED_KET, 123);
		this.r = new Random(seed);
	}

	public void add (Transition transition) {
		this.transitions.add(transition);
	}

	public Collection<Transition> sampleBatch(int batchSize)
	{
		int storageSize = transitions.size();
		if (storageSize < batchSize)
			return null;

		Set<Integer> intSet = new HashSet<>();
		while (intSet.size() < batchSize) {
			int rd = r.nextInt(storageSize);
			intSet.add(rd);
		}

		ArrayList<Transition> batch = new ArrayList<>(batchSize);
		Iterator<Integer> iter = intSet.iterator();
		while (iter.hasNext()) {
			Transition trans = transitions.get(iter.next());
			batch.add(trans);
		}

		return batch;
	}

	public int getSize () {
		return this.transitions.size();
	}
}
