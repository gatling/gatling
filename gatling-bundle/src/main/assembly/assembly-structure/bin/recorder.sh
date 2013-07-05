#!/bin/bash
#
# Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

OLDDIR=`pwd`
BIN_DIR=`dirname $0`
cd ${BIN_DIR}/.. && DEFAULT_GATLING_HOME=`pwd` && cd ${OLDDIR}

GATLING_HOME=${GATLING_HOME:=${DEFAULT_GATLING_HOME}}
GATLING_CONF=${GATLING_CONF:=$GATLING_HOME/conf}

export GATLING_HOME GATLING_CONF

echo "GATLING_HOME is set to ${GATLING_HOME}"

JAVA_OPTS="-Xms512M -Xmx512M -Xmn100M ${JAVA_OPTS}"

CLASSPATH="$GATLING_HOME/lib/*:$GATLING_CONF:${JAVA_CLASSPATH}"

java $JAVA_OPTS -cp $CLASSPATH io.gatling.recorder.GatlingRecorder $@
