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

rem set GATLING_HOME automatically if possible
set "CURRENT_DIR=%cd%"
rem if gatling home is correctly set
if exist "%GATLING_HOME%\bin\gatling.bat" goto gotHome
rem if gatling home is not correctly set
if not "%GATLING_HOME%" == "" goto badHome
rem if not try current folder
if exist "%CURRENT_DIR%\bin\gatling.bat" set "GATLING_HOME=%cd%" && goto gotHome
rem if not try parent folder
cd ..
set "CURRENT_DIR=%cd%"
if exist "%CURRENT_DIR%\bin\gatling.bat" set "GATLING_HOME=%cd%" && goto gotHome
rem else tell user to set GATLING_HOME
goto :noHome

:gotHome
set JAVA_OPTS=-XX:+UseThreadPriorities -XX:ThreadPriorityPolicy=42 -Xms512M -Xmx512M -Xmn100M -Xss512k -XX:+HeapDumpOnOutOfMemoryError -XX:+AggressiveOpts -XX:+OptimizeStringConcat -XX:+UseFastAccessorMethods -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:SurvivorRatio=8 -XX:MaxTenuringThreshold=1 -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly
set CLASSPATH=%GATLING_HOME%\lib\*;
set JAVA_PROPS=-Dlogback.configurationFile=%GATLING_HOME%\conf\logback.xml
set COMMAND=-cp %CLASSPATH% com.excilys.ebi.gatling.app.Gatling

java %JAVA_OPTS% %COMMAND% %1 %2 %3 %4 %5 %6 %7 %8 %9

goto exit


:badHome
echo The GATLING_HOME environnement variable points to the wrong directory.
echo Please set it to the correct folder and try to launch Gatling again.
goto exit

:noHome
echo GATLING_HOME environnement variable is not set and could not be guessed automatically.
echo Please set GATLING_HOME and try to launch Gatling again.
goto exit

:exit
pause
exit /b 0
