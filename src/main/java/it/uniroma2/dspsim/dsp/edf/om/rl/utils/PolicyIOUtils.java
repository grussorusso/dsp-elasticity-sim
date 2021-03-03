package it.uniroma2.dspsim.dsp.edf.om.rl.utils;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;

import java.io.File;
import java.nio.file.Paths;

public class PolicyIOUtils {

	private PolicyIOUtils() {}

	static public boolean shouldLoadPolicy (Configuration conf)
	{
		String policyDir = conf.getString(ConfigurationKeys.OM_POLICY_LOAD_DIR, "");
		return !policyDir.isEmpty();
	}

	static public boolean shouldSavePolicy (Configuration conf)
	{
		String policyDir = conf.getString(ConfigurationKeys.OM_POLICY_DUMP_DIR, "");
		return !policyDir.isEmpty();
	}

	static private String getFilename (Operator operator, String fileName)
	{
		return String.format("%s-%s", operator.getName(), fileName);
	}

	static public String getFilePathForLoading (Operator operator, String fileName)
	{
		String policyDir = Configuration.getInstance().getString(ConfigurationKeys.OM_POLICY_LOAD_DIR, "");
		return Paths.get(policyDir, getFilename(operator, fileName)).toString();
	}

	static public File getFileForLoading (Operator operator, String fileName)
	{
		return new File(getFilePathForLoading(operator, fileName));
	}

	static public String getFilePathForDumping (Operator operator, String fileName)
	{
		String policyDir = Configuration.getInstance().getString(ConfigurationKeys.OM_POLICY_DUMP_DIR, "");
		return Paths.get(policyDir, getFilename(operator, fileName)).toString();
	}

	static public File getFileForDumping (Operator operator, String fileName)
	{
		return new File(getFilePathForDumping(operator, fileName));
	}
}
