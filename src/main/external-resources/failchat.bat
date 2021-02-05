start ./runtime/bin/javaw -Xmx200m -Xms100m -XX:+UseG1GC -javaagent:java-agents/transparent-webview-patch.jar -jar failchat-${project.version}.jar
