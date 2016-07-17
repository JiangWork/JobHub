package org.smartframework.jobhub.core.support;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.smartframework.jobhub.core.JobEntry;
import org.smartframework.jobhub.core.JobRunner;
import org.smartframework.jobhub.core.Scheduler;
import org.smartframework.jobhub.core.StateChangedListener;

public class DefaultScheduler implements Scheduler {
	
	private final static Logger logger = Logger.getLogger(DefaultScheduler.class);
	
	public final static int CONCURRENT_SIZE = 10;

	private BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
	private ThreadPoolExecutor executor = new ThreadPoolExecutor(CONCURRENT_SIZE, CONCURRENT_SIZE,
            0L, TimeUnit.MILLISECONDS,
            queue);
	
	private Map<Long, JobRunner> jobRunnerMap = new HashMap<Long, JobRunner>();
	
	private StateChangedListener completionListener = new StateChangedAdapter() {
		@Override
		public void onDone(JobEntry jobEntry) {
			synchronized(jobRunnerMap) {
				jobRunnerMap.remove(jobEntry.getId());
			}
		}
	};
	
	/**
	 * No schedule strategy, just submit to executor.
	 */
	@Override
	public void schedule(JobEntry jobEntry) {
		JobRunner runner = new JobRunner(jobEntry);
		runner.addStateChangedListener(completionListener);
		synchronized(jobRunnerMap) {
			jobRunnerMap.put(jobEntry.getId(), runner);
		}
		executor.submit(runner);
		logger.info("schedule job successfully.");
	}
	
	@Override
	public boolean cancel(long jobId) {
		JobRunner runner = null;
		synchronized(jobRunnerMap) {
			if (jobRunnerMap.containsKey(jobId)) {
				runner = jobRunnerMap.remove(jobId);
			}
		}
		if (runner != null) {
			executor.remove(runner);
			runner.stop();
			logger.info("Job " + jobId + "is canceled successfully.");
			return true;
		} else {
			logger.info("Request cancelled job doesn't exists: " + jobId);
			return false;
		}
		
	}

}
