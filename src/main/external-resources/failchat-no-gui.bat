java -Xmx200m -Xms100m -XX:+UseG1GC -javaagent:webview-patch/webview-patch.jar -jar failchat-${project.version}.jar --no-gui --enable-console-logging
