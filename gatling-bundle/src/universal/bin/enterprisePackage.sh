#!/bin/sh
#
# Copyright 2011-2017 GatlingCorp (http://gatling.io)
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
    JAR="$JAVA_HOME"/bin/jar
else
    JAVA=java
    JAR=jar
fi

OLDDIR=`pwd`
BIN_DIR=`dirname $0`
cd "${BIN_DIR}/.." && DEFAULT_GATLING_HOME=`pwd` && cd "${OLDDIR}"

GATLING_HOME="${GATLING_HOME:=${DEFAULT_GATLING_HOME}}"
GATLING_CONF="${GATLING_CONF:=$GATLING_HOME/conf}"

export GATLING_HOME GATLING_CONF

echo "GATLING_HOME is set to ${GATLING_HOME}"

DEFAULT_JAVA_OPTS="-server"
DEFAULT_JAVA_OPTS="${DEFAULT_JAVA_OPTS} -Xmx1G -XX:+HeapDumpOnOutOfMemoryError"
DEFAULT_JAVA_OPTS="${DEFAULT_JAVA_OPTS} -XX:+UseG1GC -XX:+ParallelRefProcEnabled"
DEFAULT_JAVA_OPTS="${DEFAULT_JAVA_OPTS} -XX:MaxInlineLevel=20 -XX:MaxTrivialSize=12 -XX:-UseBiasedLocking"
COMPILER_OPTS="-Xss100M $DEFAULT_JAVA_OPTS $JAVA_OPTS"

# Setup classpaths
COMPILER_CLASSPATH="$GATLING_HOME/lib/*:$GATLING_CONF:"


# Use the extra compiler options flag only if they are provided
if [ -n "$EXTRA_SCALAC_OPTIONS" ]; then
    EXTRA_COMPILER_OPTIONS="-eso $EXTRA_SCALAC_OPTIONS"
fi

# Run the compiler
"$JAVA" $COMPILER_OPTS -cp "$COMPILER_CLASSPATH" io.gatling.compiler.ZincCompiler $EXTRA_COMPILER_OPTIONS "$@" 2> /dev/null

GATLING_VERSION="$(ls "${GATLING_HOME}"/lib/gatling-app-*.jar | rev | cut -d'-' -f'1' | cut -d'.' -f'2-' | rev)"

echo "GATLING_VERSION is set to '$GATLING_VERSION'"

MANIFEST_FILE="$(mktemp)"
cat <<EOT >> "${MANIFEST_FILE}"
Gatling-Version: ${GATLING_VERSION}
Gatling-Packager: bundle
EOT

# Create the jar
echo -n "Creating jar..."
"$JAR" cfm "${GATLING_HOME}/target/artifact.jar" "${MANIFEST_FILE}" \
  -C "${GATLING_HOME}/target/test-classes" . \
  -C "${GATLING_HOME}/user-files/resources" .
echo "done"

rm "${MANIFEST_FILE}"
