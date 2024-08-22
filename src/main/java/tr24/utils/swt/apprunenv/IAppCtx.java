package tr24.utils.swt.apprunenv;

import org.eclipse.swt.widgets.Shell;

/**
 * An Application can do stuff with the entire app or list-of-apps
 */
public interface IAppCtx {

    /**
     * when true: app could trigger {@link #appIsReady}
     */
    boolean isTestMode();

    /**
     * for test-driven App(s)
     * - App(2) wait for App(1) to publish something or saying "I am ready"
     */
    void appIsReady(Object outObj);

    void registerShutdownCode(Runnable code);

    /**
     * Trigger code-induced shutdown
     */
    void doShutdownByProgram();

    /**
     * Most important:
     * - each app can register one or more shells
     * - a [X]-click on the shell will trigger the ENTIRE APP(s) shutdown
     */
    void registerShell(Shell shell);

    void asyncExec(Runnable uiCode);

    void syncExec(Runnable uiCode);
}