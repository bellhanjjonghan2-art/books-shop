@echo off
setlocal enabledelayedexpansion

:: ===================== Deploy settings =====================
set "SSH_KEY=C:\aws-key\my-shop-key.pem"
set "SSH_USER=ec2-user"
set "SSH_HOST=54.180.106.150"
set "REMOTE_TMP_DIR=/tmp/shop-back-jar-deploy"
set "REMOTE_JAR_NAME=shop-back.jar"
set "GRADLEW=..\gradlew.bat"
set "REMOTE_DEPLOY_SCRIPT=remote_deploy.sh"
set "REMOTE_VERIFY_SCRIPT=remote_verify.sh"
:: =============================================================

echo ==============================
echo  shop-back deploy start
echo ==============================

:: 0. Move to shop-back root (folder containing this script)
cd /d "%~dp0"

if not exist "%SSH_KEY%" (
    echo [ERROR] SSH key file not found: %SSH_KEY%
    exit /b 1
)

if not exist "%GRADLEW%" (
    echo [ERROR] Gradle wrapper not found: %GRADLEW%
    exit /b 1
)

if not exist "%REMOTE_DEPLOY_SCRIPT%" (
    echo [ERROR] %REMOTE_DEPLOY_SCRIPT% not found next to this script
    exit /b 1
)

if not exist "%REMOTE_VERIFY_SCRIPT%" (
    echo [ERROR] %REMOTE_VERIFY_SCRIPT% not found next to this script
    exit /b 1
)

:: 1. Build boot jar
echo [1/5] Building boot jar (gradlew :shop-back:bootJar)
call "%GRADLEW%" :shop-back:bootJar
if errorlevel 1 (
    echo [ERROR] Build failed
    exit /b 1
)

set "LOCAL_JAR="
for %%F in ("build\libs\*.jar") do (
    echo %%~nxF | findstr /i /c:"-plain" >nul
    if errorlevel 1 set "LOCAL_JAR=%%F"
)

if not defined LOCAL_JAR (
    echo [ERROR] Could not find boot jar under build\libs
    exit /b 1
)
echo       Found jar: !LOCAL_JAR!

:: 2. Prepare remote temp directory
echo [2/5] Preparing remote temp directory
ssh -i "%SSH_KEY%" %SSH_USER%@%SSH_HOST% "rm -rf %REMOTE_TMP_DIR% && mkdir -p %REMOTE_TMP_DIR%"
if errorlevel 1 (
    echo [ERROR] Failed to prepare remote temp directory
    exit /b 1
)

:: 3. Upload jar
echo [3/5] Uploading jar
scp -i "%SSH_KEY%" "!LOCAL_JAR!" %SSH_USER%@%SSH_HOST%:%REMOTE_TMP_DIR%/%REMOTE_JAR_NAME%
if errorlevel 1 (
    echo [ERROR] File upload failed
    exit /b 1
)

:: 4. Stop existing server (if running) and start the new one.
::    The stop/start logic lives in remote_deploy.sh and is streamed to the
::    remote host over stdin, so there is no Windows cmd quoting/escaping
::    involved in the remote shell logic (avoids "!" / "$" mangling issues).
::    Runs as a login shell (-l) so profile-based PATH/JAVA_HOME apply to "java".
echo [4/5] Stopping old server (if any) and deploying new one
ssh -i "%SSH_KEY%" %SSH_USER%@%SSH_HOST% "sudo bash -l -s" < "%REMOTE_DEPLOY_SCRIPT%"
if errorlevel 1 (
    echo [ERROR] Remote deploy script failed
    exit /b 1
)

:: 5. Verify the process actually started
echo [5/5] Verifying server process
set "VERIFY_TMP=%TEMP%\shop_back_verify_status.txt"
ssh -i "%SSH_KEY%" %SSH_USER%@%SSH_HOST% "sudo bash -s" < "%REMOTE_VERIFY_SCRIPT%" > "%VERIFY_TMP%"
set "REMOTE_STATUS="
for /f "usebackq delims=" %%P in ("%VERIFY_TMP%") do set "REMOTE_STATUS=%%P"
del /q "%VERIFY_TMP%" >nul 2>&1

echo       Status: !REMOTE_STATUS!
if not defined REMOTE_STATUS (
    echo [WARN] Could not confirm the server process is running. Check manually on the server.
)

echo ==============================
echo  Deploy complete
echo ==============================

endlocal
exit /b 0
