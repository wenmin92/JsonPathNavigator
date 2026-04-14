package cc.wenmin92.jsonkeyfinder.compatibility

import cc.wenmin92.jsonkeyfinder.JsonPathTestBase
import org.junit.Assert.*

class SimpleVersionTest : JsonPathTestBase() {

    fun `test basic functionality`() {
        createSimpleJsonFile("test.json", "name", "value")
        val results = searchService.findKey("name")
        assertEquals(1, results.size)
    }
}