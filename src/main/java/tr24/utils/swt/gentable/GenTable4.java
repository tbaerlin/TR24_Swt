package tr24.utils.swt.gentable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import tr24.utils.swt.apprunenv.Tr24GuiCore;

import java.util.*;
import java.util.List;

/**
 * GenTable4 (GT4) - High-Performance SWT Table Component
 *
 * Designed to handle and render millions of rows efficiently by delegating data management
 * entirely to the user-code (UC). Utilizes row indices and rendering callbacks for optimal performance.
 */
public class GenTable4 {

    /**
     * Callback Interface for Defining Columns
     */
    public interface IColumnBuilder {
        /**
         * Adds a column to the table.
         *
         * @param id         Unique identifier for the column.
         * @param width      Initial width of the column.
         * @param alignment  Text alignment within the column (SWT.LEFT, SWT.CENTER, SWT.RIGHT).
         * @param sortable   Indicates if the column is sortable.
         */
        void addColumn(int id, int width, int alignment, boolean sortable);
    }

    /**
     * Callback Interface for Rendering Rows
     */
    public interface IRowRenderer {
        /**
         * Renders a row based on its index.
         *
         * @param rowIdx Index of the row to render (0-based).
         * @return Array of Strings representing cell values for each column.
         */
        String[] renderRow(int rowIdx);
    }

    /**
     * Callback Interface for Handling Sorting
     */
    public interface ISortHandler {
        /**
         * Initiates sorting based on user interaction.
         *
         * @param colIdx        Index of the column to sort.
         * @param sortDirection Direction of the sort (SWT.UP for ascending, SWT.DOWN for descending).
         * @param withCtrl      Indicates if the Ctrl key was held during the sort trigger (for multi-column sorting).
         * @return SortMeta object representing the current sort state, or null if sorting is already in progress.
         */
        SortMeta triggerSorting(int colIdx, int sortDirection, boolean withCtrl);
    }

    /**
     * Callback Interface for Handling Filtering
     */
    public interface IFilterHandler {
        /**
         * Initiates filtering based on the provided search string.
         *
         * @param searchString The string to filter rows by. Null or empty string indicates clearing the filter.
         */
        void triggerSearch(String searchString);
    }

    /**
     * Class Representing Sort Metadata
     */
    public static class SortMeta {
        private final List<SortColumn> sortColumns;

        public SortMeta() {
            this.sortColumns = new ArrayList<>();
        }

        public void addSortColumn(int colIdx, int sortDirection) {
            sortColumns.add(new SortColumn(colIdx, sortDirection));
        }

        public List<SortColumn> getSortColumns() {
            return sortColumns;
        }

        /**
         * Inner Class Representing a Single Sort Column
         */
        public static class SortColumn {
            public final int colIdx;
            public final int sortDirection;

            public SortColumn(int colIdx, int sortDirection) {
                this.colIdx = colIdx;
                this.sortDirection = sortDirection;
            }
        }
    }

    /**
     * Callback Interface for Context Menu Actions
     */
    public interface IContextMenuAware {
        /**
         * Defines the context menu for a given row and column.
         *
         * @param rowIdx Index of the row.
         * @param colIdx Index of the column.
         * @return MenuDefinition object defining the context menu, or null if no menu should be shown.
         */
        MenuDefinition defineMenu(int rowIdx, int colIdx);

        /**
         * Handles context menu selection actions.
         *
         * @param rowIdx    Index of the row.
         * @param colIdx    Index of the column.
         * @param menuCode  Identifier for the selected menu item.
         */
        void onMenuClick(int rowIdx, int colIdx, Object menuCode);
    }

    /**
     * Callback Interface for Key Events
     */
    public interface IKeyAware {
        /**
         * Handles key events for a specific row.
         *
         * @param rowIdx    Index of the row.
         * @param key       The character of the key pressed.
         * @param shiftDown Indicates if the Shift key is pressed.
         * @param ctrlDown  Indicates if the Ctrl key is pressed.
         * @param keyCode   The SWT keyCode of the key pressed.
         */
        void onKey(int rowIdx, char key, boolean shiftDown, boolean ctrlDown, int keyCode);
    }

    /**
     * Class Representing a Context Menu Definition
     */
    public static class MenuDefinition {

        public static final String SEP = "SEP";

        private final List<String> labels = new ArrayList<>();
        private final List<Object> vals = new ArrayList<>();
        private String unique = null;
        private Menu menu;

        /**
         * Constructor accepting label-code pairs.
         *
         * @param definitions Pairs of label and corresponding code.
         */
        public MenuDefinition(Object... definitions) {
            int i = 0;
            unique = "";
            while (i < definitions.length) {
                String label = (String) definitions[i++];
                Object val = definitions[i++];
                labels.add(label);
                vals.add(val);
                unique += label + "-" + val;
            }
        }

        /**
         * Adds a menu item.
         *
         * @param label Label of the menu item.
         * @param code  Identifier or callback for the menu action.
         * @return The current MenuDefinition instance (for chaining).
         */
        public MenuDefinition addMenu(String label, Object code) {
            labels.add(label);
            vals.add(code);
            unique = null; // Reset unique key
            return this;
        }

        /**
         * Adds a separator to the menu.
         *
         * @return The current MenuDefinition instance (for chaining).
         */
        public MenuDefinition addSeparator() {
            labels.add(SEP);
            vals.add(SEP);
            return this;
        }

        /**
         * Generates a unique key for caching purposes.
         *
         * @return The unique key string.
         */
        public String getUnique() {
            if (unique == null) {
                unique = "";
                for (int i = 0; i < labels.size(); i++) {
                    unique += labels.get(i) + "-" + vals.get(i);
                }
            }
            return unique;
        }

        /**
         * Builds the SWT Menu based on the definition.
         *
         * @param parent The parent control for the menu.
         */
        public void buildMenu(Control parent) {
            if (menu != null && !menu.isDisposed()) {
                return; // Menu already built
            }
            menu = new Menu(parent);
            for (int i = 0; i < labels.size(); i++) {
                String label = labels.get(i);
                Object code = vals.get(i);
                if (SEP.equals(label)) {
                    new MenuItem(menu, SWT.SEPARATOR);
                    continue;
                }
                MenuItem mi = new MenuItem(menu, SWT.PUSH);
                mi.setText(label);
                mi.setData(code);
            }
        }

        /**
         * Retrieves the built menu.
         *
         * @return The SWT Menu instance.
         */
        public Menu getMenu() {
            return menu;
        }
    }

    /**
     * Core GUI Elements and State
     */
    private final Tr24GuiCore core;
    private final Composite container;
    private final Table table;
    private IRowRenderer rowRenderer;
    private ISortHandler sortHandler;
    private IFilterHandler filterHandler;
    private IContextMenuAware contextMenuHandler;
    private IKeyAware keyAwareHandler;
    private List<Integer> rowIdList = new ArrayList<>();
    private int rowCount = 0;
    private final Map<String, Menu> contextMenuCache = new HashMap<>();
    private boolean isBusy = false;
    private Control busyIndicator;

    /**
     * Constructor for GenTable4
     *
     * @param parent     The parent Composite.
     * @param core       The GUI core (handles threading and async operations).
     * @param showBorder Whether to show a border around the table.
     */
    public GenTable4(Composite parent, Tr24GuiCore core, boolean showBorder) {
        this.core = core;

        // Initialize container with FormLayout
        container = new Composite(parent, SWT.NONE);
        FormLayout formLayout = new FormLayout();
        container.setLayout(formLayout);

        // Initialize Table
        if (showBorder) {
            table = new Table(container, SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.BORDER);
        } else {
            table = new Table(container, SWT.VIRTUAL | SWT.FULL_SELECTION);
        }

        // Set layout data for the Table to fill the container
        FormData tableFormData = new FormData();
        tableFormData.left = new FormAttachment(0, 0);
        tableFormData.right = new FormAttachment(100, 0);
        tableFormData.top = new FormAttachment(0, 0);
        tableFormData.bottom = new FormAttachment(100, 0);
        table.setLayoutData(tableFormData);

        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        // Add DisposeListener to clean up resources
        table.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                // Cleanup if necessary
            }
        });

        // Handle SetData event for virtual table
        table.addListener(SWT.SetData, new Listener() {
            @Override
            public void handleEvent(Event e) {
                TableItem item = (TableItem) e.item;
                if (rowRenderer != null && rowIdxListValid()) {
                    int rowIdx = rowIdList.get(table.indexOf(item));
                    String[] cellData = rowRenderer.renderRow(rowIdx);
                    if (cellData != null) {
                        for (int i = 0; i < cellData.length && i < table.getColumnCount(); i++) {
                            item.setText(i, cellData[i]);
                        }
                    }
                }
            }
        });

        // Add listeners for sorting and context menu
        addListeners();
    }

    /**
     * Sets the number of rows in the table.
     *
     * @param n The total number of rows.
     */
    public void setRowCount(int n) {
        core.asyncExec(() -> {
            rowIdList = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                rowIdList.add(i);
            }
            rowCount = n;
            table.setItemCount(n);
        });
    }

    /**
     * Sets the row renderer callback.
     *
     * @param renderer The IRowRenderer implementation.
     */
    public void setRowRenderer(IRowRenderer renderer) {
        this.rowRenderer = renderer;
    }

    /**
     * Sets the column builder callback and initializes columns.
     *
     * @param columnBuilder The IColumnBuilder implementation.
     */
    public void setColumnBuilder(IColumnBuilder columnBuilder) {
        core.asyncExec(() -> {
            table.removeAll();
            for (TableColumn col : table.getColumns()) {
                col.dispose();
            }
            // Assume columnBuilder has already added columns via addColumn
        });
    }

    /**
     * Sets the sort handler callback.
     *
     * @param sortHandler The ISortHandler implementation.
     */
    public void setSortHandler(ISortHandler sortHandler) {
        this.sortHandler = sortHandler;
    }

    /**
     * Sets the filter handler callback.
     *
     * @param filterHandler The IFilterHandler implementation.
     */
    public void setFilterHandler(IFilterHandler filterHandler) {
        this.filterHandler = filterHandler;
    }

    /**
     * Sets the context menu handler callback.
     *
     * @param contextMenuHandler The IContextMenuAware implementation.
     */
    public void setContextMenuHandler(IContextMenuAware contextMenuHandler) {
        this.contextMenuHandler = contextMenuHandler;
    }

    /**
     * Sets the key-aware handler callback.
     *
     * @param keyAwareHandler The IKeyAware implementation.
     */
    public void setKeyAwareHandler(IKeyAware keyAwareHandler) {
        this.keyAwareHandler = keyAwareHandler;
    }

    /**
     * Adds necessary listeners for sorting, context menu, and key events.
     */
    private void addListeners() {
        // Sorting Listener
        table.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                TableColumn sortColumn = table.getSortColumn();
                int sortDirection = table.getSortDirection();
                TableColumn clickedColumn = (TableColumn) e.widget;
                int colIdx = table.indexOf(clickedColumn);

                boolean withCtrl = (e.stateMask & SWT.CTRL) != 0;

                // Determine new sort direction
                if (sortColumn == clickedColumn) {
                    sortDirection = (sortDirection == SWT.UP) ? SWT.DOWN : SWT.UP;
                } else {
                    sortDirection = SWT.UP;
                }

                table.setSortColumn(clickedColumn);
                table.setSortDirection(sortDirection);

                if (sortHandler != null) {
                    SortMeta sortMeta = sortHandler.triggerSorting(colIdx, sortDirection, withCtrl);
                    if (sortMeta != null) {
                        // Optionally update column headers based on sortMeta
                        updateSortIndicators(sortMeta);
                    }
                }
            }
        });

        // Context Menu Listener
        table.addListener(SWT.MenuDetect, new Listener() {
            @Override
            public void handleEvent(Event event) {
                Point pt = table.getDisplay().map(null, table, event.x, event.y);
                TableItem item = table.getItem(pt);
                if (item != null) {
                    int rowIdx = table.indexOf(item);
                    int colIdx = getColumnAtPoint(item, pt);
                    if (colIdx >= 0 && contextMenuHandler != null) {
                        MenuDefinition def = contextMenuHandler.defineMenu(rowIdx, colIdx);
                        if (def != null) {
                            def.buildMenu(table);
                            Menu menu = def.getMenu();
                            if (menu != null && !menu.isDisposed()) {
                                menu.setLocation(event.x, event.y);
                                menu.setVisible(true);
                            }
                        }
                    }
                }
            }
        });

        // Key Listener
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (keyAwareHandler != null) {
                    int selectedIdx = table.getSelectionIndex();
                    if (selectedIdx >= 0 && selectedIdx < rowIdList.size()) {
                        int rowIdx = rowIdList.get(selectedIdx);
                        boolean shiftDown = (e.stateMask & SWT.SHIFT) != 0;
                        boolean ctrlDown = (e.stateMask & SWT.CTRL) != 0;
                        keyAwareHandler.onKey(rowIdx, e.character, shiftDown, ctrlDown, e.keyCode);
                    }
                }
            }
        });
    }

    /**
     * Updates the column headers based on the current sort metadata.
     *
     * @param sortMeta The SortMeta object containing sort states.
     */
    private void updateSortIndicators(SortMeta sortMeta) {
        core.asyncExec(() -> {
            for (TableColumn col : table.getColumns()) {
                col.setImage(null); // Clear existing sort indicators
            }
            List<SortMeta.SortColumn> sortColumns = sortMeta.getSortColumns();
            for (int i = 0; i < sortColumns.size(); i++) {
                SortMeta.SortColumn sc = sortColumns.get(i);
                TableColumn col = table.getColumn(sc.colIdx);
                if (col != null && !col.isDisposed()) {
                    // Set sort indicator based on sortDirection
                    // Note: You can set an Image here representing up/down arrows
                    // For simplicity, we'll append an arrow to the column text
                    String text = col.getText();
                    if (sc.sortDirection == SWT.UP) {
                        col.setText(text.replaceAll(" ▲| ▼", "") + " ▲");
                    } else {
                        col.setText(text.replaceAll(" ▲| ▼", "") + " ▼");
                    }
                }
            }
        });
    }

    /**
     * Determines the column index at the given point.
     *
     * @param item The TableItem.
     * @param pt   The point relative to the table.
     * @return The column index, or -1 if not found.
     */
    private int getColumnAtPoint(TableItem item, Point pt) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            Rectangle rect = item.getBounds(i);
            if (rect.contains(pt)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks if the current rowIdList is valid.
     *
     * @return True if valid, false otherwise.
     */
    private boolean rowIdxListValid() {
        return rowIdList != null && !rowIdList.isEmpty() && rowCount > 0;
    }

    /**
     * Sets the busy indicator's visibility.
     *
     * @param busy True to show the busy indicator, false to hide it.
     */
    public void setBusy(boolean busy) {
        core.asyncExec(() -> {
            if (busy) {
                if (busyIndicator == null || busyIndicator.isDisposed()) {
                    busyIndicator = new Label(container, SWT.NONE);
                    FormData busyData = new FormData();
                    busyData.left = new FormAttachment(0, 0);
                    busyData.right = new FormAttachment(100, 0);
                    busyData.bottom = new FormAttachment(100, -5);
                    busyData.height = 20;
                    busyIndicator.setLayoutData(busyData);
                    // busyIndicator.setText("Loading data...");
                }
            } else {
                if (busyIndicator != null && !busyIndicator.isDisposed()) {
                    busyIndicator.dispose();
                }
            }
            container.layout();
        });
    }

    /**
     * Callback Method for Sorting Completion
     *
     * @param newRowIdList The new list of row indices after sorting.
     * @param sortMeta     The SortMeta object representing the current sort state.
     */
    public void onSortDone(List<Integer> newRowIdList, SortMeta sortMeta) {
        core.asyncExec(() -> {
            this.rowIdList = new ArrayList<>(newRowIdList);
            this.rowCount = rowIdList.size();
            table.setItemCount(rowCount);
            table.clearAll();
            updateSortIndicators(sortMeta);
            setBusy(false);
        });
    }

    /**
     * Callback Method for Search Completion
     *
     * @param newRowIdList The new list of row indices after filtering.
     * @param searchString The search string that was executed.
     */
    public void onSearchResult(List<Integer> newRowIdList, String searchString) {
        core.asyncExec(() -> {
            this.rowIdList = new ArrayList<>(newRowIdList);
            this.rowCount = rowIdList.size();
            table.setItemCount(rowCount);
            table.clearAll();
            setBusy(false);
            // Optionally, update filter input to reflect searchString if needed
        });
    }
}





