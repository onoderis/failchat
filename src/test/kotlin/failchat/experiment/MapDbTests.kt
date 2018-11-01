package failchat.experiment

import failchat.Origin
import failchat.config
import failchat.okHttpClient
import failchat.privateConfig
import failchat.twitch.TwitchApiClient
import failchat.twitch.TwitchEmoticon
import failchat.twitch.TwitchEmoticonUrlFactory
import org.junit.After
import org.junit.Test
import org.mapdb.DBMaker
import org.mapdb.Serializer
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.measureTimeMillis


class MapDbTests {

    private val dbFile = Paths.get("mapdbtest.db")

    private val db = DBMaker
            .fileDB(dbFile.toFile())
            .concurrencyDisable()
            .closeOnJvmShutdown()
            .fileMmapEnable()
            .make()

    /*
    private val sink = db
//            .hashMap(Origin.TWITCH.commonName, Serializer.STRING, Serializer.ELSA as Serializer<Emoticon>)
//            .hashMap(Origin.TWITCH.commonName, Serializer.STRING, Serializer.JAVA as GroupSerializer<Emoticon>)
            .treeMap(Origin.TWITCH.commonName, Serializer.STRING, Serializer.JAVA as GroupSerializer<Emoticon>)
            .createFromSink()
//            .create()
*/
//    private val map = db
////            .hashMap(Origin.TWITCH.commonName, Serializer.STRING, Serializer.JAVA as GroupSerializer<Emoticon>)
//            .treeMap(Origin.TWITCH.commonName, Serializer.STRING, Serializer.JAVA as GroupSerializer<Emoticon>)
//            .create()

    private val twitchApiClient = TwitchApiClient(
            httpClient = okHttpClient,
            mainApiUrl = config.getString("twitch.api-url"),
            badgeApiUrl = config.getString("twitch.badge-api-url"),
            token = privateConfig.getString("twitch.api-token"),
            emoticonUrlFactory = TwitchEmoticonUrlFactory(
                    config.getString("twitch.emoticon-url-prefix"),
                    config.getString("twitch.emoticon-url-suffix")
            )
    )

    /*
    @Test
    fun some() {
        lateinit var emoticons: List<TwitchEmoticon>
        val et = measureTimeMillis {
            emoticons = twitchApiClient.requestEmoticons().join()
        }
        println("Emoticons was loaded in $et ms")

//        val st = measureTimeMillis {
//            emoticons = emoticons.sortedBy { it.twitchId.toString() }
//        }
//        println("Sorted in $st ms")


        val pt = measureTimeMillis {
            emoticons.forEach {
                map.put(it.twitchId.toString(), it)
            }
        }
//        val map = sink.create()
        println("Emoticons was put in db in $pt ms")

        println("Size: ${map.size}")


//        db.getStore().compact()
//        org.mapdb.DBException$VolumeIOError
//        Caused by: java.io.IOException: The requested operation cannot be performed on a file with a user-mapped section open

        println("Size of db file: ${Files.size(dbFile)}")

    }
*/

    @After
    fun closeDb() {
//        val c = measureTimeMillis {
//            db.getStore().compact()
//        }
//        println("compacted in $c ms")

        db.close()
    }

/*
    @Test
    fun sortedTableMapTest() {
        //create memory mapped volume
        val volume = MappedFileVol.FACTORY.makeVolume(dbFile.toString(), false)

        //open consumer which will feed map with content
        val sink = SortedTableMap.create(
                volume,
                Serializer.LONG,
                Serializer.JAVA as GroupSerializer<Emoticon>
        ).createFromSink()



        lateinit var emoticons: List<TwitchEmoticon>
        val et = measureTimeMillis {
            emoticons = twitchApiClient.requestEmoticons().join()
        }
        println("Emoticons was loaded in $et ms")


        val st = measureTimeMillis {
            emoticons = emoticons.sortedBy { it.twitchId }
        }
        println("Sorted in $st ms")


        val pt = measureTimeMillis {
            emoticons.forEach {
                sink.put(it.twitchId, it)
            }
            sink.create()
        }
        println("Emoticons was put in db in $pt ms")

        volume.close()

        println("Size of db file: ${Files.size(dbFile)}")
    }
*/

    @Test
    fun putJustIds() {
        val map = db
                .hashMap(Origin.TWITCH.commonName, Serializer.JAVA, Serializer.JAVA)
                .create()


        lateinit var emoticons: List<TwitchEmoticon>
        val et = measureTimeMillis {
            emoticons = twitchApiClient.requestEmoticons().join()
        }
        println("Emoticons was loaded in $et ms")

        val pt = measureTimeMillis {
            emoticons.forEach {
                map.put(it.code, it.twitchId)
            }
        }
        println("Emoticons was put in db in $pt ms")

        println("Size: ${map.size}")

        println("Size of db file: ${Files.size(dbFile)}")

    }
}


/*
=== Results: ===

Baseline:
'emoticons\twitch.ser' serialized in 14612 ms
size: 30_040_877

HTreeMap DB:
Emoticons was put in db in 65317 ms
size: 416 MB

BTreeMap, sink:
Sorted in 177 ms
Emoticons was put in db in 61747 ms
Size: 687914
Size of db file: 383778816

BTreeMap, sink, concurrency disabled:
Emoticons was put in db in 59598 ms
Size: 687958
Size of db file: 383778816

SortedTableMap, concurrency disabled, sink:
Emoticons was put in db in 57758 ms
Size of db file: 365M

SortedTableMap, concurrency disabled, sink, no urls:
Emoticons was put in db in 43492 ms
Size of db file: 242221056

SortedTableMap, concurrency disabled, sink, no urls, Long key:
Emoticons was put in db in 46966 ms
Size of db file: 247463936

--- Java serializer ---

HTreeMap, concurrency disabled, no urls, String key
Emoticons was put in db in 8980 ms
Size of db file: 295698432

BTreeMap, concurrency disabled, no urls, String key
Emoticons was put in db in 58165 ms
Size of db file: 159383552

SortedTableMap, concurrency disabled, sink, no urls, Long key:
Emoticons was put in db in 1097 ms
Size of db file: 39845888

--- Just ids ---

HTreeMap, mapdb serializers (String to Long)
Emoticons was put in db in 5050 ms
Size of db file: 74_448896

HTreeMap, java serializers
Emoticons was put in db in 6625 ms
Size of db file: 141_557760


* */
