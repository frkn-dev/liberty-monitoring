#!/bin/bash

lein uberjar
for x in $(seq 1 5); do scp target/uberjar/liberty-monitoring-0.1.0-SNAPSHOT-standalone.jar lt$x:~/monitoring;done

