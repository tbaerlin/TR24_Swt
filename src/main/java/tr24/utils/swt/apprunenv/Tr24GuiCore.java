package tr24.utils.swt.apprunenv;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import tr24.utils.common.Task;
import tr24.utils.common.TaskQueue;
import tr24.utils.swt.CentralFontManager;
import tr24.utils.swt.api.IShutdownHook;
import tr24.utils.swt.api.IShutdownShell;
import tr24.utils.swt.gentable.GenTable2;

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
public class Tr24GuiCore {

    public final Display display;
    private final TaskQueue coreTaskQ;
    /**
     * this core simply collects hooks <br>
     * - some caller must read/run them!
     */
    public final List<IShutdownHook> shutdownList = new ArrayList<>();
    public final List<IShutdownShell> swtShutdownList = new ArrayList<>();
    private CentralFontManager centralFontManager;

    public Tr24GuiCore(Display display) {
        this.display = display;
        this.coreTaskQ = null;
    }
    public Tr24GuiCore(Display display, TaskQueue coreTaskQ) {
        this.display = display;
        this.coreTaskQ = coreTaskQ;
    }


    /**
     * run some task in mainQ
     */
    public void executeTask(Task task) {
        if (coreTaskQ != null) {
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

    /**
     * etwas kritisches ist passiert
     * - zeige GUI an wenn möglich
     */
    public void showCriticalError(final String message, Shell shell) {
        System.err.println("** CRITICAL **   " + message);
        if (display!=null && display.isDisposed()) {
            asyncExec(new Runnable() {
                public void run() {
                    // sonst: Zeige MessageBox an
                    MessageBox mb = new MessageBox(shell, SWT.ERROR_UNSPECIFIED);
                    mb.setText("ERROR!");
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

}
