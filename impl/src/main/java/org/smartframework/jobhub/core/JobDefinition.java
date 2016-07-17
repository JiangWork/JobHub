package org.smartframework.jobhub.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.smartframework.jobhub.utils.IOUtils;
import org.smartframework.jobhub.utils.ParameterAccessor;
import org.smartframework.jobhub.utils.StringUtils;


public class JobDefinition {
	
	private final static Logger logger = Logger.getLogger(JobDefinition.class);
	
	public final static String JOB_NAME_KEY = "job.name";
	public final static String JOB_MAINCLASS_KEY = "job.mainclass";
	public final static String JOB_METHOD_KEY = "job.method.name";
	public final static String JOB_METHODARGS_KEY = "job.method.args";
	public final static String JOB_SUBMITTER_KEY = "job.submitter";
	public final static String JOB_TIMEOUT_KEY = "job.timeout";
	public final static String JOB_JARS_KEY = "job.jars";
	public final static String JOB_RESOURCES_KEY = "job.resources";
	public final static String JOB_ENV_KEY = "job.env";
	public final static String JOB_REPORTER_CLASS_KEY = "job.reporter.class";
	public final static String JOB_ID_KEY = "job.id";
	
	private String jobName;
	private String mainClass;
	private String enterMethod;
	private String enterArgs;
	private String submitter;
	private long timeout;
	private List<String> jarsList;  // the depend jar files
	private List<String> resourcesList; // the resource files
	private Map<String, String> env;
	
	private long jobId = -1;
	
	/**This map contains all configurations, including above**/
	private Map<String, String> allConfigMap;
	
	public String getJobName() {
		return jobName;
	}
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	public String getMainClass() {
		return mainClass;
	}
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}
	public String getEnterMethod() {
		return enterMethod;
	}
	public void setEnterMethod(String enterMethod) {
		this.enterMethod = enterMethod;
	}
	public String getSubmitter() {
		return submitter;
	}
	public void setSubmitter(String submitter) {
		this.submitter = submitter;
	}
	public long getTimeout() {
		return timeout;
	}
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	public List<String> getJarsList() {
		return jarsList;
	}
	public void setJarsList(List<String> jarsList) {
		this.jarsList = jarsList;
	}
	public List<String> getResourcesList() {
		return resourcesList;
	}
	public void setResourcesList(List<String> resourcesList) {
		this.resourcesList = resourcesList;
	}
	public Map<String, String> getEnv() {
		return env;
	}
	public void setEnv(Map<String, String> env) {
		this.env = env;
	}
	/**
	 * Get value from allConfigMap
	 * @param key
	 * @return
	 */
	public String getProperty(String key) {
		return allConfigMap.get(key);
	}
	
	public String getEnterArgs() {
		return enterArgs;
	}
	public void setEnterArgs(String enterArgs) {
		this.enterArgs = enterArgs;
	}
	
	public synchronized Map<String, String> getAllConfigMap() {
		return allConfigMap;
	}
	public synchronized void setAllConfigMap(Map<String, String> allConfigMap) {
		this.allConfigMap = allConfigMap;
	}
	public void read(String path) throws IOException {
		FileInputStream fis = new FileInputStream(new File(path));
		read(fis);
	}
	
	public void read(InputStream is) throws IOException {
		Properties prop = new Properties();
		prop.load(is);
		allConfigMap = new HashMap<String, String>();
		Iterator<Entry<Object, Object>> iter = prop.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<Object, Object> entry = iter.next();
			allConfigMap.put((String)entry.getKey(), (String)entry.getValue());
		}
		ParameterAccessor pa = new ParameterAccessor(allConfigMap);
		jobId = pa.getLong(JOB_ID_KEY, -1);
		jobName = pa.getString(JOB_NAME_KEY, "undefined");
		mainClass = pa.getString(JOB_MAINCLASS_KEY);
		enterMethod = pa.getString(JOB_METHOD_KEY, "main");
		enterArgs = pa.getString(JOB_METHODARGS_KEY);
		submitter = pa.getString(JOB_SUBMITTER_KEY, "unknown");
		timeout = pa.getLong(JOB_TIMEOUT_KEY, 1000*1000);
		jarsList = pa.getList(JOB_JARS_KEY);
		resourcesList = pa.getList(JOB_RESOURCES_KEY);
		env = pa.getMap(JOB_ENV_KEY);
		IOUtils.closeQuietly(is);
	}
	
	public synchronized long getJobId() {
		return jobId;
	}
	public synchronized void setJobId(long jobId) {
		this.jobId = jobId;
	}
	
	public boolean write(String path) throws IOException {
		FileOutputStream fos = new FileOutputStream(new File(path));
		return this.write(fos);
	}
	
	public boolean write(OutputStream os) throws IOException {
		boolean status = false;
		BufferedWriter bw = null;
		try {
			Set<String> vistedKeys = new HashSet<String>();
			bw = new BufferedWriter(new OutputStreamWriter(os));
			bw.write(JOB_ID_KEY + "=" + jobId + "\n");
			bw.write(JOB_NAME_KEY + "=" + StringUtils.stringfyObject(jobName) + "\n");
			bw.write(JOB_MAINCLASS_KEY + "=" + StringUtils.stringfyObject(mainClass) + "\n");
			bw.write(JOB_METHOD_KEY + "=" + StringUtils.stringfyObject(enterMethod) + "\n");
			bw.write(JOB_METHODARGS_KEY + "=" + StringUtils.stringfyObject(enterArgs) + "\n");
			bw.write(JOB_SUBMITTER_KEY + "=" + StringUtils.stringfyObject(submitter) + "\n");
			bw.write(JOB_TIMEOUT_KEY + "=" + StringUtils.stringfyObject(timeout) + "\n");
			bw.write(JOB_JARS_KEY + "=" + StringUtils.stringfyList(jarsList) + "\n");
			bw.write(JOB_RESOURCES_KEY + "=" + StringUtils.stringfyList(resourcesList) + "\n");
			bw.write(JOB_ENV_KEY + "=" + StringUtils.stringfyMap(env) + "\n");
			vistedKeys.add(JOB_ID_KEY);
			vistedKeys.add(JOB_NAME_KEY);
			vistedKeys.add(JOB_MAINCLASS_KEY);
			vistedKeys.add(JOB_METHOD_KEY);
			vistedKeys.add(JOB_METHODARGS_KEY);
			vistedKeys.add(JOB_SUBMITTER_KEY);
			vistedKeys.add(JOB_JARS_KEY);
			vistedKeys.add(JOB_RESOURCES_KEY);
			vistedKeys.add(JOB_ENV_KEY);
			Iterator<String> iter = allConfigMap.keySet().iterator();
			while(iter.hasNext()) {
				String key = iter.next();
				if (!vistedKeys.contains(key)) {
					bw.write(key + "=" + StringUtils.stringfyObject(allConfigMap.get(key)) + "\n");
				}
			}
			bw.close();
			status = true;
		} catch(IOException e) {
			logger.error(e.getMessage(), e);
			throw e;
		} finally {
			IOUtils.closeQuietly(bw);
		}
		return status;
	
	}
}
