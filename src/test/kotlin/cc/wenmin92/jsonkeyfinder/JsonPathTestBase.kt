package cc.wenmin92.jsonkeyfinder

import cc.wenmin92.jsonkeyfinder.service.JsonSearchService
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Base test class for JsonPathNavigator tests.
 * Provides common setup and utility methods for testing.
 */
abstract class JsonPathTestBase : BasePlatformTestCase() {

    protected lateinit var searchService: JsonSearchService

    override fun getTestDataPath(): String {
        return "src/test/resources/testData"
    }

    override fun setUp() {
        super.setUp()
        searchService = JsonSearchService(project)
    }

    override fun tearDown() {
        try {
            searchService.invalidateCache()
        } finally {
            super.tearDown()
        }
    }

    // ==================== JSON File Creation Helpers ====================

    /**
     * Creates a JSON file with the given content in the test project.
     */
    protected fun createJsonFile(fileName: String, content: String): VirtualFile {
        return myFixture.configureByText(fileName, content).virtualFile
    }

    /**
     * Creates a simple JSON file with a single key-value pair.
     */
    protected fun createSimpleJsonFile(fileName: String, key: String, value: Any): VirtualFile {
        val valueStr = when (value) {
            is String -> "\"$value\""
            is Boolean, is Number -> value.toString()
            null -> "null"
            else -> "\"$value\""
        }
        return createJsonFile(fileName, """{"$key": $valueStr}""")
    }

    /**
     * Creates a nested JSON file with a path and value.
     */
    protected fun createNestedJsonFile(fileName: String, path: String, value: Any): VirtualFile {
        val parts = path.split(".")
        val valueStr = when (value) {
            is String -> "\"$value\""
            is Boolean, is Number -> value.toString()
            null -> "null"
            else -> "\"$value\""
        }
        
        var content = valueStr
        for (part in parts.reversed()) {
            content = """{"$part": $content}"""
        }
        
        return createJsonFile(fileName, content)
    }

    // ==================== Assertion Helpers ====================

    /**
     * Asserts that a search returns exactly one result with the expected path.
     */
    protected fun assertSingleResult(searchPath: String, expectedPath: String = searchPath) {
        val results = searchService.findKey(searchPath)
        assertEquals("Expected single result for '$searchPath'", 1, results.size)
        assertEquals("Path mismatch", expectedPath, results[0].path)
    }

    /**
     * Asserts that a search returns no results.
     */
    protected fun assertNoResults(searchPath: String) {
        val results = searchService.findKey(searchPath)
        assertTrue("Expected no results for '$searchPath', but found ${results.size}", results.isEmpty())
    }

    /**
     * Asserts that a search returns the expected number of results.
     */
    protected fun assertResultCount(searchPath: String, expectedCount: Int) {
        val results = searchService.findKey(searchPath)
        assertEquals("Result count mismatch for '$searchPath'", expectedCount, results.size)
    }

    /**
     * Asserts that a search result contains expected preview text.
     */
    protected fun assertPreviewContains(searchPath: String, expectedText: String) {
        val results = searchService.findKey(searchPath)
        assertTrue("Expected at least one result for '$searchPath'", results.isNotEmpty())
        assertTrue(
            "Preview should contain '$expectedText', but was '${results[0].preview}'",
            results[0].preview.contains(expectedText)
        )
    }

    // ==================== Utility Methods ====================

    /**
     * Refreshes the search service cache.
     */
    protected fun refreshCache() {
        searchService.invalidateCache()
    }
}
