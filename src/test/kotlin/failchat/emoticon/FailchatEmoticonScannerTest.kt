package failchat.emoticon

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FailchatEmoticonScannerTest {

    private companion object {
        val testDirPath: Path = Paths.get(FailchatEmoticonScannerTest::class.java.getResource("/failchat-emoticons").toURI())
        val failchatEmoticonScanner = FailchatEmoticonScanner(testDirPath, "/")
    }

    private val scanResult = failchatEmoticonScanner.scan()

    @Test
    fun scanJpg(){
        assertNotNull(scanResult.find { it.code == "1" })
    }

    @Test
    fun scanJpeg(){
        assertNotNull(scanResult.find { it.code == "2" })
    }

    @Test
    fun scanPng(){
        assertNotNull(scanResult.find { it.code == "3" })
    }

    @Test
    fun scanSvg(){
        assertNotNull(scanResult.find { it.code == "4" })
    }

    @Test
    fun scanGif(){
        assertNotNull(scanResult.find { it.code == "5" })
    }

    @Test
    fun ignoreUnknownFormat() {
        assertNull(scanResult.find { it.code == "unknown-format" })
    }

    @Test
    fun ignoreUnsupportedFormat() {
        assertNull(scanResult.find { it.code == "unsupported-format" })
    }

    @Test
    fun acceptMultipleDotsInName() {
        assertNotNull(scanResult.find { it.code == "so.many.dots" })
    }

    @Test
    fun acceptUnderscoreInName() {
        assertNotNull(scanResult.find { it.code == "underscore_" })
    }

    @Test
    fun acceptMinusInName() {
        assertNotNull(scanResult.find { it.code == "minus-" })
    }

    @Test
    fun ignoreCase() {
        assertNotNull(scanResult.find { it.code == "uppercase" })
    }

}
