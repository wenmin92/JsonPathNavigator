package cc.wenmin92.jsonkeyfinder.performance

import cc.wenmin92.jsonkeyfinder.JsonPathTestBase
import cc.wenmin92.jsonkeyfinder.service.JsonSearchService
import kotlin.system.measureTimeMillis

/**
 * Simple performance test to verify the basic functionality works
 */
class SimplePerformanceTest : JsonPathTestBase() {

    fun testBasicSearchPerformance() {
        // Create a simple JSON file
        val jsonContent = """
            {
                "test": {
                    "key1": "value1",
                    "key2": "value2",
                    "key3": "value3"
                }
            }
        """.trimIndent()

        createJsonFile("test.json", jsonContent)

        // Warm up
        val warmupResults = searchService.findKey("test.key1")

        // Measure performance
        val duration = measureTimeMillis {
            val results = searchService.findKey("test.key1")
            // Verify result is correct
            assert(results.size == 1) { "Expected 1 result, got ${results.size}" }
        }

        // Performance assertion - should complete in under 100ms
        assert(duration < 100) { "Search took too long: ${duration}ms" }

        println("Basic search performance test passed in ${duration}ms")
    }

    fun testMultipleFileSearchPerformance() {
        // Create multiple JSON files
        repeat(10) { i ->
            val jsonContent = """
                {
                    "common": {
                        "value": "test_$i"
                    }
                }
            """.trimIndent()
            createJsonFile("test_$i.json", jsonContent)
        }

        // Measure performance of searching across multiple files
        val duration = measureTimeMillis {
            val results = searchService.findKey("common.value")
            assert(results.size == 10) { "Expected 10 results, got ${results.size}" }
        }

        // Performance assertion - should complete in under 500ms
        assert(duration < 500) { "Multiple file search took too long: ${duration}ms" }

        println("Multiple file search performance test passed in ${duration}ms")
    }
}