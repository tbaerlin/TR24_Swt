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
    private final Tr24GuiCore tr24GuiCore;

    Object outObj = null;
    boolean shutdownActive;

    /** Ctor ----------------------------------------------- */
    public AppCtxImpl(AppRunEnv appRunEnv, ISwtApp app, Display display, boolean isTestMode, Tr24GuiCore tr24GuiCore) {
        this.appRunEnv = appRunEnv;
        this.app = app;
        this.display = display;
        this.isTestMode = isTestMode;
        this.tr24GuiCore = tr24GuiCore;
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
    public void registerSwtShutdownCode(Runnable code) {
        if (tr24GuiCore!=null) {
            tr24GuiCore.add2ShutdownShell(()->code.run());
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
        // make shell known => so showCriticalError() can show stuff WITHOUT needing to pass a shell-Object
        this.tr24GuiCore._setShell(shell);
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
