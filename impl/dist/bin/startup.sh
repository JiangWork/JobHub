#!/bin/bash

# this scrip will start the job server
# Note: the terminal will hang up while starting the server.

scriptwhere=`dirname $0`

source $scriptwhere/env.sh

pid=$$
SERVERCONFIG=$appwhere/conf/server
exec $JAVA_EXEC $JAVA_OPTS -DloggingRoot=$appwhere/log -DAPP=JOBSERVICE -DPID=$pid -DAPPLOCATION=$appwhere -cp $CLASSPATH\:$SERVERCONFIG org.smartframework.jobhub.server.JobServer