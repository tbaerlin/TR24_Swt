package tr24.utils.swt.gentable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import tr24.utils.common.ShittyCodeException;
import tr24.utils.common.Task;
import tr24.utils.common.ThreadUtil;
import tr24.utils.swt.BasisCore;
import tr24.utils.swt.FARBE;
import tr24.utils.swt.api.IShutdownHook;

import java.util.List;
import java.util.*;


/**
 * Zentrale Generisch Tabelle:
 * - wird zu Beginn mit den Spalten configuriert
 * - dann mit Daten-Rows gefüttert
 * - braucht einen ITableAdapter zur Kommunikation mit der Aussenwelt
 * 
 * T: DatenKlassen des Objects das jeder Zeile zugrunde liegt
 * 
 * @author tbaer
 * 
 * 
 * painting in SWT-Tabellen:   
 * http://www.eclipse.org/articles/article.php?file=Article-CustomDrawingTableAndTreeItems/index.html
 */
public class GenTable2<T> implements IShutdownHook {



    /**
	 * welche Arten von Zellen-Highlight gibt's ?
	 */
	public static enum HighlightSTYLE {
		/**
		 * Blinkt in einer Farbe
		 */
		SOLID_COLOR,
		
		/**
		 * ping-bar nach oben
		 */
		ACTIVITY_METER
	}
	
	/**
	 * Helfer: baue die Spalten auf
	 * 
	 * - hole den Builder über die Tabelle: {@link GenTable2#builder()}
	 */
	public static class ColBuilder {

		private final List<ColumnHandler> ch = new ArrayList<ColumnHandler>();
		
		private ColBuilder() {
		}

		public void addText(String label, int width, int swtLeftCenterRight, boolean sortable) {
			ch.add(ColumnHandler.TEXT(ch.size(), label, width, swtLeftCenterRight, sortable));
		}
		public void addFloat(String label, int width, int swtLeftCenterRight, boolean sortable) {
			ch.add(ColumnHandler.FLOAT(ch.size(), label, width, swtLeftCenterRight, sortable));
		}
		public void addFloat(String label, int width, int swtLeftCenterRight, boolean sortable, int nachkommas) {
			ColumnHandler.useNachkommaStellen = nachkommas;
			ch.add(ColumnHandler.FLOAT(ch.size(), label, width, swtLeftCenterRight, sortable));
		}
		public void addInt(String label, int width, int swtLeftCenterRight, boolean sortable) {
			ch.add(ColumnHandler.INT(ch.size(), label, width, swtLeftCenterRight, sortable));
		}
		public void addLong(String label, int width, int swtLeftCenterRight, boolean sortable) {
			ch.add(ColumnHandler.LONG(ch.size(), label, width, swtLeftCenterRight, sortable));
		}
		public void addDate(String label, int width, int swtLeftCenterRight, boolean sortable) {
			ch.add(ColumnHandler.DATE(ch.size(), label, width, swtLeftCenterRight, sortable));
		}
		public void addDateTime(String label, int width, int swtLeftCenterRight, boolean sortable) {
			ch.add(ColumnHandler.DATETIME(ch.size(), label, width, swtLeftCenterRight, sortable));
		}
		public void addTime(String label, int width, int swtLeftCenterRight, boolean sortable, boolean showSeconds) {
			ch.add(ColumnHandler.TIME(ch.size(), label, width, swtLeftCenterRight, sortable, showSeconds));
		}
		public void addMoney(String label, int width, int swtLeftCenterRight, boolean sortable) {
			ch.add(ColumnHandler.MONEY(ch.size(), label, width, swtLeftCenterRight, sortable));
		}
		/**
		 * @param showZeroPrice - false: zeige nicht für 0.0 
		 */
		public void addPrice(String label, int width, int swtLeftCenterRight, boolean sortable, boolean showZeroPrice) {
			ch.add(ColumnHandler.PRICE(ch.size(), label, width, swtLeftCenterRight, sortable, showZeroPrice));
		}
		public void addPercentage(String label, int width, int swtLeftCenterRight, boolean sortable) {
			ch.add(ColumnHandler.PERCENTAGE(ch.size(), label, width, swtLeftCenterRight, sortable));
		}
		public void addPercentage(String label, int width, int swtLeftCenterRight, boolean sortable, int nachkommas) {
			ColumnHandler.useNachkommaStellen = nachkommas;
			ch.add(ColumnHandler.PERCENTAGE(ch.size(), label, width, swtLeftCenterRight, sortable));
		}
	}
	
	/**
	 * @return true nach Ende
	 */
	public boolean isDisposed() {
		return disposed;
	}
	
	public ColBuilder builder() {
		return new ColBuilder();
	}
	public ColBuilder getColBuilder() {
		return new ColBuilder();
	}
	
	protected final BasisCore core;
	
	private Table table;
	
	/**
	 * true wenn die Tabelle SWT-mässig gekillt wurde
	 */
	protected boolean disposed = false;
	
	/**
	 * Liste der Zeilen - sortiert - ÄNDERT also die Reihenfolge der Einträge
	 * wenn der User auf eine Column klickt
	 */
	protected List<Row> rows = new ArrayList<Row>();

	protected ColumnHandler[] colList;

	protected final ITableAdapter<T> adapter;
	
	final private Color colLeft;
	final private Color colRight;

    IGenTableDefaultMenuBuilder defaultMenu;  // null-able

	/**
	 *  wenn gesetzt: Welche Spalte dienst als "text-input" für den jump-to-key?
	 */
	private int keyJumpCol = -1;

	/**
	 * Key-Lookup-Liste der aktuelle aktiven CellHighlights: [rowIdx*100 + colIdx] -> {@link CellHighlight}
	 */
	protected Map<Integer, CellHighlight> highMap = new Hashtable<Integer, CellHighlight>();

	/**
	 * Liste der aktiven Highlights, wird vom {@link ActivityThread} verwaltet
	 */
	protected List<CellHighlight> highlighters = new ArrayList<CellHighlight>();
	
	/**
	 * reverse-lookup: Finde die Row zum Daten-Element
	 */
	protected Map<T, Row> dataLookup = new HashMap<T, Row>(2000);
	
	protected boolean run = true;
	
	/**
	 * wird bei Bedarf erzeugt
	 */
	protected ActivityThread activityThread = null;
	
	/**
	 * beim render() checken wir die Breite des Anzeige-Strings und merken uns den breitesten
	 */
	private final int[] maxWidthOfColumn = new int[1024];   // viele Spalten möglich
	/**
	 * siehe {@link #setMinColumnWidth(int, int)}
	 */
	private final int[] minColWidth = new int[1024];
	
	private long lastUpdateCall = 0;
	
	/**
	 * zum Filtern von doppelten Events
	 */
	private int lastEventTime;
	
	/**
	 * see fireOnCell(TableItem, int, boolean, TypedEvent, String)
	 */
	protected boolean mouseDown;
	
	/**
	 * check die Menus wenn gesetzt
	 */
	private final IContextMenuAware<T> menus;
	
	private final Map<String, MenuDefinition> contextMenuCache = new HashMap<String, MenuDefinition>();
	
	protected final IPostSortListener<T> postSortHook;
	
	protected final IKeyAware<T> keyListener;
	
	/**
	 * gesetzt wenn gerade ein Context-Menu offen ist:
	 * - diese ZEILE
	 */
	private T curMenuData;
	private int curMenuColumn;

	/**
	 * ref auf die Zeile, die gerade ausgewählt ist
	 * - ich muss das wissen für onKey(...)
	 */
	private Row curSelectedRow;
	
	/**
	 * siehe {@link Row#selectionFlag} 
	 */
	private Row toBeSelected;

	/**
	 * SWT meldet bei Doppel-Click: click->double->click
	 * - ich muss den zweiten 'click' filtern!
	 */
	protected long lastDblClickTime;


	/**
	 * nur gesetzt wenn ein {@link #setCustomPaintColumn(int, ICustomColumnPainter)} kam
	 * - ich prüfe bei jedem Paint ob die Zelle der Spalte x vom user-Code gezeichnet werden will 
	 */
	private ICustomColumnPainter<T>[] customPainters;
	
	/**
	 * praktisch: ich brauch was um zwei INTs zu speichern
	 */
	private final Point lastSort = new Point(-1, 0);		// x=colIdx, y=up/down

	/**
	 * nur gesetzt wenn die Rows eine nicht-standard-Höhe haben sollen
	 */
	RowHeightSetter measureHandler;
	
	
	/**
	 * erzeugt initial die SWT-Tabelle
	 */
	public GenTable2(Composite parent, ITableAdapter<T> adapter, BasisCore core, boolean showBorder) {
		this(parent, adapter, core, showBorder, null, null, null);
	}
	
	/**
	 * erzeugt initial die SWT-Tabelle
	 * - ggf MIT Menu
	 * - ggf MIT Post-Sorting-User-Code
	 */
	public GenTable2(Composite parent, ITableAdapter<T> adapter, BasisCore core, boolean showBorder,
                     IContextMenuAware<T> menuOrNull, IPostSortListener<T> postSortOrNull, IKeyAware<T> keyOrNull)
	{
		this.adapter = adapter;
		this.core = core;
		core.add2Shutdown(this);
		this.menus = menuOrNull;
		this.postSortHook = postSortOrNull;
		this.keyListener = keyOrNull;
		
		// Selected-Row Farbverlauf
		colLeft = FARBE.ORANGE_1;
		colRight = FARBE.YELLOW_1;
		
		// create a virtual table to display data
		if (showBorder) {
			table = new Table(parent, SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.BORDER);
		} else {
			table = new Table(parent, SWT.VIRTUAL | SWT.FULL_SELECTION);
		}
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		// data-get event verlinken
		table.addListener(SWT.SetData, new Listener() {		// virtual table: GIB mir Daten für Zeile x
			public void handleEvent(Event e) {
				TableItem item = (TableItem) e.item;
				onDisplayTableItem(item);
			}
		});
		// custom paint
		table.addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(Event event) {
				paintCell(event);
			}
		});		
//		table.addListener(SWT.MeasureItem, new Listener() {		NICHT benötigt momentan
//			public void handleEvent(Event event) {
//				measureCell(event);
//			}
//		});		
		table.addListener(SWT.EraseItem, new Listener() {
			public void handleEvent(Event event) {
				paintCellBackground(event);
			}
		});	
		
		// fange double-click auf Zellen ab:
	    table.addMouseListener(new MouseAdapter() {
	    	@Override
	    	public void mouseDoubleClick(MouseEvent e) {
	    		processClick(e, true);
	    	}
	    	@Override
	    	public void mouseUp(MouseEvent e) {
	    		mouseDown = false;
	    		if (e.button==3) {				// höre auf Rechts-Clicks -> Menus checken
	    			if (menus!=null) {
	    				buildMenu(e);
	    			}
	    		}
	    		if (e.button==1) {				// links click
	    			processClick(e, false);
	    		}
	    	}
	    	/**
	    	 * Row-Click/Widget-Select-FILTER: 
	    	 * - ich muss wissen dass die Mouse gerade down ist
	    	 *  => dann UNTERBINDE das Widget-Selected Event !
	    	 */
	    	@Override
	    	public void mouseDown(MouseEvent e) {
	    		mouseDown = true;
	    	}
			private void processClick(MouseEvent e, boolean dlbClick) {
				// Filtern:
				if (!dlbClick) {
					long time = e.time & 0xFFFFFFFFL;		// siehe JAVADOC!!
					if ((time-lastDblClickTime) < 200) {
						return;
					}
				}
				Point pt = new Point(e.x, e.y);
				//Übersetze den Click auf eine Zelle in Zeile und Spalte
    			TableItem item = table.getItem(pt);		// liefert immer das erste Item der Zeile
    			if (item==null) {
    				return;			// click ausserhalb
    			}
    			// finde die richtige Spalte
    			int column = -1;
    	        for (int i=0, n=table.getColumnCount(); i<n; i++) {
    	        	Rectangle rect = item.getBounds(i);
    	        	if (rect.contains(pt)) {
    	        		column = i;
    	        		break;
    	        	}
    	        }
    	        if (dlbClick) {
    	        	lastDblClickTime = e.time & 0xFFFFFFFFL;
    	        } 
    	        fireOnCell(item, column, dlbClick, e);
			}
	    });
	    table.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem item = (TableItem) e.item;
				// Filter: feure NICHT wenn die Mouse gerade DOWN ist
				if (mouseDown==false) {
					fireOnCell(item, -1, false, e);
				} else {
					// System.err.println(">> skip WidgetSelected !");
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// widgetSelected(e);
			}
		});
	    table.addKeyListener(new KeyAdapter() {
	    	@Override
	    	public void keyReleased(KeyEvent e) {
	    		if (keyListener!=null && curSelectedRow!=null) {
	    			boolean ctrl  = (e.stateMask & SWT.CTRL) > 0;
	    			boolean shift = (e.stateMask & SWT.SHIFT) > 0;
	    			keyListener.onKey(curSelectedRow.data, e.character, shift, ctrl, e.keyCode);
	    		} else {
	    			handleJumpToKey(e.character);
	    		}
	    	}
		});
	    // ich kann von aussen abfragen ob die Tabelle überhaupt noch gültig ist
	    table.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				disposed = true;
			}
		});
	}

	/**
	 * kann nach dem Constructor kommen:
	 * - zeige die ROWS so hoch an
	 * @param height
	 */
	public void setRowHeight(int height) {
		if (measureHandler==null) {				// erzeuge den Listener nur EINMAL
			measureHandler = new RowHeightSetter();
			measureHandler.useHeight = Math.max(height, 10);
			table.addListener(SWT.MeasureItem, measureHandler);
		} else {
			measureHandler.useHeight = Math.max(height, 10);		// ab dem zweiten Call: verändere einfach
			table.redraw();
		}
	}

	static class RowHeightSetter implements Listener {
		/** so hoch */
		public int useHeight;

		public void handleEvent(Event event) {
			event.height = useHeight;
		}
	}
	
	
	@Override
	public void shutdown() {
		run = false;		// der Activity-Thread muss aufhören, sonst knallt's dauernd (Widget is disposed)
	}
	
	
	
	/**
	 * Zugriff auf die echte SWT-Tabelle
	 */
	public Table getTableControl() {
		return table;
	}

	/**
	 * setzt eine Mindest-Breite für eine Spalte
	 */
	public void setMinColumnWidth(int colIdx, int minWithForAutoResize) {
		minColWidth[colIdx] = minWithForAutoResize;
	}
	
	/**
	 * erstellt einmalig die Spalten
	 *  - kann auch <b>ERNEUT</b> die kompletten Spalten ERSETZEN
	 */
	public void configColumns(ColBuilder builder) {
		ColumnHandler[] list = builder.ch.toArray(new ColumnHandler[builder.ch.size()]);
		configColumns(list);
	}
	
	
	/**
	 * erstellt einmalig die Spalten
	 * - kann auch <b>ERNEUT</b> die kompletten Spalten ERSETZEN
	 */
	public void configColumns(ColumnHandler[] colList) {
		// ist das ein RE-call ?
		if (this.colList!=null) {
			TableColumn[] cols = table.getColumns();
			for (TableColumn oldC : cols) {
				oldC.dispose();		// mal sehen ob das so klappt...
			}
		}
		
		this.colList = colList;		// merken
		for (ColumnHandler c : colList) {
			TableColumn tc = new TableColumn(table, c.swtAlignment);  // LEFT , RIGHT, CENTER
			tc.setWidth(c.width);
			tc.setText(c.label);
			tc.setData(c);			// Ref auf Meta-Info-Object herstellen

			if (c.sortable) {
				// Sortierung: Spalte sort-enablen und Sort-ID setzen
				tc.addListener(SWT.Selection, sortListener);
			}
		}
	}

	/**
	 * Dynamisch NEUE Spalte(n) anlegen:
	 */
	public void addColumns(final ColBuilder newCols) {
		
		Runnable r = new Runnable() {
			public void run() {
				ColumnHandler[] list = newCols.ch.toArray(new ColumnHandler[newCols.ch.size()]);
				
				// Achtung: wenn's noch gar keine cols gibt:
				if (colList==null) {
					configColumns(list);
					return;
				}
				
				// die neuen Spalte/n müssen auch in die colList, ist ein Array => also NEU anlegen
				int n = colList.length;
				ColumnHandler[] newList = new ColumnHandler[n + list.length];		// baue NEUE Liste!
				
				for (int i=0; i<list.length; i++) {
					ColumnHandler c = list[i];
					TableColumn tc = new TableColumn(table, c.swtAlignment);  // LEFT , RIGHT, CENTER
					tc.setWidth(c.width);
					tc.setText(c.label);
					tc.setData(c);			// Ref auf Meta-Info-Object herstellen
					if (c.sortable) {
						// Sortierung: Spalte sort-enablen und Sort-ID setzen
						tc.addListener(SWT.Selection, sortListener);
					}
					newList[n+i] = c;								// füge die neuen schon mal "hinten" an
					
					// ACHTUNG: der col.colIdx stimmt nicht!!
					c.columnIdx = n+i;
				}
				System.arraycopy(colList, 0, newList, 0, n);		// kopiere bekannten vorderen Teil
				colList = newList;
			}
		};
		
		if (core.isSwtThread()) {
			r.run();
		} else {
			core.asyncExecAndWait(r);
		}
	}

	
	/**
	 * Sonderfall: 
	 * - füge die Row ein
	 * - und liefere gleich alle Zeilen
	 */
	public List<T> addRowAndGetAll(final T data) {
		if (core.isSwtThread()) {
			new RowAdder(data, false, false).run();
			return getAllRows();
		} else {
			final Object monitor = new Object();
			final List<T> temp = new ArrayList<T>();
			synchronized (monitor) {
				core.asyncExec(new Runnable() {
					public void run() {
						new RowAdder(data, false, false).run();
						temp.addAll(getAllRows());
						synchronized (monitor) {
							monitor.notify();
						}
					}
				});
				try {
					monitor.wait();		// ich muss den lock-handshake hier von Hand machen
				} catch (InterruptedException e) { }		 
			}	// synched
			return temp;
		}
	}
	
	/**
	 * fügt eine Zeile zur Tabelle
	 */
	public void addRow(T data) {
		addRow(data, false);
	}
	
	/**
	 * füge eine Zeile OBEN ein
	 */
	public void addRowFirst(T data, boolean selectRow) {
		if (core.isSwtThread()) {
			new RowAdder(data, selectRow, true).run();
		} else {
			core.asyncExec(new RowAdder(data, selectRow, true));
		}
	}
	
	/**
	 * füge die Zeile hinzu und SELEKTIERE sie
	 */
	public void addRow(T data, boolean selectRow) {
		if (core.isSwtThread()) {
			new RowAdder(data, selectRow, false).run();
		} else {
			core.asyncExec(new RowAdder(data, selectRow, false));
		}
	}
	
	private class RowAdder implements Runnable  {
		private final T data;
		private final boolean selectRow;
		private final boolean insertOnTop;
		/** constructor */
		public RowAdder(T data, boolean selectRow, boolean insertOnTop) {
			this.data = data;
			this.selectRow = selectRow;
			this.insertOnTop = insertOnTop;
		}
		public void run() {
			Row r = new Row(data);
			boolean clearTab = false;
			synchronized (rows) {
				if (insertOnTop==false) {
					rows.add(r);
				} else {
					if (rows.size()>0) {
						rows.add(0, r);
						clearTab = true;
					} else {
						rows.add(r);
					}
				}
				dataLookup.put(data, r);
				// jetzt noch neu sorieren: aktuelle Spalte, in aktueller Richtung
				// sortData(lastSortColumn, table.getSortDirection());
				table.setItemCount(rows.size());		// daaaas trigger onDisplayTableItem
				if (clearTab) {
					table.clearAll();		// das wird die Tabelle neu einlesen und zeichnen!
				}
				table.clearAll();		// das wird die Tabelle neu einlesen und zeichnen!
				if (selectRow) {
					_rowSelection(data);
				}
			}
		}
	}
	
	/**
	 * bulk insert version
	 */
	public void addRows(final Iterable<T> bulk) {
		RowsAdder rowsAdder = new RowsAdder(bulk, false, false);
		if (core.isSwtThread()) {
			rowsAdder.run();
		} else {
			core.asyncExec(rowsAdder);
		}
	}
	
	/**
	 * cool: Bulk-insert in der AKTUELLEN Sortier-Reihenfolge
	 * 
	 * @param replaceRows - true: lösche vorher alles, false: append/merge 
	 *
	 */
	public void addRowsAndResort(Iterable<T> bulk, boolean replaceRows) {
		RowsAdder rowsAdder = new RowsAdder(bulk, true, replaceRows);
		if (core.isSwtThread()) {
			rowsAdder.run();
		} else {
			core.asyncExec(rowsAdder);
		}
	}
	

	/**
	 * Helfer: Füge eine Menge von Zeilen ein
	 * - und RE-Sortiere wenn gewünscht gleich
	 */
	private class RowsAdder implements Runnable {
		private final Iterable<T> bulk;
		/** true: wende die letzte Sortierung an (wenn verfügbar) */
		private final boolean applyLastSort;
		/** true: LÖSCHE alle Zeilen, dann füge 'bulk' ein */
		private final boolean replaceRows;
		/** constructor */
		public RowsAdder(Iterable<T> bulk, boolean withSort, boolean replaceRows) {
			this.bulk = bulk;
			this.applyLastSort = withSort;
			this.replaceRows = replaceRows;
		}
		public void run() {
			synchronized (rows) {
				if (replaceRows) {				// alles vorher löschen ?
					rows.clear();				
					dataLookup.clear();
					table.setItemCount(0);		// MUSS kommen, sonst triggert die SWT-Table kein onDisplayTableItem() !!
				}
				for (T data : bulk) {
					Row r = new Row(data);		// neue Zeilen an-/ein-fügen
					rows.add(r);
					dataLookup.put(data, r);
				}
			}
			// soll ich gleich noch sortieren (so wie die lezte Sort-Aktion) ?
			if (applyLastSort) {
				if (lastSort.x >= 0) {		// wenn's überhaupt was zum Sortieren gibt
					ColumnHandler ch = colList[lastSort.x];
					int swtUpOrDown = lastSort.y;
					sortData(ch, swtUpOrDown);
				}
			}
			// jetzt weiss die Table: es hat sich was verändert
			table.setItemCount(rows.size());		// daaaas trigger onDisplayTableItem
		}		
	}

	
	/**
	 * Zeile kicken
	 */
	public void deleteRow(final T data) {
		if (data==null) {
			return;
		}
		core.asyncExec(new Runnable() {
			public void run() {
				synchronized (rows) {
					Row r = dataLookup.get(data);		// finde die Table-Row zum Daten-Item
					rows.remove(r);		// sequentiell, geht nicht anders
					dataLookup.remove(data);
				}
				// ich muss die Tabelle komplett neu aufbauen
				table.setItemCount(rows.size());
				table.clearAll();
			}
		});
	}
	/**
	 * Zeilen-Bulk Delete
	 */
	public void deleteRows(final List<T> data) {
		if (data==null || data.size()==0) {
			return;
		}
		core.asyncExec(new Runnable() {
			public void run() {
				synchronized (rows) {
					for (T row : data) {
						Row r = dataLookup.get(row);		// finde die Table-Row zum Daten-Item
						rows.remove(r);						// sequentiell, geht nicht anders
						dataLookup.remove(data);
					}
				}
				// ich muss die Tabelle komplett neu aufbauen
				table.setItemCount(rows.size());
				table.clearAll();
			}
		});
	}
	
	/**
	 * Auswahl der aktuellen Zeile per Programm-Code
	 */
	public void setSelectecdRow(final T data) {
		core.asyncExec(new Runnable() {
			public void run() {
				synchronized (rows) {
					_rowSelection(data);
				}
			}
		});
	}

	/**
	 * Löscht alle Auswahl (eh nur eine)
	 */
	public void deSelectRows() {
		Runnable code = new Runnable() {
			@Override
			public void run() {
				table.deselectAll();
			}
		};
		if (core.isSwtThread()) {
			code.run();
		} else {
			core.asyncExec(code);
		}
	}
	
	/**
	 * Gehe eine Zeile weiter (wenn möglich) oder zurück
	 * - das feuert dann auch den auf-Zeile-doppel-geclickt 
	 */
	public void rowMove(boolean forward) {
		if (core.isSwtThread()) {
			new RowChanger(+1, 2).run();
		} else {
			core.asyncExec(new RowChanger(-1, 2));
		}
	}
	
	/**
	 * gehe ein Zeile hoch oder runter
	 */
	private class RowChanger implements Runnable {
		private final int moveBy;
		private final int doFire;	

		/**
		 * @param moveBy - 1: hoch, +2: zwei vor
		 * @param doFire - 	0: löse nix aus, 
		 * 					1: löse einfach Klick aus
		 * 					2: löse Dbl-Klick aus 
		 */
		public RowChanger(int moveBy, int doFire) {
			this.moveBy = moveBy;
			this.doFire = doFire;
		}

		@Override
		public void run() {
			int idx = table.getSelectionIndex();
			
			int newIdx = idx + moveBy;
			if (newIdx>=0 && newIdx<table.getItemCount()) {
				table.setSelection(newIdx);
				TableItem item = table.getItem(newIdx);
				if (doFire>0) {
					fireOnCell(item, -1, (doFire==2), null);		// setzt auch curSelectedRow
				} else {
					curSelectedRow = rows.get(newIdx);
				}
			}
		}
	}
	
	/**
	 * ausgelagert, kann von 2 Stellen gerufen werden
	 */
	private void _rowSelection(final T data) {
		Row r = dataLookup.get(data);		// finde die Table-Row zum Daten-Item
		curSelectedRow = r;
		
		// siehe Row#selectionFlag: Workaround wenn die Zeile noch NIE sichtbar war
		if (r.item==null) {
			if (toBeSelected!=null) {		// wenn schon jemand wartet
				toBeSelected.selectionFlag = false;		// du wirst abgelöst
			}
			toBeSelected = r;
			r.selectionFlag = true;
		} else {
			table.setSelection(r.item);			// die Zeile ist schon sichtbar -> springe da hin
		}
	}

	/**
	 * löscht alle Zeilen
	 */
	public void clear() {
		if (core.isSwtThread()) {
			rows.clear();
			dataLookup.clear();
			table.setItemCount(0);
		} else {
			core.asyncExec(new Runnable() {
				public void run() {
					rows.clear();
					dataLookup.clear();
					table.setItemCount(0);
				}
			});
		}
	}
	
	/**
	 * Lese/erzeuge den Index der Zeile
	 */
	private int getRowIndex(TableItem item) {
		Integer index = (Integer) item.getData();
		if (index == null) {
			index = table.indexOf(item); // suchen, geht im worst-case sequenziell durch!
			item.setData(index); 		 // daher: merken für quick-nach-guggen
		}
		return index;
	}
	
	/**
	 * reverse lookup: Finde die Daten-Zeile zur Tabellen-Zeile
	 * 
	 * @return null wenn item null war
	 */
	public T getRowByTabItem(TableItem item) {
		if (item==null) {
			return null;
		}
		synchronized (rows) {
			int idx = getRowIndex(item);
			return rows.get(idx).data;
		}
	}
	
	/**
	 * call von der Tabelle: - ich brauche den Inhalt dieser Zeile
	 */
	protected void onDisplayTableItem(TableItem item) {
		int index = getRowIndex(item);
		// check
		if (index >= rows.size()) {
			return; // habe noch keine Daten
		}
		Row row = rows.get(index);
		row.item = item;		// back-ref
		row.setDataIntoItem();
		
		if (row.selectionFlag) {
			row.selectionFlag = false;
			toBeSelected = null;
			table.setSelection(index);
		}
	}

	
	// Sortierung: call kommt im SWT
	private final Listener sortListener = new Listener() {
		public void handleEvent(Event e) {
			try {
				// determine new sort column and direction
				TableColumn sortColumn = table.getSortColumn();
				TableColumn currentColumn = (TableColumn) e.widget;
				int dir = table.getSortDirection();
				if (sortColumn == currentColumn) {
					dir = (dir == SWT.UP) ? SWT.DOWN : SWT.UP;
				} else {
					table.setSortColumn(currentColumn);
					sortColumn = currentColumn;
					dir = SWT.UP;
				}
				sortData((ColumnHandler) sortColumn.getData(), dir);
			} catch (Exception ex) {
				core.error("GenTable.sort()", ex);
			}
		}
	};

	/**
	 * Spalten sortieren: - diese Methode ist nur die allgemeine Hülle 
	 * - der Adapter muss die Arbeit machen
	 */
	protected void sortData(final ColumnHandler ch, int swtUpOrDown) {
		final int direction = (swtUpOrDown==SWT.UP ? 1 : -1);
		lastSort.x = ch.columnIdx;
		lastSort.y = swtUpOrDown;
		Collections.sort(rows, new Comparator<Row>() {
			public int compare(Row r1, Row r2) {
				return adapter.sortCell(ch, direction, r1.data, r2.data, r1.item, r2.item);
			};
		});

		// beim Sortieren ändern sich ja die Zeilen-Indices => lösche alle Highlighters
		synchronized (highlighters) {
			highlighters.clear();
			highMap.clear();
		}
		// Tabelle aktualisieren:
		table.setSortDirection(swtUpOrDown);
		table.clearAll();		// das wird die Tabelle neu einlesen und zeichnen
		
		// HIER kann jetzt der User-Code irgendwas um-bauen, zB eine dynamische Summen-Spalte neu berechnen
		if (postSortHook!=null) {
			IRowAccess<T> access = new IRowAccess<T>() {
				@Override
				public T get(int idx) {
					return rows.get(idx).data;
				}
				@Override
				public int size() {
					return rows.size();
				}
			};
			postSortHook.postSort(ch.columnIdx, swtUpOrDown, access);
		}
	}
	
	/**
	 * Sortiere die Tabelle:
	 * 
	 * - Der call kann im SWT-Thread oder sonst-Thread kommen, egal
	 * 
	 * @param colIdx - Spalte 0..n
	 * @param SWT_upOrDown - {@link SWT#UP} oder {@link SWT#DOWN}
	 */
	public void triggerManualSort(final int colIdx, final int SWT_upOrDown) {
		if (core.isSwtThread()) {
			ColumnHandler ch = colList[colIdx];
			sortData(ch, SWT_upOrDown);
		} else {
			core.asyncExec(new Runnable() {
				public void run() {
					ColumnHandler ch = colList[colIdx];
					sortData(ch, SWT_upOrDown);
				}
			});
		}
	}
	
	/**
	 * Kapselt eine Zeile: - das brauchen wir als ref-Speicher auf 
	 * die echte Daten-Zeile
	 */
	public class Row {

		/**
		 * Workaround: Bsp:
		 * - es kommt eine neue Zeile hinzu (User legt ein neues Setup an)
		 * - der User-Code will diese Zeile auch gleich selektieren {@link GenTable2#setSelectecdRow(Object)}
		 * - ABER: die Tabelle liegt hinter einem Fenster oder ist weg-gescollt -> die neue Zeile wurde 
		 *   NOCH NIE gezeichnet!
		 * - da die Tabelle VIRUELL ist: onDisplayTableItem() wurde noch gar nicht GERUFEN! 
		 *   -> daher ist die Verlinkung Row->TableItem noch null -> setSelectedRow() würde KNALLEN!
		 *   
		 * Lösung: 
		 * - merke dir dass die Zeile selektiert werden soll (dieses Flag)
		 * - sobald getSelectedRow läuft: TableItem ist bekannt -> table.setSelection(tableItem)  
		 * - ich muss mir noch zentral die todo-Row merken, denn die kann sich ja ändern  
		 */
		boolean selectionFlag;

		/**
		 * Ref auf das Orginal-Object
		 */
		public T data;
		
		TableItem item;
		
		/**
		 * constructor
		 */
		public Row(T data) {
			this.data = data;
		}

		/**
		 * lässt sich vom Adapter die Zellen in Strings rendern
		 */
		public void setDataIntoItem() {
			if (item!=null && !item.isDisposed()) {
				for (int i=0; i<colList.length; i++) {
				    ColumnHandler ch = colList[i];
					String s;
					try {
						s = adapter.renderCell(data, ch);
					} catch (Exception e) {
						e.printStackTrace();
						s = "";
					}
					if (s==null) {
						s = "";
					}
					
					// bestimme die maximale Breite der Zelle
					int cidx = ch.columnIdx;
					if (s.length() > maxWidthOfColumn[cidx]) {
						maxWidthOfColumn[cidx] = s.length();
					}
					
					item.setText(i, s);
				}
			}
		}
	}	// Row

//	/**
//	 * kommt ZUERST
//	 */
//	protected void measureCell(Event event) {
//		// event.height = 40;		// Zeilen-Höhe
//		// event.width *= 2;
//	}
	
	/**
	 * kommt VOR dem HINTERGRUND-Paint:
	 * - male die {@link SWT#SELECTED}-Zeile mit Farbverlauf
	 * - wenn event.doit==true bleibt dann zeichnet SWT den Foreground 
	 */
	protected void paintCellBackground(Event event) {
		// event.doit = false;   // false: zeiche NICHT den Vordergrund
		event.detail &= ~SWT.HOT;		// Hover-Effect löschen
		boolean isHigh = (event.detail & SWT.SELECTED)!=0;

		// pre-check: will der User-Code diese Zelle SELBER zeichnen ?
		if (customPainters!=null) {
			int colIdx = event.index;
			ICustomColumnPainter<T> painter = customPainters[colIdx];
			if (painter!=null) {
				int rowIdx = (Integer)event.item.getData();
				Row row = rows.get(rowIdx);
				T data = row.data;
				boolean paintDone = painter.paint(data, event.gc, event.x, event.y, event.width, event.height, isHigh);
				if (paintDone) {
					event.detail &= ~SWT.SELECTED;			// SWT darf nichts mehr machen!
					event.detail &= ~SWT.FOCUSED;
					return;			// bin fertig
				}
			}
		}
		
		if (isHigh) {			// wenn die Zeile der Zelle selected ist
			int clientWidth = table.getClientArea().width;
			GC gc = event.gc; 
			// Color oldForeground = gc.getForeground(); 
			Color oldBackground = gc.getBackground(); 
			gc.setForeground(colLeft); 
			gc.setBackground(colRight); 
			gc.fillGradientRectangle(0, event.y, clientWidth, event.height, false); 
			gc.setBackground(oldBackground); 
			event.detail &= ~SWT.SELECTED;
			event.detail &= ~SWT.FOCUSED;
			// setze die Farbe für's weitere Zeichnen
			gc.setForeground(FARBE.RED_DARK_1);
		}
		
		// Hier: check ob die Zelle ge-highlight-ed ist: dann zeichne den Background entsprechend
		int key = ((Integer)event.item.getData()) * 100 + event.index;
		CellHighlight ch = highMap.get(key);
		if (ch!=null) {
			ch.drawCell(event);
		}
	}

	/**
	 * NACH dem Vordergrund-Zeichnen
	 */
	protected void paintCell(Event event) {
	}
	

	/**
	 * wenn eine Zelle einen activity-ping bekommen hat:
	 * - es existiert EIN Object für diese Zelle, darin steht:
	 * - WIE soll die Zelle gehighlighted werden
	 * - für activiy-meter: die noch-Höhe
	 * 
	 * - die paintCell-Methode fragt pro Zelle ob's so ein Object gibt 
	 *   und malt dann den Cell-Background entsprechend
	 *   
	 * - create/update/delete-d werden die Objs vom Activity-Thread
	 */
	private class CellHighlight {

		private final long startedAt;
		public boolean alive = true;
		public int key;
		public final int rowIdx;
		private final HighlightSTYLE style;
		private final Color color;
		private int activityHigh;
		
		/**
		 * constructor
		 */
		public CellHighlight(int rowIdx, int key, HighlightSTYLE style, Color color) {
			this.rowIdx = rowIdx;
			this.key = key;
			this.style = style;
			this.color = color;
			startedAt = System.currentTimeMillis();
			if (style==HighlightSTYLE.ACTIVITY_METER) {
				activityHigh = 15;
			}
		}
		
		/**
		 * Zeichnet den Highhight-Hintergrund
		 */
		public void drawCell(Event event) {
			GC gc = event.gc;
			if (style==HighlightSTYLE.SOLID_COLOR) {
				Color oldBackground = gc.getBackground(); 
				gc.setBackground(color);
				gc.fillRectangle(event.x, event.y, event.width, event.height);
				gc.setBackground(oldBackground);		// restore 
			}
			if (style==HighlightSTYLE.ACTIVITY_METER) {
				Color oldBackground = gc.getBackground(); 
				gc.setBackground(color);
				int y0 = event.y + event.height - activityHigh - 1;
				gc.fillRectangle(event.x, y0, 5, activityHigh);
				gc.setBackground(oldBackground);		// restore 
			}
		}

		/**
		 * call vom Thread (zyklisch):
		 * - ich checke: bin ich noch aktiv ?
		 */
		public boolean age(long now) {
			if (style==HighlightSTYLE.ACTIVITY_METER) {
				activityHigh -= 2;
				if (activityHigh<0) {
					alive = false;
				}
				return true;		// muss IMMER neu zeichnen
			}
			
			// sonst: SOLID_PATTERN
			long myAge = now - startedAt;
			if (myAge >= 600) {
				alive = false;
				return true;		// zeichne mich neu, also wieder im Orginal
			}
			return false;
		}
		
	}
	
	
	
	/**
	 * eine Info aus einer Spalte (also eine Zelle) hat sich geändert
	 * - die Tabelle soll dass per activity-ping/highlight anzeigen
	 * - die Tabelle lässt einen eigenen Thread laufen, der das highlighten steuert
	 *   (und der wird erst erzeugt wenn pingCell() auch verwendet wird)
	 *   
	 * @param color - in der Farbe soll's Highlighten 
	 */
	public void pingCell(final T data, final int columnIndex, final HighlightSTYLE style, Color color) {
		final Color useColor = (color!=null) ? color : FARBE.RED_1;
		// der ganze Zugriff muss im SWT-Thread laufen!
		core.asyncExec(new Runnable() {
			public void run() {
				// zu welcher Zeile gehört 'data'
				Row row;
				synchronized (rows) {
					if (activityThread==null) {
						activityThread = new ActivityThread();
						activityThread.setDaemon(true);
						activityThread.start();
					}
					row = dataLookup.get(data);
				}
				if (row==null || row.item==null || row.item.isDisposed()) {
					return;			// sicher ist sicher
				}
				int rowIdx = getRowIndex(row.item);
				int key = rowIdx*100 + columnIndex;		// es dürfte wohl kaum eine Tabelle mit 100 Spalten geben
				CellHighlight ch = new CellHighlight(rowIdx, key, style, useColor);
				highMap.put(key, ch);
				synchronized (highlighters) {
					highlighters.add(ch);
				}
				// redraw Zeile x
				table.clear(rowIdx);
			}
		});
	}
	
	/**
	 * eine Row hat sich geändert, zeige das an, alles OHNE ge-ping-e! <br>
	 * 
	 * @param allowSpeedSkip - true: wir überwachen, dass SWT nicht zu viele calls machen muss <br>
	 *   - wenn speedSkip = true: schmeiss den Call weg wenn er zu schnell nach dem letzten kam <br>
	 *   - wenn false: die Anzeige ist WICHTIG, mache den SWT-Call auf jeden Fall
	 */
	public void updateRow(final T data, boolean allowSpeedSkip) {
		if (allowSpeedSkip) {
			long now = System.currentTimeMillis();
			if ((now - lastUpdateCall) < 500) {
				return;
			}
			lastUpdateCall = now;
		}
		
		// der ganze Zugriff muss im SWT-Thread laufen!
		core.asyncExec(new Runnable() {
			public void run() {
				// zu welcher Zeile gehört 'data'
				Row row = dataLookup.get(data);
				if (row==null || row.item==null || row.item.isDisposed()) {
					return;			// sicher ist sicher
				}
				int rowIdx = getRowIndex(row.item);
				// redraw Zeile x
				table.clear(rowIdx);
			}
		});
	}
	
	/**
	 * Bulk Version of {@link #updateRow(Object, boolean)}
	 * 
	 * - die Zeilen fromIdx <= i < endIdx haben sich geändert 
	 * 
	 * @param lockWhileUpdate : sollte true sein wenn die 'rows' auch von ausserhalb weiter-verwendet werden
	 */
	public void updateRows(final List<T> rows, final int fromIdx, final int endIdx, final boolean lockWhileUpdate) {
		Runnable code = new Runnable() {
			@Override
			public void run() {
				synchronized (rows) {		// warte bis die rows "frei" sind, das ist immer gut
					for (int i=fromIdx; i<endIdx; i++) {
						T data = rows.get(i);
						// zu welcher Zeile gehört 'data'
						Row row = dataLookup.get(data);
						if (row==null || row.item==null || row.item.isDisposed()) {
							continue;			// sicher ist sicher
						}
						int rowIdx = getRowIndex(row.item);
						// redraw Zeile x
						table.clear(rowIdx);
					}
					if (lockWhileUpdate) {
						rows.notify();			// wecke den caller wieder auf
					}
				}
			}
		};
		
		// ACHTUNG: wenn ich die 'rows' nur für mich haben soll:
		if (lockWhileUpdate) {			// dann muss der calling-Thread WARTEN!
			
			/* ACHTUNG: HIER liegt ne GEILE Dead-Lock-FALLE!!!!
			   Wenn der Call im SWT-Thread kommt, dann lege ich mit 
			   rows.wait()   weiter unten
			   FAKTISCH die SWQ-Queue lahm !
			   und kann daher NIE den obrigen [code] ausführen!! 
				LÖSUNG: ich muss hier UNERSCHEIDEN ob der caller im SWT ist oder nicht!!
			*/
			synchronized (rows) {		// ok, die sind jetzt blockiert
				if (core.isSwtThread()) {		
					// A) wenn ich gerde EH im SWT bin: rufe den code DIREKT auf
					code.run();
				} else {
					// B) code soll im SWT laufen:
					core.asyncExec(code);	// starte den Code
					try {
						rows.wait();		// und gehe schlafen, das ist jetzt auch OK!
					} catch (InterruptedException e) { e.printStackTrace(); }
				}
			}
		} else {
			// sonst: un-ge-lock-te Ausführung
			core.asyncExec(code);		
		}
	}


	/**
	 * - gehe die Liste der Highlighters durch
	 * - lasse jeden 'altern': entferne ihn ggf aus der lookup Map
	 */
	private class ActivityThread extends Thread {
		
		/**
		 * nach jedem Durchlauf stehen hier die Rows, die neu gezeichnet werden müssen
		 */
		private final List<CellHighlight> clearRowList = new ArrayList<CellHighlight>();
		
		@Override
		public void run() {
			while (run) {
				ThreadUtil.sleepUnhandled(120);
				if (!run) {
					return;		// check öfters...
				}
				synchronized (highlighters) {
					int deadCount = 0;
					long now = System.currentTimeMillis();
					
					/*
					 * Konzept: 
					 * - highlighters enthält alle CellHighligh objects
					 * - wenn ein CH ausgedient hat, dann wird's alive=false
					 * - es VERBLBEIBT aber zunächst auf der Liste (da wir im loop ja kein Remove machen können)
					 * - erst wenn zuviel tot sind räumen wir die Liste auf
					 */
					synchronized (clearRowList) {
						for (CellHighlight ch : highlighters) {
							boolean needRedraw = false;
							if (ch.alive) {
								needRedraw = ch.age(now);
							}
							// checks machen: 
							if (!ch.alive) {				// 1) wenn die Highligh-Zeit rum ist: löschen
								highMap.remove(ch.key);		// damit wird das nächste paintCell() die Zelle wieder NORMAL zeichnen
							}
							if (needRedraw) {				// muss die Zelle neu gezeichnet werden ?
								clearRowList.add(ch);		// wir müssen die ganze Zeile invalidieren!
							}
							if (!ch.alive) {
								deadCount++;
							}
						}
					}
					// Jetzt: WENN's was zum neu-zeichnen gibt, muss das im SWT-Thread laufen
					if (clearRowList.size()>0) {
						try {
							core.asyncExec(new Runnable() {
								public void run() {
									synchronized (clearRowList) {
										try {
											// hier kann's mal knallen, warum auch immer... => IndexOutOfBounds
											int max = table.getItemCount();
											for (CellHighlight ch : clearRowList) {
												if (ch.rowIdx<max) {
													table.clear(ch.rowIdx);
												}
											}
										} catch (Exception e) {		// kann knallen, zB beim App-shutdown: table ist dann schon DISPOSED
											System.err.println(e.toString());
										}
										clearRowList.clear();
									}
								}
							});
						} catch (Exception e) {
							// kann knallen beim Shutdown: die Table-activity läuft noch
							// aber core.display ist schon disposed!
							return;		// dann beende Thread!
						}
					}
					
					// Aufräumen ?
					if (deadCount>20) {
						List<CellHighlight> temp = new ArrayList<CellHighlight>();
						for (CellHighlight ch : highlighters) {
							if (ch.alive) {
								temp.add(ch);
							}
						}
						highlighters.clear();
						highlighters = temp;		// Austauschen
					}
				}		// sync(highlighters)
			}	// while 
		}
	}


	/**
	 * der callback wird hier ausgelagert, da er 
	 *  - per mouse-click auf eine Zelle
	 *  - oder per Up/Down ausgelöst werden kann
	 *  
	 *  für normale Clicks: colIdx ist gesetzt (einfach-Click oder double-Click)
	 *  
	 * @param colIdx -1 bei Up/Down, sonst die Spalte 0..n
	 * 
	 * @param e - bei Doppel-Klicks kommen die Events doppelt -> wir müssen nach der event-Time filtern
	 * 
	 * PROBLEM: 
	 * - Mouse-Click und Widge-Selected feuern BEIDE hier her
	 * - ich brauch aber beide "Quellen", weil a) Clicks und b) Cursor-Zeilen-Selects beim Adapter ankommen sollen 
	 *    (und zwar in der gleichen Methode)
	 *    
	 * => bei CLICK auf Zeile kämen da aber BEIDE an
	 * => Daher: Filtere anhand der Event-Time 
	 * 
	 */
	protected void fireOnCell(TableItem item, final int colIdx, final boolean isDlbClick, TypedEvent e) {
		if (e!=null) {
			if ((lastEventTime-e.time)==0) {
				return;
			}
			lastEventTime = e.time;
		}
		
        synchronized (rows) {
        	final int rowIdx = getRowIndex(item);
			final Row row = rows.get(rowIdx);
			curSelectedRow = row;					// aktuelle Zeile merken, für onKey()
        	// mache den callback ASYNCHRON, denn das hier läuft im SWT-Thread !!
        	core.executeTask(new Task() {
				@Override
				public void process() {
					adapter.onMouseClick(row.data, colIdx, rowIdx, isDlbClick);
				}
			});
		}
	}

	/**
	 * setzt die Spalten-Breiten so wie in {@link #maxWidthOfColumn} gesetzt
	 */
	public void resizeAllColumns() {
		int idx = 0;
		for (TableColumn tc : table.getColumns()) {
			int w = maxWidthOfColumn[idx];
			if (w>0) {		// NUR wenn auch was da ist!
				if (minColWidth[idx]>0) {
					tc.setWidth(minColWidth[idx]);
				} else 
				// mache breite Text-Spalten nicht ganz so breit
				if (w > 25) {
					tc.setWidth(20 + w*5);
				} else {
					tc.setWidth(20 + w*6);
				}
			}
			idx++;
		}
	}

	/**
	 * Arbeits-Callback für {@link GenTable2#findRow(RowFinder)}
	 */
	public interface RowFinder<T> {
		/**
		 * für jede Row/Daten-Object: passt die Zeile ?
		 * 
		 * @return true wenn ja
		 */
		boolean check(T data);
	}

	/**
	 * Use-Case: ich will eine bestimme Row/Daten-Object haben und 
	 * weiss (warum auch immer) nicht welche es ist:
	 * - also muss ich alle durchsuchen nach einem bestimmten Wert
	 *  
	 * @param finder - der Such-Arbeiter
	 * @return eine Liste der passenden Rows (kann auch leer sein)
	 */
	public List<T> findRow(RowFinder<T> finder) {
		List<T> result = new ArrayList<T>();
		synchronized (rows) {
			for (Row r : rows) {
				T data = r.data;
				if (finder.check(data)) {
					result.add(data);
				}
			}
		}
		return result;
	}
	
	
	/**
	 * Low-Level-Zugriff: 
	 * - jemand will alle Rows haben
	 * - das erzeugt eine KOPIE der internen Liste
	 */
	public List<T> getAllRows() {
		synchronized (rows) {
			List<T> result = new ArrayList<T>(rows.size());
			for (Row r : rows) {
				result.add(r.data);
			}
			return result;
		}
	}
	
	/**
	 * wen's interessiert... 
	 * 
	 * @return aktuelle Liste der Spalten
	 * 
	 * Achtung: das sind die ORGINAL-ColumnHandler-Objs, nix dran drehen, gelle!!!
	 */
	public List<ColumnHandler> getAllColumns() {
		int n = colList.length;
		List<ColumnHandler> result = new ArrayList<>(n);
		for (int i=0; i<n; i++) {
			result.add(colList[i]);
		}
		return result;
	}
	
	/**
	 * User-Code will Tasten mitbekommen
	 */
	public static interface IKeyAware<T> {
		/**
		 * Kommt nach Tastendruck
		 * 
		 * ACHTUNG: d+Shift kommt als D an (und shiftDown=true)
		 * 
		 * - diese Call läuft IM SWT-Thread!
		 * 
		 * 
		 * @param row  - auf dieser Zeile stehen wir gearde
		 * @param key  - die Taste
		 * @param shiftDown - true: Shift ist auch gedrückt
		 * @param strgDown  - true: Ctrl/Strg ist auch gedrückt
		 * @param keyCode - {@link KeyEvent#keyCode}, zB für ARROW_UP/DOWN, ...
		 */
		public void onKey(T row, char key, boolean shiftDown, boolean strgDown, int keyCode);
	}
	
	/**
	 * Wenn die Table Menus haben soll:
	 */
	public static interface IContextMenuAware<T> {

		/**
		 * User will ein Menu auf dieser Zeile öffnen
		 * - der Handler muss sagen welche Menus gezeigt werden sollen
		 * 
		 * @return null wenn KEIN Menu hochkommen soll
		 */
		public MenuDefinition defineMenu(T row, int colIdx);
		
		/**
		 * der User hat auf dieser Zeile, auf die Spalte dieses Menu geclickt
		 * 
		 * - dieser Call läuft IM SWT-Thread!
		 */
		public void onMenuClick(T row, int colIdx, Object menuCode);
	}
	
	/**
	 * Wrapper für ein Table-Context-Menu mit allen Einträgen
	 */
	public static class MenuDefinition {

		public static final String SEP = "SEP";
		
		public List<String> labels = new ArrayList<String>();
		public List<Object> vals = new ArrayList<Object>();
		
		private String unique = null;
		
		public Menu menu;
		
		/**
		 * constructor, Bsp MenuDefinition("Mach das", 1, "Mach jenes", handlerRef, "Mach das 3.", "three-code")
		 * 
		 * @param definitions: Paare aus [Label+Code]
		 */
		public MenuDefinition(Object... definitions) {
			int i=0;
			unique = "";
			while (i<definitions.length) {
				String key = (String) definitions[i++];
				Object val = definitions[i++];
				labels.add(key);
				vals.add(val);
				unique += key + "-" + val;
			}
		}
		/**
		 * Pärchenweise: [label1, code1, label2, code2, ...]
		 */
		public MenuDefinition(List<String> definitions) {
			int i=0;
			unique = "";
			int n = definitions.size();
			while (i<n) {
				String key = definitions.get(i++);
				Object val = definitions.get(i++);
				labels.add(key);
				vals.add(val);
				unique += key + "-" + val;
			}
		}

		/**
		 * Diese Constructor für die Verwendung mit 
		 */
		public MenuDefinition() {
		}
		/**
		 * Einzel-Aufbau, chainable
		 */
		public MenuDefinition addMenu(String label, Object callback) {
			labels.add(label);
			vals.add(callback);
			unique = null;			// reset falls "gemischte Art"
			return this;
		}
		public MenuDefinition addSeparator() {
			labels.add(SEP);
			vals.add(SEP);
			return this;
		}
		/**
		 * liefere den eindeutigen Key: "Mach das-1-Mach-jenes-ref.toString()-mach-das 3.-three-code"
		 */
		public String getUnique() {
			if (unique==null) {
				unique = "";
				for (int i=0; i<labels.size(); i++) {
					unique += labels.get(i) + "-" + vals.get(i);
				}
			}
			return unique;
		}
		@Override
		public String toString() {
			return unique;
		}

        /**
         * packe die andere Einträge VOR meine
         */
        public void mergeOnTop(MenuDefinition def, boolean withSeparator) {
            List<String> labels2 = new ArrayList<String>(def.labels);
            List<Object> vals2 = new ArrayList<Object>(def.vals);
            if (withSeparator) {
                labels2.add(SEP);   vals2.add(SEP);
            }
            labels2.addAll(this.labels);
            vals2.addAll(this.vals);
            this.labels = labels2;
            this.vals = vals2;
        }
	}

    public interface IGenTableDefaultMenuBuilder {
        MenuDefinition defineDefaultMenu();
    }

    /**
     * Wenn ein Rechts-Click nicht auf einer Row landet => lass diesen Builder die menu-Dinge machen
     */
    public void addDefaultMenuBuilder(IGenTableDefaultMenuBuilder builder) {
        this.defaultMenu = builder;
    }



    /**
	 * - lasse den Menu-Handler bestimmen WAS zu zeigen ist
	 * - dann zeige das Menu 
	 * - und warte bis es wieder zu ist
	 */
	protected void buildMenu(MouseEvent e) {
		// finde die Zeile unter der Maus:
		Point pt = new Point(e.x, e.y);
		// pt = table.toControl(pt);
		// Übersetze den Click auf eine Zelle in Zeile und Spalte
		TableItem item = table.getItem(pt);		// liefert immer das erste Item der Zeile

        MenuDefinition def = null;
        int column = -1;
        T data = null;

		if (item!=null) {
            // finde die richtige Spalte
            for (int i=0, n=table.getColumnCount(); i<n; i++) {
                Rectangle rect = item.getBounds(i);
                if (rect.contains(pt)) {
                    column = i;
                    break;
                }
            }
            // finde die Row/Daten zum Item
            int index = getRowIndex(item);
            if (index >= rows.size()) {
                return; // habe noch keine Daten
            }
            Row row = rows.get(index);

            // jetzt frage ab OB und WAS gezeigt werden soll
            data = row.data;
            def = menus.defineMenu(data, column);
		} else {
            // default menu ?
            if (defaultMenu!=null) {
                def = defaultMenu.defineDefaultMenu();
            }
        }
        if (def==null) {
            return;			// null -> User will nix zeigen für diese Zeile/Spalte und auch kein Default-Menu
        }

        // jetzt baue das SWT-Menu zur Definition auf: verwende einen Cache, da idR immer das gleich Menu kommt
        String defKey = def.getUnique();
        MenuDefinition cached = contextMenuCache.get(defKey);
        if (cached==null) {
        	contextMenuCache.put(defKey, def);
        	// baue die SWT-Dinge
			Menu menu = new Menu(table); //., SWT.POP_UP);
			for (int i=0; i<def.labels.size(); i++) {
				String label = def.labels.get(i);
				Object code  = def.vals.get(i);
				// check auf Seperator
				if (label==MenuDefinition.SEP) {		// hier bewusst obj-ref!
					new MenuItem(menu, SWT.SEPARATOR);
					continue;
				}
				MenuItem mi = new MenuItem(menu, SWT.PUSH);
				mi.setText(label);
				mi.setData(code);
				mi.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						menus.onMenuClick(curMenuData, curMenuColumn, e.widget.getData());
					}
				});
			}
			def.menu = menu;
        } else {
        	def = cached;		// verwende den Cache
       	}
        Menu menu = def.menu;
        curMenuData   = data;
        curMenuColumn = column;		// refs setzen, leer bei Default-Menu
        
        // jetzt zeige das Menu
        pt = table.toDisplay(e.x, e.y);		// table-relativ -> screen-absolute
		menu.setLocation(pt.x-10, pt.y-10);
		menu.setVisible(true);
		Display display = menu.getDisplay();
		while (!menu.isDisposed() && menu.isVisible()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		// hier: menu ging zu
		menu.setVisible(false);		// KEIN Dispose, das Wiederverwendung
		
		curMenuData = null;		// reset
		curMenuColumn = -1;
	}

	
	public static interface IRowAccess<T> {
		public int size();
		public T get(int at);
	}
	/**
	 * Der User-Code kann sich an die Zeilen-Sortierung hängen:
	 * 
	 * - UseCase-BSP: ich habe eine dynamische Summen-Spalte, die 
	 *   UNABHÄNGIG von der Zeilen-Sortierung IMMER eine andere Spalte auf-summiert.
	 */
	public static interface IPostSortListener<T> {

		/**
		 * der call kommt NACH dem Sortieren:
		 * - die Tabelle hat bereits adapter.sortCell(...) gemacht
		 * - und sich komplett ge-CLEAR-t. 
		 * - der Adapter-Code kann also HIER jetzt Daten verändern
		 * - Die Tabelle wird NACH diesem Call ALLE Zeilen NEU anfordern, adapter.renderCell(...)
		 */
		public void postSort(int colIdx, int swtUpOrDown, IRowAccess<T> access);
		
	}
	

	
	/**
	 * wenn jemand eine Spalte/Zelle selber zeichnen will
	 */
	public static interface ICustomColumnPainter<T> {
		/**
		 * Zeichne die Zelle der Zeile
		 * 
		 * @param data - diese Row
		 * @param gc - damit zeichnen
		 * @param isSelected - true: Zeile ist gerade ausgewählt
		 * 
		 * @return true wenn der User-Code selbst Hand angelegt hat, false: die GenTable wird die Zelle ganz normal zeichnen
		 */
		public boolean paint(T data, GC gc, int x, int y, int width, int height, boolean isSelected);
	}

	/**
	 * UserCode sagt: diese Spalte will ich mit diesem Callback selber zeichnen
	 */
	@SuppressWarnings("unchecked")
	public void setCustomPaintColumn(int colIdx, ICustomColumnPainter<T> painter) {
		if (colList==null) {
			throw new ShittyCodeException("Call only AFTER configure-columns!");
		}
		if (customPainters==null) {
			customPainters = new ICustomColumnPainter[colList.length];
		}
		customPainters[colIdx] = painter;
	}
	
	/**
	 * Definiere Spalte x: 
	 * - wenn der User eine Taste drückt:
	 * - dann springe auf den ersten oder nächsten Eintrag der mit diesem Buchstaben beginnt
	 */
	public void setKeyJumpToCol(int x) {
		keyJumpCol = x;
	}
	
	/**
	 * User hat zB ein "s" gedrückt: 
	 * - springe auf die erste Zeile die mit "s" oder "S" beginnt
	 * - wenn ich schon auf so einer Zeile stehe: gehe zur nächsten 
	 *   (wenn noch eine mit "s" da ist)
	 */
	protected void handleJumpToKey(char key) {
		if (curSelectedRow==null) {
			return;		// Fehler: kann eigentlich gar nicht passieren, selbst wenn die Tabelle den Focus bekommt gibt's ne selected-row
		}
		if (keyJumpCol<0 || keyJumpCol>=colList.length) {
			return;
		}
		String k = String.valueOf(key).toUpperCase();
		ColumnHandler colHdl = colList[keyJumpCol];
		// gehe alle Rows durch und suche die nächste passende 
		// !! gehe immer die Rows durch: es kann Rows geben die noch TableItem haben <- Virtual Table!
		TableItem curItem = curSelectedRow.item;
		if (curItem==null) {			// sicher ist sicher
			return;
		}
		int curIdx = getRowIndex(curItem);		// zB 5
		// System.out.println("-- start at row " + curIdx);
		// Versuch 1: gehe alle FOLGENDEN durch 
		for (int i=curIdx+1; i<rows.size(); i++) {
			Row r = rows.get(i);
			String s = adapter.renderCell(r.data, colHdl);
			// System.out.println(i + " check: " + s);
			if (s.length()>0) {
				String c = s.substring(0, 1).toUpperCase();
				if (c.equals(k)) {
					int delta = i-curIdx;
					// verwende den Row-Mover um dahin zu kommen
					new RowChanger(delta, 1).run();
					return;		// fertig!
				}
			}
		}
		// hier: habe nichts gefunden in Richtung "nach unten"
		// also probier's von Anfang an
		for (int i=0; i<curIdx; i++) {
			Row r = rows.get(i);
			String s = adapter.renderCell(r.data, colHdl);
			// System.out.println(i + " check: " + s);
			if (s.length()>0) {
				String c = s.substring(0, 1).toUpperCase();
				if (c.equals(k)) {
					int delta = i-curIdx;
					// verwende den Row-Mover um dahin zu kommen
					new RowChanger(delta, 1).run();
					return;		// fertig
				}
			}
		}
	}	// handleJumpToKey()
	
	/**
	 * Zeige diese Zeile oben an
	 */
	public void setTopIndex(final int idx) {
		if (core.isSwtThread()) {
			table.setTopIndex(idx);
		} else {
			core.asyncExec(new Runnable() {
				public void run() {
					table.setTopIndex(idx);
				}
			});
		}
	}
	/**
	 * Ändert einen Spalten-Breite per code
	 */
	public void setColumnWidth(int colIdx, int width) {
		TableColumn col = table.getColumn(colIdx);
		col.setWidth(width);
	}

}


























