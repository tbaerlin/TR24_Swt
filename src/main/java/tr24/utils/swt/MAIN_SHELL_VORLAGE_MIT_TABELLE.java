package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import tr24.utils.swt.SwtUtils.LAYOUT;
import tr24.utils.swt.api.IOnButtonClick;
import tr24.utils.swt.api.IShutdownShell;
import tr24.utils.swt.gentable.ColumnHandler;
import tr24.utils.swt.gentable.GenTable2;
import tr24.utils.swt.gentable.ITableAdapter;

import java.io.File;

/**
 * HAUPT-Einstieg
 */
public class MAIN_SHELL_VORLAGE_MIT_TABELLE {

	private static final String CONF_FILE = "some.conf";
	
	private Shell shell;

	LogTable logTab;

	private MyCore core;

	/**
	 * Ref auf einen Row in der Dev/Log-Tabelle
	 */
	public interface ILogRow {

		public void updateInfo(String info);
		
	}
	
	/**
	 * Der kommt später in EIGENE Klasse!
	 */
	public static class MyCore extends BasisCore {
		public LogTable logTab;

		/**
		 * constructor
		 */
		public MyCore(Display display) {
			super(display, "my-task-queue");
		}

		public ILogRow buildRow(String was, String id, String info) {
			return logTab.buildRow(was, id, info);
		}
	}
	

	/**
	 * START HERE
	 */
	public static void main(String[] args) {
		String confFile = CONF_FILE;
		if (args.length>0) {
			String s = args[0];
			if (s.startsWith("config=")) {
				s = s.substring(7);
				confFile = s;
			}
		}
		new MAIN_SHELL_VORLAGE_MIT_TABELLE().run(confFile);
	}

	
	private void run(String confFile) {
		Display display = new Display();
		core = new MyCore(display);
		// versuche conf über abs-path
		File f = new File(confFile);
		if (!f.exists()) {					// wenn nicht da -> da im aktuellen Verzeichnis
			f = new File(".", confFile);
		}
		ApplicationConfig conf = new ApplicationConfig(f);
		conf.loadConfigFile();
		
		shell = new Shell(display);
		shell.setText("DEV...");
		shell.setLayout(new FormLayout());
		
		core.mainShell = shell;
		core.appConfig = conf;
		
		Rectangle bounds = conf.getMainShellPosition(display.getMonitors());
		shell.setBounds(bounds);
		core.add2ShutdownShell(new IShutdownShell() {
			@Override
			public void shellShutdown(ApplicationConfig conf) {
				conf.setMainShellPosition(shell.getBounds());
			}
		});
		
		// höre auf Close der main-Shell:
		shell.addListener(SWT.Close, new Listener() {
			/** call kommt
			 * 1) vom User
			 * 2) vom Shutdown-Proc wenn alles rum ist
			 */
			@Override
			public void handleEvent(Event event) {
				core.triggerAppShutdown();
				if (!core.shellCanClose()) {
					event.doit = false;			// beim erste Durchgang: nöö, noch nicht
				}
			}
		});
		
		Composite boxOben = LAYOUT.layout_ObenLeiste(shell, 40, 3, FARBE.BLUE_5);
		LAYOUT.layout_button("Exit", 10, 5, 40, 25, boxOben, new IOnButtonClick() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});
		
		Composite boxUnten = new Composite(shell, SWT.NONE);
		boxUnten.setBackground(FARBE.YELLOW_5);
		boxUnten.setLayout(new FillLayout());
		LAYOUT.layout_useRestOfSpace(boxUnten, boxOben, 3);
		
		logTab = new LogTable(boxUnten);
		core.logTab = logTab;
		
		shell.open();
		
		core.buildRow("MAIN", "-", "Started");
		
		core.runSwtLoop(shell);
	}
	
	
	class LogRow implements ILogRow {
		public final String was;		// SIM, CLIENT, LOG
		public final String id;
		public String info;
		public LogRow(String was, String id, String info) {
			this.was = was;
			this.id = id;
			this.info = info;
		}
		@Override
		public void updateInfo(String info) {
			this.info = info;
			logTab.tab.pingCell(this, 2, GenTable2.HighlightSTYLE.SOLID_COLOR, FARBE.GREEN_1);
		}
	}
	
	
	class LogTable implements ITableAdapter<LogRow> {

		private GenTable2<LogRow> tab;

		public ILogRow buildRow(String was, String id, String info) {
			LogRow row = new LogRow(was, id, info);
			tab.addRow(row);
			return row;
		}

		
		/**
		 * constructor
		 */
		public LogTable(Composite parent) {
			tab = new GenTable2<LogRow>(parent, this, core, true);
			GenTable2.ColBuilder cb = tab.getColBuilder();
			cb.addText("Was", 80, SWT.LEFT, false);
			cb.addText("ID", 30, SWT.CENTER, false);
			cb.addText("Info", 500, SWT.LEFT, false);
			tab.configColumns(cb);
		}

		@Override
		public String renderCell(LogRow row, ColumnHandler col) {
			switch (col.columnIdx) {
				case 0: return row.was;
				case 1: return row.id;
				case 2: return row.info;
			}
			return null;
		}
		@Override
		public int sortCell(ColumnHandler ch, int sortDirection, LogRow r1, LogRow r2, TableItem item1, TableItem item2) {
			return 0;
		}
		@Override
		public void onMouseClick(LogRow dataObject, int columnIdx, int rowIdx, boolean isDlbClick) {
		}
		@Override
		public Table getTableControl() {
			return tab.getTableControl();
		}
		@Override
		public void resizeTable() {
		}
	}	// MS3DevTable

	
}













