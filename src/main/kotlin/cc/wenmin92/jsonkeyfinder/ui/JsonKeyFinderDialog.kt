package cc.wenmin92.jsonkeyfinder.ui

import cc.wenmin92.jsonkeyfinder.service.JsonSearchService
import cc.wenmin92.jsonkeyfinder.service.SearchResult
import cc.wenmin92.jsonkeyfinder.util.LogUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

/**
 * JSON Key Finder dialog class.
 * Provides a search interface that allows users to search for keys in JSON files and displays the search results.
 *
 * @property project The current project
 * @property initialSearchText Initial search text, can be null
 */
class JsonKeyFinderDialog(
    private val project: Project,
    private val initialSearchText: String? = null
) : DialogWrapper(project, true) {
    private val LOG = LogUtil.getLogger<JsonKeyFinderDialog>()

    private val searchField = JBTextField(initialSearchText ?: "")
    private val searchButton = JButton("Search").apply {
        toolTipText = "Click to search (Enter)"
    }
    private val columnNames = arrayOf("File", "Path", "Preview", "Line")
    private val model = object : DefaultTableModel(columnNames, 0) {
        override fun isCellEditable(row: Int, column: Int) = false
        override fun getColumnClass(column: Int): Class<*> {
            return when (column) {
                3 -> Integer::class.java // Line number column
                else -> String::class.java
            }
        }
    }
    private val resultsTable = JBTable(model).apply {
        toolTipText = "Double click to jump to the selected item"
        showHorizontalLines = false
        showVerticalLines = false
        intercellSpacing = Dimension(0, 0)
    }
    private val noResultsLabel = JBLabel("", SwingConstants.CENTER)
    private val searchService = JsonSearchService(project)
    private val suggestionList = JBList<String>().apply {
        border = null
    }
    private val suggestionModel = DefaultListModel<String>()
    private val suggestionLabel = JBLabel("", SwingConstants.CENTER)
    private var lastSearchResults = listOf<SearchResult>()
    private var isInitialSearchDone = false
    private lateinit var scrollPane: JBScrollPane

    init {
        LOG.info("Initializing JSON Key Finder dialog")
        title = "Find JSON Key"
        
        // Initialize components
        setupComponents()
        
        init()

        // If there is initial search text, perform search
        if (!initialSearchText.isNullOrBlank()) {
            LOG.debug("Performing initial search")
            performInitialSearch()
        } else {
            updateEmptyState(EmptyStateType.INITIAL)
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = createMainPanel()
        
        // Ensure suggestion area is fully hidden
        SwingUtilities.invokeLater {
            val splitPane = panel.getComponent(1) as JSplitPane
            splitPane.dividerLocation = splitPane.height
            splitPane.resizeWeight = 1.0
        }
        
        return panel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return searchField
    }

    override fun getDimensionServiceKey(): String {
        return "JsonKeyFinder.Dialog"
    }

    /**
     * Perform initial search.
     * Execute before showing the dialog to ensure search results are visible when the dialog opens.
     */
    private fun performInitialSearch() {
        val searchText = initialSearchText ?: return
        LOG.info("Performing initial search for: $searchText")

        try {
            // Execute search directly in current thread since this is initialization phase
            val results = searchService.findKey(searchText)
            lastSearchResults = results
            LOG.debug("Initial search completed, found ${results.size} results")

            // Add results to table
            results.forEach { result ->
                LOG.trace("Adding initial result: ${result.file.name} - ${result.path}")
                model.addRow(
                    arrayOf(
                        result.file.name,
                        result.path,
                        result.preview,
                        result.lineNumber
                    )
                )
            }

            // If there are results, select the first row
            if (results.isNotEmpty()) {
                resultsTable.selectionModel.setSelectionInterval(0, 0)
            }

            // Update dialog title to show result count
            title = "Find JSON Key - ${results.size} results found"

            isInitialSearchDone = true
        } catch (e: Exception) {
            LOG.error("Error during initial search", e)
            title = "Find JSON Key - Initial search failed"
        }
    }

    /**
     * Set up dialog components.
     * Initialize table, search field, suggestion list and other components, and set up their event listeners.
     */
    private fun setupComponents() {
        LOG.debug("Setting up dialog components")

        // Set up table properties
        setupResultsTable()
        // Set up search field
        setupSearchField()
        // Set up suggestion list
        setupSuggestionList()

        LOG.debug("Components setup completed")
    }

    /**
     * Set up the properties of the results table.
     */
    private fun setupResultsTable() {
        resultsTable.apply {
            setShowGrid(false)  // Hide grid lines
            autoResizeMode = JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
            rowSorter = TableRowSorter(model) // Add sorting functionality
            fillsViewportHeight = true // Fill entire viewport height
            showHorizontalLines = false // Hide horizontal lines
            showVerticalLines = false // Hide vertical lines
            intercellSpacing = Dimension(0, 0) // Set cell spacing to 0
            border = BorderFactory.createEmptyBorder() // Remove table border

            columnModel.apply {
                getColumn(0).apply { // File
                    preferredWidth = 150
                    minWidth = 100
                }
                getColumn(1).apply { // Path
                    preferredWidth = 250
                    minWidth = 150
                }
                getColumn(2).apply { // Preview
                    preferredWidth = 300
                    minWidth = 200
                }
                getColumn(3).apply { // Line
                    preferredWidth = 50
                    minWidth = 50
                    maxWidth = 100
                }
            }

            // Add double-click event listener
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        LOG.debug("Double click on results table, opening selected file")
                        openSelectedFile()
                    }
                }
            })
        }
    }

    /**
     * Set up the search field properties and event listeners.
     */
    private fun setupSearchField() {
        // Set up search button click event
        searchButton.addActionListener {
            performSearch()
        }

        // Set up search field keyboard events
        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                // Expand suggestion area immediately when key is pressed
                if (e.keyChar.isLetterOrDigit() || e.keyChar == '.') {
                    SwingUtilities.invokeLater {
                        val suggestionScrollPane = suggestionList.parent as JViewport
                        val suggestionParent = suggestionScrollPane.parent as JBScrollPane
                        val cardPanel = suggestionParent.parent as JPanel
                        val splitPane = cardPanel.parent as JSplitPane
                        
                        // Set initial height for suggestion area
                        val suggestedHeight = (splitPane.height * 0.3).toInt().coerceIn(100, 200)
                        splitPane.dividerLocation = splitPane.height - suggestedHeight
                        splitPane.resizeWeight = 0.7
                        
                        updateSuggestionState(SuggestionStateType.LOADING)
                        splitPane.revalidate()
                        splitPane.repaint()
                    }
                }
            }

            override fun keyReleased(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_ENTER -> {
                        LOG.debug("Enter key pressed, performing search")
                        performSearch()
                    }
                    KeyEvent.VK_ESCAPE -> {
                        LOG.debug("Escape key pressed, closing dialog")
                        dispose()
                    }
                    else -> {
                        if (e.keyChar.isLetterOrDigit() || e.keyChar == '.') {
                            LOG.trace("Key released: ${e.keyChar}, updating suggestions")
                            updateSuggestions()
                        } else if (e.keyCode == KeyEvent.VK_BACK_SPACE || e.keyCode == KeyEvent.VK_DELETE) {
                            // Handle delete key, hide suggestion area if text is empty
                            if (searchField.text.isEmpty()) {
                                updateSuggestionState(SuggestionStateType.EMPTY)
                            } else {
                                updateSuggestions()
                            }
                        }
                    }
                }
            }
        })
    }

    /**
     * Set up suggestion list properties and event listeners.
     */
    private fun setupSuggestionList() {
        suggestionList.apply {
            model = suggestionModel
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 1) {
                        val selected = selectedValue
                        if (selected != null) {
                            LOG.debug("Suggestion selected: $selected")
                            searchField.text = selected
                            performSearch()
                        }
                    }
                }
            })
        }
    }

    /**
     * Create the main panel of the dialog.
     * Contains search field, results table, and suggestion list.
     *
     * @return Panel containing all components
     */
    private fun createMainPanel(): JComponent {
        val panel = JPanel(BorderLayout())

        // Create search panel
        val searchPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5) // Add inner padding
        }
        searchPanel.add(JBLabel("Search: "), BorderLayout.WEST)
        val searchFieldPanel = JPanel(BorderLayout())
        searchFieldPanel.add(searchField, BorderLayout.CENTER)
        searchFieldPanel.add(searchButton, BorderLayout.EAST)
        searchPanel.add(searchFieldPanel, BorderLayout.CENTER)
        panel.add(searchPanel, BorderLayout.NORTH)

        // Create results panel (contains table and no results label)
        val resultsPanel = JPanel(CardLayout()).apply {
            border = BorderFactory.createEmptyBorder() // Remove border
        }
        scrollPane = JBScrollPane(resultsTable).apply {
            border = BorderFactory.createEmptyBorder() // Remove scroll panel border
            viewport.background = resultsTable.background // Set viewport background color to match table
        }
        scrollPane.preferredSize = Dimension(600, 400)
        resultsPanel.add(scrollPane, "results")
        resultsPanel.add(noResultsLabel, "empty")

        // Create suggestion list scroll panel, allowing resizing
        val suggestionPanel = JPanel(CardLayout()).apply {
            border = BorderFactory.createEmptyBorder() // Remove border
            preferredSize = Dimension(600, 0) // Initial height set to 0
        }
        val suggestionScrollPane = JBScrollPane(suggestionList).apply {
            border = BorderFactory.createEmptyBorder() // Remove scroll panel border
            viewport.background = suggestionList.background // Set viewport background color to match list
        }
        suggestionScrollPane.preferredSize = Dimension(600, 100)
        suggestionScrollPane.minimumSize = Dimension(600, 50)
        suggestionPanel.add(suggestionScrollPane, "list")
        suggestionPanel.add(suggestionLabel, "message")

        // Create split panel
        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, resultsPanel, suggestionPanel).apply {
            border = BorderFactory.createEmptyBorder() // Remove split panel border
            dividerSize = 2 // Set split bar width to 2 pixels
            resizeWeight = 1.0 // Default to allocate all space to results panel
            isContinuousLayout = true // Enable continuous layout update for real-time adjustment
            dividerLocation = Int.MAX_VALUE // Initial time set split bar to bottom
        }
        panel.add(splitPane, BorderLayout.CENTER)

        return panel
    }

    fun showDialog() {
        show()
    }

    /**
     * Perform search operation.
     * Execute search in background thread and update results in UI thread.
     */
    private fun performSearch() {
        val searchText = searchField.text
        LOG.info("Performing search for: $searchText")

        if (searchText.isBlank()) {
            LOG.debug("Search text is blank, clearing results")
            SwingUtilities.invokeLater {
                model.setRowCount(0)
                updateEmptyState(EmptyStateType.INVALID_SEARCH)
                (scrollPane.parent as JPanel).let { panel ->
                    (panel.layout as CardLayout).show(panel, "empty")
                }
            }
            return
        }

        // Clear old results
        SwingUtilities.invokeLater {
            model.setRowCount(0)
            updateEmptyState(EmptyStateType.SEARCHING)
            title = "Find JSON Key - Searching..."
            (scrollPane.parent as JPanel).let { panel ->
                (panel.layout as CardLayout).show(panel, "empty")
            }
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // Use ReadAction to wrap search operation
                val results = ApplicationManager.getApplication().runReadAction<List<SearchResult>> {
                    searchService.findKey(searchText)
                }
                lastSearchResults = results
                LOG.debug("Search completed, found ${results.size} results")

                SwingUtilities.invokeLater {
                    updateSearchResults(results)
                    val panel = scrollPane.parent as JPanel
                    val layout = panel.layout as CardLayout
                    if (results.isEmpty()) {
                        updateEmptyState(EmptyStateType.NO_RESULTS)
                        layout.show(panel, "empty")
                    } else {
                        layout.show(panel, "results")
                    }
                }
            } catch (e: Exception) {
                LOG.error("Error during search operation", e)
                SwingUtilities.invokeLater {
                    title = "Find JSON Key - Search failed"
                    updateEmptyState(EmptyStateType.NO_RESULTS)
                    (scrollPane.parent as JPanel).let { panel ->
                        (panel.layout as CardLayout).show(panel, "empty")
                    }
                }
            }
        }
    }

    /**
     * Update search results.
     * Update table and title in UI thread.
     *
     * @param results Search results list
     */
    private fun updateSearchResults(results: List<SearchResult>) {
        try {
            // Clear old results
            model.setRowCount(0)
            
            // Add new results
            results.forEach { result ->
                LOG.trace("Adding result: ${result.file.name} - ${result.path}")
                model.addRow(
                    arrayOf(
                        result.file.name,
                        result.path,
                        result.preview,
                        result.lineNumber
                    )
                )
            }

            // If there are results, select the first row
            if (results.isNotEmpty()) {
                resultsTable.selectionModel.setSelectionInterval(0, 0)
            }

            // Update dialog title to show result count
            title = "Find JSON Key - ${results.size} results found"

            // Ensure table is updated
            model.fireTableDataChanged()
            resultsTable.revalidate()
            resultsTable.repaint()
        } catch (e: Exception) {
            LOG.error("Error updating results table", e)
            title = "Find JSON Key - Error updating results"
        }
    }

    /**
     * Update suggestions.
     * Get suggestions based on current search field text and update suggestions list in UI thread.
     */
    private fun updateSuggestions() {
        val partialKey = searchField.text
        LOG.debug("Updating suggestions for: $partialKey")

        if (partialKey.isBlank()) {
            updateSuggestionState(SuggestionStateType.EMPTY)
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val suggestions = searchService.getSuggestions(partialKey)
                LOG.debug("Got ${suggestions.size} suggestions")

                SwingUtilities.invokeLater {
                    try {
                        suggestionModel.clear()
                        suggestions.forEach {
                            LOG.trace("Adding suggestion: $it")
                            suggestionModel.addElement(it)
                        }

                        // Update suggestion list state
                        if (suggestions.isEmpty()) {
                            updateSuggestionState(SuggestionStateType.NO_SUGGESTIONS)
                        } else {
                            updateSuggestionState(SuggestionStateType.HAS_SUGGESTIONS)
                            suggestionList.selectedIndex = 0
                        }
                    } catch (e: Exception) {
                        LOG.error("Error updating suggestions list", e)
                        updateSuggestionState(SuggestionStateType.ERROR)
                    }
                }
            } catch (e: Exception) {
                LOG.error("Error getting suggestions", e)
                SwingUtilities.invokeLater {
                    updateSuggestionState(SuggestionStateType.ERROR)
                }
            }
        }
    }

    /**
     * Open selected file.
     * Open selected search result file in editor and jump to specified line.
     */
    private fun openSelectedFile() {
        val selectedRow = resultsTable.selectedRow
        LOG.debug("Opening file for selected row: $selectedRow")

        if (selectedRow >= 0 && selectedRow < lastSearchResults.size) {
            try {
                val result = lastSearchResults[selectedRow]
                LOG.info("Opening file: ${result.file.path} at line: ${result.lineNumber}")

                FileEditorManager.getInstance(project).openTextEditor(
                    OpenFileDescriptor(project, result.file, result.lineNumber - 1, 0),
                    true
                )
            } catch (e: Exception) {
                LOG.error("Error opening selected file", e)
            }
        } else {
            LOG.warn("No row selected in results table or invalid selection")
        }
    }

    private enum class EmptyStateType {
        INITIAL,
        NO_RESULTS,
        INVALID_SEARCH,
        SEARCHING
    }

    private fun updateEmptyState(type: EmptyStateType) {
        val message = when (type) {
            EmptyStateType.INITIAL -> "<html><center>Enter a JSON key path to search<br>Example: root.child.property</center></html>"
            EmptyStateType.NO_RESULTS -> "<html><center>No results found for '${searchField.text}'<br>Try a different search term</center></html>"
            EmptyStateType.INVALID_SEARCH -> "<html><center>Please enter a valid search term<br>Search term cannot be empty</center></html>"
            EmptyStateType.SEARCHING -> "<html><center>Searching...<br>Please wait</center></html>"
        }
        
        SwingUtilities.invokeLater {
            noResultsLabel.text = message
            val layout = (scrollPane.parent as JPanel).layout as CardLayout
            if (model.rowCount == 0) {
                layout.show(scrollPane.parent, "empty")
            } else {
                layout.show(scrollPane.parent, "results")
            }
        }
    }

    private enum class SuggestionStateType {
        EMPTY,
        LOADING,
        NO_SUGGESTIONS,
        HAS_SUGGESTIONS,
        ERROR
    }

    private fun updateSuggestionState(type: SuggestionStateType) {
        val message = when (type) {
            SuggestionStateType.EMPTY -> "<html><center>Suggestion Area<br>Suggestions will be shown here when you start typing</center></html>"
            SuggestionStateType.LOADING -> "<html><center>Loading suggestions...</center></html>"
            SuggestionStateType.NO_SUGGESTIONS -> "<html><center>Suggestion Area<br>No suggestions found for '${searchField.text}'</center></html>"
            SuggestionStateType.HAS_SUGGESTIONS -> ""
            SuggestionStateType.ERROR -> "<html><center>Suggestion Area<br>Error loading suggestions</center></html>"
        }

        SwingUtilities.invokeLater {
            suggestionLabel.text = message
            // Get suggestion panel (CardLayout)
            val suggestionScrollPane = suggestionList.parent as JViewport
            val suggestionParent = suggestionScrollPane.parent as JBScrollPane
            val cardPanel = suggestionParent.parent as JPanel
            val layout = cardPanel.layout as CardLayout
            val splitPane = cardPanel.parent as JSplitPane

            when (type) {
                SuggestionStateType.EMPTY -> {
                    // Completely hide suggestion area
                    splitPane.resizeWeight = 1.0
                    splitPane.dividerLocation = splitPane.height
                    layout.show(cardPanel, "message")
                }
                SuggestionStateType.HAS_SUGGESTIONS -> {
                    // Display suggestion list and allocate appropriate space
                    layout.show(cardPanel, "list")
                    splitPane.resizeWeight = 0.7
                    val suggestedHeight = (splitPane.height * 0.3).toInt().coerceIn(100, 200)
                    splitPane.setDividerLocation(splitPane.height - suggestedHeight)
                    splitPane.revalidate()
                    splitPane.repaint()
                }
                else -> {
                    // Display message and allocate smaller space
                    layout.show(cardPanel, "message")
                    splitPane.resizeWeight = 0.85
                    val messageHeight = 60 // Fixed message area height
                    splitPane.setDividerLocation(splitPane.height - messageHeight)
                    splitPane.revalidate()
                    splitPane.repaint()
                }
            }
        }
    }
} 