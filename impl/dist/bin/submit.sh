#!/bin/bash

# this script is used to submit the job to JobServer

scriptwhere=`dirname $0`

source $scriptwhere/env.sh

CLIENTCONFIG=$appwhere/conf/client

exec $JAVA_EXEC $JAVA_OPTS -DloggingRoot=$appwhere/log -DAPP=JOBCLIENT -cp $CLASSPATH\:$CLIENTCONFIG org.smartframework.jobhub.client.JobSubmitter "$@"