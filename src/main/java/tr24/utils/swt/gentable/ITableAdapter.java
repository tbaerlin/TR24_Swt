package tr24.utils.swt.gentable;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TableItem;


/**
 * so redet/füttert/hört der user-code auf eine {@link GenTable2}
 */
public interface ITableAdapter<T> {

	/**
	 * Der Adapter muss die Daten jeder Spalte rendern können
	 */
	public String renderCell(T row, ColumnHandler col);
	
	/**
	 * Sortiere die Daten einer Spalte
	 * @param ch - diese Spalte
	 * @param sortDirection - 1 oder -1 Wichtig damit der Sortierer zB leere Cellen "ausblenden" kann
	 * @param r1 - Row 1
	 * @param r2 - Row 2
	 * @param item1 - der Sortierer hat DIREKTEN Zugriff auf den String-Inhalt der Zelle 
	 * @param item2 - wenn er zB direkt diesen Inhalt vergleichen will
	 */
	public int sortCell(ColumnHandler ch, int sortDirection, T r1, T r2, TableItem item1, TableItem item2);
	
	/**
	 * Der User hat auf eine Zelle geklickt
	 * 
	 * ACHTUNG: hier kommen einfache und doppel-Clicks! vor dem double-click kommt auch noch ein einfacher!
	 * 
	 * - der Call IST schon in einem Task ge-wrap-t sein und kann somit auch rechenintensiv werden
	 * - call ist also NICHT SWT
	 * 
	 * @param columnIdx - auf dieser Spalte: 0..n, -1 wenn's per Tastatur war
	 * @param rowIdx    - nur der Vollständigkeit halber: das ist die Zeile x in der Tabelle
	 * @param isDlbClick - false für single-mouse click
	 */
	public void onMouseClick(T row, int columnIdx, int rowIdx, boolean isDlbClick);

	public Control getTableControl();

	/**
	 * Muss {@link GenTable2#resizeAllColumns()} triggern
	 */
	public void resizeTable();
	
}
