package tr24.utils.swt.gentable;


/**
 * Helfer: erstellt den Code für eine Tabelle
 */
public class GenTableBuilder {

	public static final String RowClass = "InfoTableRow";
	public static final String TableClassName = "InfoTable";
	
	
	/**
	 * Baue den Code auf und gebe aus in sysout
	 */
	public static void main(String[] args) {
		System.out.println("private class " + TableClassName + " implements ITableAdapter<" + RowClass + "> {\n");
		System.out.println("\tprivate GenTable<" + RowClass + "> tab;\n");
		System.out.println("\t/**\n\t * constructor\n\t */");
		System.out.println("\tpublic " + TableClassName + "(Composite parent) {");
		System.out.println("\t\ttab = new GenTable<" + RowClass + ">(parent, this, core, true);");
		System.out.println("\t\tColBuilder cb = tab.getColBuilder();");
		System.out.println("\t\tcb.addText(\"name\", 200, SWT.LEFT, false);");
		System.out.println("\t\ttab.configColumns(cb);");
		System.out.println("\t}\n");
		System.out.println("\t@Override");
		System.out.println("\tpublic String renderCell("+RowClass+" row, ColumnHandler col) {");
		System.out.println("\t\tswitch (col.columnIdx) {");
		System.out.println("\t\t\tcase 0: return row.name;");
		System.out.println("\t\t\tcase 1: return row.ref;");
		System.out.println("\t\t}");
		System.out.println("\t\treturn null;");
		System.out.println("\t}\n");
		System.out.println("\t@Override");
		System.out.println("\tpublic int sortCell(ColumnHandler ch, int sortDirection, "+RowClass+" r1, "+RowClass+" r2, TableItem item1, TableItem item2) {");
		System.out.println("\t\treturn 0;");
		System.out.println("\t}\n");
		System.out.println("\t@Override");
		System.out.println("\tpublic void onMouseClick("+RowClass+" dataObject, int columnIdx, int rowIdx, boolean isDlbClick) {");
		System.out.println("\t}\n");
		System.out.println("\t@Override");
		System.out.println("\tpublic Table getTableControl() {");
		System.out.println("\t\treturn tab.getTableControl();");
		System.out.println("\t}\n");
		System.out.println("\t@Override");
		System.out.println("\tpublic void resizeTable() {");
		System.out.println("\t}\n");
		System.out.println("}\t// " + TableClassName);
	}
	
}
/*

private class MetaTable implements ITableAdapter<MetaRow> {

private GenTable<MetaRow> tab;

/**
 * constructor
 *
	public MetaTable(Composite parent) {
		tab = new GenTable<MetaRow>(parent, this, core, true);
		ColBuilder cb = tab.getColBuilder();
		cb.addText("name", 200, SWT.LEFT, false);
		tab.configColumns(cb);
	}
	
	@Override
	public String renderCell(MetaRow row, ColumnHandler col) {
		switch (col.columnIdx) {
			case 0: return row.name;
			case 1: return row.ref;
		}
		return null;
	}
	@Override
	public int sortCell(ColumnHandler ch, int sortDirection, MetaRow r1, MetaRow r2, TableItem item1, TableItem item2) {
		return 0;
	}
	@Override
	public void onMouseClick(MetaRow dataObject, int columnIdx, int rowIdx, boolean isDlbClick) {
	}
	
	@Override
	public Table getTableControl() {
		return tab.getTableControl();
	}
	@Override
	public void resizeTable() {
	}
	}	// MetaTable
*/

