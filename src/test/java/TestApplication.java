import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.ApplicationBuilder;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import org.junit.Before;
import org.junit.Test;

public class TestApplication {

	@Before
	public void setup()
	{
		ComputingInfrastructure.initDefaultInfrastructure(2);
	}

	@Test
	public void testPaths()
	{
		Application app = ApplicationBuilder.buildApplication();
		System.out.println(app.getAllPaths());

		app = ApplicationBuilder.buildForkJoinApplication();
		System.out.println(app.getAllPaths());
	}
}
