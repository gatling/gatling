#!/bin/sh
#
# Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
if [ -n "$JAVA_HOME" ]; then
    JAVA="$JAVA_HOME"/bin/java
else
    JAVA=java
fi

OLDDIR=`pwd`
BIN_DIR=`dirname $0`
cd "${BIN_DIR}/.." && DEFAULT_GATLING_HOME=`pwd` && cd "${OLDDIR}"

GATLING_HOME="${GATLING_HOME:=${DEFAULT_GATLING_HOME}}"
GATLING_CONF="${GATLING_CONF:=$GATLING_HOME/conf}"

export GATLING_HOME GATLING_CONF

echo "GATLING_HOME is set to ${GATLING_HOME}"

DEFAULT_JAVA_OPTS="-server"
DEFAULT_JAVA_OPTS="${DEFAULT_JAVA_OPTS} -Xmx1G"
DEFAULT_JAVA_OPTS="${DEFAULT_JAVA_OPTS} -XX:+UseG1GC -XX:MaxGCPauseMillis=30 -XX:G1HeapRegionSize=16m -XX:InitiatingHeapOccupancyPercent=75 -XX:+ParallelRefProcEnabled"
DEFAULT_JAVA_OPTS="${DEFAULT_JAVA_OPTS} -XX:+PerfDisableSharedMem -XX:+AggressiveOpts -XX:+OptimizeStringConcat"
DEFAULT_JAVA_OPTS="${DEFAULT_JAVA_OPTS} -XX:+HeapDumpOnOutOfMemoryError"
DEFAULT_JAVA_OPTS="${DEFAULT_JAVA_OPTS} -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false"
COMPILER_OPTS="-Xss100M $DEFAULT_JAVA_OPTS $JAVA_OPTS"

# Setup classpaths
COMMON_CLASSPATH="$GATLING_CONF:${JAVA_CLASSPATH}"
COMPILER_CLASSPATH="$GATLING_HOME/lib/zinc/*:$COMMON_CLASSPATH"
GATLING_CLASSPATH="$GATLING_HOME/lib/*:$GATLING_HOME/user-files:$COMMON_CLASSPATH"

# Build compilation classpath
COMPILATION_CLASSPATH=`find "$GATLING_HOME/lib" -maxdepth 1 -name "*.jar" -type f -exec printf :{} ';'`

# Run the compiler
"$JAVA" $COMPILER_OPTS -cp "$COMPILER_CLASSPATH" io.gatling.compiler.ZincCompiler -ccp "$COMPILATION_CLASSPATH" "$@"  2> /dev/null
# Run Gatling
"$JAVA" $DEFAULT_JAVA_OPTS $JAVA_OPTS -cp "$GATLING_CLASSPATH" io.gatling.app.Gatling "$@"
