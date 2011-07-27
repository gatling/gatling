#!/bin/bash

if [ -n "${GATLING_HOME+x}" ]; then
  echo "GATLING_HOME already set to: $GATLING_HOME"
else
  GATLING_HOME=`pwd`
  echo "GATLING_HOME not set, using default location"
fi

java -cp $GATLING_HOME/lib/*:$GATLING_HOME/lib/deps/* com.excilys.ebi.gatling.app.App
