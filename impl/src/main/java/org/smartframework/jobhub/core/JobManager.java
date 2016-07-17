package org.smartframework.jobhub.core;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.smartframework.jobhub.core.support.DefaultJobRegistry;
import org.smartframework.jobhub.core.support.DefaultScheduler;
import org.smartframework.jobhub.protocol.JobState;
import org.smartframework.jobhub.server.ServerContext;


public class JobManager {
	
	private static final Logger logger = Logger.getLogger(JobManager.class);
	
	private JobRegistry jobRegistry;
	private Scheduler scheduler;
	
	private AtomicLong jobId;
	
	private ServerContext ctx;
	
	public JobManager(ServerContext ctx) {
		jobRegistry = new DefaultJobRegistry();
		scheduler = new DefaultScheduler();
		jobId = new AtomicLong(1);
		this.ctx = ctx;
	}

	/**
	 * submit the job to {@link Scheduler} to run.
	 * @param jobId
	 */
	public void submit(long jobId) throws JobException {
		JobEntry entry = getJobEntry(jobId);
		if (entry == null) {
			throw new JobException("No such job id was found:" + jobId + ", you didn't register ");
		}
		scheduler.schedule(entry);
	}
	
	
	public boolean cancel(long jobId) {
		return scheduler.cancel(jobId);
	}
	
	public void registerJobEntry(JobEntry entry) {
		entry.setCreatedTime(System.currentTimeMillis());
		entry.setLastUpdated(System.currentTimeMillis());
		entry.setState(JobState.PREPARE);
		jobRegistry.registerJobEntry(entry);
	}

	public JobEntry getJobEntry(long jobId) {
		return jobRegistry.getJobEntry(jobId);
	}


	public JobEntry remove(long jobId) {
		return jobRegistry.remove(jobId);
	}


	public Set<JobEntry> getJobEntries(JobState state) {
		return jobRegistry.getJobEntries(state);
	}

	public boolean contains(long jobId) {
		// TODO Auto-generated method stub
		return jobRegistry.contains(jobId);
	}

	public long getJobId () {
		return jobId.getAndIncrement();
	}

	public JobRegistry getJobRegistry() {
		return jobRegistry;
	}

	public void setJobRegistry(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
	}

	public Scheduler getScheduler() {
		return scheduler;
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	public ServerContext getCtx() {
		return ctx;
	}

	public void setCtx(ServerContext ctx) {
		this.ctx = ctx;
	}
	
	
}
