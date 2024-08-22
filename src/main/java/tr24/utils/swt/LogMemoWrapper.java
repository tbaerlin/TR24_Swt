package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import tr24.utils.common.ThreadUtil;
import tr24.utils.scheduler.IScheduleClient;
import tr24.utils.scheduler.IScheduleContext;
import tr24.utils.scheduler.SchedulerService;
import tr24.utils.swt.SwtUtils.LAYOUT;
import tr24.utils.swt.api.IOnButtonClick;
import tr24.utils.swt.api.IShutdownShell;

/**
 * Kapselt eine "Console in SWT"
 *
 * - API kann einfach new-lines reinpusten
 * - intern handle ich das zyklsiche Update der GUI
 * 
 * 
 * INTERNES Handling:
 * - für die Gesamt-Size-Limitierung; geht so:
 * - jede neue Zeile mit add(..) kommt auf die Gesamt-Liste
 * - ich merke mir das als linked-Liste
 * - es gibt einen PING-er (entweder eigener Thread hier oder über globalen {@link tr24.utils.scheduler.SchedulerService}
 * - alle 100ms prüfe ich 
 *   - gibt's neue Dinge ?
 *   - wenn ja: check ob das Gesamt-Size-Limit drüber ist
 *      - wenn Limit gerissen: Schneide solange "vorne" Zeilen ab, bis es wieder passt
 *   - dann: erzeugen neuen Swt-Text-String
 * 
 */
public class LogMemoWrapper implements IShutdownShell {

	private static final int SLEEP = 200;		// update alle 200ms

	final BasisCore core;

	final Object LOCKER = new Object();
	boolean active = true;

	/**
	 * Anzeige-Memo
	 */
	private final Text text;

	private int nextLineNum = 1000;

	private final int maxSize;
	private final int maxLineLength;
	private final boolean showLineNumber;

	/**
	 * FIFO-Linked-List
	 */
	private LineObj first, last;

	/**
	 * Aktuelle Gesamt-Menge, kann/wird GRÖSSER werden als erlaubt (durch add() ), wird dann beim update() wieder kleiner
	 */
	private int curLinesSize = 0;

	/**
	 * quick-check Flag für {@link #checkUpdate()}
	 */
	private boolean doUpdate;
	
	private StringBuilder sb;
	private MemoUpdateSwt textUpdater;
	
	/**
	 * Constructor (in SWT) mit ALLEM:
	 * 
	 * @param pool 			- WENN gesetzt: Memo holt sich hier den "alle 100ms"-Ping; bei NULL: Eigener ping-Thread
	 * @param maxSize 		- wenn <b>(>0)</b>: LIMITIERE die ganzen Anzeige auf zB 1000*1000 bytes => vordere Zeilen fliegen dann raus
	 * @param maxLineLength - wenn <b>(>0)</b>: Begrenze jede addLine(..) auf diese Länge
	 * @param showLineNumber - true: zeige vorne mit an "[1234] "
	 */
	public LogMemoWrapper(Composite parent, BasisCore core, boolean withBorder, Color bgColor, Color fgColor, SchedulerService pool, int maxSize, int maxLineLength, boolean showLineNumber) {
		this.core = core;
		this.maxLineLength = maxLineLength;
		this.showLineNumber = showLineNumber;
		
		// maxSize (wenn verwendet!) : Muss immer grösser sein als maxLineLength: Damit ich immer (1+) LineObj in der Chain habe (sonst nullpointer)
		if (maxSize>0) {		// wenn verwendet: dann min 1kb
			maxSize = Math.max(1000, maxSize);
		}
		if (maxLineLength>0 && maxSize>0) {
			maxSize = Math.max(maxSize, maxLineLength+10);		// +10: sicher ist sicher
		}
		this.maxSize = maxSize;
		
		text = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		text.setBackground(bgColor);
		text.setForeground(fgColor);
		text.setFont(new Font(core.getDisplay(), "Consolas", 10, SWT.NORMAL));
		textUpdater = new MemoUpdateSwt(text);
		
		core.add2ShutdownShell(this);		// active = false, für alle Fälle
		
		// pool oder eigener?
		if (pool!=null) {
			pool.registerClient(new IScheduleClient() {
                @Override
                public void onSchedulePing(IScheduleContext ctx) {
                    checkUpdate();
                }
            }, SLEEP);
		} else {
			// eigener Thread
			Thread th = new Thread(()->{
				while (active) {
					ThreadUtil.sleepUnhandled(SLEEP);
					checkUpdate();
				}
			});
			th.setName("LogMemo.UpdateLoop");
			th.setDaemon(true);
			th.start();		// Läuft gleich los
		}
		this.sb = new StringBuilder(maxLineLength+1000);		// mal bisle drüber
		// immer: lege eine Dummy-Zeile an
		clear();
		
	}
	/**
	 * in SWT
	 */
	public LogMemoWrapper(Composite parent, BasisCore core, boolean withBorder, Color bgColor, Color fgColor) {
		this(parent, core, withBorder, bgColor, fgColor, null, 0, 0, true);
	}
	/**
	 * Einfache Version: Limit auf 1MB, 5KB-LineLen, zeige "[1234].."
	 */
	public LogMemoWrapper(Composite parent, BasisCore core) {
		this(parent, core, true, FARBE.BLUE_DARK_3, FARBE.WHITE, null, 1024*1000, 5*1000, true);
	}

	/**
	 * für's Layouten
	 */
	public Control getSwtControl() {
		return text;
	}
	@Override
	public void shellShutdown() {
		active = false;
	}
	public void setFormData(FormData fd) {
		text.setLayoutData(fd);
	}


	/**
	 * Füge Zeile ans Memo-Ende
	 * scrolle gleich dahin
	 */
	public void addLine(String line) {
		if (!active) {
			return;			// nach SWT-close: nehme nichts mehr an
		}
		synchronized (LOCKER) {
			// packe die Zeile "ready for display" gleich auf den Stack:
			if (maxLineLength>0) {
				if (line.length()> maxLineLength) {
					line = line.substring(0, maxLineLength) + "...";
				}
			}
			if (showLineNumber) {
				line = "[" + (nextLineNum++) + "] " + line;
			}
			LineObj next = new LineObj(line, null);
			last.next = next;			// append
			last = next;	
			curLinesSize += line.length();		// führe Gesamt-Menge mit
			doUpdate = true;
		}
	}

	/**
	 * Lösche alle Zeilen
	 */
	public void clear() {
		synchronized (LOCKER) {
			LineObj lo = new LineObj("", null);
			this.first = lo;
			this.last = lo;
			this.curLinesSize = 0;
			doUpdate = true;
		}
	}

	/**
	 * Linked-List-Eintrag
	 */
	static class LineObj {
		final String line;
		LineObj next;
		public LineObj(String line, LineObj next) {
			this.line = line;
			this.next = next;
		}
	}
	

	/**
	 * Hier pingt's (vom TimePingPool oder von mir selber)
	 */
	void checkUpdate() {
		if (!doUpdate) {
			return;			// ich spare mir den LOCKER wenn möglich
		}
		synchronized (LOCKER) {
			
			// 1) wenn es was neues gibt: Check die Gesamt-Länge
			if (maxSize>0 && curLinesSize>maxSize) {					// muss ich vorne-abschneiden ?
				while (curLinesSize > maxSize) {
					int cutSize = first.line.length();			// Länge des 'first'-Eintrags, zb 543 chars
					curLinesSize -= cutSize;					// 543 weniger
					first = first.next;							// kick 1st			!! HIER ist's WICHTIG, dass die maxSize immer größer als eine Zeile ist => sonst first==null 
				}
			}
			
			// 2) jetzt RE-erzeuge den gesamten String für das Text-Feld
			LineObj lo = first;
			do {
				sb.append(lo.line);		sb.append("\r\n");
				lo = lo.next;
			} while (lo!=null);
			
			textUpdater.newFull = sb.toString();		// packe da hin
			sb.setLength(0);		// gleich clear'en
			
			core.asyncExec(this.textUpdater);			// hihi, nehme immer die gleiche Instanz
			doUpdate = false;
		}
	}	// checkUpdate


	/**
	 * in SWT:
	 * - update den Text
	 * - scrolle da hin
	 */
	class MemoUpdateSwt implements Runnable {
		private Text myText;
		/**
		 * etwas dirty: kommt einfach so rein, race-technisch könnte auch schon der nächsten Update-String kommen, noch vor dem run()-in-SWT
		 */
		public String newFull;

		/** constructor */
		public MemoUpdateSwt(Text text) {
			this.myText = text;
		}
		/**
		 * in SWT jetzt
		 */
		@Override
		public void run() {
			if (!active || myText.isDisposed()) return;
			String s = newFull;		// cache mal weg
			myText.setText(s);
			int n = s.length();
			text.setSelection(n, n);
		}

	}	// MemoUpdateSwt


	
	/**
	 * Swt-Test-Runner
	 */
	static class DEV_LogMemo {
		private LogMemoWrapper memo;

		public static void main(String[] args) {
			new DEV_LogMemo().run();
		}

		private void run() {
			Display display = new Display();
			BasisCore core = new BasisCore(display);
			Shell shell = new Shell(display);
			shell.setText("LogMemo");
			core.mainShell = shell;
			shell.setBounds(100, 100, 500, 500);
			shell.setLayout(new FormLayout());
			Composite boxOben = LAYOUT.layout_ObenLeiste(shell, 40, 3, FARBE.BLUE_5);
			LAYOUT.layout_button("Exit", 10, 5, 40, 25, boxOben, new IOnButtonClick() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					shell.close();
				}
			});
			Composite boxUnten = new Composite(shell, SWT.NONE);
			// boxUnten.setBackground(FARBE.YELLOW_5);
			boxUnten.setLayout(new FillLayout());
			LAYOUT.layout_useRestOfSpace(boxUnten, boxOben, 3);
			
			int maxLine = 17;
			int maxSize = 500;
            SchedulerService sched = new SchedulerService("test scheduler");
			// TimePingPool pool = new TimePingPool("test time ping pool");
			
/*DEV*/	memo = new LogMemoWrapper(boxUnten, core, true, FARBE.BLUE_DARK_2, FARBE.WHITE_BLUE_ish, sched, maxSize, maxLine, true);
			
			// pool.startPinging();		// das muss von aussen kommen

			shell.open();
			core.exeNow(()->{
				logMemoDevStuff();
			});
			
			core.runSwtLoop(shell);
		}

		void logMemoDevStuff() {
			// idee: Ballere viel rein; LogMemo muss "begrenzen"
			for (int i=0; i<10000; i++) {
				String line = "Line " + i + "," + i*2 +"," + i*3 +"," + i*4;
				memo.addLine(line);
				ThreadUtil.sleepUnhandled(8);
			}
		}
	}	// Dev ding

}

















































