package cc.wenmin92.jsonkeyfinder.performance

import cc.wenmin92.jsonkeyfinder.JsonPathTestBase
import cc.wenmin92.jsonkeyfinder.service.JsonSearchService
import com.intellij.openapi.vfs.VirtualFile
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * Performance tests for JsonSearchService.
 * Tests various performance aspects of JSON search operations.
 *
 * These tests measure:
 * - Search performance with different data volumes
 * - Cache effectiveness
 * - Large file handling
 * - Memory efficiency
 */
class JsonSearchPerformanceTest : JsonPathTestBase() {

    private val performanceResults = mutableMapOf<String, PerformanceMetric>()

    data class PerformanceMetric(
        val testName: String,
        val operation: String,
        val durationMs: Long,
        val itemCount: Int,
        val avgTimePerItemMs: Double,
        val memoryDelta: Long = 0
    )

    // ==================== Setup and Teardown ====================

    override fun setUp() {
        super.setUp()
        // Warm up the JVM before running performance tests
        warmUpJvm()
    }

    override fun tearDown() {
        try {
            printPerformanceReport()
        } finally {
            super.tearDown()
        }
    }

    private fun printPerformanceReport() {
        println("\n" + "=".repeat(60))
        println("PERFORMANCE TEST REPORT")
        println("=".repeat(60))

        performanceResults.values.forEach { metric ->
            println("\nTest: ${metric.testName}")
            println("Operation: ${metric.operation}")
            println("Duration: ${metric.durationMs} ms")
            println("Item Count: ${metric.itemCount}")
            println("Avg Time Per Item: ${metric.avgTimePerItemMs} ms")
            if (metric.memoryDelta > 0) {
                println("Memory Delta: ${metric.memoryDelta / 1024} KB")
            }
        }

        println("\n" + "=".repeat(60))
    }

    // ==================== Small Dataset Performance Tests ====================

    fun testSearchPerformanceWithSmallDataset() {
        val fileCount = 10
        val keysPerFile = 5

        val files = createTestFiles(fileCount, keysPerFile)

        val (results, duration) = measureSearchTime("prop1")

        recordPerformance(
            testName = "Small Dataset Search",
            operation = "findKey",
            durationMs = duration,
            itemCount = fileCount * keysPerFile
        )

        // Assertions（createTestFiles 在根级生成 prop0..propN，无 "root" 包裹）
        assertEquals("Should find key in all files", fileCount, results.size)
        assertTrue("Search should complete in less than 100ms", duration < 100)
        println("Small dataset search completed in ${duration}ms")
    }

    fun testNestedPathSearchPerformanceWithSmallDataset() {
        val fileCount = 10

        repeat(fileCount) { i ->
            createNestedJsonFile(
                "nested_$i.json",
                "level1.level2.level3.level4.target",
                "value$i"
            )
        }

        val (results, duration) = measureSearchTime("level1.level2.level3.level4.target")

        recordPerformance(
            testName = "Nested Path Search (Small)",
            operation = "findKey",
            durationMs = duration,
            itemCount = fileCount
        )

        assertEquals("Should find key in all files", fileCount, results.size)
        assertTrue("Nested search should complete in less than 150ms", duration < 150)
        println("Nested path search completed in ${duration}ms")
    }

    // ==================== Medium Dataset Performance Tests ====================

    fun testSearchPerformanceWithMediumDataset() {
        val fileCount = 100
        val keysPerFile = 5

        val files = createTestFiles(fileCount, keysPerFile)

        val (results, duration) = measureSearchTime("prop1")

        recordPerformance(
            testName = "Medium Dataset Search",
            operation = "findKey",
            durationMs = duration,
            itemCount = fileCount * keysPerFile
        )

        assertEquals("Should find key in all files", fileCount, results.size)
        assertTrue("Medium dataset search should complete in less than 500ms", duration < 500)
        println("Medium dataset search (${fileCount} files) completed in ${duration}ms")
    }

    fun testCachePerformanceWithRepeatedSearches() {
        val fileCount = 50
        val keysPerFile = 10

        createTestFiles(fileCount, keysPerFile)

        // First search (uncached)
        val (results1, duration1) = measureSearchTime("prop1")

        // Repeated searches (cached)
        val durations = mutableListOf<Long>()
        repeat(10) {
            val (_, duration) = measureSearchTime("prop1")
            durations.add(duration)
        }

        val avgCachedDuration = durations.average()

        recordPerformance(
            testName = "Cache Performance",
            operation = "cached findKey (avg)",
            durationMs = avgCachedDuration.toLong(),
            itemCount = 10
        )

        assertTrue("Cached searches should not be significantly slower than first search",
            avgCachedDuration < duration1 * 2)
        println("First search: ${duration1}ms, Avg cached search: ${avgCachedDuration}ms")
    }

    // ==================== Large Dataset Performance Tests ====================

    fun testSearchPerformanceWithLargeDataset() {
        val fileCount = 500
        val keysPerFile = 5

        val files = createTestFiles(fileCount, keysPerFile)

        val (results, duration) = measureSearchTime("prop1")

        recordPerformance(
            testName = "Large Dataset Search",
            operation = "findKey",
            durationMs = duration,
            itemCount = fileCount * keysPerFile
        )

        assertEquals("Should find key in all files", fileCount, results.size)
        assertTrue("Large dataset search should complete in less than 2 seconds", duration < 2000)
        println("Large dataset search (${fileCount} files) completed in ${duration}ms")
    }

    fun testSearchPerformanceWithVeryLargeFile() {
        val largeJsonContent = generateLargeJsonObject(10000)
        createJsonFile("large.json", largeJsonContent)

        // 使用 prop5000：warmUpJvm 生成的文件只有 prop0..prop2，避免与大量 prop0 结果冲突
        val (results, duration) = measureSearchTime("prop5000")

        recordPerformance(
            testName = "Large File Search",
            operation = "findKey",
            durationMs = duration,
            itemCount = 10000
        )

        assertEquals("Should find exactly one result", 1, results.size)
        assertTrue("Large file search should complete in less than 1 second", duration < 1000)
        println("Large file (10000 keys) search completed in ${duration}ms")
    }

    // ==================== Suggestion Performance Tests ====================

    fun testSuggestionGenerationPerformance() {
        val fileCount = 50
        val keysPerFile = 10

        createTestFiles(fileCount, keysPerFile)

        val duration = measureTimeMillis {
            repeat(10) {
                searchService.getSuggestions("prop")
            }
        }

        val avgDuration = duration / 10.0

        recordPerformance(
            testName = "Suggestion Generation",
            operation = "getSuggestions (avg)",
            durationMs = avgDuration.toLong(),
            itemCount = 10
        )

        assertTrue("Suggestion generation should be fast", avgDuration < 100)
        println("Suggestion generation avg: ${avgDuration}ms")
    }

    // ==================== Memory Performance Tests ====================

    fun testMemoryUsageWithLargeDataset() {
        val fileCount = 200
        val keysPerFile = 10

        val memoryBefore = getUsedMemory()

        createTestFiles(fileCount, keysPerFile)

        // Trigger indexing
        searchService.invalidateCache()
        searchService.findKey("prop1")

        val memoryAfter = getUsedMemory()
        val memoryDelta = memoryAfter - memoryBefore

        recordPerformance(
            testName = "Memory Usage",
            operation = "dataset loading",
            durationMs = 0,
            itemCount = fileCount * keysPerFile,
            memoryDelta = memoryDelta
        )

        // Memory delta should be reasonable (less than 50MB for this dataset)
        assertTrue("Memory usage should be reasonable (< 50MB)",
            memoryDelta < 50 * 1024 * 1024)
        println("Memory delta: ${memoryDelta / 1024} KB")
    }

    // ==================== Concurrent Performance Tests ====================

    fun testConcurrentSearchPerformance() {
        val fileCount = 100
        val keysPerFile = 5

        createTestFiles(fileCount, keysPerFile)

        val searchKeys = (0 until keysPerFile).map { "prop$it" }

        val duration = measureTimeMillis {
            val threads = searchKeys.map { key ->
                Thread {
                    val results = searchService.findKey(key)
                    assertEquals("Should find key in all files", fileCount, results.size)
                }
            }

            threads.forEach { it.start() }
            threads.forEach { it.join() }
        }

        recordPerformance(
            testName = "Concurrent Search",
            operation = "${searchKeys.size} concurrent searches",
            durationMs = duration,
            itemCount = searchKeys.size
        )

        assertTrue("Concurrent searches should complete in less than 3 seconds", duration < 3000)
        println("Concurrent search (${searchKeys.size} threads) completed in ${duration}ms")
    }

    // ==================== Edge Case Performance Tests ====================

    fun testNonExistentKeySearchPerformance() {
        val fileCount = 100
        val keysPerFile = 5

        createTestFiles(fileCount, keysPerFile)

        val (results, duration) = measureSearchTime("nonexistent.key")

        recordPerformance(
            testName = "Non-existent Key Search",
            operation = "findKey",
            durationMs = duration,
            itemCount = 0
        )

        assertTrue("Results should be empty for non-existent key", results.isEmpty())
        assertTrue("Non-existent key search should be fast (< 200ms)", duration < 200)
        println("Non-existent key search completed in ${duration}ms")
    }

    fun testDeepNestingSearchPerformance() {
        val depth = 50
        // createDeeplyNestedFile 生成 root -> level1 -> ... -> level50，路径须含 root 前缀
        val path = "root." + (1..depth).joinToString(".") { "level$it" }

        createDeeplyNestedFile("deep.json", depth)

        val (results, duration) = measureSearchTime(path)

        recordPerformance(
            testName = "Deep Nesting Search",
            operation = "findKey (depth=$depth)",
            durationMs = duration,
            itemCount = 1
        )

        assertEquals("Should find exactly one result", 1, results.size)
        assertTrue("Deep nesting search should be fast", duration < 100)
        println("Deep nesting search (depth=$depth) completed in ${duration}ms")
    }

    // ==================== Helper Methods ====================

    /**
     * Creates multiple test JSON files with random keys
     */
    private fun createTestFiles(fileCount: Int, keysPerFile: Int): List<VirtualFile> {
        val files = mutableListOf<VirtualFile>()

        repeat(fileCount) { fileIndex ->
            val jsonBuilder = StringBuilder()
            jsonBuilder.append("{\n")

            repeat(keysPerFile) { keyIndex ->
                val key = "prop$keyIndex"
                val value = "value${fileIndex}_$keyIndex"
                jsonBuilder.append("  \"$key\": \"$value\"")
                if (keyIndex < keysPerFile - 1) jsonBuilder.append(",")
                jsonBuilder.append("\n")
            }

            jsonBuilder.append("}")

            val file = createJsonFile("test_$fileIndex.json", jsonBuilder.toString())
            files.add(file)
        }

        return files
    }

    /**
     * Creates a deeply nested JSON file
     */
    private fun createDeeplyNestedFile(fileName: String, depth: Int) {
        var content = "\"targetValue\""

        repeat(depth) { level ->
            content = "{\n  \"level${depth - level}\": $content\n}"
        }

        // Wrap in root object
        content = "{\n  \"root\": $content\n}"

        createJsonFile(fileName, content)
    }

    /**
     * Generates a large JSON object with specified number of keys
     */
    private fun generateLargeJsonObject(keyCount: Int): String {
        val jsonBuilder = StringBuilder()
        jsonBuilder.append("{\n")

        repeat(keyCount) { index ->
            val key = "prop$index"
            val value = "value$index"
            jsonBuilder.append("  \"$key\": \"$value\"")
            if (index < keyCount - 1) jsonBuilder.append(",")
            jsonBuilder.append("\n")
        }

        jsonBuilder.append("}")
        return jsonBuilder.toString()
    }

    /**
     * Measures search execution time
     */
    private fun measureSearchTime(searchText: String): Pair<List<Any>, Long> {
        var results: List<Any> = emptyList()
        val duration = measureTimeMillis {
            results = searchService.findKey(searchText)
        }
        return results to duration
    }

    /**
     * Records performance metric
     */
    private fun recordPerformance(
        testName: String,
        operation: String,
        durationMs: Long,
        itemCount: Int,
        memoryDelta: Long = 0
    ) {
        val avgTimePerItem = if (itemCount > 0) {
            durationMs.toDouble() / itemCount
        } else {
            0.0
        }

        val metric = PerformanceMetric(
            testName = testName,
            operation = operation,
            durationMs = durationMs,
            itemCount = itemCount,
            avgTimePerItemMs = avgTimePerItem,
            memoryDelta = memoryDelta
        )

        performanceResults["${testName}_$operation"] = metric
    }

    /**
     * Gets current JVM memory usage
     */
    private fun getUsedMemory(): Long {
        val runtime = Runtime.getRuntime()
        runtime.gc()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    /**
     * Warms up the JVM to ensure stable performance measurements
     */
    private fun warmUpJvm() {
        println("Warming up JVM...")
        repeat(20) {
            createTestFiles(5, 3)
            searchService.findKey("prop1")
            searchService.invalidateCache()
        }
        println("JVM warm-up complete")
    }
}