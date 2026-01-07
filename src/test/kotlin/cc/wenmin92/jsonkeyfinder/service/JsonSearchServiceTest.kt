package cc.wenmin92.jsonkeyfinder.service

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for JsonSearchService.
 * Uses IntelliJ Platform test framework to test JSON search functionality.
 */
class JsonSearchServiceTest : BasePlatformTestCase() {

    private lateinit var searchService: JsonSearchService

    override fun getTestDataPath(): String {
        return "src/test/resources/testData"
    }

    override fun setUp() {
        super.setUp()
        searchService = JsonSearchService(project)
    }

    // ==================== findKey Tests ====================

    fun `test findKey with simple key`() {
        // Create a test JSON file
        val jsonContent = """
            {
                "name": "test",
                "value": 123
            }
        """.trimIndent()
        
        createJsonFile("simple.json", jsonContent)
        
        // Search for the key
        val results = searchService.findKey("name")
        
        assertEquals(1, results.size)
        assertEquals("name", results[0].path)
        assertTrue(results[0].preview.contains("test"))
    }

    fun `test findKey with nested key path`() {
        val jsonContent = """
            {
                "root": {
                    "child": {
                        "property": "value"
                    }
                }
            }
        """.trimIndent()
        
        createJsonFile("nested.json", jsonContent)
        
        val results = searchService.findKey("root.child.property")
        
        assertEquals(1, results.size)
        assertEquals("root.child.property", results[0].path)
        assertTrue(results[0].preview.contains("value"))
    }

    fun `test findKey with deeply nested structure`() {
        val jsonContent = """
            {
                "level1": {
                    "level2": {
                        "level3": {
                            "level4": {
                                "deepValue": "found"
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        
        createJsonFile("deep.json", jsonContent)
        
        val results = searchService.findKey("level1.level2.level3.level4.deepValue")
        
        assertEquals(1, results.size)
        assertEquals("level1.level2.level3.level4.deepValue", results[0].path)
    }

    fun `test findKey returns empty for non-existent key`() {
        val jsonContent = """
            {
                "existing": "value"
            }
        """.trimIndent()
        
        createJsonFile("exists.json", jsonContent)
        
        val results = searchService.findKey("nonexistent")
        
        assertTrue(results.isEmpty())
    }

    fun `test findKey with empty search text`() {
        val jsonContent = """
            {
                "key": "value"
            }
        """.trimIndent()
        
        createJsonFile("test.json", jsonContent)
        
        val results = searchService.findKey("")
        
        assertTrue(results.isEmpty())
    }

    fun `test findKey searches across multiple files`() {
        val jsonContent1 = """
            {
                "shared": {
                    "value1": "first"
                }
            }
        """.trimIndent()
        
        val jsonContent2 = """
            {
                "shared": {
                    "value2": "second"
                }
            }
        """.trimIndent()
        
        createJsonFile("file1.json", jsonContent1)
        createJsonFile("file2.json", jsonContent2)
        
        val results1 = searchService.findKey("shared.value1")
        val results2 = searchService.findKey("shared.value2")
        
        assertEquals(1, results1.size)
        assertEquals(1, results2.size)
    }

    fun `test findKey with object value shows preview correctly`() {
        val jsonContent = """
            {
                "parent": {
                    "child": {
                        "nested": "value"
                    }
                }
            }
        """.trimIndent()
        
        createJsonFile("object.json", jsonContent)
        
        val results = searchService.findKey("parent.child")
        
        assertEquals(1, results.size)
        assertTrue(results[0].preview.contains("{ ... }"))
    }

    fun `test findKey with string value shows value in preview`() {
        val jsonContent = """
            {
                "message": "Hello World"
            }
        """.trimIndent()
        
        createJsonFile("string.json", jsonContent)
        
        val results = searchService.findKey("message")
        
        assertEquals(1, results.size)
        assertTrue(results[0].preview.contains("Hello World"))
    }

    fun `test findKey with numeric value`() {
        val jsonContent = """
            {
                "count": 42
            }
        """.trimIndent()
        
        createJsonFile("number.json", jsonContent)
        
        val results = searchService.findKey("count")
        
        assertEquals(1, results.size)
        assertTrue(results[0].preview.contains("42"))
    }

    fun `test findKey with boolean value`() {
        val jsonContent = """
            {
                "enabled": true
            }
        """.trimIndent()
        
        createJsonFile("boolean.json", jsonContent)
        
        val results = searchService.findKey("enabled")
        
        assertEquals(1, results.size)
        assertTrue(results[0].preview.contains("true"))
    }

    fun `test findKey with null value`() {
        val jsonContent = """
            {
                "nullable": null
            }
        """.trimIndent()
        
        createJsonFile("null.json", jsonContent)
        
        val results = searchService.findKey("nullable")
        
        assertEquals(1, results.size)
        assertTrue(results[0].preview.contains("null"))
    }

    fun `test findKey with array value`() {
        val jsonContent = """
            {
                "items": [1, 2, 3]
            }
        """.trimIndent()
        
        createJsonFile("array.json", jsonContent)
        
        val results = searchService.findKey("items")
        
        assertEquals(1, results.size)
    }

    fun `test findKey returns correct line number`() {
        val jsonContent = """
            {
                "first": "value1",
                "second": "value2",
                "third": "value3"
            }
        """.trimIndent()
        
        createJsonFile("lines.json", jsonContent)
        
        val results = searchService.findKey("third")
        
        assertEquals(1, results.size)
        assertEquals(4, results[0].lineNumber) // "third" is on line 4
    }

    fun `test findKey with partial path match does not return results`() {
        val jsonContent = """
            {
                "config": {
                    "settings": {
                        "option": "value"
                    }
                }
            }
        """.trimIndent()
        
        createJsonFile("partial.json", jsonContent)
        
        // Partial path should not match
        val results = searchService.findKey("config.settings.option.nonexistent")
        
        assertTrue(results.isEmpty())
    }

    // ==================== findKeyWithTiming Tests ====================

    fun `test findKeyWithTiming returns timing information`() {
        val jsonContent = """
            {
                "timing": {
                    "test": "value"
                }
            }
        """.trimIndent()
        
        createJsonFile("timing.json", jsonContent)
        
        val resultWithTiming = searchService.findKeyWithTiming("timing.test")
        
        assertEquals(1, resultWithTiming.results.size)
        assertTrue("Elapsed time should be non-negative", resultWithTiming.elapsedTimeMs >= 0)
    }

    fun `test findKeyWithTiming with no results has timing`() {
        val jsonContent = """
            {
                "existing": "value"
            }
        """.trimIndent()
        
        createJsonFile("no-match.json", jsonContent)
        
        val resultWithTiming = searchService.findKeyWithTiming("nonexistent.key")
        
        assertTrue(resultWithTiming.results.isEmpty())
        assertTrue("Elapsed time should be non-negative", resultWithTiming.elapsedTimeMs >= 0)
    }

    fun `test findKeyWithTiming with empty search text`() {
        val jsonContent = """
            {
                "key": "value"
            }
        """.trimIndent()
        
        createJsonFile("empty-search.json", jsonContent)
        
        val resultWithTiming = searchService.findKeyWithTiming("")
        
        assertTrue(resultWithTiming.results.isEmpty())
        assertTrue("Elapsed time should be non-negative", resultWithTiming.elapsedTimeMs >= 0)
    }

    fun `test findKey and findKeyWithTiming return same results`() {
        val jsonContent = """
            {
                "consistency": {
                    "check": "value"
                }
            }
        """.trimIndent()
        
        createJsonFile("consistency.json", jsonContent)
        
        val results = searchService.findKey("consistency.check")
        val resultWithTiming = searchService.findKeyWithTiming("consistency.check")
        
        assertEquals(results.size, resultWithTiming.results.size)
        if (results.isNotEmpty()) {
            assertEquals(results[0].path, resultWithTiming.results[0].path)
            assertEquals(results[0].file, resultWithTiming.results[0].file)
        }
    }

    // ==================== getSuggestions Tests ====================

    fun `test getSuggestions with short input returns empty`() {
        val jsonContent = """
            {
                "test": "value"
            }
        """.trimIndent()
        
        createJsonFile("suggestions.json", jsonContent)
        
        // Input less than 2 characters should return empty
        val suggestions = searchService.getSuggestions("t")
        
        assertTrue(suggestions.isEmpty())
    }

    fun `test getSuggestions with valid prefix`() {
        val jsonContent = """
            {
                "config": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
        """.trimIndent()
        
        createJsonFile("config.json", jsonContent)
        searchService.invalidateCache()
        
        // First trigger a search to populate the cache
        searchService.findKey("config")
        
        val suggestions = searchService.getSuggestions("config.")
        
        // Should return suggestions starting with "config."
        assertTrue(suggestions.all { it.startsWith("config.", ignoreCase = true) })
    }

    // ==================== isValidRootProperty Tests ====================

    fun `test isValidRootProperty returns true for existing property`() {
        val jsonContent = """
            {
                "rootKey": "value"
            }
        """.trimIndent()
        
        createJsonFile("root.json", jsonContent)
        searchService.invalidateCache()
        
        assertTrue(searchService.isValidRootProperty("rootKey"))
    }

    fun `test isValidRootProperty returns false for non-existing property`() {
        val jsonContent = """
            {
                "existingKey": "value"
            }
        """.trimIndent()
        
        createJsonFile("existing.json", jsonContent)
        searchService.invalidateCache()
        
        assertFalse(searchService.isValidRootProperty("nonExistingKey"))
    }

    // ==================== Cache Tests ====================

    fun `test invalidateCache clears cached data`() {
        val jsonContent = """
            {
                "cacheTest": "value"
            }
        """.trimIndent()
        
        createJsonFile("cache.json", jsonContent)
        
        // First access to populate cache
        searchService.isValidRootProperty("cacheTest")
        
        // Invalidate cache
        searchService.invalidateCache()
        
        // Should still work after cache invalidation
        assertTrue(searchService.isValidRootProperty("cacheTest"))
    }

    // ==================== Edge Cases ====================

    fun `test findKey with special characters in path`() {
        val jsonContent = """
            {
                "my_key": {
                    "sub_key": "value"
                }
            }
        """.trimIndent()
        
        createJsonFile("special.json", jsonContent)
        
        val results = searchService.findKey("my_key.sub_key")
        
        assertEquals(1, results.size)
    }

    fun `test findKey with unicode characters in value`() {
        val jsonContent = """
            {
                "greeting": "你好世界"
            }
        """.trimIndent()
        
        createJsonFile("unicode.json", jsonContent)
        
        val results = searchService.findKey("greeting")
        
        assertEquals(1, results.size)
        assertTrue(results[0].preview.contains("你好世界"))
    }

    fun `test findKey with very long value truncates preview`() {
        val longValue = "x".repeat(100)
        val jsonContent = """
            {
                "longValue": "$longValue"
            }
        """.trimIndent()
        
        createJsonFile("long.json", jsonContent)
        
        val results = searchService.findKey("longValue")
        
        assertEquals(1, results.size)
        // Preview should be truncated to 50 characters
        assertTrue(results[0].preview.length <= 70) // "longValue: " + 50 chars
    }

    fun `test findKey with same key in different files`() {
        val jsonContent1 = """
            {
                "common": {
                    "data": "value1"
                }
            }
        """.trimIndent()
        
        val jsonContent2 = """
            {
                "common": {
                    "data": "value2"
                }
            }
        """.trimIndent()
        
        createJsonFile("common1.json", jsonContent1)
        createJsonFile("common2.json", jsonContent2)
        
        val results = searchService.findKey("common.data")
        
        assertEquals(2, results.size)
    }

    fun `test findKey with empty JSON object`() {
        val jsonContent = "{}"
        
        createJsonFile("empty.json", jsonContent)
        
        val results = searchService.findKey("anyKey")
        
        assertTrue(results.isEmpty())
    }

    fun `test findKey with JSON array at root`() {
        val jsonContent = """
            [
                {"key": "value1"},
                {"key": "value2"}
            ]
        """.trimIndent()
        
        createJsonFile("array_root.json", jsonContent)
        
        // Array at root is not supported, should return empty
        val results = searchService.findKey("key")
        
        assertTrue(results.isEmpty())
    }

    // ==================== Helper Methods ====================

    private fun createJsonFile(fileName: String, content: String): VirtualFile {
        return myFixture.configureByText(fileName, content).virtualFile
    }
}
