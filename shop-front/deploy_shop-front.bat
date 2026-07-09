@echo off
setlocal enabledelayedexpansion

:: ===================== Deploy settings =====================
set "SSH_KEY=C:\aws-key\my-shop-key.pem"
set "SSH_USER=ec2-user"
set "SSH_HOST=54.180.106.150"
set "REMOTE_DEPLOY_DIR=/usr/share/nginx/html/shop-front"
set "REMOTE_TMP_DIR=/tmp/shop-front-dist-deploy"
set "LOCAL_DIST_DIR=dist"
:: =============================================================

echo ==============================
echo  shop-front deploy start
echo ==============================

:: 0. Move to shop-front root (folder containing this script)
cd /d "%~dp0"

if not exist "%SSH_KEY%" (
    echo [ERROR] SSH key file not found: %SSH_KEY%
    exit /b 1
)

:: 1. Install dependencies if node_modules is missing
if not exist "node_modules" (
    echo [1/5] node_modules not found - running npm install
    call npm install
    if errorlevel 1 (
        echo [ERROR] npm install failed
        exit /b 1
    )
) else (
    echo [1/5] node_modules found - skipping install
)

:: 2. Production build
echo [2/5] Running production build (npm run build)
call npm run build
if errorlevel 1 (
    echo [ERROR] Build failed
    exit /b 1
)

if not exist "%LOCAL_DIST_DIR%" (
    echo [ERROR] Build output folder not found: %LOCAL_DIST_DIR%
    exit /b 1
)

:: 3. Clean remote temp directory before upload
echo [3/5] Cleaning remote temp directory
ssh -i "%SSH_KEY%" %SSH_USER%@%SSH_HOST% "rm -rf %REMOTE_TMP_DIR%"
if errorlevel 1 (
    echo [ERROR] SSH connection failed
    exit /b 1
)

:: 4. Upload build output (dist folder -> remote temp directory)
echo [4/5] Uploading build output
scp -i "%SSH_KEY%" -r "%LOCAL_DIST_DIR%" %SSH_USER%@%SSH_HOST%:%REMOTE_TMP_DIR%
if errorlevel 1 (
    echo [ERROR] File upload failed
    exit /b 1
)

:: 5. Apply to deploy path with sudo, then clean up temp files
echo [5/5] Applying to deploy path (sudo)
ssh -i "%SSH_KEY%" %SSH_USER%@%SSH_HOST% "sudo mkdir -p %REMOTE_DEPLOY_DIR% && sudo rm -rf %REMOTE_DEPLOY_DIR%/* && sudo cp -r %REMOTE_TMP_DIR%/* %REMOTE_DEPLOY_DIR%/ && sudo find %REMOTE_DEPLOY_DIR% -type d -exec chmod 755 {} \; && sudo find %REMOTE_DEPLOY_DIR% -type f -exec chmod 644 {} \; && sudo rm -rf %REMOTE_TMP_DIR%"
if errorlevel 1 (
    echo [ERROR] Deploy step failed
    exit /b 1
)

echo ==============================
echo  Deploy complete: %REMOTE_DEPLOY_DIR%
echo ==============================

endlocal
exit /b 0
