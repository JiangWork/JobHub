package org.smartframework.jobhub.utils;

import java.util.Date;

/**
 * 
 * The results of process execution.
 *
 * @author jiangzhao
 * @date Apr 14, 2016
 * @version V1.0
 */
public class ProcResult {
	
	private String procName;
	private InMemoryOutputStream stderr;
	private InMemoryOutputStream stdout;
	private int exitCode;
	private long startTime;
	private long endTime;
	private final static int MAX_OUTPUT_SIZE = 1 * 1024 * 1024;  // 1 MB
	
	
	public ProcResult(String procName) {
		this.procName = procName;
		this.stdout = new InMemoryOutputStream(true, MAX_OUTPUT_SIZE);
		this.stderr = new InMemoryOutputStream(true, MAX_OUTPUT_SIZE);
	}
	
	public String getProcName() {
		return procName;
	}
	public void setProcName(String procName) {
		this.procName = procName;
	}
	public InMemoryOutputStream getStderr() {
		return stderr;
	}
	public void setStderr(String errMsg) {
		this.stderr.write(errMsg.getBytes());
	}
	public InMemoryOutputStream getStdout() {
		return stdout;
	}
	public void setStdout(InMemoryOutputStream stdout) {
		this.stdout = stdout;
	}
	public int getExitCode() {
		return exitCode;
	}
	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	public long getEndTime() {
		return endTime;
	}
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
	
	public String getOutput() {
		return stdout.toString();
	}
	
	public String getError() {
		return stderr.toString();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(">ProcName: " + procName + "\n");
		sb.append(">ExitCode: " + exitCode + "\n");
		sb.append(">Stdout: " + getOutput() + "\n");
		sb.append(">Stderr: " + getError() + "\n");
		sb.append(">StartTime: " + new Date(startTime) + "\n");
		sb.append(">EndTime:" + new Date(endTime) + "\n");
		return sb.toString();
	}
}
