package org.smartframework.jobhub.core;

/**
 * A set of methods triggered by the change of job state.
 * 
 * @author Miller
 * @date Jul 3, 2016 9:28:45 AM
 */
public interface StateChangedListener {

	/**
	 * Fired when submitting the job.
	 * @param jobEntry
	 */
	public void onPrepare(JobEntry jobEntry);
	
	
	public void onSubmit(JobEntry jobEntry);
	
	public void onRunning(JobEntry jobEntry);
	
	public void onReSchedule(JobEntry jobEntry);
	
	public void onDone(JobEntry jobEntry);
}
