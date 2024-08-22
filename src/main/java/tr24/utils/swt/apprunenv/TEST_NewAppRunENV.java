package tr24.utils.swt.apprunenv;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import tr24.utils.common.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

public class TEST_NewAppRunENV {

    private RunEnvironment runEnv;
    private MyApp1 app1;
    private MyApp2 app2;


    public static void main(String[] args) {
        new TEST_NewAppRunENV().test_DEV_POC();
    }

    void test_DEV_POC() {
        runEnv = new RunEnvironment();

        app1 = new MyApp1();
        app2 = new MyApp2();

        // Initialize both apps with the shared RunEnvironment
        app1.init(runEnv);
        app2.init(runEnv);


        // At this point, both apps are initialized
        // You can now interact with them, e.g., simulate user actions, or just observe


        // Example test cases could go here

        // Start the event loop in a separate thread so that the main thread can control the test
        Thread eventLoopThread = new Thread(() -> {
            runEnv.runEventLoop();
        });

        eventLoopThread.start();

        // Allow time for the UI to run


        // ... Your test code here, e.g., simulate actions, check app states
        ThreadUtil.sleepUnhandled(2000);

        // Shutdown apps after the test
        runEnv.startShutdown(app1);
        runEnv.startShutdown(app2);

        try {
            eventLoopThread.join();  // Wait for the event loop to finish
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    public class RunEnvironment {
        private Display display;
        private List<App> apps = new ArrayList<>();
        private List<Runnable> shutdownHooks = new ArrayList<>();

        public RunEnvironment() {
            this.display = new Display();
        }

        public Display getDisplay() {
            return display;
        }

        // Register the main shell and set up a listener for standalone mode
        public void registerShell(Shell shell, App app) {
            apps.add(app);

            shell.addListener(SWT.Close, event -> {
                if (isStandaloneMode()) {
                    event.doit = false;  // Prevent the shell from closing immediately
                    startShutdown(app);
                } else {
                    event.doit = false;  // Prevent closing in shared/test mode
                }
            });
        }

        public void startShutdown(App app) {
            app.startShutdown();
            checkAndExit();  // Check if all apps are closed
        }

        public void triggerShutdownHooks(App app) {
            for (Runnable hook : shutdownHooks) {
                hook.run();
            }
        }

        public void addShutdownHook(Runnable hook) {
            shutdownHooks.add(hook);
        }

        private void checkAndExit() {
            for (App app : apps) {
                if (!app.shell.isDisposed()) {
                    return;  // Some app is still running
                }
            }
            display.dispose();  // All apps are done, dispose of the display
        }

        public void runEventLoop() {
            while (!display.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
        }

        public boolean isStandaloneMode() {
            // Determine if the app is running in standalone mode
            return true;  // Simplified for demonstration
        }
    }


    public abstract class App {
        protected Shell shell;
        protected RunEnvironment env;

        // Initialize the app
        public void init(RunEnvironment env) {
            this.env = env;
            initializeServices();  // Backend and service level initialization
            initializeUI(env.getDisplay());  // GUI initialization
        }

        // Method to be implemented by each specific app to initialize services
        protected abstract void initializeServices();

        // Method to be implemented by each specific app to create the SWT UI
        protected abstract void initializeUI(Display display);

        // Method to start the shutdown process
        public void startShutdown() {
            // Trigger any shutdown hooks
            env.triggerShutdownHooks(this);

            // Dispose of the shell
            if (!shell.isDisposed()) {
                shell.dispose();
            }
        }

        // Method to register the main shell with the RunEnvironment
        protected void registerShell() {
            env.registerShell(shell, this);
        }
    }

    public class MyApp1 extends App {
        @Override
        protected void initializeServices() {
            // Initialize backend services, e.g., network, database connections
        }

        @Override
        protected void initializeUI(Display display) {
            shell = new Shell(display);
            // Create UI components for MyApp1
            shell.setText("MyApp1 Main Window");
            shell.setSize(400, 300);

            // Register the shell with the RunEnvironment
            registerShell();

            shell.open();
        }
    }

    public class MyApp2 extends App {
        @Override
        protected void initializeServices() {
            // Initialize backend services, e.g., network, database connections
        }

        @Override
        protected void initializeUI(Display display) {
            shell = new Shell(display);
            // Create UI components for MyApp2
            shell.setText("MyApp2 Main Window");
            shell.setSize(400, 300);

            // Register the shell with the RunEnvironment
            registerShell();

            shell.open();
        }
    }


}





















