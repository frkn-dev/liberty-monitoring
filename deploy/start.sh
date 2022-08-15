#!/bin/bash
# The script for running Liberty Monitoring service
# SSH tunnel are needed for conneting to riemann server

JAR=${JAR:-target/uberjar/liberty-monitoring-0.1.0-SNAPSHOT-standalone.jar}

if ! nc -z localhost 5555 > /dev/null 2>&1; then
    echo "Riemann server port is not available"
    echo "Running SSH tunnel to riemann server"
    ssh -f -N -L 5555:localhost:5555 s.fuckrkn1.xyz
fi 

# Running main service
java -jar ${JAR} 