package cc.wenmin92.jsonkeyfinder.service

import cc.wenmin92.jsonkeyfinder.JsonPathTestBase

/**
 * Simplified tests for JsonSearchService using the test base class.
 * Demonstrates usage of the test utilities.
 */
class JsonSearchServiceSimpleTest : JsonPathTestBase() {

    // ==================== Basic Functionality Tests ====================

    fun `test simple key search`() {
        createSimpleJsonFile("test.json", "name", "TestValue")
        assertSingleResult("name")
        assertPreviewContains("name", "TestValue")
    }

    fun `test nested path search`() {
        createNestedJsonFile("nested.json", "level1.level2.level3", "DeepValue")
        assertSingleResult("level1.level2.level3")
        assertPreviewContains("level1.level2.level3", "DeepValue")
    }

    fun `test non-existent key returns no results`() {
        createSimpleJsonFile("exists.json", "existingKey", "value")
        assertNoResults("nonExistentKey")
    }

    fun `test multiple files with same key structure`() {
        createNestedJsonFile("file1.json", "config.setting", "value1")
        createNestedJsonFile("file2.json", "config.setting", "value2")
        assertResultCount("config.setting", 2)
    }

    // ==================== Data Type Tests ====================

    fun `test search with string value`() {
        createSimpleJsonFile("string.json", "stringKey", "Hello World")
        assertSingleResult("stringKey")
        assertPreviewContains("stringKey", "Hello World")
    }

    fun `test search with number value`() {
        createSimpleJsonFile("number.json", "numberKey", 42)
        assertSingleResult("numberKey")
        assertPreviewContains("numberKey", "42")
    }

    fun `test search with boolean true value`() {
        createSimpleJsonFile("bool-true.json", "boolKey", true)
        assertSingleResult("boolKey")
        assertPreviewContains("boolKey", "true")
    }

    fun `test search with boolean false value`() {
        createSimpleJsonFile("bool-false.json", "boolKey", false)
        assertSingleResult("boolKey")
        assertPreviewContains("boolKey", "false")
    }

    // ==================== Cache Tests ====================

    fun `test cache refresh updates results`() {
        createSimpleJsonFile("cache.json", "cacheKey", "initialValue")
        
        // Initial search
        assertSingleResult("cacheKey")
        
        // Refresh cache
        refreshCache()
        
        // Results should still be found
        assertSingleResult("cacheKey")
    }

    // ==================== Validation Tests ====================

    fun `test root property validation positive`() {
        createSimpleJsonFile("root.json", "rootProperty", "value")
        refreshCache()
        assertTrue(searchService.isValidRootProperty("rootProperty"))
    }

    fun `test root property validation negative`() {
        createSimpleJsonFile("root.json", "existingRoot", "value")
        refreshCache()
        assertFalse(searchService.isValidRootProperty("missingRoot"))
    }
}
