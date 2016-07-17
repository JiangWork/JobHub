package org.smartframework.jobhub.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.smartframework.jobhub.utils.MemoryUtil;
import org.smartframework.jobhub.utils.Proc;
import org.smartframework.jobhub.utils.ProcResult;

/**
 * Monitor the memory usage for given PIDs. 
 * It will update the memory usage for alive PIDs periodically.
 * 
 * Also it provide facility to purge Pids which are dead.
 *  
 * @author jiangzhao
 * @date Jul 12, 2016
 * @version V1.0
 */
public class MemoryMonitor {
	
	private final static Logger logger = Logger.getLogger(MemoryMonitor.class);

	private Lock addLock;
	private Lock removeLock;
	private Lock mainLock;
	
	private Map<Long, PidStatus> statusMap;
	
	/**the pids need to be monitored, only Worker can access and modify it.**/
	private List<Long> pids;
	
	private List<Long> pidsToBeAdded;
	
	private List<Long> pidsToBeRemoved;
	
	private long interval;
	
	/**if we need to remove the dead pid from monitoring**/
	private boolean autoPurge;
	
	private Worker worker;
	
	public MemoryMonitor() {
		addLock = new ReentrantLock();
		removeLock = new ReentrantLock();
		mainLock = new ReentrantLock();
		statusMap = new HashMap<Long, PidStatus>();
		pids = new ArrayList<Long>();
		pidsToBeAdded = new ArrayList<Long>();
		pidsToBeRemoved = new ArrayList<Long>();
		interval = 1 * 1000;  // 1 s
		autoPurge = false;
		worker = new Worker();
	}
	
	
	/**
	 * Request to add the pid to monitor.
	 * The pid will be monitored interval ms later at most.
	 * @param pid
	 */
	public void add(long pid) {
		try {
			addLock.lock();
			this.pidsToBeAdded.add(pid);
		} finally {
			addLock.unlock();
		}
		worker.interrupt();
	}
	
	/**
	 * Request to remove the pid from monitoring
	 * @param pid
	 */
	public void remove(long pid) {
		try {
			removeLock.lock();
			this.pidsToBeRemoved.add(pid);
		} finally {
			removeLock.unlock();
		}
	}
	
	public void remove(List<Long> pids) {
		try {
			removeLock.lock();
			this.pidsToBeRemoved.addAll(pids);
		} finally {
			removeLock.unlock();
		}
	}
	
	/**
	 * Get the memory usage for pid.
	 * 0 will be returned if pid is not monitored or start to monitor.
	 * 
	 * @param pid
	 * @return
	 */
	public long memUsage(long pid) {
		PidStatus ps = statusMap.get(pid);
		if (ps == null) {
			return 0;
		}
		return ps.getMemUsage();
	}
	
	/**
	 * Return a copy of PidStatus if exists.
	 * Else null.
	 * @param pid
	 * @return
	 */
	public PidStatus getPidStatus(long pid) {
		PidStatus ps = statusMap.get(pid);
		if (ps == null) {
			return null;
		}
		return ps.copy();
	}
	
	/**
	 * Wake up the worker thread to monitor the pid right now.
	 */
	public void refresh() {
		if (worker.isRunning()) {
			worker.interrupt();
		}
	}
	
	public void start() {
		if(!worker.isRunning()) {
			worker.startup();
		}
	}
	
	public void stop() {
		logger.debug("Stopping MemoryMonitor ...");
		worker.interrupt();
		worker.shutdown();
		try {
			worker.join();
		} catch (InterruptedException e) {
			// ignored
		}
		statusMap.clear();
		pids.clear();
		pidsToBeAdded.clear();
		pidsToBeRemoved.clear();
	}
	
	/**
	 * Test if pid is alive or not.
	 * @param pid
	 * @return
	 */
	public boolean isAlive(long pid) {
		boolean isAlive = false;
		try {
			ProcResult result = new Proc.ProcBuilder("isAlive", "ps", "-p", String.valueOf(pid)).build().run();
			isAlive = (result.getExitCode() == 0);
		}
		catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
		return isAlive;
	}
	
	/**for test purpose**/
	public void printStatus() {
		System.out.println("##################printStatus###################");
		try {
			mainLock.lock();
			Iterator<Long> iter = statusMap.keySet().iterator();
			while(iter.hasNext()) {
				long pid = iter.next();
				PidStatus ps = statusMap.get(pid);
				if (ps !=null) {
					System.out.println(String.format("pid=%d, memusage=%d, isAlive=%s", 
							ps.pid, ps.memUsage, ps.isAlive?"true":"false"));
				}
			} } finally {
			mainLock.unlock();
		}
	}

	public long getInterval() {
		return interval;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	public boolean isAutoPurge() {
		return autoPurge;
	}

	public void setAutoPurge(boolean autoPurge) {
		this.autoPurge = autoPurge;
	}


	
	private class Worker extends Thread {
		
		private volatile boolean stop = true;
		
		private long runTimes = 0;
		
		
		public void run() {
			System.out.println("Starting MemoryMonitorWorker");
		    setName("MemoryMonitorWorker");	
		 
			while(!stop) {
				// remove the pids
				List<Long> removeCands = new ArrayList<Long>();
				try {
					removeLock.lock();
					pids.removeAll(pidsToBeRemoved);
					removeCands.addAll(pidsToBeRemoved);
					pidsToBeRemoved.clear();
				} finally {
					removeLock.unlock();
				}
				try {
				mainLock.lock();
				for (long removeKey: removeCands) {
					statusMap.remove(removeKey);
				}
				if(!removeCands.isEmpty()) logger.debug("removed pids: " + removeCands);
				} finally {
					mainLock.unlock();
				}
				
				// add the pids at head
				try {
					addLock.lock();
					pids.addAll(0, pidsToBeAdded);
					pidsToBeAdded.clear();
				} finally {
					addLock.unlock();
				}

				//go through all Pids
				List<Long> purgeList = new ArrayList<Long>();
				for (long pid: pids) {	
					try {
						mainLock.lock();
					if (!statusMap.containsKey(pid)) {
						PidStatus pidStatus = new PidStatus(pid);
						statusMap.put(pid, pidStatus);
					}
					PidStatus pidStatus = statusMap.get(pid);
					boolean isAlive = MemoryMonitor.this.isAlive(pid);
					if (!isAlive && autoPurge) {
						purgeList.add(pid);
					}
					pidStatus.setAlive(isAlive);
					try {
						long bytes = MemoryUtil.usage(String.valueOf(pid));
						pidStatus.setMemUsage(bytes);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
					pidStatus.setLastUpdated(System.currentTimeMillis());
					} finally {
						mainLock.unlock();
					}
				}
				remove(purgeList);
				logger.debug("Scheduled run at " + (runTimes++) + " time, scanned " + pids);
				long sleepTime = interval;
				if (pids.isEmpty()) {
					sleepTime = 10 * 60 * 1000;
				}
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					// ignored
					// System.out.println("Wake up");
					Thread.interrupted();
				}
			}
		}
		
		public void shutdown() {
			this.stop = true;
		}
		
		public void startup() {
			if(stop) {
				stop = false;
				start();
			}
		}
		
		public boolean isRunning() {
			return !this.stop;
		}
	}
	
	public static class PidStatus {
		private long pid;
		/**indicate the process is alive or not**/
		private boolean isAlive;
		/**the memory usage in bytes**/
		private long memUsage;
		/**last updated time**/
		private long lastUpdated;
		
		public PidStatus() {
			
		}
		
		public PidStatus(long pid) {
			this.pid = pid;
		}
		
		public long getPid() {
			return pid;
		}
		public void setPid(long pid) {
			this.pid = pid;
		}
		public boolean isAlive() {
			return isAlive;
		}
		public void setAlive(boolean isAlive) {
			this.isAlive = isAlive;
		}
		public long getMemUsage() {
			return memUsage;
		}
		public void setMemUsage(long memUsage) {
			this.memUsage = memUsage;
		}
		public long getLastUpdated() {
			return lastUpdated;
		}
		public void setLastUpdated(long lastUpdated) {
			this.lastUpdated = lastUpdated;
		}
		
		public PidStatus copy() {
			PidStatus ps = new PidStatus(pid);
			ps.isAlive = isAlive;
			ps.lastUpdated = lastUpdated;
			ps.memUsage = memUsage;
			return ps;
		}
	}
	
	public static void main(String[] args) throws InterruptedException {
		MemoryMonitor monitor = new MemoryMonitor();
		monitor.start();
		Thread.sleep(10 * 1000);
		monitor.add(14501);
		
//		MemoryMonitor monitor = new MemoryMonitor();
//		monitor.setAutoPurge(true);
//		for (int i = 1; i < 1000; ++i) {
//			monitor.add(i);
//		}
//		monitor.start();
//		while(true) {
//			monitor.printStatus();
//			try {
//				Thread.sleep(5000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
	}
}
