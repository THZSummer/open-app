#!/bin/bash
cd /home/usb/wks/open-app/connector-api
mkdir -p logs
nohup java -jar target/connector-api-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev > logs/connector-api.log 2>&1 &
echo $! > logs/app.pid
echo "Connector API started with PID $(cat logs/app.pid)"
