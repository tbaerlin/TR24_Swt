package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import tr24.utils.annotations.Nullable;
import tr24.utils.common.*;
import tr24.utils.swt.api.IAlarmManager;
import tr24.utils.swt.api.IProgressMonitor;
import tr24.utils.swt.api.IShutdownHook;
import tr24.utils.swt.api.IShutdownShell;

import java.io.File;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BASIS Core-Version, kann nur die Basis-Dinge
 * - idR werden alle APPS eine Ableitung machen 
 * - und ihre Dinge dazu-hängen
 */
public class BasisCore {

	/**
	 * Debug-Mode: Erzeuge ein {@link Display} mit Resource-Leak Monitor
	 *
	public static Display getSleakDisplay() {
		// fun: check https://www.eclipse.org/articles/swt-design-2/sleak.htm
		DeviceData data = new DeviceData();
	    data.tracking = true;
	    Display display = new Display(data);
	    Swt_Leak_Checker sleak = new Swt_Leak_Checker();
	    sleak.open();
	    return display;
	}*/

	public  Display display;
	
	/**
	 * Kinder-COREs können direkt zugreifen
	 */
	protected TaskQueue worker;
	
	/**
	 * Logger kann gesetzt sein, wenn nicht => log /dev/null
	 */
	private ITrLogger logger;

    /**
     * use it when needed
     */
    @Nullable
    public CentralFontManager centralFontManager;

	/**
	 * optional: REF kann hier durch-gereicht werden
	 */
	public Shell mainShell;
	/**
	 * kann gesetzt sein
	 */
	public ApplicationConfig appConfig;
	private boolean shellCloseAllowed;
	
	/**
	 * wird immer gleich mit angelegt
	 */
	private final AppShutdownProcess appCloser;
	
	/**
	 * UserCode sagt: ich brauche ggf eine ThreadPool mit so vielen Threads
	 */
	private int allowWorkerThreads = -1;
	private final Object threadPoolSyncer = new Object();
	/** optinal: User führt Dinge über @link #exeNow() aus */
	private ExecutorService threadPool;
	
	/**
	 * wenn gesetzt: log nach GUI, siehe <br><br>
	 * 
	 * {@link #logToGui(String)}
	 */
	private Text swtLogText;

	/**
	 * on-demand erzeugt, sobald der erste {@link #showAlarmDialog(String, String, boolean)} kommt
	 */
	public AlarmManager alarmManager;
	
	private Color alarmStdColor;
	
	/**
	 * logger-Name -> Logger
	 */
	private final Map<String, ITrLogger> namedLoggers = new HashMap<String, ITrLogger>();
	
	/**
	 * true bei LINUX!
	 */
	private boolean runsHeadless = false;

	/** on demand... */
	private NextWindowPos neighborWinHelper;
	
	
	/**
	 * momentan für MegaSim-Server verwendet
	 * - ein atomic/global Counter
	 */
	public AtomicInteger atomicCounter1 = new AtomicInteger();

	/**
	 * log4j kann/kann-nicht im class-path sein
	 * - siehe {@link #getLogger(String)}
	 */
	private Object log4jAdapter;
	private Method log4jGetter; 

	
	public void setRunsHeadless(boolean runsHeadless) {
		this.runsHeadless = runsHeadless;
	}

	/**
	 * empty constructor: lege GAR NIX AN
	 */
	public BasisCore() { 
		this.logger = null;		// muss mit present(logger) bekannt gemacht werden
		this.appCloser = new AppShutdownProcess();
	}
	
	// -------------- PRESENT Methoden -----------------------------
	
	public void present(ITrLogger logger) {
		this.logger = logger;
	}
	/**
	 * HIER kann {@link #logToGui(String)} rein-schreiben
	 */
	public void presentLogText(Text logText) {
		swtLogText = logText;
	}
	
	/**
	 * Die App will Alarm-Dialog anzeigen
	 * 
	 * @param thisSound - null-able
	 */
	public void presentAlarmManager(Color stdBgColor, File thisSound, int maxConcurrentDialogs) {
		if (alarmManager==null) {
			Image img = mainShell!=null ? mainShell.getImage() : null;
			alarmManager = new AlarmManager(this, img, maxConcurrentDialogs, thisSound);
			alarmStdColor = stdBgColor;
		}
	}
	/**
	 * Die App will Alarm-Dialog anzeigen
	 * 
	 * @param alarmImg - mit diesem Icon
	 * @param thisSound - null-able
	 */
	public void presentAlarmManager(Image alarmImg, Color stdBgColor, File thisSound, int maxConcurrentDialogs) {
		if (alarmManager==null) {
			alarmManager = new AlarmManager(this, alarmImg, maxConcurrentDialogs, thisSound);
			alarmStdColor = stdBgColor;
		}
	}
	// -------------- PRESENT Methoden -----------------------------
	

	/**
	 * constructor
	 */
	public BasisCore(Display display) {
		this(display, "Basis-Core-TaskQueue");
	}
	/**
	 * constructor mit Namen der Task-Queue
	 * 
	 * @param taskQueueThreadName - wenn NULL: Lege KEINE Queue an
	 */
	public BasisCore(Display display, String taskQueueThreadName) {
		this();
		this.display = display;
		if (taskQueueThreadName!=null) {
			worker = new TaskQueue(taskQueueThreadName, Thread.MIN_PRIORITY);
		}
	}

	public Display getDisplay() {
		return display;
	}

	public IAlarmManager getAlarmManager() {
		return alarmManager;
	}
	
	/**
	 * führe was im SWT-Thread aus
	 */
	public void asyncExec(Runnable runnable) {
		if (runsHeadless) {
			return;
		}
		try {
			if (display.isDisposed()==false) {
				display.asyncExec(runnable);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// fangen, aber NICHT hoch-reichen, sonst geht die ganze APP flöten!
		}
	}

	/**
	 * führe was im SWT-Thread aus und WARTET mit dem return 
	 */
	public void asyncExecAndWait(final Runnable thisSwtCode) {
		if (thisSwtCode==null || display==null || display.isDisposed()) {
			return;
		}
		// ich muss ein neues Runnable erzeugen!
		final Runnable run = new Runnable() {
			@Override
			public void run() {
				try {
					thisSwtCode.run();
				} catch (Exception e) {
					e.printStackTrace();		// egal
				}
				// wecke den caller wieder auf
				synchronized (thisSwtCode) {
					thisSwtCode.notify();
				}
			}
		};
		synchronized (thisSwtCode) {
			display.asyncExec(run);		// starte das Ding im SWT-Thread
			try {
				thisSwtCode.wait();		// lege den Caller schlafen
			} catch (InterruptedException e) {
				e.printStackTrace();
			}					
		}
	}
	
	/**
	 * Lässt was im SWT-Thread laufen
	 *  UND WARETET bis das fertig ist
	 */
	public <T> T asyncExecWaitCallingThread(final Callable<T> runThis_returnT) {
		if (display==null || display.isDisposed()) {
			return null;
		}
		class Monitor {
			public T returnValue;
		}
		final Monitor monitor = new Monitor();
		synchronized (monitor) {
			display.asyncExec(new Runnable() {
				public void run() {
					synchronized (monitor) {
						try {
							monitor.returnValue = runThis_returnT.call();
						} catch (Exception e) {
							e.printStackTrace();
						}
						monitor.notify();		// caller: wach auf
					}
				}
			});
			try {
				monitor.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}		// Caller geht schlafen
		}
		return monitor.returnValue;
	}

	/**
	 * wenn die mainShell gesetzt ist UND ein Bild hat:
	 * - setze das auch in diese Shell
	 */
	public void copyMainShellImageTo(Shell shell) {
		if (mainShell!=null) {
			Image img = mainShell.getImage();
			if (img!=null) {
				shell.setImage(img);
			}
		}
	}
	
	/**
	 * Log-e einen Fehler, wenn der Logger nicht gesetzt ist: auf syserr
	 */
	public void error(String msg, Throwable error) {
		if (logger!=null) {
			logger.error(msg, error);
		} else {
			System.err.println(msg);
			if (error!=null) {
				System.err.println(error);
			}
		}
	}

	/**
	 * Führe einen Task aus
	 */
	public void executeTask(Task task) {
		if (worker==null) {
			System.err.println("BasisCore.queue==null, can't run " + task);
			return;
		}
		worker.execute(task);
	}

	public boolean isSwtThread() {
		if (display==null) {
			return false;
		}
		return (display.getThread() == Thread.currentThread());
	}

	/**
	 * Log-e etwas in die GUI, wenn {@link #presentLogText(Text)} gesetzt wurde
	 */
	public void logToGui(final String message) {
		if (swtLogText!=null) {
			asyncExec(new Runnable() {
				@Override
				public void run() {
					String msg = swtLogText.getText() + "\r\n" + message;
					swtLogText.setText(msg);
				}
			});
		} else {
			System.err.println("logToGui:  " + message);
		}
	}
	
	/**
	 * ein Prozess will ein länger-laufenen Task anzeigen
	 * 
	 * @return null - wenn der Core dieses Feature nicht unterstützt
	 *                oder wenn gerade schon einer angezeigt wird
	 */
	public IProgressMonitor getProgressMonitor(int stepCount) {
		return null;
	}

	/**
	 * wie heisst die Maschine hier ?
	 */
	public String getComputerName() {
		String computername;
		try {
			computername = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			computername = "n/a";
		}
		return computername;
	}
	
	/**
	 * Wer ist gerade eingeoggt?
	 * 
	 * @return null bei Feher
	 */
	public String getUserName() {
		try {
			return System.getProperty("user.name");
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Zugriff auf den zentralen Logger
	 * - wenn nicht da, per {@link #present(ITrLogger)}
	 * => dann liefert das getLogger("default") !
	 * 
	 */
	public ITrLogger logger() {
		if (logger==null) {
			return getLogger("default");
		}
		return logger;
	}

	/**
	 * Baue/Hole einen Logger
	 * - wenn log4j im classpath ist: dann nehme das
	 * - sonst: es gibt einen sysout-Logger mit prefix = name
	 * 
	 * @param - kann dann über log4j.properties eingestellt werden
	 */
	public ITrLogger getLogger(String loggerName) {
		synchronized (namedLoggers) {
			if (namedLoggers.size()==0) {		// beim ersten mal:
				if (log4jAdapter == null) {
					// suche:
					try {
						Class<?> clz = Class.forName("tr.basics.common.Log4jAdapter");
						log4jAdapter = clz.newInstance();	// der versucht gleich, die 'log4j.properties' zu lesen
						// Zugriff geht dann über die Methode getLogger(String name)
						log4jGetter = log4jAdapter.getClass().getMethod("getLogger", String.class);
					} catch (Exception e) {
						// e.printStackTrace();
					}
				}
			}
			// hier: log4jAdapter ist gesetzt oder halt nicht
			
			ITrLogger logger = namedLoggers.get(loggerName);
			if (logger==null) {
				// versuch's über log4j:
				if (log4jGetter!=null) {
					try {
						logger = (ITrLogger) log4jGetter.invoke(log4jAdapter, loggerName);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				// gehe auf Nummer sicher: wenn 'logger' immer noch null ist => verwende eine non-log4j-Logger
				if (logger==null) {
					logger = new TrNamedLogger(loggerName);
				}
				namedLoggers.put(loggerName, logger);
			}
			return logger;
		}
	}

	
	/**
	 * etwas kritisches ist passiert
	 * - zeige GUI an wenn möglich
	 */
	public void showCriticalError(final String message) {
		System.err.println("** CRITICAL **   " + message);
		if (logger!=null) {
			logger.fatalError(message);
		}
		if (display!=null && mainShell!=null && !display.isDisposed()) {
			asyncExec(new Runnable() {
				public void run() {
					// sonst: Zeige MessageBox an
					MessageBox mb = new MessageBox(mainShell, SWT.ERROR_UNSPECIFIED);
					mb.setText("ERROR!");
					mb.setMessage(message);
					mb.open();
				}
			});
		}
	}
	
	/**
	 * Zeige einen Fehler/Message im AlarmDialog <br>
	 * 
	 * intern wird der {@link AlarmManager} verwendet
	 */
	public void showAlarmDialog(final String title, final String message) {
		showAlarmDialog(title, message, true);
	}
	
	/**
	 * Zeige einen Fehler/Message im AlarmDialog <br>
	 * 
	 * intern wird der {@link AlarmManager} verwendet
	 * 
	 * @param forceShow - true: zeige das Fenster direkt, keine Alarm-Manager-Warte-Liste
	 */
	public void showAlarmDialog(final String title, final String message, boolean forceShow) {
		if (display==null || display.isDisposed()) {
			System.err.println("showAlarmDialog() " + title + " => " + message);
			return;
		}
		
		if (forceShow==true) {		// umgehe den Alarm-Manager
			if (isSwtThread()) {
				Image img = mainShell!=null ? mainShell.getImage() : null;
				new AlarmDialog(title, message, display, img);
			} else {
				asyncExec(new Runnable() {
					public void run() {
						Image img = mainShell!=null ? mainShell.getImage() : null;
						new AlarmDialog(title, message, display, img);
					}
				});
			}
			return;
		}
		
		// hier: verwende den AlarmManager
		_showAlarmWithAlarmManager(title, message, alarmStdColor);
	}
	
	
	/**
	 * Zeige einen Fehler/Message im AlarmDialog <br>
	 * über den <b>AlarmManager</b>, d.h. wenn gerade zu viele Alarm-Dialog offen sind -> Warte-Liste
	 */
	public void showAlarmDialog(final String title, final String message, Color bgColorOrNull) {
		if (alarmManager==null) {
			System.err.println("AlarmManager is not set, use present(color, sound, ..) !");
			System.err.println("showAlarmDialog() " + title + " => " + message);
			return;
		}
		
		// hier: verwende den AlarmManager
		_showAlarmWithAlarmManager(title, message, bgColorOrNull);
	}
	
	/**
	 * darf nur kommen wenn der AlarmManager 'oben' ist
	 */
	private void _showAlarmWithAlarmManager(String title, String message, Color bgColor) {
		if (display==null || display.isDisposed()) {
			System.err.println("showAlarmDialog() " + title + " => " + message);
			return;
		}
		if (alarmManager==null) {
			System.err.println("AlarmManager is not set, use present(color, sound, ..) !");
			System.err.println("showAlarmDialog() " + title + " => " + message);
			return;
		}
		alarmManager.addAlarm(title, message, bgColor);
	}
	
	
	/**
	 * manchmal wird's etwas zuviel alarm-ig auf dem Screen: Schliesse alle auf einmal
	 */
	public void closeAllAlarms() {
		AlarmDialog.closeAllDialogs();
	}
	
	
	/**
	 * fügt das Object/Komponente in die Liste der Shutdowns ein
	 */
	public void add2Shutdown(IShutdownHook object) {
		appCloser.shutdownList.add(object);
	}
	
	/**
	 * Analog zum Shutdown: für Shells
	 */
	public void add2ShutdownShell(IShutdownShell shellListener) {
		appCloser.shellShutdownList.add(shellListener);
	}
	
	/**
	 * Praktisch für Log/Ausgabe: liefert die Java-Klasse + Zeile
	 *  - vgl Stack-Trace
	 */
	public String codePlace() {
		StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
		StackTraceElement x = stacks[2];
		return x.toString();
	}

	/**
	 * lasse den SWT-Event-Loop laufen
	 */
	public void runSwtLoop(Shell shell) {
		if (display==null) {
			return;
		}
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

	/**
	 * Starte das Runterfahren der App
	 */
	public void triggerAppShutdown() {
		appCloser.startProcess();
	}
	
	/**
	 * Die Main-Shell triggert idR das Runterfahren <br>
	 * - sie DARF aber noch nicht gleich "gehen" da erst alle shutdowns laufen!
	 */
	public boolean shellCanClose() {
		return (appCloser.shutdownState == 2);		// erst ab 2 ist's ok
	}
	/**
	 * true beim Runterfahren
	 */
	public boolean shutdownInProgress() {
		return appCloser.shutdownState == 1;
	}
	
	/**
	 * Wrapper für's saubere Runterfarhen einer Anwendung:
	 * - getrigger über triggerAppShutdown()
	 * - renne alle swtOnClose-Listener
	 * - renne alle ShutdownHooks  
	 * - dann am Schluss: rufe close auf der mainShell
	 *
	 */
	private class AppShutdownProcess extends Thread {
		

		public int shutdownState = 0;		// 1=run
		
		/**
		 * Dinge, die den shutdown()-call wollen
		 */
		public List<IShutdownHook> shutdownList = new ArrayList<IShutdownHook>();
		public List<IShutdownShell> shellShutdownList = new ArrayList<IShutdownShell>();

		/**
		 * Jemand sagt: mach zu
		 */
		public synchronized void startProcess() {
			if (shutdownState>0) {
				return;			// nur EINMAL starten
			}
			shutdownState = 1;	
			start();		// laufe los
		}
		
		/**
		 * kann (leider noch) auch von aussen getriggert werden
		 */
		public void runShutdowns() {
			for (IShutdownHook hook : shutdownList) {
				try {
					hook.shutdown();
				} catch (Throwable e) {
					e.printStackTrace();
					// egal
				}
			}
			if (worker!=null) {
				worker.shutdown();
			}
			if (threadPool!=null) {
				threadPool.shutdown();
			}
		}
		private void runShellShutdowns() {
			// gehe auf SWT!
			asyncExecWaitCallingThread(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					for (IShutdownShell listener : shellShutdownList) {
						try {
							listener.shellShutdown();
						} catch (Throwable e) {
							// sicher ist sicher
						}
					}
					return null;
				}
			});
		}

		/**
		 * Hier rennt ein extra Mach-alles-zu-Thread
		 */
		@Override
		public void run() {
			
			// 1) lass all Shells ihre Position in der config eintragen
			runShellShutdowns();
			
			// 2) jetzt alle andere Shutdown-Dinge...
			runShutdowns();
			
			// jetzt ist's ok
			shutdownState = 2;
			
			// Speichern wenn möglich
			if (appConfig!=null) {
				appConfig.saveConfig();
			}
			
			// idR: re-trigger das Ding von der Main-Shell
			if (mainShell!=null) {
				asyncExec(new Runnable() {
					public void run() {
						if (mainShell.isDisposed()==false) {			// sicher ist sicher: jemand anders könnte die Shell schon gekickt haben
							mainShell.close();
						}
					}
				});
			}
		}
		
	}	// AppShutdownProcess
	
	/**
	 * Aktiviere den WorkerPool <br>
	 * - UserCode kann dann Dinge mit #exeNow() asynchron und parallel ausführen lassen <br>
	 * - der Pool> wird <b>erst erzeugt</b> wenn der erste exeNow() kommt!
	 * 
	 * @param nThreads - so viele Threads maximal
	 */
	public void activateWorkerPool(int nThreads) {
		this.allowWorkerThreads = nThreads;
	}

	/**
	 * callback-Hook für calls auf 
	 */
	public interface IOnTaskDone<T> {
		public void onTaskDone(T info);
	}
	
	/**
	 * Führt einen Task SOFORT aus <br>
	 * - im internen WorkerPool <b>WENN</b> der Pool aktiviert wurde! <br>
	 * - <b>Sonst</b>: die Aufgabe wird als <b>normaler Task</b> ein-gereiht
	 */
	public void exeNow(Task task) {
		// erster call? -> anlegen
		synchronized (threadPoolSyncer) {
			if (threadPool==null && allowWorkerThreads>=1) {
				threadPool = buildPool();
			}
		}
		TaskRunnable<Object> wrapper = new TaskRunnable<Object>(task, null, null);
		if (threadPool!=null) {
			threadPool.execute(wrapper);
		} else {
			executeTask(wrapper);		// sonst: führe aus in der zentralen Queue
		}
	}
	
	private ExecutorService buildPool() {
		return Executors.newFixedThreadPool(allowWorkerThreads, new ThreadFactory() {
			private int thNum = 1;
			// baue unbedingt DAEMON Threads
			@Override
			public Thread newThread(Runnable r) {
				Thread th = new Thread(r);
				th.setName("BasisCore.ThreadPool.Worker-" + (thNum++));
				th.setDaemon(true);
				return th;
			}
		});
	}

	/**
	 * wie {@link #exeNow(Task)}, aber zusätzlich: <br>
	 * - Ein Listener informiert werden wenn der Task rum ist
	 * @param doneInfo - diese Info kommt im Callback zurück
	 */
	public <T> void exeNow(Task task, T doneInfo, IOnTaskDone<T> doneListener) {
		// erster call? -> anlegen
		synchronized (threadPoolSyncer) {
			if (threadPool==null && allowWorkerThreads>=1) {
				threadPool = buildPool();
			}
		}
		TaskRunnable<T> wrapper = new TaskRunnable<T>(task, doneListener, doneInfo);
		if (threadPool!=null) {
			threadPool.execute(wrapper);
		} else {
			executeTask(wrapper);		// sonst: führe aus in der zentralen Queue
		}
	}
	
	
	/**
	 * Wrapper: run() -> process()
	 * - wenn's callback gibt: aufrufen
	 */
	private static class TaskRunnable<T> implements Runnable, Task {
		private final Task task;
		private final T doneInfo;					// null-able
		private final IOnTaskDone<T> callback;		// null-able
		/** constructor */
		public TaskRunnable(Task task, IOnTaskDone<T> callback, T doneInfo) {
			this.task = task;
			this.callback = callback;
			this.doneInfo = doneInfo;
		}
		// ThreadPool Version -> run()
		@Override
		public void run() {
			try {
				task.process();
				if (callback!=null) {
					callback.onTaskDone(doneInfo);
				}
			} catch (Throwable e) {
				e.printStackTrace();	// mehr aber auch nicht...
			}
		}
		// TaskQueue Version -> process()
		@Override
		public void process() {
			run();
		}
	}	// TaskRunnable
	
	/**
	 * Executor-Service Test
	 */
	public static void main(String[] args) {
		BasisCore core = new BasisCore(null);

		core.activateWorkerPool(10);
		
		IOnTaskDone<Integer> doneChecker = new IOnTaskDone<Integer>() {
			@Override
			public void onTaskDone(Integer info) {
				System.out.println("ist fertig " + info);
			}
		};
		for (int i=0; i<10; i++) {
			core.exeNow(new PoolTest(i), i, doneChecker);
		}
		System.out.println("-- habe alles eingereiht --");
		ThreadUtil.sleepUnhandled(10*1000);
		System.err.println("-- fertig --");
		core.triggerAppShutdown();
	}

	private static class PoolTest implements Task {
		private final int i;
		public PoolTest(int i) {
			this.i = i;
		}
		@Override
		public void process() {
			System.err.println(">> work " + i);
			ThreadUtil.sleepUnhandled(400);
		}
	}	// PoolTest

	
	
	/**
	 * Für Fenster, die "in der Nähe" des Hauptfensters aufgehen sollen  <br>
	 *   <br>
	 * - wenn KEINE mainShell gesetzt ist: Mitte des Bildschirms  <br>
	 * - mehrere calls erzeugen immer UNTERSCHIEDLICHE Koordinaten  <br>
	 *   => so dass viele neue Fenster nicht direkt übereinander liegen   <br> 
	 * 
	 */
	public Point getPosForNeighborWindow() {
		if (neighborWinHelper==null) {
			neighborWinHelper = new NextWindowPos();
		}
		return neighborWinHelper.nextWindow(mainShell);
	}

	/**
	 * Helfer für getPosForNeighborWindow()
	 */
	class NextWindowPos {
		private final Random ran = new Random();

		private final Point nextOffsets = new Point(0, 0);
		
		public Point nextWindow(Shell mainShell) {
			// wackle den nächsten Punkt irgendo hin
			nextOffsets.x = ran.nextInt(100) - 50;		// kann auch links von der shell sein
			nextOffsets.y = ran.nextInt(100) + 30;		// auf jeden Fall etwas tiefer
			if (mainShell==null) {
				Rectangle box = getDisplay().getMonitors()[0].getBounds();
				return new Point(box.width/2+nextOffsets.x, box.height/2+nextOffsets.y);
			}
			Point p = mainShell.getLocation();		// praktisch: p IST ein neues Object
			p.x += nextOffsets.x;
			p.y += nextOffsets.y;
			return p;
		}
	}

	

}




























