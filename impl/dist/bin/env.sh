#!/bin/bash

# setup the environment


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

export APPWHERE=$appwhere
export SCRIPTWHERE=$scriptwhere
export LIBWHERE=$libwhere
export JAVA_EXEC=$JAVA_EXEC
export JAVA_OPTS=$JAVA_OPTS
export CLASSPATH=$CLASSPATH
