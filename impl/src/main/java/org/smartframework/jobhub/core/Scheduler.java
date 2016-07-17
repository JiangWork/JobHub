package org.smartframework.jobhub.core;


public interface Scheduler {
	
	/**
	 * Schedule the submitted jobs to run.
	 */
	public void schedule(JobEntry jobEntry);
	
	/**
	 * Cancel the job. 
	 * If the job is not running, then remove from pending list.
	 * If the job is running, then stop it.
	 * @param jobId
	 */
	public boolean cancel(long jobId);
	
}
