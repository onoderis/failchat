#!/bin/sh
java -Xmx200m -Xms100m -XX:+UseG1GC -javaagent:java-agents/transparent-webview-patch.jar -jar failchat-${project.version}.jar > /dev/null 2>&1 &
