package org.smartframework.jobhub.core;

public class JobException extends Exception {

	public JobException() {};
	
	public JobException(String msg) {
		super(msg);
	}
	
	public JobException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
