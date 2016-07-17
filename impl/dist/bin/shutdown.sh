# this script use kill command to shutdown the server
# in Java code it will handle this command properly.

PIDs=`ps -ef | grep JobServer | grep -v "grep" | awk '{print $2}'`
if [ -z "$PIDs" ]; then
    echo "JobServer is not running. "
else
    for PID in $PIDs
    do
        kill $PID
        tryTimes=10
        try=0
        while [ $try -lt $tryTimes ]
        do
            sleep 1s
            ps -p $PID > /dev/null
            if [ $? -ne 0 ]; then
                break
            fi
            try=$(($try+1))
        done
        ps -p $PID > /dev/null
        if [ $? -eq 0 ]; then
            echo "JobServer $PID wasn't shut down succesfully, please try later."
        else
            echo "JobServer $PID was shut down succesfully."
        fi
    done
fi
