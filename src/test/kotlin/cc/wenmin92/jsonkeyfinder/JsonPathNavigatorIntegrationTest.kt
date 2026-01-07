package cc.wenmin92.jsonkeyfinder

import cc.wenmin92.jsonkeyfinder.service.JsonSearchService
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Integration tests for JsonPathNavigator plugin.
 * Tests end-to-end scenarios and interactions between components.
 */
class JsonPathNavigatorIntegrationTest : BasePlatformTestCase() {

    private lateinit var searchService: JsonSearchService

    override fun setUp() {
        super.setUp()
        searchService = JsonSearchService(project)
    }

    // ==================== Real-world Scenario Tests ====================

    fun `test search in configuration files`() {
        // Simulate a typical project configuration structure
        val appConfig = """
            {
                "app": {
                    "name": "MyApplication",
                    "version": "2.0.0",
                    "environment": "production"
                }
            }
        """.trimIndent()
        
        val dbConfig = """
            {
                "database": {
                    "url": "jdbc:postgresql://localhost:5432/mydb",
                    "pool": {
                        "maxConnections": 20
                    }
                }
            }
        """.trimIndent()
        
        createJsonFile("app-config.json", appConfig)
        createJsonFile("db-config.json", dbConfig)
        
        // Search for app configuration
        val appResults = searchService.findKey("app.name")
        assertEquals(1, appResults.size)
        assertTrue(appResults[0].preview.contains("MyApplication"))
        
        // Search for database configuration
        val dbResults = searchService.findKey("database.pool.maxConnections")
        assertEquals(1, dbResults.size)
        assertTrue(dbResults[0].preview.contains("20"))
    }

    fun `test search in i18n files`() {
        // Simulate i18n JSON files
        val enMessages = """
            {
                "messages": {
                    "greeting": "Hello",
                    "farewell": "Goodbye",
                    "error": {
                        "notFound": "Not Found",
                        "serverError": "Server Error"
                    }
                }
            }
        """.trimIndent()
        
        val zhMessages = """
            {
                "messages": {
                    "greeting": "你好",
                    "farewell": "再见",
                    "error": {
                        "notFound": "未找到",
                        "serverError": "服务器错误"
                    }
                }
            }
        """.trimIndent()
        
        createJsonFile("en.json", enMessages)
        createJsonFile("zh.json", zhMessages)
        
        // Search should find results in both files
        val greetingResults = searchService.findKey("messages.greeting")
        assertEquals(2, greetingResults.size)
        
        // Search for nested error message
        val errorResults = searchService.findKey("messages.error.notFound")
        assertEquals(2, errorResults.size)
    }

    fun `test search in package json like structure`() {
        val packageJson = """
            {
                "name": "my-project",
                "version": "1.0.0",
                "dependencies": {
                    "lodash": "^4.17.21",
                    "axios": "^1.6.0"
                },
                "devDependencies": {
                    "jest": "^29.7.0",
                    "typescript": "^5.3.0"
                },
                "scripts": {
                    "build": "tsc",
                    "test": "jest",
                    "start": "node dist/index.js"
                }
            }
        """.trimIndent()
        
        createJsonFile("package.json", packageJson)
        
        // Search for scripts
        val buildResults = searchService.findKey("scripts.build")
        assertEquals(1, buildResults.size)
        assertTrue(buildResults[0].preview.contains("tsc"))
        
        // Search for dependencies
        val depResults = searchService.findKey("dependencies.lodash")
        assertEquals(1, depResults.size)
    }

    fun `test cache invalidation works correctly`() {
        val content = """
            {
                "setting": {
                    "value": "testValue"
                }
            }
        """.trimIndent()
        
        createJsonFile("settings.json", content)
        
        // First search
        val initialResults = searchService.findKey("setting.value")
        assertEquals(1, initialResults.size)
        assertTrue(initialResults[0].preview.contains("testValue"))
        
        // Invalidate cache and search again - should still work
        searchService.invalidateCache()
        
        val resultsAfterInvalidation = searchService.findKey("setting.value")
        assertEquals(1, resultsAfterInvalidation.size)
        assertTrue(resultsAfterInvalidation[0].preview.contains("testValue"))
        
        // Add a new file and search
        val newContent = """
            {
                "setting": {
                    "newValue": "anotherValue"
                }
            }
        """.trimIndent()
        
        createJsonFile("settings2.json", newContent)
        searchService.invalidateCache()
        
        val newResults = searchService.findKey("setting.newValue")
        assertEquals(1, newResults.size)
    }

    fun `test search performance with multiple files`() {
        // Create multiple JSON files
        val fileCount = 10
        for (i in 1..fileCount) {
            val content = """
                {
                    "file$i": {
                        "data": "value$i"
                    }
                }
            """.trimIndent()
            createJsonFile("file$i.json", content)
        }
        
        searchService.invalidateCache()
        
        // Measure search time
        val startTime = System.currentTimeMillis()
        
        for (i in 1..fileCount) {
            val results = searchService.findKey("file$i.data")
            assertEquals(1, results.size)
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Search should complete in reasonable time (less than 5 seconds for 10 files)
        assertTrue("Search took too long: ${totalTime}ms", totalTime < 5000)
    }

    fun `test root property validation`() {
        val content = """
            {
                "config": {
                    "enabled": true
                },
                "settings": {
                    "theme": "dark"
                }
            }
        """.trimIndent()
        
        createJsonFile("root-test.json", content)
        searchService.invalidateCache()
        
        // Valid root properties
        assertTrue(searchService.isValidRootProperty("config"))
        assertTrue(searchService.isValidRootProperty("settings"))
        
        // Invalid root properties
        assertFalse(searchService.isValidRootProperty("nonexistent"))
        assertFalse(searchService.isValidRootProperty("enabled")) // This is a nested property
    }

    fun `test search with empty project has no results`() {
        // Don't create any JSON files
        val results = searchService.findKey("any.path")
        assertTrue(results.isEmpty())
    }

    fun `test findKeyWithTiming returns reasonable timing`() {
        // Create multiple JSON files to have measurable search time
        val fileCount = 5
        for (i in 1..fileCount) {
            val content = """
                {
                    "timing$i": {
                        "data": "value$i"
                    }
                }
            """.trimIndent()
            createJsonFile("timing$i.json", content)
        }
        
        searchService.invalidateCache()
        
        val resultWithTiming = searchService.findKeyWithTiming("timing1.data")
        
        assertEquals(1, resultWithTiming.results.size)
        assertTrue("Elapsed time should be positive", resultWithTiming.elapsedTimeMs >= 0)
        // Search should complete in reasonable time (less than 5 seconds)
        assertTrue("Search took too long: ${resultWithTiming.elapsedTimeMs}ms", 
            resultWithTiming.elapsedTimeMs < 5000)
    }

    fun `test search result file information`() {
        val content = """
            {
                "test": {
                    "key": "value"
                }
            }
        """.trimIndent()
        
        val file = createJsonFile("info-test.json", content)
        
        val results = searchService.findKey("test.key")
        
        assertEquals(1, results.size)
        assertEquals("info-test.json", results[0].file.name)
        assertEquals("test.key", results[0].path)
        assertTrue(results[0].lineNumber > 0)
    }

    // ==================== Edge Cases and Error Handling ====================

    fun `test malformed JSON file is skipped gracefully`() {
        // Create a valid JSON file
        val validContent = """
            {
                "valid": {
                    "key": "value"
                }
            }
        """.trimIndent()
        
        createJsonFile("valid.json", validContent)
        
        // Create a file with JSON extension but invalid content
        // Note: IntelliJ's JSON parser will handle this, but our code should not crash
        createTextFile("invalid.json", "{ not valid json }")
        
        // Search should still work and find results from valid file
        val results = searchService.findKey("valid.key")
        assertEquals(1, results.size)
    }

    fun `test concurrent searches`() {
        val content = """
            {
                "concurrent": {
                    "test": "value"
                }
            }
        """.trimIndent()
        
        createJsonFile("concurrent.json", content)
        
        // Perform multiple searches in quick succession
        val results1 = searchService.findKey("concurrent.test")
        val results2 = searchService.findKey("concurrent.test")
        val results3 = searchService.findKey("concurrent.test")
        
        // All should return consistent results
        assertEquals(results1.size, results2.size)
        assertEquals(results2.size, results3.size)
        assertEquals(1, results1.size)
    }

    // ==================== Helper Methods ====================

    private fun createJsonFile(fileName: String, content: String): VirtualFile {
        return myFixture.configureByText(fileName, content).virtualFile
    }

    private fun createTextFile(fileName: String, content: String): VirtualFile {
        return myFixture.configureByText(fileName, content).virtualFile
    }
}
