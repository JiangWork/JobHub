# JobHub
A simple implementation of JobService which user-submitted jobs can be runned in this service. 
User can submit Java program packaged as jar file to this service, watch the progress of this job and get the result.
Also we can query job status or kill the jobs.

Folder <b>impl</b> contains the implementation of JobService functionalities, utilities to interact with JobService, etc.
Folder <b>example-job</b> contains a simple job for demonstrating the pipeline of job execution.

To start up the server, download jobhub.zip to you computer, unzip it and modify the jobhub.workdir in conf/server/server.properties to you own directory. And then start the server using bin/startup.sh.

To submit the job, use bin/submit.sh, here is a example:

sh ../jobhub/bin/submit.sh \
    -Djob.name=DemoRun-Xml \
    -Djob.mainclass=org.smartframework.jobhub.example.xmlparser.DemoRunner \
    -Djob.method.name=parse \
    -Djob.method.args=flights.xml,flights-mapping.xml\
    -Djob.jars=job-example.jar,lib/castor-core-1.3.3.jar,lib/castor-xml-1.3.3.jar,lib/commons-lang-2.6.jar,lib/commons-logging.jar,lib/log4j-1.2.14.jar \
    -Djob.resources=resources/flights.xml,resources/flights-mapping.xml \
    -Djob.timeout=100000 \
    -Djob.submitter=Jiang
    
    
To monitor the job status, memory usage or kill a job, use bin/admin.sh.


