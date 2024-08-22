package tr24.utils.swt.apprunenv;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class SWTThreadExperiment {

    public static void main(String[] args) {
        // Create a new thread for the SWT UI
        Thread swtThread = new Thread(() -> {
            // Create the Display object in this thread, making it the UI thread
            Display display = new Display();
            Shell shell = new Shell(display);
            shell.setText("SWT Thread Experiment");
            shell.setSize(300, 200);
            shell.open();

            // Run the event loop in this thread
            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            display.dispose();
        });

        // Start the SWT thread
        swtThread.start();

        // Main thread logic (if any)
        try {
            swtThread.join(); // Wait for the SWT thread to finish
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("SWT application exited.");
    }
}
