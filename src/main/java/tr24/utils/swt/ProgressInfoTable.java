package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TableItem;
import tr24.utils.swt.apprunenv.Tr24GuiCore;
import tr24.utils.swt.gentable.ColumnHandler;
import tr24.utils.swt.gentable.GenTable2;
import tr24.utils.swt.gentable.ITableAdapter;


/**
 * Hilfs-GUI-DING für die Anzeige von "hier bewegst sich was und wir haben gerade 45% erledigt" 
 * 
 * - Zeigt eine Tabelle mit Spalten "Context" und "Status"
 * - User-Code kann sich einfach eine Zeile erzeugen und die 
 *   mit Infos zuballern
 * - wenn der Vorgang fertig ist, kann der User-Code die Zeile einfach löschen
 * 
 * 
 * Diese Anzeige ist PRAKTISCH wenn parallel mehrere Vorgangs-Dinge laufen können
 *   (zB beim Mega-Sim: viele *.sim werden geladen und das dauert halt etwas....)
 * 
 *
 *
 * Die Klasse IST ein {@link Composite}, der erzeugende Code kann also direkt layout-en
 * 
 * @author tbaer
 *
 */
public class ProgressInfoTable extends Composite implements IProgressInfoTable {

	
	// private MyTable tab;
	GenTable2<MyRow> tab;
	
	private boolean insertNewLinesOnTop;

	/**
	 * constructor
	 * 
	 * @param insertNewLinesOnTop false=append, true:add-on-top
	 */
	public ProgressInfoTable(Composite parent, Tr24GuiCore core, boolean insertNewLinesOnTop) {
		super(parent, SWT.NONE);
		this.insertNewLinesOnTop = insertNewLinesOnTop;
		setLayout(new FillLayout());		// die Tabelle wird alles ausfüllen
		new MyTable(core);
	}

	/**
	 * Tabellen-Anzeige-Obj und interface-impl
	 */
	class MyRow implements IInfoRow {
		
		public final String context;
		public String status;
		
		/**
		 * wenn true: ignoriere alle weiteren calls()
		 */
		private boolean done = false;
		
		/** constructor */
		public MyRow(String context, String status) {
			this.context = context;
			this.status = status;
		}
	
		@Override
		public void update(String status) {
			if (done) { return; }
			this.status = status;
			tab.updateRow(this, false);
		}
		@Override
		public void remove() {
			if (done) { return; }
			done = true;
			tab.deleteRow(this);
		}
		@Override
		public IInfoRow showClock(boolean onOff) {
			if (done) { return this; }
			System.err.println("IInfoRow.showClock() imp me !!");
			return this;
		}
		
	}	// eine Zeile
	
	/**
	 * Kapsle die Adapter-Methode nicht-sichtbar-für-aussen
	 */
	class MyTable implements ITableAdapter<MyRow> {
		
		public MyTable(Tr24GuiCore core) {
			tab = new GenTable2<>(ProgressInfoTable.this, this, core, true);
			GenTable2.ColBuilder cb = tab.getColBuilder();
			cb.addText("Context", 150, SWT.LEFT, false);
			cb.addText("Status", 300, SWT.LEFT, false);
			tab.configColumns(cb);
		}
		@Override
		public String renderCell(MyRow row, ColumnHandler col) {
			if (col.columnIdx==0) {
				return row.context;
			} 
			return row.status;
		}
		
		@Override
		public Control getTableControl() {
			return null;
		}
		@Override
		public void onMouseClick(MyRow row, int columnIdx, int rowIdx, boolean isDlbClick) {
		}
		@Override
		public void resizeTable() {
		}
		@Override
		public int sortCell(ColumnHandler ch, int sortDirection, MyRow r1, MyRow r2, TableItem item1, TableItem item2) {
			return 0;
		}
	}	// Table-Wrapper
	
	
	/*
	 * (non-Javadoc)
	 * @see tr.basics.gui.IProgressInfoTable#getRow(java.lang.String, java.lang.String)
	 */
	@Override
	public IInfoRow show(String context, String status) {
		if (context==null) {
			context = "??";
		}
		if (status==null) {
			status = "";
		}
		MyRow row = new MyRow(context, status);
		if (insertNewLinesOnTop) {
			tab.addRowFirst(row, false);
		} else {
			tab.addRow(row);
		}
		return row;
	}
	
}



















