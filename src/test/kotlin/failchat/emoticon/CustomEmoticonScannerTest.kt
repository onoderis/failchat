package failchat.emoticon

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CustomEmoticonScannerTest {

    private companion object {
        val testDirPath: Path = Paths.get(CustomEmoticonScannerTest::class.java.getResource("/custom-emoticons").toURI())
        val customEmoticonScanner = CustomEmoticonScanner(testDirPath, "/")
    }

    private val scanResult = customEmoticonScanner.scan()

    @Test
    fun scanJpg(){
        assertNotNull(scanResult["1"])
    }

    @Test
    fun scanJpeg(){
        assertNotNull(scanResult["2"])
    }

    @Test
    fun scanPng(){
        assertNotNull(scanResult["3"])
    }

    @Test
    fun scanSvg(){
        assertNotNull(scanResult["4"])
    }

    @Test
    fun scanGif(){
        assertNotNull(scanResult["5"])
    }

    @Test
    fun ignoreUnknownFormat() {
        assertNull(scanResult["unknown-format"])
    }

    @Test
    fun ignoreUnsupportedFormat() {
        assertNull(scanResult["unsupported-format"])
    }

    @Test
    fun acceptMultipleDotsInName() {
        assertNotNull(scanResult["so.many.dots"])
    }

    @Test
    fun acceptUnderscoreInName() {
        assertNotNull(scanResult["_"])
    }

    @Test
    fun acceptMinusInName() {
        assertNotNull(scanResult["-"])
    }

    @Test
    fun ignoreCase() {
        assertNotNull(scanResult["upper_case"])
    }

}
