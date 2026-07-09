#!/bin/bash
SERVICE_NAME="shop-back"

sleep 5

if systemctl is-active --quiet "$SERVICE_NAME"; then
    MAIN_PID=$(systemctl show -p MainPID --value "$SERVICE_NAME")
    echo "RUNNING:$MAIN_PID"
else
    echo "DEAD"
fi
