#!/bin/bash
#
# Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# 		http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

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

JAVA_OPTS="-XX:+UseThreadPriorities -XX:ThreadPriorityPolicy=42 -Xms512M -Xmx512M -Xmn100M -Xss512k -XX:+HeapDumpOnOutOfMemoryError -XX:+AggressiveOpts -XX:+OptimizeStringConcat -XX:+UseFastAccessorMethods -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:SurvivorRatio=8 -XX:MaxTenuringThreshold=1 -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly"

CLASSPATH="$GATLING_HOME/lib/*:$GATLING_HOME/conf"

java $JAVA_OPTS -cp $CLASSPATH com.excilys.ebi.gatling.app.Gatling $@
