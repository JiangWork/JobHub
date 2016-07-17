package org.smartframework.jobhub.server;

import org.smartframework.jobhub.core.JobManager;

/**
 * A {@link ServerContext} contains general server information that can be
 * accessed by other classes.
 * 
 * @author Miller
 * @date Jul 2, 2016 9:53:53 PM
 */
public class ServerContext {
	
	public static int CLIENT_PROTOCOL_PORT = 32100;
	public static int INNER_PROTOCOL_PORT = 32101;
	public static int UPLOAD_PROTOCOL_PORT = 32102;
	
	
	private JobServer jobServer;
	private JobManager jobManager;
	private UploadServer uploadServer;
	private ClientProtocolServer clientProtocolServer;
	private InnerProtocolServer innerProtocolServer;
	
	
	
	public JobServer getJobServer() {
		return jobServer;
	}
	public void setJobServer(JobServer jobServer) {
		this.jobServer = jobServer;
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
	public JobManager getJobManager() {
		return jobManager;
	}
	public void setJobManager(JobManager jobManager) {
		this.jobManager = jobManager;
	}
	public InnerProtocolServer getInnerProtocolServer() {
		return innerProtocolServer;
	}
	public void setInnerProtocolServer(InnerProtocolServer innerProtocolServer) {
		this.innerProtocolServer = innerProtocolServer;
	}
	
	
}
