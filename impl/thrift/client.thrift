/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


/**
 * The first thing to know about are types. The available types in Thrift are:
 *
 *  bool        Boolean, one byte
 *  byte        Signed byte
 *  i16         Signed 16-bit integer
 *  i32         Signed 32-bit integer
 *  i64         Signed 64-bit integer
 *  double      64-bit floating point value
 *  string      String
 *  binary      Blob (byte array)
 *  map<t1,t2>  Map from one type to another
 *  list<t1>    Ordered list of one type
 *  set<t1>     Set of unique elements of one type
 *
 */
include "common.thrift"

namespace java org.smartframework.jobhub.protocol

struct JobStatus {
  1: common.JobState state,    // the job state 
  2: bool success,      // if the job is success or not
  3: string reason,     // the reason of job failure
  4: i32 progress,   // the progress of the job, 30%
  5: i64 startTime,      // the start time
}

struct MemoryStatus {
  1: string name,
  2: i64 jobId,
  3: i64 usedByte,
  4: i64 lastUpdated,
}

/**
* Protocol between server and client.
*/
service ClientProtocol {
    
    /**
    * Get a new job id.
    */
    i64 newJobId(),

    /**
    * Submit a job to server, configData contains the configuration information.
    */
    common.ActionResult submit(1:i64 jobId, 2:binary configData),

    

    
// Following functions are for management

    /**
    * Query the job status.
    */
    JobStatus query(1:i64 jobId),

    /**
    * kill the job by jobId.
    **/
    common.ActionResult kill(1:i64 jobId),

    /***
    * query for the memory usage for jobId
    **/
    MemoryStatus memory(1:i64 jobId),

    /**
    * Query for job id in certain JobState
    */
    list<i64> getJobsFor(1: common.JobState state),
}

