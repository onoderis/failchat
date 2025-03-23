package failchat.gui

import failchat.skin.Skin
import java.nio.file.Paths
import kotlin.test.assertEquals
import org.junit.Test

class SkinConverterTest {
    private val skinOne = Skin("one", Paths.get("/skins/one.html"))

    private val skinConverter =
        SkinConverter(listOf(skinOne, Skin("two", Paths.get("/skins/two.html"))))

    @Test
    fun defaultSkinTest() {
        val s = skinConverter.fromString("three")

        assertEquals(skinOne, s)
    }
}
