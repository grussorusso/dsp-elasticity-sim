import it.uniroma2.dspsim.dsp.Application;
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
		Application app = Application.buildDefaultApplication();
		System.out.println(app.getAllPaths());

		app = Application.buildForkJoinApplication();
		System.out.println(app.getAllPaths());
	}
}
