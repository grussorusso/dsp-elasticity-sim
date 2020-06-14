package stats;

import it.uniroma2.dspsim.stats.metrics.CountMetric;
import it.uniroma2.dspsim.stats.metrics.RealValuedMetric;
import it.uniroma2.dspsim.stats.Statistics;
import org.junit.Test;

public class TestStatistics {

	@Test
	public void testDump() throws Statistics.MetricExistsException {
		Statistics s = Statistics.getInstance();
		s.registerMetric(new CountMetric("test"));
		s.updateMetric("test", 2);

		s.registerMetric(new RealValuedMetric("test2"));
		s.updateMetric("test2", 4);

		s.dumpAll();
	}

	@Test(expected = Statistics.MetricExistsException.class)
	public void testDuplicateMetric() throws Statistics.MetricExistsException {
		Statistics s = Statistics.getInstance();
		s.registerMetric(new CountMetric("test"));
		s.registerMetric(new CountMetric("test"));
	}
}
