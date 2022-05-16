#!/bin/bash
#
# Copyright 2011-2022 GatlingCorp (http://gatling.io)
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

OLDDIR=$(pwd)
BIN_DIR=$(dirname "$0")
cd "${BIN_DIR}/.." && DEFAULT_GATLING_HOME=$(pwd) && cd "${OLDDIR}"

GATLING_HOME="${GATLING_HOME:=${DEFAULT_GATLING_HOME}}"

JAVA_OPTS="${JAVA_OPTS} -Xms32M -Xmx128M"

# Setup classpath
CLASSPATH="$GATLING_HOME/lib/*"

"$JAVA" $JAVA_OPTS -cp "$CLASSPATH" io.gatling.bundle.GatlingCLI "$@"
