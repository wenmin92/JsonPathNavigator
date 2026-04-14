package cc.wenmin92.jsonkeyfinder.service

import cc.wenmin92.jsonkeyfinder.util.LogUtil
import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope

/**
 * Startup activity to warm up PSI cache for JSON files.
 * This ensures that the first search is fast by pre-parsing JSON files.
 */
class JsonIndexWarmupActivity : ProjectActivity {

    companion object {
        private val EXCLUDED_DIRS = JsonSearchService.EXCLUDED_DIRS
    }

    private val LOG = LogUtil.getLogger<JsonIndexWarmupActivity>()

    /**
     * Check if a file should be excluded (e.g., node_modules, build directories)
     */
    private fun shouldExcludeFile(file: VirtualFile, projectFileIndex: ProjectFileIndex): Boolean {
        if (!projectFileIndex.isInContent(file)) return true
        var parent = file.parent
        while (parent != null) {
            if (EXCLUDED_DIRS.contains(parent.name)) return true
            parent = parent.parent
        }
        return false
    }

    override suspend fun execute(project: Project) {
        // Wait for indexing to complete, then warm up PSI cache
        DumbService.getInstance(project).runWhenSmart {
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    LOG.info("Warming up JSON PSI cache...")
                    val startTime = System.currentTimeMillis()

                    ApplicationManager.getApplication().runReadAction {
                        val psiManager = PsiManager.getInstance(project)
                        val projectFileIndex = ProjectFileIndex.getInstance(project)
                        val jsonFiles = FileTypeIndex.getFiles(
                            JsonFileType.INSTANCE,
                            GlobalSearchScope.projectScope(project)
                        )

                        var totalCount = 0
                        var parsedCount = 0
                        for (file in jsonFiles) {
                            totalCount++
                            // Skip files in excluded directories
                            if (shouldExcludeFile(file, projectFileIndex)) continue

                            try {
                                // Parse the file to warm up PSI cache
                                val psiFile = psiManager.findFile(file) as? JsonFile ?: continue
                                // Access topLevelValue to trigger full parsing
                                val topLevel = psiFile.topLevelValue as? JsonObject
                                if (topLevel != null) {
                                    // Just access property list to ensure it's cached
                                    topLevel.propertyList
                                    parsedCount++
                                }
                            } catch (e: Exception) {
                                // Skip problematic files
                            }
                        }

                        val elapsedTime = System.currentTimeMillis() - startTime
                        LOG.info("JSON PSI cache warmed up: $parsedCount files parsed (${totalCount - parsedCount} excluded) in ${elapsedTime}ms")
                    }
                } catch (e: Exception) {
                    LOG.warn("Failed to warm up JSON PSI cache: ${e.message}")
                }
            }
        }
    }
}
