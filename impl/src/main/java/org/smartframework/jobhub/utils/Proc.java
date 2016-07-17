package org.smartframework.jobhub.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;


public class Proc {

	private static final Logger logger = Logger.getLogger(Proc.class);
	
	private String workingDirectory;  // the working directory
	private long timeOut;  // the time out
	private Map<String, String> envMap;  // the additional environment
	private String procName; // the name of current process
	private String command;
	private List<String> args;
	
	private ByteArrayInputStream stdin;   // the input of the command
	private ByteArrayOutputStream stdout; // the output of the command
	
	private CommandRunner runner;
	
	// construct Proc must through ProcBuilder
	private Proc(String command, List<String> args, String workingDirectory,
			Map<String, String> envs, String procName, long timeOut,
			ByteArrayInputStream bis, ByteArrayOutputStream bos) {
		this.command = command;
		this.args = args;
		this.workingDirectory = workingDirectory;
		this.envMap = envs;
		this.procName = procName;
		this.timeOut = timeOut;
		this.stdin = bis;
		this.stdout = bos;
	}

	private ProcResult runInternal() throws InterruptedException, IOException {
		ProcResult result = new ProcResult(procName);
		ProcessBuilder pb = new ProcessBuilder(concatenateCommands());;
		if (workingDirectory != null) {
			pb.directory(new File(workingDirectory));
		}
		if (envMap != null) {
			Map<String, String> env = pb.environment();
			env.putAll(envMap);
		}
		Semaphore lock = new Semaphore(1);
		// run the command in a thread
		lock.acquire();
		runner = new CommandRunner(lock, pb, 
				stdin, stdout, result);
		runner.start();
		boolean done = lock.tryAcquire(timeOut, TimeUnit.MILLISECONDS);
		if (!done) { // runner is still running
			logger.info("Timeout after waiting " + timeOut + " ms, " + procName);
			runner.terminate();
			result.setExitCode(-255);
			result.setStderr("Timeout after waiting " + timeOut + " ms");
			result.setEndTime(System.currentTimeMillis());
		}
		runner.join();
		return result;
	}
	
	public void terminate() {
		runner.terminate();
	}
	
	/**
	 * Launch the process to run the command.
	 * @return the result of command, {@link ProcResult}.
	 * @throws Exception 
	 */
	public ProcResult run() throws Exception {
		try {
			return runInternal();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw e;
		}
	}
	
	private String[] concatenateCommands() {
		List<String> commands = new ArrayList<String>();
		commands.add(command);
		if (args != null) {
			commands.addAll(args);
		}
		return commands.toArray(new String[commands.size()]);
	}
	
	public class CommandRunner extends Thread
	{
		private Semaphore lock;
		private ProcessBuilder pb;
		private ByteArrayInputStream stdin;
		private ByteArrayOutputStream stdout;
		private ProcResult result;
		
		private Process process;
		StreamCopier inCopier;
		StreamCopier outCopier;
		StreamCopier defaultOutCopier;
		StreamCopier errCopier;
		
		
		public CommandRunner(Semaphore lock, ProcessBuilder pb, 
				ByteArrayInputStream stdin, ByteArrayOutputStream stdout,
				ProcResult result) {
			this.lock = lock;
			this.pb = pb;
			this.stdin = stdin;
			this.stdout = stdout;
			this.result = result;
		}
		
		@Override
		public void run() {
			result.setStartTime(System.currentTimeMillis());
			logger.info("Start to run: " + pb.command() + ".");
			process = null;
			try {
				process = pb.start();
				inCopier = new StreamCopier(stdin, process.getOutputStream());
				outCopier = new StreamCopier(process.getInputStream(), stdout);
				defaultOutCopier = new StreamCopier(process.getInputStream(), result.getStdout());
				errCopier = new StreamCopier(process.getErrorStream(), result.getStderr());
				if (stdin != null) {
					inCopier.start();
				}
				if (stdout != null) {
					outCopier.start();
				} else {
					defaultOutCopier.start();
				}
				errCopier.start();
				process.waitFor();
				join(inCopier);
				join(outCopier);
				join(errCopier);
				join(defaultOutCopier);
			} catch (Exception e) {
				result.setExitCode(-254);
				logger.error(e.getMessage(), e);
			} finally {
				lock.release();
				result.setEndTime(System.currentTimeMillis());
				if (process != null) result.setExitCode(process.exitValue());
				logger.info("Command done, exit code: " + result.getExitCode() + ", output: " + result.getOutput());
			}
		}
		
		public void join(Thread thread) throws InterruptedException {
			if (thread != null) thread.join();
		}
		
		private void stopCopier(StreamCopier copier) {
			if (copier != null) copier.stopCopy();
		}
		
		public void terminate() {
			logger.info("Try to terminate the command runner.");
			stopCopier(inCopier);
			stopCopier(outCopier);
			stopCopier(errCopier);
			stopCopier(defaultOutCopier);
			process.destroy();
			logger.info("Terminated.");
		}
	}
	
	public class StreamCopier extends Thread
	{
		private InputStream is;
		private OutputStream os;
		private boolean stop = false;
		private int BUFFER_SIZE = 1024;
		
		/**
		 * Concatenate two streams, copy {@code InputStream} to {@code OutputStream}.
		 * The streams will be closed after finishing copying.
		 * @param is
		 * @param os
		 */
		public StreamCopier(InputStream is, OutputStream os) {
			this.is = is;
			this.os = os;
		}
		
		public void run() {
			blockingCopy();
		}
		
		public void blockingCopy() { 
			if (is == null || os == null) {
				// log
				return ;
			}
			
			byte[] buffer = new byte[BUFFER_SIZE];
			int readBytes = 0;
			try {
				while(!isStop() && (readBytes = is.read(buffer)) != -1) {
					os.write(buffer, 0, readBytes);
				}
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			} finally {
				try {
					is.close();
					os.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}		
			}
		}
		
		public void stopCopy() {
			this.stop = true;
		}
		
		public boolean isStop() {
			return this.stop;
		}
		
	}
	
	public static class ProcBuilder {

		public static final long DEFAULT_TIMEOUT = 60000;  // 1 min
		
		private String workingDirectory;  // the working directory
		private long timeOut;  // the time out
		private Map<String, String> envMap;  // the additional environment
		private String procName; // the name of current process
		private String command;
		private List<String> args;
		
		private ByteArrayInputStream inputStream = null;   // 
		private ByteArrayOutputStream outputStream = null;
		
		
		public ProcBuilder(String procName, String command, String... args) {
			this.envMap = new HashMap<String, String>();
			this.args = new ArrayList<String>();
			this.procName = procName;
			this.command = command;
			this.timeOut = DEFAULT_TIMEOUT;
			this.withArgs(args);
		}
		
		public ProcBuilder withArg(String arg) {
			return this.withArgs(arg);
		}
		
		public ProcBuilder withArgs(String... args) {
			for(String arg: args) {
				this.args.add(arg);
			}
			return this;
		}
		
		/**
		 * Set the environment.
		 * @param key
		 * @param value
		 * @return
		 */
		public ProcBuilder withVar(String key, String value) {
			envMap.put(key, value);
			return this;
		}
		
		public ProcBuilder withVars(Map<String, String> envs) {
			this.envMap.putAll(envs);
			return this;
		}
		
		/**
		 * set the timeOut value. If time out, an ProExecutionException exception will throw.
		 * And the exit code will be set as -255.
		 *  
		 * @param timeOutMillis
		 * @return
		 */
		public ProcBuilder withTimeOut(long timeOutMillis) {
			this.timeOut = timeOutMillis;
			return this;
		}
		
		/**
		 * Set the command input as <code>input</code>.
		 * For some linux commands like echo, wc, we might need the input.
		 * @param input
		 * @return
		 */
		public ProcBuilder withInput(String input) {
			return this.withInputStream(new ByteArrayInputStream(input.getBytes()));
		}
		
		/**
		 * Set the command input Stream as <code>bis</code>.
		 * @param bis
		 * @return
		 */
		public ProcBuilder withInputStream(ByteArrayInputStream bis) {
			inputStream = bis;
			return this;
		}
		
		/**
		 * Set the output of command to <code>bos</bos>.
		 * Notice that no output will be kept in {@link ProcResult} object.
		 * @param bos
		 * @return
		 */
		public ProcBuilder withOutputStream(ByteArrayOutputStream bos) {
			this.outputStream = bos;
			return this;
		}
		
		public ProcBuilder workingDirectory(String wd) {
			this.workingDirectory = wd;
			return this;
		}
		
		public Proc build() {
			if (command == null) {
				throw new IllegalArgumentException("Command is not set.");
			}
			if (workingDirectory != null) {
				File file = new File(workingDirectory);
				if (!file.exists()) {
					throw new IllegalArgumentException("No such working directory:" + workingDirectory);
				}
			}
			return new Proc(command, args, workingDirectory,
					envMap, procName, timeOut,
					inputStream, outputStream);			
		}	
		
//		public ProcResult run() {
//			Proc proc = this.build();
//			return proc.run();
//		}
		
	}

}
