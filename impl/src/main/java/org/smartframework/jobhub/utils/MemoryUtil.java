package org.smartframework.jobhub.utils;

public class MemoryUtil {
	
	/**
	 * Get the memory occupation by process pid, including all sub-processes.
	 * 
	 * @param pid the pid to check.
	 * @return the memory usage in bytes.
	 * @throws Exception
	 */
	public static long usage(String pid) throws Exception {
		if (!ProcfsBasedProcessTree.isAvailable()) {
			throw new Exception("Only support Linux system.");
		}
		ProcfsBasedProcessTree tree = new ProcfsBasedProcessTree(pid);
		tree.getProcessTree();
		return tree.getCumulativeRssmem();
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println(usage("17844"));
	}
}
