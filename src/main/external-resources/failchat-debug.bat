start javaw -Xmx200m -Xms100m -XX:+UseG1GC -javaagent:webview-patch/webview-patch.jar -jar failchat-${project.version}.jar --logger-failchat-level DEBUG
