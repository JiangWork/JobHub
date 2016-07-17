package org.smartframework.jobhub.common;

/**
 * Mark a job is progressable.
 * A progressable job can report its progress to server.
 * 
 * @author Miller
 * @date Jul 2, 2016 11:12:13 PM
 */
public interface Progressable {
	
	/**set the reporter used by this object**/
	public void setReporter(ProgressReporter reporter);
}
