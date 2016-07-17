package org.smartframework.jobhub.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.smartframework.jobhub.protocol.JobState;
import org.smartframework.jobhub.server.ServerContext;
import org.smartframework.jobhub.utils.IOUtils;
import org.smartframework.jobhub.utils.Proc;
import org.smartframework.jobhub.utils.ProcResult;

/**
 * Run the submitted job.
 * 
 * @author Miller
 * @date Jul 3, 2016 7:43:24 PM
 */
public class JobRunner implements Runnable {
	
	private final static Logger logger = Logger.getLogger(JobRunner.class);
	
	private JobEntry entry;
	private long jobId;
	
	private volatile boolean lanuchJvm = false;
	private volatile boolean finished = false;
	private volatile boolean shouldRun = true;
	
	private Proc process;
	
	private List<StateChangedListener> listeners;
	
	public JobRunner(JobEntry entry) {
		this.entry = entry;
		jobId = entry.getId();
		listeners = new ArrayList<StateChangedListener>();
	}
	

	@Override
	public void run() {
		try {
			setup();
		} catch (Exception e) {
			entry.finishFail(e.getMessage());
			logger.error(e.getMessage(), e);
			return;
		}
		process = new Proc.ProcBuilder("JobRunner", "/bin/bash", DirectoryAllocator.scriptPath(jobId))
				.withTimeOut(entry.getDefinition().getTimeout()).build();
		lanuchJvm = true;
		if (!shouldRun) {  // in case of cancel
			finished = true;
			return;
		}
		ProcResult result;
		try {
			result = process.run();
			if (result.getExitCode() == 0) {
				entry.finishSuccess();
			} else {
				entry.finishFail(IOUtils.read(DirectoryAllocator.stderrPath(jobId)));
				logger.error(entry.getReason());
			}
		} catch (Exception e) {
			entry.finishFail(e.getMessage());
			logger.error(e.getMessage(), e);
		}
		for (StateChangedListener listener: listeners) {
			listener.onDone(entry);
		}
		finished = true;
	}
	
	
	/**
	 * 1. Create the working directory for this job
	 * 2. write the configuration to file
	 * 3. write the script
	 * @throws JobException
	 * @throws IOException
	 */
	public void setup() throws JobException, IOException {
		entry.setStartTime(System.currentTimeMillis());
		entry.setState(JobState.RUNNING);
		createWorkingDirectory();
		// write the job configuration
		entry.getDefinition().write(DirectoryAllocator.configPath(jobId));
		//construct the script
		createScript();
		logger.info("Setup done.");
	}
	
	/**
	 * Invoke the {@link LanuchJob} to run the job in another JVM.
	 * Set working directory,  redirect the standard out and error
	 * to files.
	 * 
	 * @throws IOException
	 */
	public void createScript() throws IOException {
		File file = new File(DirectoryAllocator.scriptPath(jobId));
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		bw.write(buildEnvs());
		// log the pid
		bw.write("echo $$ > " + DirectoryAllocator.pidPath(jobId) + "\n");
		bw.write("cd " + DirectoryAllocator.workingDirectory(jobId) + "\n");
		bw.write("JAVA_OPTS=\"-Xms20m -Xmx4g\"\n");
		bw.write("JAVA_EXEC=`which java`\n");
		bw.write("exec $JAVA_EXEC $JAVA_OPTS ");
		bw.write(LaunchJobTask.class.getName());
		bw.write(" 	127.0.0.1 " + ServerContext.INNER_PROTOCOL_PORT + 
				" " + DirectoryAllocator.configPath(jobId) + 
				" " + DirectoryAllocator.logPath(jobId));
		bw.write(" 1>>" + DirectoryAllocator.stdoutPath(jobId));
		bw.write(" 2>>" + DirectoryAllocator.stderrPath(jobId) + "\n");
		bw.close();
	}
	
	public String buildClasspath() {
		StringBuilder sb = new StringBuilder();
		List<String> jars = entry.getDefinition().getJarsList();
		for (String jar: jars) {
			sb.append(":" + DirectoryAllocator.uploadDirectory(jobId) + File.separator + jar);
		}
		// add framework dependencies: thrift and api
		String appDir = DirectoryAllocator.APP_LOCATION;
		if (appDir != null) {
			String libDir = appDir + File.separator + "lib";
			File file = new File(libDir);
			String[] dependJars = file.list(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".jar");
				}
				
			});
			if (dependJars != null) {
				for (String dependJar: dependJars) {
					sb.append(":" + libDir + File.separator + dependJar);
				}
			}
		}
		String classpath = sb.toString();
		if (classpath.length() > 1) {
			return classpath.substring(1);
		}
		return classpath;
	}
	
	public String buildEnvs() {
		StringBuilder sb = new StringBuilder();
		Map<String, String> envMap = buildEnvMap();
		Iterator<Entry<String, String>> iter = envMap.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<String, String> ent = iter.next();
			sb.append("export " + ent.getKey() + "=" + ent.getValue());
			sb.append("\n");
		}
		return sb.toString();
	}
	
	
	public Map<String, String> buildEnvMap() {
		Map<String, String> envMap = new HashMap<String, String>(entry.getDefinition().getEnv());
		envMap.put("PID", "`echo $$`");
		envMap.put("WORK_DIR", "\"" + DirectoryAllocator.workingDirectory(jobId) + "\"");
		envMap.put("USER", "\"" + entry.getDefinition().getSubmitter() + "\"");
		envMap.put("CLASSPATH", "\"" + buildClasspath() + "\"");
		return envMap;
	}
	
	
	public void createWorkingDirectory() throws JobException {
		String workingDir = DirectoryAllocator.workingDirectory(jobId);
		File dir = new File(workingDir);
		if(dir.exists()) {
			throw new JobException("Directory exists:" + workingDir);
		}
		if (!dir.mkdirs()) {
			throw new JobException("Can't make directory:" + workingDir);
		}
	}
	
	
	/**
	 * Cancel the job
	 */
	public void stop() {
		shouldRun = false;
		logger.debug("try to stop JobRunner jobid=" + jobId);
		if (finished) {
			entry.finishFail("can't cancel due to completion.");
			return;
		}
		if (lanuchJvm) {
			process.terminate();
			entry.finishFail("canceled by request.");
			logger.info("jobid=" + jobId + " is stopped.");
		}
	}
	
	public void addStateChangedListener(StateChangedListener listener) {
		this.listeners.add(listener);
	}
	
	
	public static void main(String[] args) throws JobException, IOException {
		JobEntry entry = new JobEntry();
		entry.setId(1);
		JobDefinition def = new JobDefinition();
		entry.setDefinition(def);
		def.setEnterMethod("enterMethod");
		def.setJobName("testJob");
		def.setMainClass("org.smartframework.mainClass");
		def.setSubmitter("Jiang");
		def.setTimeout(10000);
		Map<String, String> envs = new HashMap<String, String>();
		envs.put("WORKING_DIR", "/home/jiang");
		envs.put("VAR", "VAL");
		def.setEnv(envs);
		List<String> list = new ArrayList<String>();
		list.add("A.jar");
		list.add("b.jar");
		def.setJarsList(list);
		def.setAllConfigMap(envs);
		JobRunner runner = new JobRunner(entry);
		runner.run();
		System.out.println(entry.isSuccess());
		System.out.println(entry.getReason());
	}

}
