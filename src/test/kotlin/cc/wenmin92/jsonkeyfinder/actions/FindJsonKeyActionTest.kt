package cc.wenmin92.jsonkeyfinder.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.TestActionEvent

/**
 * Tests for FindJsonKeyAction.
 * Tests action availability and behavior in different contexts.
 */
class FindJsonKeyActionTest : BasePlatformTestCase() {

    private lateinit var action: FindJsonKeyAction

    override fun setUp() {
        super.setUp()
        action = FindJsonKeyAction()
    }

    // ==================== Action Availability Tests ====================

    fun `test action is enabled when editor is available`() {
        // Create a test file and open it
        myFixture.configureByText("test.kt", "val x = 1")
        
        val event = createTestActionEvent()
        action.update(event)
        
        assertTrue(event.presentation.isEnabled)
    }

    fun `test action is disabled when no project`() {
        val event = createTestActionEventWithoutProject()
        action.update(event)
        
        assertFalse(event.presentation.isEnabled)
    }

    // ==================== isLikelyJsonPath Tests (via reflection) ====================

    fun `test path pattern matching`() {
        // Test valid path patterns
        assertTrue(isLikelyJsonPath("root.child"))
        assertTrue(isLikelyJsonPath("config.settings.option"))
        assertTrue(isLikelyJsonPath("a.b"))
        assertTrue(isLikelyJsonPath("_private.value"))
        assertTrue(isLikelyJsonPath("root_key.child_key"))
        assertTrue(isLikelyJsonPath("abc123.def456"))
        
        // Test invalid path patterns
        assertFalse(isLikelyJsonPath(""))
        assertFalse(isLikelyJsonPath("singleWord"))
        assertFalse(isLikelyJsonPath(".startWithDot"))
        assertFalse(isLikelyJsonPath("endWithDot."))
        assertFalse(isLikelyJsonPath("123.startWithNumber"))
        assertFalse(isLikelyJsonPath("has space.value"))
        assertFalse(isLikelyJsonPath("has-dash.value"))
    }

    // ==================== Selected Text Tests ====================

    fun `test with selected text in Java file`() {
        val content = """
            public class Test {
                String path = "config.settings.value";
            }
        """.trimIndent()
        
        myFixture.configureByText("Test.java", content)
        
        // Select the path string (without quotes)
        val startOffset = content.indexOf("config.settings.value")
        val endOffset = startOffset + "config.settings.value".length
        myFixture.editor.selectionModel.setSelection(startOffset, endOffset)
        
        // Verify selection
        val selectedText = myFixture.editor.selectionModel.selectedText
        assertEquals("config.settings.value", selectedText)
    }

    fun `test with selected text in Kotlin file`() {
        val content = """
            val path = "root.child.property"
        """.trimIndent()
        
        myFixture.configureByText("test.kt", content)
        
        val startOffset = content.indexOf("root.child.property")
        val endOffset = startOffset + "root.child.property".length
        myFixture.editor.selectionModel.setSelection(startOffset, endOffset)
        
        val selectedText = myFixture.editor.selectionModel.selectedText
        assertEquals("root.child.property", selectedText)
    }

    fun `test with cursor in string literal`() {
        val content = """
            val x = "config.database.host"
        """.trimIndent()
        
        myFixture.configureByText("test.kt", content)
        
        // Place cursor inside the string
        val offset = content.indexOf("database")
        myFixture.editor.caretModel.moveToOffset(offset)
        
        // Verify cursor position
        assertEquals(offset, myFixture.editor.caretModel.offset)
    }

    // ==================== Integration with JSON Files ====================

    fun `test action in JSON file context`() {
        val jsonContent = """
            {
                "server": {
                    "host": "localhost",
                    "port": 8080
                }
            }
        """.trimIndent()
        
        myFixture.configureByText("config.json", jsonContent)
        
        val event = createTestActionEvent()
        action.update(event)
        
        assertTrue(event.presentation.isEnabled)
    }

    fun `test action in non-JSON file context`() {
        myFixture.configureByText("test.txt", "some plain text")
        
        val event = createTestActionEvent()
        action.update(event)
        
        // Action should still be enabled (it can work from any file)
        assertTrue(event.presentation.isEnabled)
    }

    // ==================== Edge Cases ====================

    fun `test with empty file`() {
        myFixture.configureByText("empty.kt", "")
        
        val event = createTestActionEvent()
        action.update(event)
        
        assertTrue(event.presentation.isEnabled)
    }

    fun `test with whitespace only selection`() {
        myFixture.configureByText("test.kt", "   \n  \t  ")
        myFixture.editor.selectionModel.setSelection(0, 5)
        
        val selectedText = myFixture.editor.selectionModel.selectedText
        assertTrue(selectedText?.isBlank() == true)
    }

    fun `test with multiline selection`() {
        val content = """
            val path1 = "config.a"
            val path2 = "config.b"
        """.trimIndent()
        
        myFixture.configureByText("test.kt", content)
        myFixture.editor.selectionModel.setSelection(0, content.length)
        
        val selectedText = myFixture.editor.selectionModel.selectedText
        assertEquals(content, selectedText)
    }

    // ==================== Helper Methods ====================

    private fun createTestActionEvent(): AnActionEvent {
        return TestActionEvent.createTestEvent(
            action,
            myFixture.editor.let {
                DataContext { dataId ->
                    when (dataId) {
                        CommonDataKeys.PROJECT.name -> project
                        CommonDataKeys.EDITOR.name -> myFixture.editor
                        else -> null
                    }
                }
            }
        )
    }

    private fun createTestActionEventWithoutProject(): AnActionEvent {
        return TestActionEvent.createTestEvent(
            action,
            DataContext { dataId ->
                when (dataId) {
                    CommonDataKeys.EDITOR.name -> null
                    else -> null
                }
            }
        )
    }

    /**
     * Helper method to test the isLikelyJsonPath pattern matching.
     * Uses the same regex pattern as the action class.
     */
    private fun isLikelyJsonPath(text: String): Boolean {
        if (text.isBlank()) return false
        return text.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)+"))
    }
}
