package cc.wenmin92.jsonkeyfinder.service

import cc.wenmin92.jsonkeyfinder.util.LogUtil
import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonValue
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope

data class SearchResult(
    val file: VirtualFile,
    val path: String,
    val preview: String,
    val lineNumber: Int
)

data class SearchResultWithTiming(
    val results: List<SearchResult>,
    val elapsedTimeMs: Long
)

class JsonSearchService(private val project: Project) {
    private val LOG = LogUtil.getLogger<JsonSearchService>()
    private val psiManager = PsiManager.getInstance(project)
    private val projectFileIndex = ProjectFileIndex.getInstance(project)
    private val pathCache = mutableMapOf<String, MutableSet<String>>()

    companion object {
        internal val EXCLUDED_DIRS = setOf(
            "node_modules", ".gradle", "build", "dist", "out",
            ".idea", "target", ".git", "vendor", "packages", "bin"
        )
    }

    private fun shouldSearchFile(file: VirtualFile): Boolean {
        if (!projectFileIndex.isInContent(file)) return false
        var parent = file.parent
        while (parent != null) {
            if (EXCLUDED_DIRS.contains(parent.name)) return false
            parent = parent.parent
        }
        return true
    }

    fun findKey(searchText: String): List<SearchResult> {
        LOG.info("Starting search for key: $searchText")
        val results = mutableListOf<SearchResult>()

        try {
            // Parse search path
            val searchParts = searchText.split(".")
            if (searchParts.isEmpty()) {
                return emptyList()
            }

            val allJsonFiles = FileTypeIndex.getFiles(JsonFileType.INSTANCE, GlobalSearchScope.projectScope(project))
            val jsonFiles = allJsonFiles.filter { shouldSearchFile(it) }
            LOG.info("Searching ${jsonFiles.size} JSON files (${allJsonFiles.size} total, ${allJsonFiles.size - jsonFiles.size} excluded)")

            for (file in jsonFiles) {
                try {
                    val psiFile = psiManager.findFile(file) as? JsonFile ?: continue
                    searchInFile(psiFile, searchParts, results)
                } catch (e: Exception) {
                    LOG.error("Error processing file ${file.path}", e)
                }
            }

            LOG.info("Search completed. Found ${results.size} matches")
        } catch (e: Exception) {
            LOG.error("Error during search operation", e)
        }

        return results
    }

    private fun searchInFile(jsonFile: JsonFile, searchParts: List<String>, results: MutableList<SearchResult>) {
        val rootValue = jsonFile.topLevelValue
        if (rootValue !is com.intellij.json.psi.JsonObject) {
            return
        }

        // Try to find the parent object of the last property directly
        var currentObject = rootValue
        var currentPath = ""

        // Traverse all parts except the last one
        for (i in 0 until searchParts.size - 1) {
            val part = searchParts[i]
            val property = findPropertyInObject(currentObject, part) ?: return
            val value = property.value
            if (value !is com.intellij.json.psi.JsonObject) {
                return
            }
            currentObject = value
            currentPath = if (currentPath.isEmpty()) part else "$currentPath.$part"
        }

        // Search for target property in the last level
        val lastPart = searchParts.last()
        val targetProperty = findPropertyInObject(currentObject, lastPart)

        if (targetProperty != null) {
            val finalPath = if (currentPath.isEmpty()) lastPart else "$currentPath.$lastPart"
            val preview = buildPreview(targetProperty)
            val lineNumber = getLineNumber(targetProperty)
            LOG.debug("Found exact match: $finalPath at line $lineNumber")
            results.add(SearchResult(jsonFile.virtualFile, finalPath, preview, lineNumber))
        }
    }

    private fun findPropertyInObject(jsonObject: JsonValue?, propertyName: String): JsonProperty? {
        if (jsonObject !is JsonObject) {
            return null
        }

        return jsonObject.propertyList.find { it.name == propertyName }
    }

    private fun buildPreview(property: JsonProperty): String {
        val value = property.value
        return when {
            value is JsonObject -> "${property.name}: { ... }"
            else -> "${property.name}: ${value?.text?.take(50)}"
        }
    }

    private fun getLineNumber(property: JsonProperty): Int {
        return property.containingFile.viewProvider.document?.getLineNumber(property.textOffset)?.plus(1) ?: 0
    }

    fun findKeyWithTiming(searchText: String): SearchResultWithTiming {
        val startTime = System.currentTimeMillis()
        val results = findKey(searchText)
        val elapsedTimeMs = System.currentTimeMillis() - startTime
        return SearchResultWithTiming(results, elapsedTimeMs)
    }

    fun invalidateCache() {
        LOG.info("Invalidating path cache")
        pathCache.clear()
    }

    fun isValidRootProperty(propertyName: String): Boolean {
        return if (ApplicationManager.getApplication().isReadAccessAllowed) {
            computeIsValidRootProperty(propertyName)
        } else {
            ApplicationManager.getApplication().runReadAction(ThrowableComputable<Boolean, Throwable> {
                computeIsValidRootProperty(propertyName)
            })
        }
    }

    private fun computeIsValidRootProperty(propertyName: String): Boolean {
        val jsonFiles = FileTypeIndex.getFiles(JsonFileType.INSTANCE, GlobalSearchScope.projectScope(project))
        for (file in jsonFiles) {
            val psiFile = psiManager.findFile(file) as? JsonFile ?: continue
            val rootObject = psiFile.topLevelValue as? JsonObject ?: continue
            if (rootObject.propertyList.any { it.name == propertyName }) {
                return true
            }
        }
        return false
    }

    fun getSuggestions(partialKey: String): List<String> {
        LOG.info("Getting suggestions for: $partialKey")

        if (partialKey.length < 2) {
            return emptyList()
        }

        try {
            val parts = partialKey.split(".")
            if (parts.size > 1) {
                val parentPath = parts.dropLast(1).joinToString(".")

                val cachedPaths = pathCache.getOrPut(parentPath) {
                    collectPathsForPrefix(parentPath)
                }

                return cachedPaths
                    .filter { it.startsWith(partialKey, ignoreCase = true) }
                    .sorted()
            }

            return pathCache.values.flatten()
                .filter { it.contains(partialKey, ignoreCase = true) }
                .sorted()
        } catch (e: Exception) {
            LOG.error("Error getting suggestions", e)
            return emptyList()
        }
    }

    private fun collectPathsForPrefix(prefix: String): MutableSet<String> {
        val paths = mutableSetOf<String>()
        val parts = prefix.split(".")

        val jsonFiles = FileTypeIndex.getFiles(JsonFileType.INSTANCE, GlobalSearchScope.projectScope(project))
        for (file in jsonFiles) {
            if (!shouldSearchFile(file)) continue
            try {
                val psiFile = psiManager.findFile(file) as? JsonFile ?: continue
                val rootValue = psiFile.topLevelValue as? JsonObject ?: continue
                collectPathsFromObject(rootValue, "", parts, 0, paths)
            } catch (e: Exception) {
                LOG.error("Error collecting paths from file ${file.path}", e)
            }
        }

        return paths
    }

    private fun collectPathsFromObject(
        jsonObject: com.intellij.json.psi.JsonObject,
        currentPath: String,
        targetParts: List<String>,
        currentPartIndex: Int,
        paths: MutableSet<String>
    ) {
        if (currentPartIndex >= targetParts.size) {
            // Collect all direct child paths of current object
            for (property in jsonObject.propertyList) {
                val path = if (currentPath.isEmpty()) property.name else "$currentPath.${property.name}"
                paths.add(path)

                val value = property.value
                if (value is com.intellij.json.psi.JsonObject) {
                    collectPathsFromObject(value, path, targetParts, currentPartIndex, paths)
                }
            }
            return
        }

        val targetPart = targetParts[currentPartIndex]
        val property = findPropertyInObject(jsonObject, targetPart) ?: return

        val newPath = if (currentPath.isEmpty()) property.name else "$currentPath.${property.name}"
        val value = property.value
        if (value is com.intellij.json.psi.JsonObject) {
            collectPathsFromObject(value, newPath, targetParts, currentPartIndex + 1, paths)
        }
    }
} 
