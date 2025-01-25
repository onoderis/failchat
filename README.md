Failchat is a desktop application for streamers. It aggregates chat messages from multiple sources, shows you viewer
count, and more.   
Detailed description could be found [on the site](https://onoderis.github.io/failchat/).

### Before you run or build

1. Java 11 with bundled JavaFX is
   required. [Liberica full JDK 11.0.22+12](https://bell-sw.com/pages/downloads/?version=java-11&release=11.0.22%2B12)
   is
   recommended.


2. Create a file `src/main/resources/config/private.properties` with the following properties and replace the values:

```properties
twitch.bot-name=BOT_NAME
twitch.bot-password=BOT_PASSWORD (has prefix "oauth:")
twitch.client-id=API_TOKEN
twitch.client-secret=CLIENT_SECRET
```

3. In order to do `mvn package` you have to put desired JDK to `jdk/` directory. See goal `build-app-runtime` in pom.xml
   for additional info.

### How to run

```shell
./run.sh
```

### How to build a distributable archive

```shell
mvn package
```
