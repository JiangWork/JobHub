package org.smartframework.jobhub.core;

import java.util.Set;

import org.smartframework.jobhub.protocol.JobState;

/**
 * A container contains {@link JobEntry}.
 * 
 * @author Miller
 * @date Jul 2, 2016 11:19:17 PM
 */
public interface JobRegistry {

	public void registerJobEntry(JobEntry entry);
	
	public JobEntry getJobEntry(long jobId);
	
	public JobEntry remove(long jobId);
	
	public Set<JobEntry> getJobEntries(JobState state);
	
	public boolean contains(long jobId);
	
}
