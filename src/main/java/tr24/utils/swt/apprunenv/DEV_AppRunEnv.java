package tr24.utils.swt.apprunenv;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import tr24.utils.common.ILogger;
import tr24.utils.common.LogLevel;
import tr24.utils.common.TaskQueue;
import tr24.utils.common.ThreadUtil;
import tr24.utils.scheduler.SchedulerService;
import tr24.utils.swt.apprunenv.AppRunEnv.IStandaloneARE;
import tr24.utils.swt.apprunenv.AppRunEnv.ITestARE;

import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 * simulate test code
 */
public class DEV_AppRunEnv {

    public static void main(String[] args) {
        new DEV_AppRunEnv().simTest();
       // new DEV_AppRunEnv().simStandalone();
    }
    private void simStandalone() {
        File someConfig = new File("abc");
        boolean blocking = true;

        IStandaloneARE SA_ARE = AppRunEnv.RUN_STANDALONE(null, null, 2, "scheduler", someConfig, new TestApp1(), blocking);

        // no need to call anything else
        System.out.println("app is done");
    }


    private void simTest() {
        ILogger logger = new ILogger.SysoutLogger(LogLevel.INFO.DEBUG);

        // mode "shared = test": Start the SWT-loop in the background right away!
        ITestARE ARE = AppRunEnv.BUILD_TEST_ENV(logger, "taskQ", 0, "scheduler");

        // test starts app 1 and WAITs for app to say "I am up and here is some info"
        String someInitObject = "fileRef-to-config";
        Integer port = ARE.addApp(new TestApp1(), someInitObject, Integer.class);
        // nice: since SWT is already running, TestApp1 is response! Even when TestApp2 was not added yet!

        // test now starts app 2
        ARE.addApp(new TestApp2(), null);

        // now do the tests
        // run test A
        // run test B
        // run test C
        ThreadUtil.sleepUnhandled(2000);

        // test is done: I NEED TO init the SHUTDOWN!
        ARE.triggerAppShutdown();

        logger.debug("done");
    }





    public class TestApp1 implements ISwtApp<String> {

        private Label label;
        private ILogger logger;

        @Override
        public void initServices(String initObject, TaskQueue taskQ, ExecutorService pool, SchedulerService scheduler, IAppCtx appCtx, ILogger logger) {
            this.logger = logger;
            logger.info("TestApp1.initServices;");

            appCtx.registerShutdownCode(()->{
                logger.info("TestApp1: sim long shutdown;");
                ThreadUtil.sleepUnhandled(2000);
            });
        }

        @Override
        public void initGui(IAppCtx appCtx, Tr24GuiCore core) {
            logger.info("TestApp1.initGui;");

            Shell shell = new Shell(core.display);
            shell.setText("TestApp1 Main Window");
            shell.setSize(300, 200);

            // Create Label
            label = new Label(shell, SWT.NONE);
            label.setText("Initial Text");
            label.setBounds(20, 20, 200, 20);

            // Create Button
            Button button = new Button(shell, SWT.PUSH);
            button.setText("Change Label");
            button.setBounds(20, 60, 100, 30);
            button.addListener(SWT.Selection, event -> {
                // Simulate delayed label update using non-UI-thread
                new Thread(() -> {
                    try {
                        Thread.sleep(1000); // 1000ms delay
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    appCtx.asyncExec(() -> label.setText("Text Updated"));
                }).start();
            });

            // Register the shell
            appCtx.registerShell(shell);   // this does handle the close-event
            // Open the shell
            shell.open();

            // say I am ready
            appCtx.appIsReady(123);
        }
    }









    static class TestApp2 implements ISwtApp {

        @Override
        public void initServices(Object initObject, TaskQueue taskQ, ExecutorService pool, SchedulerService scheduler, IAppCtx appCtx, ILogger logger) {

        }

        @Override
        public void initGui(IAppCtx appCtx, Tr24GuiCore core) {

        }
    }


}









