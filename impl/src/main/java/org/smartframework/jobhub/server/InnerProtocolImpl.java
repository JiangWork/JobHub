package org.smartframework.jobhub.server;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.smartframework.jobhub.core.JobEntry;
import org.smartframework.jobhub.core.JobManager;
import org.smartframework.jobhub.protocol.InnerProtocol;
import org.smartframework.jobhub.protocol.InnerProtocol.Iface;
import org.smartframework.jobhub.protocol.JobState;

/**
 * A InnerProtocolImpl used to report job progress.
 * 
 * @author Miller
 * @date Jul 2, 2016 10:32:11 PM
 */
public class InnerProtocolImpl implements InnerProtocol.Iface {

	private final Logger logger = Logger.getLogger(InnerProtocolImpl.class);
	
	private JobManager jobManager;
	
	public InnerProtocolImpl(JobManager jobManager) {
		this.jobManager = jobManager;
	}
	
	@Override
	public boolean progress(long jobId, int precent) throws TException {
		logger.debug("Report progress, jobId=" + jobId + " precent=" + precent);
		if (jobManager.contains(jobId)) {
			JobEntry entry = jobManager.getJobEntry(jobId);
			int beforePrecent = entry.getProgress();
			if (beforePrecent < precent) {
				entry.setProgress(precent);
			}
		} else {
			logger.error("Fail to report progress, no such job is running, jobId=" + jobId);
		}
		return false;
	}

	@Override
	public boolean message(long jobId, String message) throws TException {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void setPid(long jobId, int pid) throws TException {
		logger.debug("SetPid is invoked.");
		if (jobManager.contains(jobId)) {
			JobEntry entry = jobManager.getJobEntry(jobId);
			entry.setPid(pid);
			this.jobManager.getCtx().getJobServer().getMemoryMonitor().add(pid);
			logger.info("Job " + jobId + " was set PID as " + pid);
		}
		
	}

	@Override
	public void setJobState(long jobId, JobState jobState) throws TException {
		logger.debug("SetJobState is invoked.");
		if (jobManager.contains(jobId)) {
			JobEntry entry = jobManager.getJobEntry(jobId);
			entry.setState(jobState);
			logger.info("Job " + jobId + " was set JobState as " + jobState);
		}
	}


}
