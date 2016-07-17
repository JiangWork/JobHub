package org.smartframework.jobhub.common;

/**
 * A reporter to report progress.
 * @author Miller
 * @date Jul 2, 2016 11:10:35 PM
 */
public interface ProgressReporter extends Reporter {

	/**
	 * Report the progress percent to framework.
	 * @param precent
	 */
	public void report(int percent);
}
