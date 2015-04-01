#!/bin/sh
#
# Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

USER_ARGS="$@"

OLDDIR=`pwd`
BIN_DIR=`dirname $0`
cd "${BIN_DIR}/.." && DEFAULT_GATLING_HOME=`pwd` && cd "${OLDDIR}"

GATLING_HOME="${GATLING_HOME:=${DEFAULT_GATLING_HOME}}"
GATLING_CONF="${GATLING_CONF:=$GATLING_HOME/conf}"

export GATLING_HOME GATLING_CONF

echo "GATLING_HOME is set to ${GATLING_HOME}"

JAVA_OPTS="-server -XX:+UseThreadPriorities -XX:ThreadPriorityPolicy=42 -Xms512M -Xmx512M -Xmn100M -XX:+HeapDumpOnOutOfMemoryError -XX:+AggressiveOpts -XX:+OptimizeStringConcat -XX:+UseFastAccessorMethods -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false ${JAVA_OPTS}"
COMPILER_OPTS="$JAVA_OPTS -Xss10M"

# Setup classpaths
COMMON_CLASSPATH="$GATLING_CONF:${JAVA_CLASSPATH}"
COMPILER_CLASSPATH="$GATLING_HOME/lib/zinc/*:$COMMON_CLASSPATH"
GATLING_CLASSPATH="$GATLING_HOME/lib/*:$GATLING_HOME/user-files:$COMMON_CLASSPATH"

# Build compilation classpath
COMPILATION_CLASSPATH=`find $GATLING_HOME/lib -maxdepth 1 -name "*.jar" -type f -exec printf :{} ';'`

# Run the compiler
java $COMPILER_OPTS -cp "$COMPILER_CLASSPATH" io.gatling.compiler.ZincCompiler -ccp "$COMPILATION_CLASSPATH" $USER_ARGS  2> /dev/null
# Run Gatling
java $JAVA_OPTS -cp "$GATLING_CLASSPATH" io.gatling.app.Gatling $USER_ARGS
