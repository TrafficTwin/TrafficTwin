package traffictwin.dsl

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DslResourcesTest {

    @Test
    fun `test_mesto dsl file exists`() {
        val resource = javaClass.classLoader.getResource("test_mesto.dsl")
        assertNotNull(resource, "test_mesto.dsl mora obstajati v src/main/resources")
    }

    @Test
    fun `bnf grammar file exists`() {
        val resource = javaClass.classLoader.getResource("grammar/TrafficTwin.bnf")
        assertNotNull(resource, "TrafficTwin.bnf mora obstajati v src/main/resources/grammar")
    }

    @Test
    fun `tokens regex file exists`() {
        val resource = javaClass.classLoader.getResource("grammar/tokens.regex")
        assertNotNull(resource, "tokens.regex mora obstajati v src/main/resources/grammar")
    }

    @Test
    fun `expected results file exists`() {
        val resource = javaClass.classLoader.getResource("test_expected.json")
        assertNotNull(resource, "test_expected.json mora obstajati v src/main/resources")
    }

    @Test
    fun `test_mesto contains ten tests`() {
        val text = javaClass.classLoader
            .getResource("test_mesto.dsl")
            ?.readText()

        assertNotNull(text)
        val count = Regex("// Test [0-9]+").findAll(text).count()

        assertTrue(count == 10, "test_mesto.dsl mora vsebovati točno 10 testov")
    }
}