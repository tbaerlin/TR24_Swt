package tr24.utils.swt.apprunenv;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import tr24.utils.annotations.Nullable;
import tr24.utils.common.*;
import tr24.utils.swt.CentralFontManager;
import tr24.utils.swt.api.IShutdownHook;
import tr24.utils.swt.api.IShutdownShell;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * New version of my old beloved BasisCore:
 *
 *   - user code talks to stuff in SWT
 *
 */
public class Tr24GuiCore implements LogCore {

    public final Display display;
    private final TaskQueue coreTaskQ;
    /**
     * this core simply collects hooks <br>
     * - some caller must read/run them!
     */
    public final List<IShutdownHook> shutdownList = new ArrayList<>();
    public final List<IShutdownShell> swtShutdownList = new ArrayList<>();
    private CentralFontManager centralFontManager;
    /** some shell, so some calls will work that need a shell, e.g. showAlarm() */
    private Shell mainShell;
    private ILogger logger;

    public Tr24GuiCore(Display display) {
        this(display, null);
    }
    public Tr24GuiCore(Display display, TaskQueue coreTaskQ) {
        this.display = display;
        this.coreTaskQ = coreTaskQ;
    }
    public Tr24GuiCore(Display display, TaskQueue taskQueue, @Nullable ILogger logger) {
        this(display, taskQueue);
        this.logger = logger;
    }


    @Override
    public ILogger getLogger() {
        if (logger == null) {
            return new ILogger.SysoutLogger(LogLevel.TRACE);
        }
        return logger;
    }

    /**
     * run some task in mainQ
     */
    public void executeTask(Task task) {
        if (coreTaskQ == null) {
            throw new IllegalArgumentException("Tr24GuiCore.taskQ is not set. Use Ctor(display, taskQ)!");
        }
        coreTaskQ.execute(task);
    }

    /**
	 * run SWT code
	 */
    public void asyncExec(Runnable runnable) {
        try {
            if (display.isDisposed()==false) {
                display.asyncExec(runnable);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs SWT-Code and WAITs
     */
    public void asyncExecAndWait(final Runnable thisSwtCode) {
        if (thisSwtCode==null || display==null || display.isDisposed()) {
            return;
        }
        display.syncExec(thisSwtCode);
    }

    /**
     * Runs code in SWT and RETURn sth
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



    public boolean isSwtThread() {
        if (display==null) {
            return false;
        }
        return (display.getThread() == Thread.currentThread());
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

    @Override
    public void showError(String msg) {
        System.err.println("Error: " + msg);
    }

    @Override
    public void showAlarmDialog(String heading, String warnMsg) {
        // for now:
        _showMessageBox(heading, warnMsg);
    }

    /**
     * etwas kritisches ist passiert
     * - zeige GUI an wenn möglich
     */
    public void showCriticalError(final String message) {
        _showMessageBox("Critical", message);
    }

    private void _showMessageBox(String headline, String message) {
        System.err.println("** CRITICAL **   " + message);
        if (mainShell!=null && display!=null && !display.isDisposed()) {
            asyncExec(new Runnable() {
                public void run() {
                    // sonst: Zeige MessageBox an
                    MessageBox mb = new MessageBox(mainShell, SWT.ERROR_UNSPECIFIED);
                    mb.setText(headline);
                    mb.setMessage(message);
                    mb.open();
                }
            });
        }
    }

    public <T> void add2Shutdown(IShutdownHook hook) {
        synchronized (shutdownList) {
            shutdownList.add(hook);
        }
    }
    public void add2ShutdownShell(IShutdownShell swtHook) {
        synchronized (swtShutdownList) {
            swtShutdownList.add(swtHook);
        }
    }

    /**
     * create on demand
     */
    public CentralFontManager getCentralFontManager() {
        if (centralFontManager==null) {
            centralFontManager = new CentralFontManager(this, "Arial");
        }
        return centralFontManager;
    }

    public void copyMainShellImageTo(Shell shell) {
        System.err.println("copyMainShellImageTo: imp me");
    }

    /**
     * intern only
     */
    public void _setShell(Shell shell) {
        this.mainShell = shell;
    }
}














