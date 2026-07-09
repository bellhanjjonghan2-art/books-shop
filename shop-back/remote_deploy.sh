#!/bin/bash
set -e

DEPLOY_DIR="/usr/local/projects"
JAR_NAME="shop-back.jar"
TMP_DIR="/tmp/shop-back-jar-deploy"
SERVICE_NAME="shop-back"

# Stop the service before swapping the jar
systemctl stop "$SERVICE_NAME" || true

# Move newly uploaded jar into place
mkdir -p "$DEPLOY_DIR"
mv "$TMP_DIR/$JAR_NAME" "$DEPLOY_DIR/$JAR_NAME"
chmod 644 "$DEPLOY_DIR/$JAR_NAME"
rm -rf "$TMP_DIR"

# Start via systemd (auto-restarts on crash and on instance reboot)
systemctl start "$SERVICE_NAME"

echo "STARTED_SERVICE:$SERVICE_NAME"
