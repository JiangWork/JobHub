package org.smartframework.jobhub.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.smartframework.jobhub.protocol.ActionResult;
import org.smartframework.jobhub.protocol.ClientProtocol;
import org.smartframework.jobhub.protocol.JobState;
import org.smartframework.jobhub.protocol.JobStatus;
import org.smartframework.jobhub.protocol.MemoryStatus;
import org.smartframework.jobhub.utils.ParameterAccessor;

public class AdminTool {
	
	private final static Logger logger = Logger.getLogger(AdminTool.class);

	public static void main(String[] args) {
		if (args.length == 0) {
			onError(usage(), 1);
		}
		String hostName = JobSubmitter.DEFAULT_HOSTNAME;
		int port = JobSubmitter.DEFAULT_JOB_SERVER_PORT;
		InputStream is = JobSubmitter.class.getClassLoader().getResourceAsStream(JobSubmitter.APP_CONFIG_FILE);
		if (is != null) {  // has configuration
			try {
				Properties prop = new Properties();
				prop.load(is);
				ParameterAccessor pa = new ParameterAccessor(prop);
				hostName = pa.getString(JobSubmitter.HOSTNAME_KEY, JobSubmitter.DEFAULT_HOSTNAME);
				port = pa.getInt(JobSubmitter.JOBSERVER_PORT_KEY, JobSubmitter.DEFAULT_JOB_SERVER_PORT);
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				onError(e.getMessage(), 2);
			}
		}
		ClientProtocol.Client client = ClientProtocolClient.newClient(hostName, port);
		if (client == null) {
			onError(String.format("Can't connect to %d@%s", port, hostName), 2);
		} else {
			System.out.println(String.format("Successfully connect to %d@%s", port, hostName));
		}
		
		String command = args[0];
		if ("kill".equalsIgnoreCase(command)) {
			doKillAction(args, client);
		} else if ("showjob".equalsIgnoreCase(command)) {
			doShowJobAction(args, client);
		} else if ("memory".equalsIgnoreCase(command)) {
			doMonitorMemoryAction(args, client);
		} else if ("status".equalsIgnoreCase(command)) {
			doJobStatusAction(args, client);
		}
		else {
			ClientProtocolClient.close(client);
			onError(usage(), 2);
		}
	}
	
	public static String usage() {
		return "Usage: command [options]. Command can be one of following: \n"
				+ "\tkill jobIds: kill a job by giving jobIds, jobIds is in format of 1,2,3.\n"
				+ "\tshowjob [all|submitted|running|finished]: show job ids in JobServer "
				+ "whose state is submitted, running, finished or all.\n"
				+ "\tmemory jobIds fetch-times: monitor the memory usage for jobIds, jobIds is in format of 1,2,3.\n"
				+ "\tstatus jobIds fetch-times: monitor the job status for jobIds, jobIds is in format of 1,2,3.\n";
	}
	
	public static void onError(String msg, int exitCode) {
		System.err.println(msg);
		logger.error(msg);
		System.exit(exitCode);
	}
	
	public static void doKillAction(String args[], ClientProtocol.Client client) {
		if (args.length != 2) {
			ClientProtocolClient.close(client);
			onError(usage(), 2);
		}
		String[] jobIds = args[1].split(",");
		for(String jobId: jobIds) {
			try {
				ActionResult rslt = client.kill(Long.parseLong(jobId));
				if (rslt.success) {
					System.out.println(String.format("%d was killed.", jobId));
				} else {
					System.out.println(String.format("%d wasn't killed due to ", jobId, rslt.reason));
				}
			} catch (NumberFormatException e) {
				System.err.println("Error job id: " + jobId);
				logger.error(e.getMessage(), e);
			} catch (TException e) {
				System.err.println(e.getMessage());
				logger.error(e.getMessage(), e);
			}
		}
		ClientProtocolClient.close(client);
	}
	
	public static void doShowJobAction(String args[], ClientProtocol.Client client) {
		if (args.length != 2) {
			ClientProtocolClient.close(client);
			onError(usage(), 2);
		}
		String jobState = args[1];
		if ("all".equalsIgnoreCase(jobState)) {
			try {
				System.out.println("Submitted jobs: " + client.getJobsFor(JobState.SUBMITTED));
				System.out.println("Running jobs: " + client.getJobsFor(JobState.RUNNING));
				System.out.println("Finished jobs: " + client.getJobsFor(JobState.DONE));
			} catch (TException e) {
				System.err.println(e.getMessage());
				logger.error(e.getMessage(), e);
			}
		} else if ("running".equalsIgnoreCase(jobState)) {
			try {
				System.out.println("Running jobs: " + client.getJobsFor(JobState.RUNNING));
			} catch (TException e) {
				System.err.println(e.getMessage());
				logger.error(e.getMessage(), e);
			}
		
		} else if ("submitted".equalsIgnoreCase(jobState)) {
			try {
				System.out.println("Submitted jobs: " + client.getJobsFor(JobState.SUBMITTED));
			} catch (TException e) {
				System.err.println(e.getMessage());
				logger.error(e.getMessage(), e);
			}
		} else if ("finished".equalsIgnoreCase(jobState)) {
			try {
				System.out.println("Finished jobs: " + client.getJobsFor(JobState.DONE));
			} catch (TException e) {
				System.err.println(e.getMessage());
				logger.error(e.getMessage(), e);
			}
		} else {
			ClientProtocolClient.close(client);
			onError(usage(), 2);
		}
		ClientProtocolClient.close(client);
	}

	public static void doMonitorMemoryAction(String args[], ClientProtocol.Client client) {
		if (args.length != 3) {
			ClientProtocolClient.close(client);
			onError(usage(), 2);
		}
		String[] jobIds = args[1].split(",");
		List<Long> ids = new ArrayList<Long>();
		for(String id: jobIds) {
			try {
			ids.add(Long.parseLong(id));
			} catch (NumberFormatException e) {
				System.err.println("Error job id: " + id + ", ignored.");
			}
		}
		System.out.println("Name\tId\tusedBytes (KB)\tlastUpdated");
		int times = 10;
		try {
			times = Integer.parseInt(args[2]);
		} catch(Exception e) {
			System.err.println("Error times: " + args[2] + ", 10 is used.");
		}
		while(times-- > 0) {
			for (long id: ids) {
				try {
					MemoryStatus ms = client.memory(id);
					System.out.println(String.format("%s\t%d\t%.3f\t%s", ms.name, ms.jobId, ms.usedByte*1.0/1000, new Date(ms.lastUpdated)));
				} catch (TException e) {
					logger.error(e.getMessage(), e);
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				//ignored.
			}
		}
		ClientProtocolClient.close(client);
	}
	
	public static void doJobStatusAction(String args[], ClientProtocol.Client client) {
		if (args.length != 3) {
			ClientProtocolClient.close(client);
			onError(usage(), 2);
		}
		String[] jobIds = args[1].split(",");
		List<Long> ids = new ArrayList<Long>();
		for(String id: jobIds) {
			try {
			ids.add(Long.parseLong(id));
			} catch (NumberFormatException e) {
				System.err.println("Error job id: " + id + ", ignored.");
			}
		}
		System.out.println("Id\tProgress\tStatus\tstartTime\tReason");
		int times = 10;
		try {
			times = Integer.parseInt(args[2]);
		} catch(Exception e) {
			System.err.println("Error times: " + args[2] + ", 10 is used.");
		}
		while(times-- > 0) {
			for (long id: ids) {
				try {
					JobStatus js = client.query(id);
					System.out.println(String.format("%d\t%d\t%s\t%s\t%s", id, js.state, js.progress, js.success?"Success":"Fail", new Date(js.startTime), js.reason));
				} catch (TException e) {
					logger.error(e.getMessage(), e);
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				//ignored.
			}
		}
		ClientProtocolClient.close(client);
	}
}
