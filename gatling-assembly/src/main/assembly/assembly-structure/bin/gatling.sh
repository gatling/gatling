#!/bin/bash

if [ -n "${GATLING_HOME+x}" ]; then
  echo "GATLING_HOME already set to: $GATLING_HOME"
else
  if [ $(basename `pwd -P`) = "bin" ]; then
    export GATLING_HOME=$(dirname $(dirname $(dirname `pwd`/$0)))
  else
    export GATLING_HOME=$(cd $(dirname $(dirname $0)); pwd -P)
  fi
  echo "GATLING_HOME not set, using default location ($GATLING_HOME)"
fi


JAVA_OPTS="-XX:+UseThreadPriorities -XX:ThreadPriorityPolicy=42 -Xms2048M -Xmx2048M -Xmn100M -Xss512k -XX:+HeapDumpOnOutOfMemoryError -XX:+AggressiveOpts -XX:+OptimizeStringConcat -XX:+UseFastAccessorMethods -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:SurvivorRatio=8 -XX:MaxTenuringThreshold=1 -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly"

java $JAVA_OPTS -Dlogback.configurationFile=$GATLING_HOME/conf/logback.xml -cp $GATLING_HOME/lib/*:$GATLING_HOME/lib/deps/* com.excilys.ebi.gatling.app.App
