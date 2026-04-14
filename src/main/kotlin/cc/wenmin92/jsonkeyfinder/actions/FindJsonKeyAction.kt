package cc.wenmin92.jsonkeyfinder.actions

import cc.wenmin92.jsonkeyfinder.ui.JsonKeyFinderDialog
import cc.wenmin92.jsonkeyfinder.util.LogUtil
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

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
        val initialSearchText = getInitialSearchText(editor, project)
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
        val selectedText = editor.selectionModel.selectedText
        if (!selectedText.isNullOrBlank()) {
            return stripSurroundingQuotes(selectedText)
        }

        val offset = editor.caretModel.offset
        val document = editor.document
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return null

        val lineNumber = document.getLineNumber(offset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineEndOffset = document.getLineEndOffset(lineNumber)

        val stringLiterals = mutableListOf<PsiElement>()
        val processedRanges = mutableSetOf<com.intellij.openapi.util.TextRange>()
        PsiTreeUtil.processElements(psiFile) { element ->
            val elementRange = element.textRange
            if (elementRange != null &&
                elementRange.startOffset >= lineStartOffset &&
                elementRange.endOffset <= lineEndOffset &&
                !processedRanges.contains(elementRange)) {

                val elementText = element.text
                if (elementText.length >= 2 &&
                    (elementText.startsWith("\"") && elementText.endsWith("\"") ||
                     elementText.startsWith("'") && elementText.endsWith("'"))) {

                    val stringValue = elementText.substring(1, elementText.length - 1)
                    if (stringValue.isNotEmpty() && isLikelyJsonPath(stringValue)) {
                        stringLiterals.add(element)
                        processedRanges.add(elementRange)
                    }
                }
            }
            true
        }

        if (stringLiterals.isEmpty()) return null

        val stringAtCursor = stringLiterals.find { literal ->
            val range = literal.textRange
            offset >= range.startOffset && offset <= range.endOffset
        }
        if (stringAtCursor != null) return getStringValue(stringAtCursor)

        return stringLiterals.minByOrNull { literal ->
            val center = (literal.textRange.startOffset + literal.textRange.endOffset) / 2
            Math.abs(center - offset)
        }?.let { getStringValue(it) }
    }

    private fun stripSurroundingQuotes(text: String): String {
        return if (text.length >= 2 &&
            (text.startsWith("\"") && text.endsWith("\"") ||
             text.startsWith("'") && text.endsWith("'"))) {
            text.substring(1, text.length - 1)
        } else text
    }

    private fun isLikelyJsonPath(text: String): Boolean {
        return text.contains('.') && text.matches(Regex("""\w+(\.\w+)+"""))
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
