package org.smartframework.jobhub.core;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.thrift.TException;
import org.smartframework.jobhub.client.InnerProtocolClient;
import org.smartframework.jobhub.common.JobIdAware;
import org.smartframework.jobhub.common.ProgressReporter;
import org.smartframework.jobhub.common.Progressable;
import org.smartframework.jobhub.core.support.DefaultProgressReporter;
import org.smartframework.jobhub.protocol.InnerProtocol;
import org.smartframework.jobhub.protocol.JobState;
import org.smartframework.jobhub.utils.StringUtils;

/**
 * Launch the job started by separated JVM.
 * @author Miller
 * @date Jul 3, 2016 7:47:36 PM
 */

public class LaunchJobTask {
	
	private final static Logger logger = Logger.getLogger(LaunchJobTask.class);
	
	private JobDefinition def;
	private InnerProtocol.Client client;
	private int pid;
	private long jobId;
	
	public static void main(String[] args)  {
		int ret = 0;
		LaunchJobTask task = new LaunchJobTask();
		try {
			task.setup(args);
			task.doRun();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			System.err.println(StringUtils.stringfyException(e)); // to be caught and redirected to err
			ret = 1;       // to be caught by script runner
		} finally {
			task.complete();
		}
		System.exit(ret);
	}
	
	public void setup(String[] args) throws JobException, IOException, TException {
		if (args.length < 4) {
			throw new JobException("LaunchJob needs four args: hostname port job-config logpath");
		}
		String hostName = args[0];
		int port = Integer.parseInt(args[1]);
		String configPath = args[2];
		String logPath = args[3];
		configureLog(logPath);
		System.out.println("Input parameters: hostName=" + hostName +  " port=" 
				+ port + " config=" + configPath + " logpath=" + logPath);
		logger.info("Input parameters: hostName=" + hostName +  " port=" 
				+ port + " config=" + configPath + " logpath=" + logPath);
		client = InnerProtocolClient.newClient(hostName, port);
		if (client == null) {
			throw new JobException("Can't connect to inner-job server: " + hostName + "@" + port);
		}
		def = new JobDefinition();
		def.read(configPath);
		pid = Integer.parseInt(System.getenv("PID"));
		jobId = Long.parseLong(def.getProperty(JobDefinition.JOB_ID_KEY));
		client.setPid(jobId, pid);
		client.progress(jobId, 5);
	}
	
	public void complete() {
		try {
			client.setJobState(jobId, JobState.DONE);
			client.progress(jobId, 100);
			InnerProtocolClient.close(client);
		} catch (TException e) {
			//ignored 
			logger.error(e.getMessage(), e);
		}
		
	}
	
	public void doRun() throws ClassNotFoundException, InstantiationException, IllegalAccessException, TException, IllegalArgumentException, InvocationTargetException {
		String mainClass = def.getMainClass();
		Class<?> clazz =  Class.forName(mainClass);
		Object obj = clazz.newInstance();
		// set the interfaces
		setInterfaces(obj, def);
		client.progress(jobId, 10);
		invokeMethod(obj, def);
	}
	
	/**
	 * Override the user-configured logger setting 
	 * Redirect the log to job working directory. 
	 * @param logPath
	 */
	public void configureLog(String logPath) {
		Properties pro = new Properties();
        pro.put("log4j.rootLogger", "DEBUG,C,R");
        
        pro.put("log4j.appender.C", "org.apache.log4j.ConsoleAppender");
        pro.put("log4j.appender.C.Threshold", "INFO");
        pro.put("log4j.appender.C.layout", "org.apache.log4j.PatternLayout");
        pro.put("log4j.appender.C.layout.ConversionPattern", "%n %m");

        pro.put("log4j.appender.R", "org.apache.log4j.RollingFileAppender");
        pro.put("log4j.appender.R.File", logPath);
        pro.put("log4j.appender.R.MaxFileSize", "10000KB");
        pro.put("log4j.appender.R.MaxBackupIndex", "5");
        pro.put("log4j.appender.R.Threshold", "DEBUG");
        pro.put("log4j.appender.R.layout", "org.apache.log4j.PatternLayout");
        pro.put("log4j.appender.R.layout.ConversionPattern", "%n[%d{HH:mm:ss}] [%p] %m");
        
		PropertyConfigurator.configure(pro);
	}

	public void setInterfaces(Object obj, JobDefinition def) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		// set the JobId
		if (obj instanceof JobIdAware) {
			JobIdAware jobIdAware = (JobIdAware)obj;
			jobIdAware.setJobId(jobId);
		}
		
		// set the progress reporter
		if (obj instanceof Progressable) {
			Progressable progressable = (Progressable) obj;
			String reporterClass = def.getProperty(JobDefinition.JOB_REPORTER_CLASS_KEY);
			if (reporterClass == null) {
				DefaultProgressReporter reporter = new DefaultProgressReporter();
				reporter.setClient(client);
				reporter.setJobId(jobId);
				progressable.setReporter(reporter);
			} else {
				Class<?> clazz = Class.forName(reporterClass);
				if (!ProgressReporter.class.isAssignableFrom(clazz)) {
					throw new IllegalArgumentException(reporterClass + " is not a subclass of " + ProgressReporter.class.getName());
				}
				Object reporter = clazz.newInstance();
				if (reporter instanceof JobIdAware) {
					((JobIdAware)reporter).setJobId(jobId);
				}
				progressable.setReporter((ProgressReporter)reporter);
			}
		}
	}
	
	
	public void invokeMethod(Object obj, JobDefinition def) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Class<?> clazz = obj.getClass();
		String methodName = def.getEnterMethod();
		String[] methodArgs = def.getEnterArgs().split(",");
		Method[] methods = clazz.getDeclaredMethods();
		Method candidate = null;
		for (Method method: methods) {
			if(method.getName().equals(methodName)) {
				if(candidate == null) {
					candidate = method;
				} else {
					throw new IllegalArgumentException("More than one methods are found:" + methodName);
				}
			}
		}
		// method parameters only support String array, like main
		candidate.setAccessible(true);
		Type[] types = candidate.getParameterTypes();
		if(types.length == 0) {
			candidate.invoke(obj);
		} else {
			if (types.length == 1 && types[0] == String[].class) {
				candidate.invoke(obj, new Object[]{methodArgs});
			} else {
				throw new IllegalArgumentException("Only support String array as parameters.");
			}
		}
	}
	

}
