@ECHO OFF
@REM
@REM Copyright 2011-2021 GatlingCorp (http://gatling.io)
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

set "GATLING_ENTERPRISE_CLOUD_DOMAIN=https://cloud.gatling.io"

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

if NOT [%GATLING_ENTERPRISE_API_TOKEN%] == [] (
    set API_TOKEN = %GATLING_ENTERPRISE_API_TOKEN%
)

if "%1" == "--apiToken" (
    set API_TOKEN=%2
    if "%3" == "--packageId" (
        set PACKAGE_ID=%4
    )
) else if "%1" == "--packageId" (
        set PACKAGE_ID=%2
        if "%3" == "--apiToken" (
            set API_TOKEN=%4
        )
    )
)

if [%API_TOKEN%] == [] goto usage
if [%PACKAGE_ID%] == [] goto usage

rem Check if curl is installed
WHERE curl >nul 2>nul
IF %ERRORLEVEL% NEQ 0 (
    ECHO curl is required to run this script
    set ERROR_CODE=1
    goto exitOnError
)

rem Create jar package
call enterprisePackage.bat < nul
if %errorlevel% neq 0 (
  set ERROR_CODE=%errorlevel%
  goto exitOnError
)
rem Create jar package done

rem Upload the package
for /F "delims=" %%x in ('
    curl --request PUT --upload-file "%GATLING_HOME%/target/package.jar" ^
    "%GATLING_ENTERPRISE_CLOUD_DOMAIN%/api/public/artifacts/%PACKAGE_ID%/content?filename=package.jar" ^
    --header "Authorization:%API_TOKEN%" ^
    --write-out "%%{http_code}" ^
    --silent
') do (
    for /F "tokens=1,2 delims=}" %%a in ("%%x") do (
        if "%%b" == "200" (
            echo Package successfully uploaded to Gatling Enterprise with id %PACKAGE_ID%
            goto exit
        ) else (
            echo Upload failed
            echo error: %%a
            set ERROR_CODE=1
            goto exitOnError
        )
    )
)

:usage
echo usage: "enterpriseDeploy.bat --packageId <packageId> [--apiToken <apiToken>]"
echo Options:
echo   --packageId string  Specify a packageId, Retrieve your package id here: ${GATLING_ENTERPRISE_CLOUD_DOMAIN}/#/admin/artifacts
echo   --apiToken string   Specify an apiToken, create an API token with 'Packages' permission here: ${GATLING_ENTERPRISE_CLOUD_DOMAIN}/#/api-tokens
echo                       Can also be set with the environment variable GATLING_ENTERPRISE_API_TOKEN
goto exit

:badHome
echo The GATLING_HOME environment variable points to the wrong directory.
echo Please set it to the correct folder and try to launch Gatling again.
goto exit

:noHome
echo GATLING_HOME environment variable is not set and could not be guessed automatically.
echo Please set GATLING_HOME and try to launch Gatling again.
goto exit

:exit
if not defined NO_PAUSE pause
endlocal
exit /b 0

:exitOnError
if exist "%GATLING_HOME%"\target\package.jar (
  del /f "%GATLING_HOME%"\target\package.jar
)
if not defined NO_PAUSE pause
exit /b %ERROR_CODE%
