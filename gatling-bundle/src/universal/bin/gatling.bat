@ECHO OFF
@REM
@REM Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM 		http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM

setlocal

set USER_ARGS=%*

rem set GATLING_HOME automatically if possible
set "OLD_DIR=%cd%"
cd ..
set "DEFAULT_GATLING_HOME=%cd%"
cd %OLD_DIR%

rem if gatling home is correctly set
if exist "%GATLING_HOME%\bin\gatling.bat" goto gotHome
rem if gatling home is not correctly set
if not "%GATLING_HOME%" == "" goto badHome
rem if not try current folder
if exist "%OLD_DIR%\bin\gatling.bat" set "GATLING_HOME=%OLD_DIR%" && goto gotHome
rem if not try parent folder
if exist "%DEFAULT_GATLING_HOME%\bin\gatling.bat" set "GATLING_HOME=%DEFAULT_GATLING_HOME%" && goto gotHome
rem else tell user to set GATLING_HOME
goto :noHome

:gotHome

if not defined GATLING_CONF set GATLING_CONF="%GATLING_HOME%"\conf

echo GATLING_HOME is set to "%GATLING_HOME%"

set JAVA_OPTS=-server -XX:+UseThreadPriorities -XX:ThreadPriorityPolicy=42 -Xms512M -Xmx512M -Xmn100M -XX:+HeapDumpOnOutOfMemoryError -XX:+AggressiveOpts -XX:+OptimizeStringConcat -XX:+UseFastAccessorMethods -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false %JAVA_OPTS%
set COMPILER_OPTS=-Xss10M %JAVA_OPTS%
rem Setup classpaths
set COMMON_CLASSPATH=%GATLING_CONF%;%JAVA_CLASSPATH%
set COMPILER_CLASSPATH="%GATLING_HOME%"\lib\zinc\*;%COMMON_CLASSPATH%
set GATLING_CLASSPATH="%GATLING_HOME%"\lib\*;"%GATLING_HOME%"\user-files;%COMMON_CLASSPATH%


rem Run the compiler
set COMPILATION_CLASSPATH=""
for %%i in ("%GATLING_HOME%\lib\*.jar") do call :addToPath "%%i"
java %COMPILER_OPTS% -cp %COMPILER_CLASSPATH% io.gatling.compiler.ZincCompiler -ccp %COMPILATION_CLASSPATH% %USER_ARGS%  2>NUL
rem Run Gatling
java %JAVA_OPTS% -cp %GATLING_CLASSPATH% io.gatling.app.Gatling %USER_ARGS%

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
if not defined NO_PAUSE pause
endlocal
exit /b 0

:addToPath
set COMPILATION_CLASSPATH=%1;%COMPILATION_CLASSPATH%
goto :EOF
