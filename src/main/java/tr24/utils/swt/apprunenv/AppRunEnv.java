package tr24.utils.swt.apprunenv;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import tr24.utils.annotations.Nullable;
import tr24.utils.common.ILogger;
import tr24.utils.common.LogLevel;
import tr24.utils.common.TaskQueue;
import tr24.utils.common.ThreadUtil;
import tr24.utils.scheduler.SchedulerService;
import tr24.utils.swt.api.IShutdownHook;
import tr24.utils.swt.api.IShutdownShell;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AppRunEnv {



    /* --------------- APIs --------------------------- */
    public interface ITestARE {

        /**
         * @param someInitObject - when set: addApp() BLOCKs until 'app' says {@link IAppCtx#appIsReady}
         */
        <T> T addApp(ISwtApp app, @Nullable Object someInitObject, Class<T> waitForAppReady);
        void addApp(ISwtApp app, @Nullable Object someInitObject);

        /**
         * Test-code shuts down all app(s)
         *  BLOCKING
         */
        void triggerAppShutdown();
    }
    public interface IStandaloneARE {
        /**
         * the main driver code (Standalone: main() , Test/Shared: main-thread)
         *  can trigger shutdown too
         * - Standalone: main app goes down
         * - Test: all app go down
         */
        void triggerAppShutdown();
    }
/* --------------- APIs --------------------------- */


    // Factory method for Test Mode (Shared Environment)
    public static ITestARE BUILD_TEST_ENV(@Nullable ILogger logger, @Nullable String taskQName, int threadPopolSize, @Nullable String scheduler) {
        return new TestAREImpl(logger, taskQName, threadPopolSize, scheduler);
    }

    /**
     * Run ONE standalone app
     *
     * @param blocking  - true: call BLOCKs until app is done <br>
     *                    false: the return obj {@link IStandaloneARE} can be used to trigger app-shutdown
     *                           Caller must wait-for-app-shutdown itself!
     *
     */
    // Factory method for Standalone Mode
    public static IStandaloneARE RUN_STANDALONE(@Nullable ILogger logger, @Nullable String taskQName, int threadPopolSize,
                                                @Nullable String scheduler, @Nullable Object someConfig, ISwtApp theApp, boolean blocking)
    {
        StandaloneAREImpl api = new StandaloneAREImpl(logger, taskQName, threadPopolSize, scheduler, someConfig, theApp, blocking);
        return api;
    }

    // Shared resources between the environments
    protected GuiThread guiThread;
    protected Tr24GuiCore tr24GuiCore;

    protected final ILogger logger;
    public final TaskQueue taskQueue;
    protected final ExecutorService executorService;
    protected final SchedulerService schedulerService;
    protected final List<AppCtxImpl> myApps = new ArrayList<>();
    public final boolean isStandalone;
    ShutdownHandler shutdownHandler = null;

    /**
     * for blocking standalone version
     */
    Object mainThreadBlocker = null;


    /** Ctor ----------------------------------------------- */
    protected AppRunEnv(@Nullable ILogger logger, @Nullable String taskQName, int threadPopolSize, @Nullable String scheduler, boolean isStandalone) {
        this.logger    = logger!=null ? logger : new ILogger.SysoutLogger(LogLevel.DEBUG);
        this.taskQueue = (taskQName != null) ? new TaskQueue(taskQName) : null;
        this.executorService = (threadPopolSize > 0) ? Executors.newFixedThreadPool(threadPopolSize) : null;
        this.schedulerService = (scheduler != null) ? new SchedulerService(scheduler) : null;
        this.isStandalone = isStandalone;
    }

    /**
     * one of the Apps wants to close everything
     * - start shutdown proc
     */
    void initiateShutdown() {
        if (shutdownHandler!=null) {
            return;   // run only once
        }
        shutdownHandler = new ShutdownHandler(myApps, guiThread.getDisplay(), tr24GuiCore.shutdownList, tr24GuiCore.swtShutdownList);
        // run it somewhere: do NOT run Handler in TaskQ -> that could be blocking/be-busy right now
        if (executorService != null) {
            executorService.submit(shutdownHandler::runShutdown);
        } else {
            new Thread(shutdownHandler::runShutdown).start();
        }
    }

    /**
     * TEST-Driver Apps/Apps:
     *  - start Event-Loop right away => so every addApp(..) creates a response Shell/App
     *  - NO shutdown by Shell.close() -> test code has to trigger the shutdown
     *
     */
    static class TestAREImpl extends AppRunEnv implements ITestARE {

        public TestAREImpl(@Nullable ILogger logger, @Nullable String taskQName, int threadPopolSize, @Nullable String scheduler) {
            super(logger, taskQName, threadPopolSize, scheduler, false);
            this.guiThread = new GuiThread();
            this.guiThread.start();   // Start the SWT loop immediately for test mode
            this.tr24GuiCore = new Tr24GuiCore(guiThread.getDisplay(), this.taskQueue/*may be null!*/);
        }

        @Override
        public <T> T addApp(ISwtApp app, @Nullable Object initObject, Class<T> waitForAppReady) {

            AppCtxImpl appCtx = new AppCtxImpl(this, app, guiThread.getDisplay(), true);
            myApps.add(appCtx);

            // call app-stuff (by CONTRACT)
            try {
                app.initServices(initObject, taskQueue, executorService, schedulerService, appCtx, logger);
            } catch (Exception e) {
                logger.error("Error calling initServices:");
                e.printStackTrace();
                return null;
            }
            // Run GUI initialization in the UI thread:
            guiThread.getDisplay().syncExec(() -> app.initGui(appCtx, tr24GuiCore));

            if (waitForAppReady!=null) {
                long warnAt = System.currentTimeMillis() + 5000;
                while (appCtx.outObj==null) {
                    ThreadUtil.sleepUnhandled(100);
                    long now = System.currentTimeMillis();
                    if (now>=warnAt) {
                        System.err.println("Wrn: App [" + app.getClass().getSimpleName() + "] should call IAppCtx.appIsReady(...)");
                        warnAt = now + 5000;
                    }
                }
                if (waitForAppReady.isInstance(appCtx.outObj)==false) {
                    throw new IllegalArgumentException("App [" + app.getClass().getSimpleName() + "] should push 'outObj' of type [" + waitForAppReady + "]");
                }
                return (T) appCtx.outObj;
            } else {
                return null;
            }
        }

        @Override
        public void addApp(ISwtApp app, @Nullable Object initObject) {
            addApp(app, initObject, null);
        }

        @Override
        public void triggerAppShutdown() {
            initiateShutdown();
            while (shutdownHandler.shutdownDone==false) {
                ThreadUtil.sleepUnhandled(50);
            }
        }
    }





    static class StandaloneAREImpl extends AppRunEnv implements IStandaloneARE {

        /**
         *
         * @param blocking - true: let main thread sleep
         */
        public StandaloneAREImpl(@Nullable ILogger pLogger, @Nullable String taskQName, int threadPopolSize,
                                 @Nullable String scheduler, @Nullable Object someConfig, ISwtApp theApp, boolean blocking)
        {
            super(pLogger, taskQName, threadPopolSize, scheduler, true);
            this.guiThread = new GuiThread();
            this.guiThread.start(); // Start the SWT loop for standalone mode
            this.tr24GuiCore = new Tr24GuiCore(guiThread.getDisplay(), this.taskQueue/*may be null!*/);

            // Initialize and run the app
            AppCtxImpl appCtx = new AppCtxImpl(this, theApp, guiThread.getDisplay(), false);
            myApps.add(appCtx);

            // call app-stuff (by CONTRACT)
            try {
                theApp.initServices(someConfig, taskQueue, executorService, schedulerService, appCtx, logger);
            } catch (Exception e) {
                logger.error("Error calling initServices:");
                e.printStackTrace();
                return;
            }
            guiThread.getDisplay().syncExec(() -> theApp.initGui(appCtx, tr24GuiCore));

            if (blocking) {
                mainThreadBlocker = new Object();   // shutdown-proc will wake me up again
                ThreadUtil.waitOnSYNC(mainThreadBlocker);
            }
            logger.debug("Standalone app done.");
        }
        @Override
        public void triggerAppShutdown() {
            initiateShutdown();
        }
    }




    class GuiThread {
        private Display _display;
        private Thread uiThread;

        boolean uiThreadDone = false;

        GuiThread() {
            uiThread = new Thread(() -> {
                _display = new Display();
                while (!_display.isDisposed()) {
                    if (!_display.readAndDispatch()) {
                        _display.sleep();
                    }
                }
                _display.dispose();
                uiThreadDone = true;
            });
            uiThread.setName("UI-Thread");
            uiThread.setDaemon(true);
        }

        public void start() {
            uiThread.start();
        }

        public Display getDisplay() {
            // super RC: init-code could be faster the gui-thread itself
            while (_display==null) {
                ThreadUtil.sleepUnhandled(5);
            };
            return _display;
        }

        public void join() {
            while (!uiThreadDone) {
                ThreadUtil.sleepUnhandled(10);
            }
        }
    }



    class ShutdownHandler {
        private final List<AppCtxImpl> appList;
        private final Display display;
        private final List<IShutdownHook> hookList;
        private final List<IShutdownShell> swtShutdownList;
        public boolean shutdownDone = false;
        long hookWaitTimeout;
        boolean runsAllowed;

        ProgressGuiHelper prog = null;

        ShutdownHandler(List<AppCtxImpl> myApps, Display display, List<IShutdownHook> hookList, List<IShutdownShell> swtShutdownList) {
            appList = myApps;
            this.display = display;
            this.hookList = hookList;
            this.swtShutdownList = swtShutdownList;
        }
        public void runShutdown() {

            int n = 0;
            for (AppCtxImpl ctx : appList) {
                ctx.shutdownActive = true;
                n += ctx.shutdownHooks.size();
            }
            // add hooks from core: 2 ways
            n += hookList.size();
            n += swtShutdownList.size();

            final int hookCount = n;
            final AtomicInteger countDown = new AtomicInteger(n);

            // run all hooks of all apps: wait some time, but not too long
            this.hookWaitTimeout = System.currentTimeMillis() + 10_000;
            this.runsAllowed = true;
            Thread th = new Thread(()-> {
                // 1) run ctx-hooks
                for (AppCtxImpl ctx : appList) {
                    for (Runnable code : ctx.shutdownHooks) {
                        if (runsAllowed) {
                            try {
                                code.run();
                            } catch (Exception e) {
                                e.printStackTrace(); /* ignore */
                            }
                            countDown.decrementAndGet();
                        }
                    }
                }
                // 2) run gui-core-hooks: non-swt
                for (IShutdownHook hook : hookList) {
                    if (runsAllowed) {
                        try {
                            hook.shutdown();
                        } catch (Exception e) {
                            e.printStackTrace(); /* ignore */
                        }
                        countDown.decrementAndGet();
                    }
                }
                // 3) run gui-core-hooks: swt
                for (IShutdownShell swtHook : swtShutdownList) {
                    if (runsAllowed) {
                        try {
                            swtHook.shellShutdown();
                        } catch (Exception e) {
                            e.printStackTrace(); /* ignore */
                        }
                        countDown.decrementAndGet();
                    }
                }
                hookWaitTimeout = -1;  // done
            });
            th.setDaemon(true);
            th.setName("ShutdownHook.Runner");
            th.start();

            // wait for hooks to be done; open Prog-bar is it takes some time
            long openAt = System.currentTimeMillis() + 1000;


            while (hookWaitTimeout>0) {
                ThreadUtil.sleepUnhandled(100);
                long now = System.currentTimeMillis();
                if (openAt>0 && now >= openAt) {
                    Display dis = guiThread.getDisplay();
                    if (!dis.isDisposed()) {
                        dis.asyncExec(()->{
                            prog = new ProgressGuiHelper(display, hookCount);
                        });
                    }
                    openAt = -1;
                }
                if (now > hookWaitTimeout) {
                    logger.warn("Shutdown.Runner takes too long => force exit");
                    runsAllowed = false;
                    hookWaitTimeout = -1;
                }
            }
            if (prog!=null) {
                prog.close();
            }

            // kill all SWT
            display.asyncExec(()-> display.dispose() );

            // wait for UI-Thread to be gone
            guiThread.join();

            logger.debug("shutdown complete");

            shutdownDone = true;

            if (mainThreadBlocker!=null) {
                synchronized (mainThreadBlocker) {
                    mainThreadBlocker.notify();
                }
            }

        }   // runShutdown

    }




    public class ProgressGuiHelper {

        private final Display display;
        private final Shell shell;
        private final ProgressBar progressBar;
        private final int totalHooks;
        private int hooksCompleted = 0;

        public ProgressGuiHelper(Display display, int totalHooks) {
            this.display = display;
            this.totalHooks = totalHooks + 1;

            // Initialize the shell and progress bar
            shell = new Shell(display, SWT.DIALOG_TRIM);
            shell.setText("Shutdown in Progress...");
            shell.setSize(300, 65);
            shell.setLayout(new FillLayout());

            progressBar = new ProgressBar(shell, SWT.NONE);
            progressBar.setMinimum(0);
            progressBar.setMaximum(100);
            inc();

            shell.open();
        }

        // Method to be called when a shutdown hook completes
        public void oneDone() {
            display.asyncExec(() -> {
                inc();
            });
        }

        private void inc() {
            hooksCompleted++;
            int progress = (int) ((hooksCompleted / (float) totalHooks) * 100);
            progressBar.setSelection(progress);
        }

        // Method to close the progress window
        public void close() {
            display.asyncExec(() -> {
                if (!shell.isDisposed()) {
                    shell.close();
                    shell.dispose();
                }
            });
        }
    }

}
