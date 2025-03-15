Failchat is a desktop application for streamers. It aggregates chat messages from multiple sources, shows you viewer
count, and more.   
Detailed description could be found [on the site](https://onoderis.github.io/failchat/).

### How to build and run

1. Install Java 11 with bundled
   JavaFX. [Liberica full JDK 11.0.22+12](https://bell-sw.com/pages/downloads/?version=java-11&release=11.0.22%2B12)
   is
   recommended.


2. Create a file `src/main/resources/config/private.properties` with the following properties and replace the values:

```properties
twitch.bot-name=BOT_NAME
twitch.bot-password=BOT_PASSWORD (has prefix "oauth:")
twitch.client-id=API_TOKEN
twitch.client-secret=CLIENT_SECRET
```

3. Run command

```
mvn compile org.codehaus.mojo:exec-maven-plugin:exec@run-app
```

### How to create distributable archives

1. Complete all the steps in "How to run" section.
2. Place windows and linux JDKs in `jdk/` directory. Look at executions `build-windows-runtime` and
   `build-linux-runtime`  in pom.xml
   for additional info.
3. Run command

```
mvn package
```
