package org.smartframework.jobhub.core;

import org.smartframework.jobhub.protocol.JobState;

// used to describe a job status.
public class JobEntry {
	
	private long id;
	
	private JobState state;
	
	private long createdTime;
	
	private long startTime;
	
	private boolean success;
	
	private String reason;
	
	private int progress;
	
	private long lastUpdated;
	
	private long pid;
	
	private JobDefinition definition;


	public synchronized long getId() {
		return id;
	}

	public synchronized void setId(long id) {
		this.id = id;
	}

	public synchronized JobState getState() {
		return state;
	}

	public synchronized void setState(JobState state) {
		this.state = state;
	}

	public synchronized long getCreatedTime() {
		return createdTime;
	}

	public synchronized void setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
	}

	public synchronized long getStartTime() {
		return startTime;
	}

	public synchronized void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public synchronized boolean isSuccess() {
		return success;
	}

	public synchronized void setSuccess(boolean success) {
		this.success = success;
	}

	public synchronized String getReason() {
		return reason;
	}

	public synchronized void setReason(String reason) {
		this.reason = reason;
	}

	public synchronized int getProgress() {
		return progress;
	}

	public synchronized void setProgress(int progress) {
		this.progress = progress;
	}

	public synchronized long getLastUpdated() {
		return lastUpdated;
	}

	public synchronized void setLastUpdated(long lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public synchronized long getPid() {
		return pid;
	}

	public synchronized void setPid(long pid) {
		this.pid = pid;
	}

	public synchronized JobDefinition getDefinition() {
		return definition;
	}

	public synchronized void setDefinition(JobDefinition definition) {
		this.definition = definition;
	}

	@Override
	public boolean equals(Object another) {
		return another instanceof JobEntry ? 
				this.id == ((JobEntry)another).id : false;
	}
	
	public void finishSuccess() {
		this.setLastUpdated(System.currentTimeMillis());
		this.setSuccess(true);
		this.setProgress(100);
		this.setState(JobState.DONE);
	}
	
	public void finishFail(String msg) {
		this.setLastUpdated(System.currentTimeMillis());
		this.setSuccess(false);
		this.setReason(msg);
		this.setProgress(100);
		this.setState(JobState.DONE);
	}
	
}
