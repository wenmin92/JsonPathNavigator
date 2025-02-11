package cc.wenmin92.jsonkeyfinder.actions

import cc.wenmin92.jsonkeyfinder.ui.JsonKeyFinderDialog
import cc.wenmin92.jsonkeyfinder.util.LogUtil
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.intellij.psi.PsiManager

/**
 * JSON Key Finder action class for handling JSON key search operations.
 * When triggered by the user, it displays a search dialog for finding keys in JSON files.
 */
class FindJsonKeyAction : AnAction() {
    private val LOG = LogUtil.getLogger<FindJsonKeyAction>()

    /**
     * Method executed when the user triggers the action.
     * Displays a search dialog, using selected text or text at cursor position as initial search text if available.
     *
     * @param e Action event containing current project and editor information
     */
    override fun actionPerformed(e: AnActionEvent) {
        LOG.info("JSON Key Finder action triggered")
        val project = e.project
        if (project == null) {
            LOG.warn("No project available")
            return
        }

        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor == null) {
            LOG.warn("No editor available")
            showSearchDialog(project)
            return
        }

        // Get initial search text
        val initialSearchText = ReadAction.compute<String?, Throwable> {
            getInitialSearchText(editor, project)
        }
        LOG.debug("Initial search text: $initialSearchText")

        // Show search dialog
        showSearchDialog(project, initialSearchText)
    }

    /**
     * Get initial search text.
     * Priority:
     * 1. Selected text (must be complete path)
     * 2. Complete path at cursor position
     * 3. Nearest complete path in current line
     *
     * @param editor Current editor
     * @param project Current project
     * @return Initial search text, or null if no suitable text is found
     */
    private fun getInitialSearchText(editor: Editor, project: Project): String? {
        // First check if there is selected text
        val selectedText = editor.selectionModel.selectedText
        if (!selectedText.isNullOrBlank()) {
            // Validate if selected text is a valid complete path, but always return selected text
            val isValidPath = findFullPathInText(selectedText, editor, project) != null
            if (isValidPath) {
                LOG.debug("Selected text is a valid complete path: $selectedText")
            } else {
                LOG.debug("Selected text is not a valid complete path, but will be used as initial text: $selectedText")
            }
            return selectedText
        }

        // If no text is selected, try to get complete path at cursor position
        val offset = editor.caretModel.offset
        val document = editor.document
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
        if (psiFile == null) {
            LOG.warn("No PSI file found for current document")
            return null
        }

        // Get current line range
        val lineNumber = document.getLineNumber(offset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineEndOffset = document.getLineEndOffset(lineNumber)
        val lineText = document.getText(com.intellij.openapi.util.TextRange(lineStartOffset, lineEndOffset))
        LOG.debug("Current line text: $lineText")
        LOG.debug("Current line range: $lineStartOffset - $lineEndOffset")
        LOG.debug("Cursor offset: $offset")

        // Get all string literals in current line
        val stringLiterals = mutableListOf<PsiElement>()
        val processedRanges = mutableSetOf<com.intellij.openapi.util.TextRange>()
        PsiTreeUtil.processElements(psiFile) { element ->
            val elementRange = element.textRange
            if (elementRange != null && 
                elementRange.startOffset >= lineStartOffset && 
                elementRange.endOffset <= lineEndOffset &&
                !processedRanges.contains(elementRange)) {
                
                val elementText = element.text
                // Check if it's a string (starts and ends with quotes)
                if (elementText.length >= 2 && 
                    (elementText.startsWith("\"") && elementText.endsWith("\"") ||
                     elementText.startsWith("'") && elementText.endsWith("'"))) {
                    
                    val stringValue = elementText.substring(1, elementText.length - 1)
                    // Check if it's a complete path
                    if (stringValue.isNotEmpty() && findFullPathInText(stringValue, editor, project) != null) {
                        LOG.debug("Found string literal with full path: $stringValue at range: ${elementRange.startOffset} - ${elementRange.endOffset}")
                        stringLiterals.add(element)
                        processedRanges.add(elementRange)
                    }
                }
            }
            true
        }

        LOG.debug("Found ${stringLiterals.size} unique string literals with full paths in current line")

        if (stringLiterals.isEmpty()) {
            LOG.debug("No valid full paths found in current line")
            return null
        }

        // First check if cursor is in any string literal
        val stringAtCursor = stringLiterals.find { literal ->
            val range = literal.textRange
            offset >= range.startOffset && offset <= range.endOffset
        }

        if (stringAtCursor != null) {
            val value = getStringValue(stringAtCursor)
            if (value != null && findFullPathInText(value, editor, project) != null) {
                LOG.debug("Found valid complete path at cursor: $value")
                return value
            }
        }

        // If cursor is not in any string, find the closest string
        val closestString = stringLiterals.minByOrNull { literal ->
            val center = (literal.textRange.startOffset + literal.textRange.endOffset) / 2
            val distance = Math.abs(center - offset)
            val value = getStringValue(literal)
            LOG.debug("String literal: $value, center: $center, distance to cursor: $distance")
            distance
        }

        if (closestString != null) {
            val value = getStringValue(closestString)
            if (value != null && findFullPathInText(value, editor, project) != null) {
                LOG.debug("Found valid complete path near cursor: $value")
                return value
            }
        }

        LOG.debug("No valid complete path found")
        return null
    }

    /**
     * Find complete property path in given text.
     * Complete path must start from root node, e.g., "a.b.c" not "b.c".
     *
     * @param text Text to check
     * @param editor Current editor
     * @param project Current project
     * @return The complete path if found, null otherwise
     */
    private fun findFullPathInText(text: String, editor: Editor, project: Project): String? {
        LOG.debug("Checking if '$text' is a full path")

        // Try to find this path in current file
        val parts = text.split(".")
        if (parts.isEmpty()) {
            LOG.debug("Path is empty after splitting")
            return null
        }
        LOG.debug("Path parts: ${parts.joinToString(", ")}")

        // Find all JSON files in project
        val jsonFiles = com.intellij.json.JsonFileType.INSTANCE.let { fileType ->
            com.intellij.psi.search.FileTypeIndex.getFiles(fileType, com.intellij.psi.search.GlobalSearchScope.projectScope(project))
        }
        LOG.debug("Found ${jsonFiles.size} JSON files in project")

        // First collect all root properties from JSON files
        val rootPropertyNames = mutableSetOf<String>()
        for (jsonFile in jsonFiles) {
            val psiFile = PsiManager.getInstance(project).findFile(jsonFile)
            if (psiFile == null) continue
            
            val rootObject = PsiTreeUtil.findChildOfType(psiFile, com.intellij.json.psi.JsonObject::class.java)
            if (rootObject == null) continue
            
            val rootProperties = PsiTreeUtil.findChildrenOfType(rootObject, JsonProperty::class.java)
            rootProperties.forEach { 
                rootPropertyNames.add(it.name)
            }
        }
        LOG.debug("Found root properties across all files: ${rootPropertyNames.joinToString(", ")}")

        // Check if the first part is any root property in any file
        val firstPart = parts[0]
        if (!rootPropertyNames.contains(firstPart)) {
            LOG.debug("First part '$firstPart' is not a root property in any file")
            return null
        }
        LOG.debug("First part '$firstPart' is a valid root property")

        // Now traverse all files to find the complete path
        for (jsonFile in jsonFiles) {
            LOG.debug("Checking file: ${jsonFile.path}")
            
            val psiFile = PsiManager.getInstance(project).findFile(jsonFile)
            if (psiFile == null) {
                LOG.debug("Could not get PSI for file: ${jsonFile.path}")
                continue
            }

            val rootObject = PsiTreeUtil.findChildOfType(psiFile, com.intellij.json.psi.JsonObject::class.java)
            if (rootObject == null) {
                LOG.debug("Root JSON object not found in file: ${jsonFile.path}")
                continue
            }

            // Verify the entire path from root properties
            var currentObject = rootObject
            var currentPath = ""
            var pathFound = true

            for ((index, part) in parts.withIndex()) {
                LOG.debug("Checking part $index: '$part'")
                val property = findPropertyInObject(currentObject, part)
                if (property == null) {
                    LOG.debug("Property '$part' not found in current object")
                    pathFound = false
                    break
                }
                LOG.debug("Found property '$part'")

                currentPath = if (currentPath.isEmpty()) part else "$currentPath.$part"
                LOG.debug("Current path: '$currentPath'")

                // If it's not the last part, continue to find
                if (index < parts.size - 1) {
                    val value = property.value
                    if (value !is com.intellij.json.psi.JsonObject) {
                        LOG.debug("Property '$part' is not an object, cannot continue path traversal")
                        pathFound = false
                        break
                    }
                    currentObject = value
                }
            }

            if (pathFound && currentPath == text) {
                LOG.debug("Found valid complete path '$text' starting from root")
                return text
            }
        }

        LOG.debug("No valid complete path found for '$text'")
        return null
    }

    private fun findPropertyInObject(jsonObject: com.intellij.json.psi.JsonValue?, propertyName: String): JsonProperty? {
        if (jsonObject !is com.intellij.json.psi.JsonObject) {
            LOG.debug("Not a JSON object")
            return null
        }
        
        // Get direct child properties, not recursively get all levels
        val properties = jsonObject.propertyList
        LOG.debug("Found ${properties.size} direct properties in object")
        properties.forEach { 
            LOG.debug("Direct property name: '${it.name}'")
        }
        return properties.find { it.name == propertyName }
    }

    /**
     * Get string value from PSI element
     */
    private fun getStringValue(element: PsiElement): String? {
        return when (element) {
            is JsonStringLiteral -> element.value
            else -> {
                val text = element.text
                if (text.length >= 2 && 
                    ((text.startsWith("\"") && text.endsWith("\"")) ||
                     (text.startsWith("'") && text.endsWith("'")))) {
                    text.substring(1, text.length - 1)
                } else {
                    text
                }
            }
        }
    }

    private fun findPropertiesInRange(psiFile: PsiElement, startOffset: Int, endOffset: Int): List<JsonProperty> {
        return PsiTreeUtil.collectElements(psiFile) { element ->
            element is JsonProperty &&
            element.textRange.startOffset >= startOffset &&
            element.textRange.endOffset <= endOffset
        }.mapNotNull { it as? JsonProperty }
    }

    private fun getFullPropertyPath(element: PsiElement): String? {
        // Get property
        val property = when (element) {
            is JsonProperty -> element
            is JsonStringLiteral -> element.parent as? JsonProperty
            else -> element.parentOfType<JsonProperty>()
        } ?: return null

        // Build complete path
        val pathParts = mutableListOf<String>()
        var current: JsonProperty? = property
        
        while (current != null) {
            pathParts.add(0, current.name)
            current = current.parent?.parentOfType<JsonProperty>()
        }

        // Only paths containing at least one dot are valid
        val path = pathParts.joinToString(".")
        return if (pathParts.size > 1) path else null
    }

    private fun isValidPath(text: String): Boolean {
        // Check if it's a valid complete path format (at least one dot)
        return text.matches(Regex("[a-zA-Z0-9_]+\\.[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)*"))
    }

    /**
     * Show JSON Key Finder dialog.
     *
     * @param project Current project
     * @param initialSearchText Initial search text, can be null
     */
    private fun showSearchDialog(project: Project, initialSearchText: String? = null) {
        LOG.info("Showing search dialog with initial text: $initialSearchText")
        try {
            val dialog = JsonKeyFinderDialog(project, initialSearchText)
            dialog.showDialog()
        } catch (e: Exception) {
            LOG.error("Error showing search dialog", e)
        }
    }

    /**
     * Update action state.
     * Only enable this action when there is a project and editor.
     *
     * @param e Action event
     */
    override fun update(e: AnActionEvent) {
        // Enable the action only when we have a project and editor
        val isEnabled = e.project != null && e.getData(CommonDataKeys.EDITOR) != null
        LOG.debug("Action enabled: $isEnabled")
        e.presentation.isEnabled = isEnabled
    }
} 