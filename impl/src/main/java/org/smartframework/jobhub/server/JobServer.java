package org.smartframework.jobhub.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.thrift.transport.TTransportException;
import org.smartframework.jobhub.core.DirectoryAllocator;
import org.smartframework.jobhub.core.JobException;
import org.smartframework.jobhub.core.JobManager;
import org.smartframework.jobhub.core.MemoryMonitor;
import org.smartframework.jobhub.utils.ParameterAccessor;

/**
 * A job server provide job execution services.
 * 
 * @author Miller
 * @date Jul 2, 2016 9:57:46 PM
 */
public class JobServer {
	private final static String APP_CONFIG = "server.properties";
	private final static Logger logger = Logger.getLogger(JobServer.class);
	
	private JobManager jobManager;
	private UploadServer uploadServer;
	private ClientProtocolServer clientProtocolServer;
	private InnerProtocolServer innerProtocolServer;
	private MemoryMonitor memoryMonitor;
	// create working directory
	
	private volatile boolean running = false;
	
	public JobServer() {
		
	}
	
	public synchronized void startServer() throws JobException {
		// read config
		InputStream is = JobServer.class.getClassLoader().getResourceAsStream(APP_CONFIG);
		Properties prop = new Properties();
		int uploadServerPort = ServerContext.UPLOAD_PROTOCOL_PORT;
		int jobServerPort = ServerContext.CLIENT_PROTOCOL_PORT;
		int innerServerPort = ServerContext.INNER_PROTOCOL_PORT;
		ServerContext ctx = new ServerContext();
		if (is != null) {
			try {
				prop.load(is);
				ParameterAccessor pa = new ParameterAccessor(prop);
				jobServerPort = pa.getInt("jobhub.jobserver.port", 32100);
				innerServerPort = pa.getInt("jobhub.innerserver.port", 32101);
				uploadServerPort = pa.getInt("jobhub.uploadserver.port", 32102);
				System.setProperty("jobhub.workdir", pa.getString("jobhub.workdir",
						System.getProperty("user.home")));
				ServerContext.CLIENT_PROTOCOL_PORT = jobServerPort;
				ServerContext.INNER_PROTOCOL_PORT = innerServerPort;
				ServerContext.UPLOAD_PROTOCOL_PORT = uploadServerPort;
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			logger.info("Default ports are used.");
		}
		// create working directory if necessary
		createDirIfNeed(DirectoryAllocator.WORKING_DIRECTORY);
		createDirIfNeed(DirectoryAllocator.UPLOAD_DIRECTORY);
		createDirIfNeed(DirectoryAllocator.JOB_DIRECTORY);
		
		// setup different servers
		jobManager = new JobManager(ctx);
		ctx.setJobManager(jobManager);
		
		uploadServer = new UploadServer(uploadServerPort);
		uploadServer.setUploadDirectory(DirectoryAllocator.UPLOAD_DIRECTORY);
		try {
			uploadServer.start();
			ctx.setUploadServer(uploadServer);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new JobException(e.getMessage(), e);
		}
		clientProtocolServer = new ClientProtocolServer(ctx);
		try {
			clientProtocolServer.start();
			ctx.setClientProtocolServer(clientProtocolServer);
		} catch (TTransportException e) {
			logger.error(e.getMessage(), e);
			throw new JobException(e.getMessage(), e);
		}
		innerProtocolServer = new InnerProtocolServer(ctx);
		try {
			innerProtocolServer.start();
			ctx.setInnerProtocolServer(innerProtocolServer);
		} catch (TTransportException e) {
			logger.error(e.getMessage(), e);
			throw new JobException(e.getMessage(), e);
		}
		ctx.setJobServer(this);
		memoryMonitor = new MemoryMonitor();
		memoryMonitor.setAutoPurge(true);
		memoryMonitor.start();
		running = true;
	}
	
	private void createDirIfNeed(String dir) throws JobException {
		File file = new File(dir);
		if (!file.exists()) {
			logger.debug("Creating directory " + dir);
			if(!file.mkdirs()) {
				throw new JobException("Can't create directory: " + dir);
			}
		}
	}
	
	public JobManager getJobManager() {
		return jobManager;
	}
	public void setJobManager(JobManager jobManager) {
		this.jobManager = jobManager;
	}
	public UploadServer getUploadServer() {
		return uploadServer;
	}
	public void setUploadServer(UploadServer uploadServer) {
		this.uploadServer = uploadServer;
	}
	public ClientProtocolServer getClientProtocolServer() {
		return clientProtocolServer;
	}
	public void setClientProtocolServer(ClientProtocolServer clientProtocolServer) {
		this.clientProtocolServer = clientProtocolServer;
	}
	
	public MemoryMonitor getMemoryMonitor() {
		return memoryMonitor;
	}
	
	public synchronized void cleanUp() {
		if (clientProtocolServer != null) {
			clientProtocolServer.stop();
		}
		if (innerProtocolServer != null) {
			innerProtocolServer.stop();
		}
		if (uploadServer != null) {
			uploadServer.stop();
		} 
		if (memoryMonitor != null) {
			memoryMonitor.stop();
		}
		running = false;
	}
	
	public void stop() {
		if(running) {
			logger.info("Stopping JobServer...");
			cleanUp();
			running = false;
		}
	}
	
	public void waitForServring() {
		clientProtocolServer.waitForServing();
		innerProtocolServer.waitForServing();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			//ignored.
		}
	}
	
	public Thread getStopHook() {
		return new Thread() {
			public void run() {
				setName("StoppingJobServer");
				JobServer.this.stop();
			}
		};
	}
	
	public static void main(String[] args) {
		final JobServer server = new JobServer();
		try {
			server.startServer();
			server.waitForServring();
			Runtime.getRuntime().addShutdownHook(server.getStopHook());
		} catch (JobException e) {
			logger.error(e.getMessage(), e);
			logger.info("Clean up after startup failure...");
			server.cleanUp();
			logger.error("Startup fails: " + e.getMessage());
		}
		if (server.running) {
			logger.info("JobServer starts up successfully.");
		}
	}

}
