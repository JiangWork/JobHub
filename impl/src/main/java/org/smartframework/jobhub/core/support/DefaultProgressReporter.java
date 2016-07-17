package org.smartframework.jobhub.core.support;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.smartframework.jobhub.common.JobIdAware;
import org.smartframework.jobhub.common.ProgressReporter;
import org.smartframework.jobhub.protocol.InnerProtocol;

/**
 * Report the progress to the framework.
 * 
 * @author Miller
 * @date Jul 5, 2016 11:04:45 PM
 */
public class DefaultProgressReporter implements ProgressReporter, JobIdAware {
	
	private final static Logger logger = Logger.getLogger(DefaultProgressReporter.class);
	
	private InnerProtocol.Client client;
	private long jobId;
	
	
	public DefaultProgressReporter() {
		
	}

	public void setClient(InnerProtocol.Client client) {
		this.client = client;
	}
	
	@Override
	public void report(int percent) {
		if (client != null) {
			try {
				client.progress(jobId, percent);
			} catch (TException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

}
