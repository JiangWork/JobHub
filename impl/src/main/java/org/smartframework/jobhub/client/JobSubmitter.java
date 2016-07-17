package org.smartframework.jobhub.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.smartframework.jobhub.core.JobDefinition;
import org.smartframework.jobhub.core.JobException;
import org.smartframework.jobhub.core.support.DefaultProgressReporter;
import org.smartframework.jobhub.protocol.ActionResult;
import org.smartframework.jobhub.protocol.ClientProtocol;
import org.smartframework.jobhub.protocol.JobState;
import org.smartframework.jobhub.protocol.JobStatus;
import org.smartframework.jobhub.utils.ParameterAccessor;

/**
 * This use to submit job to JobServer.
 *
 * @author jiangzhao
 * @date Jul 9, 2016
 * @version V1.0
 */
public class JobSubmitter {
	private final Logger logger = Logger.getLogger(JobSubmitter.class);
	
	public final static String DEFAULT_HOSTNAME = "localhost";
	public final static int DEFAULT_JOB_SERVER_PORT = 32100;
	public final static int DEFAULT_UPLOAD_SERVER_PORT = 32102;
	public final static int DEFAULT_UPDATE_INTERVAL = 1000;
	
	public final static String HOSTNAME_KEY = "jobhub.hostname";
	public final static String JOBSERVER_PORT_KEY = "jobhub.jobserver.port";
	public final static String UPLOADSERVER_PORT_KEY = "jobhub.uploadserver.port";
	public final static String UPDATE_INTERVAL_KEY = "jobhub.updateinterval";
	private final static String SUBMITTER_KEY = "jobhub.submitter";
	
	public final static String APP_CONFIG_FILE = "client.properties";
	
	private String hostName; // the job client server.
	private int jobServerPort;  // the port for submitting job.
	private int uploadServerPort; // the port for uploading files.
	
	private JobDefinition def;
	
	private List<String> jarsList;  // the depend jar files
	private List<String> resourcesList; // the resource files
	private Map<String, String> env;
	private Map<String, String> otherConfig;

	private ClientProtocol.Client client;
	
	private Properties prop;
	
	private long updateInterval;
	private long jobId;
	
	public JobSubmitter() {
		jarsList = new ArrayList<String>();
		resourcesList = new ArrayList<String>();
		env = new HashMap<String, String>();
		otherConfig = new HashMap<String, String>();
		prop = new Properties();
		def = new JobDefinition();
		def.setJarsList(jarsList);
		def.setResourcesList(resourcesList);
		def.setEnv(env);
		def.setAllConfigMap(otherConfig);
		this.hostName = DEFAULT_HOSTNAME;
		this.jobServerPort = DEFAULT_JOB_SERVER_PORT;
		this.uploadServerPort = DEFAULT_UPLOAD_SERVER_PORT;
		this.updateInterval = DEFAULT_UPDATE_INTERVAL;
		InputStream is = JobSubmitter.class.getClassLoader().getResourceAsStream(APP_CONFIG_FILE);
		if (is != null) {  // has configuration
			try {
				prop.load(is);
				ParameterAccessor pa = new ParameterAccessor(prop);
				this.hostName = pa.getString(HOSTNAME_KEY, DEFAULT_HOSTNAME);
				this.jobServerPort = pa.getInt(JOBSERVER_PORT_KEY, DEFAULT_JOB_SERVER_PORT);
				this.uploadServerPort = pa.getInt(UPLOADSERVER_PORT_KEY, DEFAULT_UPLOAD_SERVER_PORT);
				this.updateInterval = pa.getLong(UPDATE_INTERVAL_KEY, DEFAULT_UPDATE_INTERVAL);
				this.def.setSubmitter(pa.getString(SUBMITTER_KEY, "unknown"));
			} catch (IOException e) {
				System.err.println(e.getMessage());
				logger.error(e.getMessage(), e);
			}
		} else {
			System.out.println("No application config was found, default is used, override is allowed.");
		}
	}
	
	public void addJar(String jarName) {
		this.jarsList.add(jarName);
	}
	
	public void addJars(List<String> jarsList) {
		this.jarsList.addAll(jarsList);
	}
	
	public void addJars(String[] jars) {
		for (String jar: jars) {
			this.jarsList.add(jar);
		}
	}
	
	public void addResource(String resourceName) {
		this.resourcesList.add(resourceName);
	}
	
	public void addResources(List<String> resourcesList) {
		this.resourcesList.addAll(resourcesList);
	}
	
	public void addEnv(String key, String value) {
		this.env.put(key, value);
	}
	
	public void addEnvs(Map<String, String> envs) {
		this.env.putAll(envs);
	}
	
	public void addProperty(String key, String value) {
		this.otherConfig.put(key, value);
	}
	
	public void setJobName(String jobName) {
		def.setJobName(jobName);
	}

	public void setMainClass(String mainClass) {
		def.setMainClass(mainClass);
	}

	public void setEnterMethod(String enterMethod) {
		def.setEnterMethod(enterMethod);
	}

	public void setEnterArgs(String enterArgs) {
		def.setEnterArgs(enterArgs);
	}

	public void setSubmitter(String submitter) {
		def.setSubmitter(submitter);
	}

	public void setTimeout(long timeout) {
		def.setTimeout(timeout);
	}
	
	public long getUpdateInterval() {
		return updateInterval;
	}

	public void setUpdateInterval(long updateInterval) {
		this.updateInterval = updateInterval;
	}
	
	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public int getJobServerPort() {
		return jobServerPort;
	}

	public void setJobServerPort(int jobServerPort) {
		this.jobServerPort = jobServerPort;
	}
	
	public int getUploadServerPort() {
		return uploadServerPort;
	}

	public void setUploadServerPort(int uploadServerPort) {
		this.uploadServerPort = uploadServerPort;
	}

	/**Trim the file path of jarsList and resourceList**/
	private void trimFilePathes(JobDefinition def) {
		List<String> jars = new ArrayList<String>();
		for(String jar: jarsList) {
			int index = jar.lastIndexOf(File.separator);
			if (index != -1) {
				jars.add(jar.substring(index + 1));
			}
		}
		def.setJarsList(jars);
		List<String> resources = new ArrayList<String>();
		for(String resource: resourcesList) {
			int index = resource.lastIndexOf(File.separator);
			if (index != -1) {
				resources.add(resource.substring(index + 1));
			}
		}
		def.setResourcesList(resources);
	}
	
	/**
	 * Check the existence of files.
	 * @param def
	 * @throws JobException
	 */
	private void checkFiles(JobDefinition def) throws JobException {
		for(String jarName: def.getJarsList()) {
			File file = new File(jarName);
			if(!file.canRead()) {
				throw new JobException("Can't read jar file: " + jarName + ", wrong file name?");
			}
		}
		for (String resource: def.getResourcesList()) {
			File file = new File(resource);
			if(!file.canRead()) {
				throw new JobException("Can't read resource file: " + resource + ", wrong file name?");
			}
		}
	}
	
	private void uploadFiles(JobDefinition def) throws JobException {
		long start = System.currentTimeMillis();
		int total = def.getJarsList().size() + def.getResourcesList().size();
		System.out.println("Total upload files: " + total);
		int current = 1;
		UploadClient uploader = new UploadClient(this.hostName, this.uploadServerPort);
		List<String> allFiles = new ArrayList<String>();
		allFiles.addAll(def.getJarsList());
		allFiles.addAll(def.getResourcesList());
		for (String fileName: allFiles) {
			try {
				System.out.println(String.format("%d of %d: uploading %s", current++, total, fileName));
				boolean status = uploader.upload(fileName, def.getJobId());
				if (!status) {
					logger.error(uploader.getReply());
					throw new JobException(uploader.getReply());
				}
			} catch(Exception e) {
				logger.error(e.getMessage(), e);
				throw new JobException(e.getMessage(), e);
			}
		}
		System.out.println("Upload files done. Elapsed time:" + (System.currentTimeMillis() - start) + " ms.");
	}
	
	/**
	 * Get new jobId, upload related files and submit the job.
	 * @throws JobException
	 */
	public void submit() throws JobException {
		System.out.println(String.format("Connection configurations as follows:\n\thostname:\t\t%s\n\tjobserver port:\t\t%d\n\t"
				+ "uploadserver port:\t%d\n\trefresh interval:\t%d\n", 
				this.hostName, this.jobServerPort, this.uploadServerPort,
			    this.updateInterval));
		
		client = ClientProtocolClient.newClient(hostName, jobServerPort);
		if (client == null) {
			throw new JobException("Can't connect to JobServer " + hostName + "@" + jobServerPort);
		}
		jobId = -1;
		try {
			jobId = client.newJobId();
			def.setJobId(jobId);
			checkFiles(def);
			uploadFiles(def);
			trimFilePathes(def);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			def.write(bos);
			ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
			ActionResult result = client.submit(jobId, bb);
			if (!result.success) {
				throw new JobException(result.reason);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new JobException(e.getMessage(), e);
		} 
		System.out.println(String.format("Submit job to %s@%d successfully, returned jobid=%d.", hostName, jobServerPort, jobId));
		logger.info(String.format("Submit job to %s@%d successfully, returned jobid=%d.", hostName, jobServerPort, jobId));
	}
	
	public void submitSync() throws JobException {
		submit();
		JobStatus status = null;
		while(true) {
			try {
				Thread.sleep(updateInterval);
			} catch (InterruptedException e) {
				//ignored
			}
			try {
				status = client.query(jobId);
				System.out.println(String.format("JobStatus [%s]: %d of 100 ", status.getState(), status.progress));
				if (status.progress >= 100 || status.getState() == JobState.DONE) {
					System.out.print("SUMMARY: >> startTime=" + new Date(status.startTime));
					if (!status.success) {
						System.out.println(" status=FAIL, reason=" + status.reason);
					} else {
						System.out.println(" status=SUCCESS.");
					}
					break;
				}
			} catch (TException e) {
				logger.error(e.getMessage(), e);
			}
		}
		
	}
	
	public void close() {
		if (client != null) {
			ClientProtocolClient.close(client);
		}
	}
	
	public static void onError(String msg) {
		System.err.println(msg);
		System.exit(1);
	}
	
	
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
	public static void main(String[] args) throws JobException {
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
	
//		JobSubmitter client = new JobSubmitter();
//		client.setJobName("JobTest");
//		client.setSubmitter("Jiang");
//		client.setMainClass("test.main.class");
//		client.setEnterMethod("test.method");
//		client.setTimeout(10000);
//		client.addJar("lib/commons-codec-1.9.jar");
//		client.addJar("lib/jobhub-1.0.0.jar");
//		client.addResource("log/jobhub.log");
//		client.submitSync();
//		client.close();
	}
	
}
