#!/bin/bash

# this script is used to submit the job to JobServer

scriptwhere=`dirname $0`
cd $scriptwhere
scriptwhere=`pwd`
cd -  1>/dev/null 2>&1
appwhere=`dirname $scriptwhere`
libwhere=$appwhere/lib
libs=`find $libwhere -name "*.jar" -print`
CLASSPATH=""
JAVA_EXEC=`which java`
JAVA_OPTS="-Xms20m -Xmx4g"


for jarFile in $libs
do
    CLASSPATH=$CLASSPATH\:$jarFile
done

exec $JAVA_EXEC $JAVA_OPTS -DloggingRoot=$appwhere/log -DAPP=JOBADMIN -cp $CLASSPATH\:$CLIENTCONFIG org.smartframework.jobhub.client.AdminTool "$@"