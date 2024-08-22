package tr24.utils.swt.apprunenv;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;
import java.util.List;

public class AppCtxImpl implements IAppCtx {

    final AppRunEnv appRunEnv;
    final List<Runnable> shutdownHooks = new ArrayList<>();
    final List<Shell> shells = new ArrayList<>();
    final ISwtApp app;
    final Display display;
    private final boolean isTestMode;

    Object outObj = null;
    boolean shutdownActive;

    /** Ctor ----------------------------------------------- */
    public AppCtxImpl(AppRunEnv appRunEnv, ISwtApp app, Display display, boolean isTestMode) {
        this.appRunEnv = appRunEnv;
        this.app = app;
        this.display = display;
        this.isTestMode = isTestMode;
    }

    @Override
    public boolean isTestMode() {
        return isTestMode;
    }
    @Override
    public void appIsReady(Object outObj) {
        this.outObj = outObj;
    }

    @Override
    public void registerShutdownCode(Runnable code) {
        if (!shutdownActive) {
            shutdownHooks.add(code);
        }
    }

    @Override
    public void doShutdownByProgram() {
        appRunEnv.initiateShutdown();
    }

    @Override
    public void registerShell(Shell shell) {
        shells.add(shell);
        shell.addListener(SWT.Close, event -> {
            event.doit = false; // Prevent the shell from closing immediately
            if (appRunEnv.isStandalone) {
                doShutdownByProgram();
            }
        });
    }

    public void asyncExec(Runnable runnable) {
        if (!display.isDisposed()) {
            display.asyncExec(runnable);
        }
    }

    public void syncExec(Runnable runnable) {
        if (!display.isDisposed()) {
            display.syncExec(runnable);
        }
    }
}
