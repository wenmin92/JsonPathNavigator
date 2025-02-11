package cc.wenmin92.jsonkeyfinder.service

import cc.wenmin92.jsonkeyfinder.util.LogUtil
import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonValue
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil

data class SearchResult(
    val file: VirtualFile,
    val path: String,
    val preview: String,
    val lineNumber: Int
)

class JsonSearchService(private val project: Project) {
    private val LOG = LogUtil.getLogger<JsonSearchService>()
    private val psiManager = PsiManager.getInstance(project)
    private val pathCache = mutableMapOf<String, MutableSet<String>>()

    fun findKey(searchText: String): List<SearchResult> {
        LOG.info("Starting search for key: $searchText")
        val results = mutableListOf<SearchResult>()

        try {
            // Parse search path
            val searchParts = searchText.split(".")
            if (searchParts.isEmpty()) {
                LOG.debug("Empty search path")
                return emptyList()
            }
            LOG.debug("Search path parts: ${searchParts.joinToString(", ")}")

            // Find all JSON files in the project
            val jsonFiles = FileTypeIndex.getFiles(JsonFileType.INSTANCE, GlobalSearchScope.projectScope(project))
            LOG.info("Found ${jsonFiles.size} JSON files in project")

            // First collect all root properties from JSON files
            val rootPropertyNames = mutableSetOf<String>()
            for (jsonFile in jsonFiles) {
                val psiFile = psiManager.findFile(jsonFile) ?: continue
                val rootObject = PsiTreeUtil.findChildOfType(psiFile, JsonObject::class.java) ?: continue
                val rootProperties = PsiTreeUtil.findChildrenOfType(rootObject, JsonProperty::class.java)
                rootProperties.forEach {
                    rootPropertyNames.add(it.name)
                }
            }
            LOG.debug("Found root properties across all files: ${rootPropertyNames.joinToString(", ")}")

            // Check if the first part is a root property in any JSON file
            val firstPart = searchParts[0]
            if (!rootPropertyNames.contains(firstPart)) {
                LOG.debug("First part '$firstPart' is not a root property in any file")
                return emptyList()
            }
            LOG.debug("First part '$firstPart' is a valid root property")

            for (file in jsonFiles) {
                LOG.debug("Processing file: ${file.path}")
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
        LOG.debug("Searching in file: ${jsonFile.virtualFile.path}")
        val rootValue = jsonFile.topLevelValue
        if (rootValue !is com.intellij.json.psi.JsonObject) {
            LOG.debug("Root value is not a JSON object in file: ${jsonFile.virtualFile.path}")
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
                LOG.debug("Property value is not a JSON object at path: $currentPath.$part")
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
            LOG.debug("Not a JSON object")
            return null
        }

        // Only get direct child properties, not recursively getting all levels
        val properties = jsonObject.propertyList
        LOG.debug("Found ${properties.size} direct properties in object")
        properties.forEach {
            LOG.debug("Direct property name: '${it.name}'")
        }
        return properties.find { it.name == propertyName }
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

    fun getSuggestions(partialKey: String): List<String> {
        LOG.info("Getting suggestions for: $partialKey")

        if (partialKey.length < 2) {
            LOG.debug("Partial key too short, returning empty list")
            return emptyList()
        }

        try {
            // Wrap all PSI access operations with ReadAction
            return ApplicationManager.getApplication().runReadAction<List<String>> {
                // If it's part of a path, split and search
                val parts = partialKey.split(".")
                if (parts.size > 1) {
                    val parentPath = parts.dropLast(1).joinToString(".")
                    val lastPart = parts.last()

                    // Use cached paths
                    val cachedPaths = pathCache.getOrPut(parentPath) {
                        collectPathsForPrefix(parentPath)
                    }

                    return@runReadAction cachedPaths
                        .filter { it.startsWith(partialKey, ignoreCase = true) }
                        .sorted()
                }

                // If it's a single keyword, return all matching complete paths
                return@runReadAction pathCache.values.flatten()
                    .filter { it.contains(partialKey, ignoreCase = true) }
                    .sorted()
            }
        } catch (e: Exception) {
            LOG.error("Error getting suggestions", e)
            return emptyList()
        }
    }

    private fun collectPathsForPrefix(prefix: String): MutableSet<String> {
        val paths = mutableSetOf<String>()
        val parts = prefix.split(".")

        // Wrap all PSI access operations with ReadAction
        ApplicationManager.getApplication().runReadAction {
            val jsonFiles = FileTypeIndex.getFiles(JsonFileType.INSTANCE, GlobalSearchScope.projectScope(project))
            for (file in jsonFiles) {
                try {
                    val psiFile = psiManager.findFile(file) as? JsonFile ?: continue
                    val rootValue = psiFile.topLevelValue as? JsonObject ?: continue
                    collectPathsFromObject(rootValue, "", parts, 0, paths)
                } catch (e: Exception) {
                    LOG.error("Error collecting paths from file ${file.path}", e)
                }
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