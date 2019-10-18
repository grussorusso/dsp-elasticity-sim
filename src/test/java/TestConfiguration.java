import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.ApplicationBuilder;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import org.junit.Before;
import org.junit.Test;

public class TestConfiguration {

	@Test
	public void testPaths()
	{
		Configuration conf = Configuration.getInstance();
		conf.parseDefaultConfigurationFile();

		System.out.println(conf.getInteger("ciao", 56));
		System.out.println(conf.getInteger("prova.ciao", 56));
		System.out.println(conf.getString("edf.om.type", ""));

		conf.dump(System.err);
	}
}
