@REM
@REM Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM     http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM

java -cp $GATLING_HOME/lib/**.jar:$GATLING_HOME/lib/deps/**.jar -jar lib/gatling-app-${project.version}.jar
