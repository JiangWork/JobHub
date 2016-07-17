package org.smartframework.jobhub.server;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.smartframework.jobhub.core.DirectoryAllocator;
import org.smartframework.jobhub.core.JobDefinition;
import org.smartframework.jobhub.core.JobEntry;
import org.smartframework.jobhub.core.JobManager;
import org.smartframework.jobhub.core.MemoryMonitor.PidStatus;
import org.smartframework.jobhub.protocol.ActionResult;
import org.smartframework.jobhub.protocol.ClientProtocol;
import org.smartframework.jobhub.protocol.JobStatus;
import org.smartframework.jobhub.protocol.MemoryStatus;
import org.smartframework.jobhub.protocol.ClientProtocol.Iface;
import org.smartframework.jobhub.protocol.JobState;

/**
 * 
 * @author Miller
 * @date Jul 2, 2016 10:03:52 PM
 */
public class ClientProtocolImpl implements ClientProtocol.Iface {
	
	private JobManager jobManager;
	
	private ClientProtocol.Iface proxy;
	
	public ClientProtocolImpl(JobManager jobManager) {
		this.jobManager = jobManager;
		ClientProtocol.Iface delegate = new ClientProtocolImplDelegate(jobManager);
		proxy = (Iface) Proxy.newProxyInstance(delegate.getClass().getClassLoader(), 
				delegate.getClass().getInterfaces(), new ClientProtocolInvocation(delegate));
		
	}
	
	@Override
	public long newJobId() throws TException {
		// TODO Auto-generated method stub
		return proxy.newJobId();
	}

	@Override
	public ActionResult submit(long jobId, ByteBuffer configData) throws TException {
		// TODO Auto-generated method stub
		return proxy.submit(jobId, configData);
	}

	@Override
	public ActionResult kill(long jobId) throws TException {
		// TODO Auto-generated method stub
		return proxy.kill(jobId);
	}

	@Override
	public JobStatus query(long jobId) throws TException {
		// TODO Auto-generated method stub
		return proxy.query(jobId);
	}
	
	@Override
	public MemoryStatus memory(long jobId) throws TException {
		// TODO Auto-generated method stub
		return proxy.memory(jobId);
	}

	@Override
	public List<Long> getJobsFor(JobState state) throws TException {
		// TODO Auto-generated method stub
		return proxy.getJobsFor(state);
	}
	
	private class ClientProtocolInvocation implements InvocationHandler {
		private final Logger logger = Logger.getLogger(ClientProtocolImplDelegate.class);
		
		public Object target;

		public ClientProtocolInvocation(Object target) {
			this.target = target;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			//logger.info(method.getName() + " start to call.");
			Object ret = method.invoke(target, args);
			//logger.info(method.getName() + " end of call.");
			return ret;
		}
		
	}
	
	

	private static class ClientProtocolImplDelegate implements  ClientProtocol.Iface {
		private final Logger logger = Logger.getLogger(ClientProtocolImplDelegate.class);
		
		private JobManager jobManager;
		
		public ClientProtocolImplDelegate( JobManager jobManager) {
			this.jobManager = jobManager;
		}
		
		public long newJobId() {
			long newJobId = jobManager.getJobId();
			logger.debug("JobId " + newJobId + " is returned.");
			JobEntry entry = new JobEntry();
			entry.setId(newJobId);
			jobManager.registerJobEntry(entry);
			return newJobId;
		}

		public ActionResult submit(long jobId, ByteBuffer configData) {
			ActionResult result = new ActionResult();
			result.success = true;
			JobDefinition def = new JobDefinition();
			byte[] buf = configData.array();
			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));
			try {
				def.read(dis);
				// check the uploaded file
				StringBuilder missingFiles = new StringBuilder();
				for(String jarName: def.getJarsList()) {
					String path = DirectoryAllocator.uploadDirectory(jobId) + File.separator + jarName;
					if (!new File(path).exists()) {
						missingFiles.append(" " + jarName);
					}
				}
				for (String resource: def.getResourcesList()) {
					String path = DirectoryAllocator.uploadDirectory(jobId) + File.separator + resource;
					if (!new File(path).exists()) {
						missingFiles.append(" " + resource);
					}
				}
				if (missingFiles.length() != 0) {
					throw new IOException("Missing files: " + missingFiles.toString()
							+ ", please make sure it was uploaded successfully.");
				}
				JobEntry entry = jobManager.getJobEntry(jobId);
				entry.setDefinition(def);
				jobManager.submit(jobId);
				result.reason = "Job submitted at " + new Date() + ", jobid:" + jobId + " by " + def.getSubmitter();
				logger.debug("Job submitted at " + new Date() + ", jobid:" + jobId + " by " + def.getSubmitter());
			} catch (Exception e) {
				result.success = false;
				result.reason = e.getMessage();
				logger.error(e.getMessage(), e);
			}
			return result;
			
		}

		public ActionResult kill(long jobId) throws TException {
			ActionResult result = new ActionResult();
			if (!jobManager.contains(jobId)) {
				result.success = false;
				result.reason = "Job doesn't exists: " + jobId;
			} else {
				boolean ret = jobManager.cancel(jobId);
				if (ret) {
					result.reason = "Job was cancelled successfully: " + jobId;
				} else {
					result.reason = "Job failed to cancel: " + jobId;
				}
			}
			return result;
		}

		public JobStatus query(long jobId) throws TException {
			JobStatus status = new JobStatus();
			JobEntry entry = jobManager.getJobEntry(jobId);
			if (entry != null) {
				status.progress = entry.getProgress();
				status.success = entry.isSuccess();
				status.reason = entry.getReason();
				status.startTime = entry.getStartTime();
				status.setState(entry.getState());
			} else {
				status.success = false;
				status.reason = "Job doesn't exists: " + jobId;
				status.progress = 0;
				status.startTime = -1;
			}
			return status;
		}

		@Override
		public MemoryStatus memory(long jobId) throws TException {
			MemoryStatus ms = new MemoryStatus();
			if (jobManager.contains(jobId)) {
				JobEntry jobEntry = jobManager.getJobEntry(jobId);
				long pid = jobEntry.getPid();
				PidStatus ps = jobManager.getCtx().getJobServer().getMemoryMonitor().getPidStatus(pid);
				ms.name = jobEntry.getDefinition().getJobName();
				ms.jobId = jobId;
				ms.usedByte = ps.getMemUsage();
				ms.lastUpdated = ps.getLastUpdated();
			}
			return ms;
		}

		@Override
		public List<Long> getJobsFor(JobState state) throws TException {
			Set<JobEntry> jobEntries = jobManager.getJobEntries(state);
			List<Long> jobIds = new ArrayList<Long>();
			for (JobEntry jobEntry: jobEntries) {
				jobIds.add(jobEntry.getId());
			}
			return jobIds;
		}
	}




}
