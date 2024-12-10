package tr24.utils.swt.apprunenv;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import tr24.utils.common.ILogger;
import tr24.utils.common.LogLevel;
import tr24.utils.common.TaskQueue;
import tr24.utils.common.ThreadUtil;
import tr24.utils.scheduler.SchedulerService;
import tr24.utils.swt.ApplicationConfig;
import tr24.utils.swt.FARBE;
import tr24.utils.swt.IconBuilder;
import tr24.utils.swt.SwtUtils;
import tr24.utils.swt.api.IOnButtonClick;
import tr24.utils.swt.api.IShutdownShell;
import tr24.utils.swt.apprunenv.AppRunEnv.IStandaloneARE;
import tr24.utils.swt.apprunenv.AppRunEnv.ITestARE;

import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 * simulate test code
 */
public class DEV_AppRunEnv {

    private static final String CONF_FILE = "stepper2.conf";

    public static void main(String[] args) {

        String confFile = CONF_FILE;
        if (args.length>0) {
            String s = args[0];
            if (s.startsWith("config=")) {
                s = s.substring(7);
                confFile = s;
            }
        }
        new DEV_AppRunEnv().runApp(confFile);
    }

    private void runApp(String confFile) {
        ILogger logger = new ILogger.SysoutLogger(LogLevel.INFO.DEBUG);
        boolean blocking = true;
        // check conf File
        File cfg = new File(confFile);
        if (!cfg.exists()) {
            throw new IllegalArgumentException("config file not set/found: " + cfg.getAbsolutePath());
        }

        IStandaloneARE SA_ARE = AppRunEnv.RUN_STANDALONE(logger, "taskQ", 2, null, cfg, new StepperNT7App(), blocking);

        // no need to call anything else
        System.out.println(DEV_AppRunEnv.class.getSimpleName() + " done.");
    }


    public class StepperNT7App implements ISwtApp<File> {

        private Shell shell;
        private Label label;
        private File confFile;
        private ILogger logger;
        private ApplicationConfig conf;
        private Button btnAC;

        @Override
        public void initServices(File confFile, TaskQueue taskQ, ExecutorService pool, SchedulerService scheduler, IAppCtx appCtx, ILogger logger) {
            this.confFile = confFile;
            this.logger = logger;
            logger.info("StepeprNT7.initServices;");

            appCtx.registerShutdownCode(()->{
                logger.info("sim long shutdown;");
                conf.setMainShellPosition(shell.getBounds());
                ThreadUtil.sleepUnhandled(1000);
            });
        }

        @Override
        public void initGui(IAppCtx appCtx, Tr24GuiCore core) {
            logger.info("TestApp1.initGui;");

            Display display = core.display;
            conf = new ApplicationConfig(confFile);
            conf.loadConfigFile();

            this.shell = new Shell(display);
            shell.setText("Stepper NT7/V2");
            shell.setLayout(new FormLayout());
            Image img = new IconBuilder().buildBarIcon(display, FARBE.BLACK_BLUE_ish);
            shell.setImage(img);

            Rectangle bounds = conf.getMainShellPosition(display.getMonitors());
            shell.setBounds(bounds);

            Composite boxOben = SwtUtils.LAYOUT.layout_ObenLeiste(shell, 60, 1, FARBE.BLACK_BLUE_ish);
            btnAC = SwtUtils.LAYOUT.layout_button("loading...", 4, 4, 100, 25, boxOben, new IOnButtonClick() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                /*    if (core.runThisAc!=null) {			// sicher ist sicher
                        core.run(new Task_StartAC(core.runThisAc, core.runtThisInst));
                    }
                    // btnAC.setEnabled(false);		// FIXME: multi start gehen momentan */
                }
            });
            btnAC.setEnabled(false);

            // Slider : zum Einstellen der scrollMiddle im Chart
            int sliderPos = conf.getIntProp("slider", 80);		// optional!
            final Slider slider = new Slider (boxOben, SWT.HORIZONTAL);
            slider.setBounds(15, 32, 200, 20);
            slider.setMinimum(5);		// von 10%-99%
            slider.setMaximum(109);
            slider.setSelection(sliderPos);	// start-Value
            slider.addListener (SWT.Selection, new Listener () {
                public void handleEvent (Event event) {
                    int selection = slider.getSelection();
                    System.err.println(selection);
                    // Sende an AC-Ctx:
/*                    if (core.curAcCtx!=null) {
                        //core.curAcCtx.changeChartScrollMiddle(selection);
                    }  */
                }
            });

            // Contract with app-runner: Register the shell
            appCtx.registerShell(shell);   // this does handle the close-event
            // Open the shell
            shell.open();

            // say I am ready
            appCtx.appIsReady(123);
        }
    }



}









