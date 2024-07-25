package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import tr24.utils.common.ThreadUtil;
import tr24.utils.common.TimeUnit;
import tr24.utils.swt.api.IProgressMonitor;

/**
 * Wrapper/Handler/Manager für die Anzeige von Progress-Monitoren
 * - hier: zeige eine Bar an. Position ist in der main-Shell oben links
 * - wir bauen eine eigene Shell, so dass die Anzeige immer geht
 * 
 */
public class ProgressMonitorManager {

	private final Display display;
	private final Shell parentShell;
	private Monitor curMon;
	protected Shell ms;
	protected ProgressBar bar;
	protected ProgressBar myBar;
	

	/**
	 * constructor
	 * @param parentShell - die Monitor-Shell orientiert sich an der Postion der Haupt-Shell
	 */
	public ProgressMonitorManager(Shell parentShell) {
		this.parentShell = parentShell;
		display = parentShell.getDisplay();
	}

	/**
	 * Liefere einen Monitor
	 * 
	 * @param stepCount - wieviele Steps werden's denn ? 
	 * 
	 * @return immer was, auch wenn nur einer zur Anzeige kommt
	 */
	public synchronized IProgressMonitor getNextMonitor(int stepCount) {
		if (parentShell==null || curMon!=null) {
			return new Monitor(0);		// dummy version
		}
		Monitor m = new Monitor(stepCount);
		m.go();
		// curMon = m;
		return m;
	}

	/**
	 * die Shell lebt zentral
	 * - kann wiederverwendet werden
	 */
	protected void showMonitor(final int stepCount, final int nowStep) {
		display.asyncExec(new Runnable() {
			public void run() {
				if (ms==null) {
					ms = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
					ms.setLayout(new FillLayout());
					bar = new ProgressBar(ms, SWT.HORIZONTAL);
					bar.setMinimum(0);
				}
				Rectangle r = parentShell.getBounds();
				int x = r.x  + r.width/2 - 150; 
				int y = r.y + 3;
				bar.setMaximum(stepCount);
				bar.setSelection(nowStep);		// zeige schon mal den aktuellen an
				ms.setBounds(x, y, 300, 20);
				ms.open();
				myBar = bar; 		// jetzt ist die ProgressBar verfügbar
			}
		});
	}
	
	/**
	 * verstecke die Shell wieder
	 */
	protected void monitorDone() {
		display.asyncExec(new Runnable() {
			public void run() {
				ms.setVisible(false);		// nur verstecken, KEIN Close!
				curMon = null;				// reset -> ab JETZT kann ein neuer ran
			}
		});
	}



	/**
	 * eine konkrete Instanz
	 * - wenn's losgeht: merke die Start-Zeit. Erst nach 1sec zeige die Shell mit dem Monitor
	 *   (damit können super schnelle Tasks auch nix anzeigen)
	 * - der Loop überwacht ob sich der step geändert hat und macht dann den Update
	 *   (wir machen das per Loop, so dass viele schnelle step() nur zu einem grossen SWT-call führen)
	 */
	private class Monitor extends Thread implements IProgressMonitor {

		public final int stepCount;
		/**
		 * wo sind wir gerade ?
		 */
		public int curStep;
		
		private int lastStep = -1;
		
		private boolean finished;
		
		/**
		 * - ist gesetzt nach {@link #done()}
		 * - Bedeutung: lösche die Shell spätestens nach dieser Zeit
		 */
		private long kickTime = Long.MAX_VALUE;
		
		/**
		 * constructor
		 */
		public Monitor(int stepCount) {
			this.stepCount = stepCount;
		}

		@Override
		public void step() {
			curStep++;
		}

		@Override
		public void step(int nowWeAreHere) {
			curStep = nowWeAreHere;
		}

		/**
		 * fertig. Verstecke den Monitor nach 1sec
		 */
		@Override
		public void done() {
			//System.out.println("monitor done() called");
			finished = true;
			kickTime = System.currentTimeMillis() + 500;
			System.out.println("Kick time @ " + new TimeUnit(kickTime));
		}

		/**
		 * initialisiere und starte den Überwachungs-Loop
		 */
		public void go() {
			setDaemon(true);
			setName("ProgressMonitor");
			start();
		}
		
		/**
		 * nur der aktive Monitor hat einen laufenenden Überwachungs-Thread
		 */
		@Override
		public void run() {
			ThreadUtil.sleepUnhandled(100);		// warte 1sec bis die Shell hochkommt
			if (finished) {
				//System.out.println("Monitor: war zu schnell, gehe gleich schlafen");
				return;
			}
			// ok, zeige den Monitor
			showMonitor(stepCount, curStep);		// das machen wir zentral -> Shell + Bar wiederverwenden
			
			while (true) {
				if (myBar!=null) {
					final int nowStep = curStep;
					if (nowStep != lastStep) {
						display.asyncExec(new Runnable() {
							public void run() {
								myBar.setSelection(nowStep);
								lastStep = nowStep;
							}
						});
					}
					lastStep = nowStep;		// curStep könnte sich idZ schon wieder geändert haben
				}
				if (!finished && curStep > stepCount) {
					done();
				}
				// prüfe ob wir done() sind
				long time = System.currentTimeMillis();
				if (time > kickTime) {
					monitorDone();
					//System.out.println("monitor fertig");
					return;		// ich habe fertig
				}
				ThreadUtil.sleepUnhandled(200);
			}	// check-loop
		}	// run
		
	}		// class Monitor


	

}









