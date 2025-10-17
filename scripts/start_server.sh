#!/bin/bash
cd /home/ec2-user
sudo fuser -k 8080/tcp || true
nohup java -jar yourappwordcounter.jar > app.log 2>&1 &
chmod +x scripts/start_server.sh
