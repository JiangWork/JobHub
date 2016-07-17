package org.smartframework.jobhub.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.smartframework.jobhub.core.JobDefinition;
import org.smartframework.jobhub.core.JobException;
import org.smartframework.jobhub.core.support.DefaultProgressReporter;
import org.smartframework.jobhub.utils.ParameterAccessor;

/**
 * Invoked by the script.
 *
 * @author jiangzhao
 * @date Jul 15, 2016
 * @version V1.0
 */
public class Shell {
	
	/**
	 * The full command is:
	 *  org.smartframework.jobhub.client.Shell 
	 *  -Djobhub.hostname=localhost   [optional] // also configured in file client.properties
	 *  -Djobhub.jobserver.port=32100  [optional]
	 *  -Djobhub.uploadserver.port=32102 [optional]
	 *  -Djobhub.updateinterval=1000 [optional] 
	 *  
	 *  -Djob.name=TestRun         [optional]        // following is job definition
	 *  -Djob.mainclass=org.smartframework.jobhub.client.TestRun   
	 *  -Djob.method.name=runMethod
	 *  -Djob.method.args=1,2  [optional] 
	 *  -Djob.jars=1.jar,2.jar
	 *  -Djob.resources=path/to/resources/dict.txt,path/log.config  [optional] 
	 *  -Djob.timeout=1000  [optional] 
	 *  -Djob.env=KEY:VALUE,KEY:VALUE
	 *  -Djob.submitter=Jiang   [optional]
	 *  -Djob.reporter.class=org.smartframework.jobhub.core.support.DefaultProgressReporter
	 *  
	 * @param args
	 */
	public static void main(String[] args) {
		// parse the args
		Map<String, String> argsMap = new HashMap<String, String>();
		for(int i = 0; i < args.length; ++i) {
			String argument = args[i];
			int index = argument.indexOf("=");
			if (!argument.startsWith("-D") || index == -1) {
				onError("Argument uses following schema: -Dkey=value, input " + argument);
			}
			String key = argument.substring(0, index).substring(2);
			String value = argument.substring(index + 1);
			argsMap.put(key, value);
		}		
		// setting parameters
		JobSubmitter client = new JobSubmitter();
		String hostName = argsMap.remove(JobSubmitter.HOSTNAME_KEY);
		if (hostName != null) {
			client.setHostName(hostName);
		}
		String port = argsMap.remove(JobSubmitter.JOBSERVER_PORT_KEY);
		if (port != null) {
			try {
				client.setJobServerPort(Integer.parseInt(port));
			} catch(NumberFormatException e) {
				onError("Error port for job server: " + port);
			}
		}
		port = argsMap.remove(JobSubmitter.UPLOADSERVER_PORT_KEY);
		if (port != null) {
			try {
				client.setUploadServerPort(Integer.parseInt(port));
			} catch(NumberFormatException e) {
				onError("Error port for upload server: " + port);
			}
		}
		String refeshInterval = argsMap.remove(JobSubmitter.UPDATE_INTERVAL_KEY);
		if (refeshInterval != null) {
			try {
				client.setUpdateInterval(Long.parseLong(port));
			} catch(NumberFormatException e) {
				onError("Error refresh setting: " + refeshInterval);
			}
		}
		String mainClass = argsMap.remove(JobDefinition.JOB_MAINCLASS_KEY);
		if (mainClass == null) {
			onError("No main class was found, it must be specified.");
		} else {
			client.setMainClass(mainClass);
		}
		String method = argsMap.remove(JobDefinition.JOB_METHOD_KEY);
		if (method == null) {
			onError("No method was found, it must be specified.");
		} else {
			client.setEnterMethod(method);
		}
		String jars = argsMap.remove(JobDefinition.JOB_JARS_KEY);
		if (jars == null) {
			onError("No jar file was found, it must be specified.");
		} else {
			client.addJars(jars.split(","));
		}
		ParameterAccessor pa = new ParameterAccessor(argsMap);
		client.setJobName(pa.getString(JobDefinition.JOB_NAME_KEY, "undefined"));
		client.setEnterArgs(pa.getString(JobDefinition.JOB_METHODARGS_KEY, ""));
		client.addResources(pa.getList(JobDefinition.JOB_RESOURCES_KEY));
		client.setTimeout(pa.getLong(JobDefinition.JOB_TIMEOUT_KEY, 1*60*60*1000));
		client.setSubmitter(pa.getString(JobDefinition.JOB_SUBMITTER_KEY, "unknown"));
		client.addEnvs(pa.getMap(JobDefinition.JOB_ENV_KEY));
		client.addProperty(JobDefinition.JOB_REPORTER_CLASS_KEY, pa.getString(JobDefinition.JOB_REPORTER_CLASS_KEY, DefaultProgressReporter.class.getName()));
		argsMap.remove(JobDefinition.JOB_NAME_KEY);
		argsMap.remove(JobDefinition.JOB_METHODARGS_KEY);
		argsMap.remove(JobDefinition.JOB_RESOURCES_KEY);
		argsMap.remove(JobDefinition.JOB_TIMEOUT_KEY);
		argsMap.remove(JobDefinition.JOB_SUBMITTER_KEY);
		argsMap.remove(JobDefinition.JOB_ENV_KEY);
		argsMap.remove(JobDefinition.JOB_REPORTER_CLASS_KEY);
		if (argsMap.size() != 0) {
			Set<String> keys = argsMap.keySet();
			StringBuilder sb = new StringBuilder();
			for (String key: keys) {
				sb.append(" " + key);
			}
			onError("Following keys are unrecognized:" + sb.toString());
		}
		try {
			client.submitSync();
		} catch (JobException e) {
			System.err.println(e.getMessage());
		} finally {
			client.close();
		}
	}	
	
	public static void onError(String msg) {
		System.err.println(msg);
		System.exit(1);
	}
}
