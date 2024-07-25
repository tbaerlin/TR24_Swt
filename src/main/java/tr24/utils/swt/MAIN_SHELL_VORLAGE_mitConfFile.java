package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import tr24.utils.swt.SwtUtils.LAYOUT;
import tr24.utils.swt.api.IOnButtonClick;
import tr24.utils.swt.api.IShutdownShell;

import java.io.File;

/**
 * HAUPT-Einstieg
 */
public class MAIN_SHELL_VORLAGE_mitConfFile {

	private static final String CONF_FILE = "some-config-file.conf";
	
	private BasisCore core;
	private Shell shell;

	/**
	 * START HERE
	 */
	public static void main(String[] args) {
		String confFile = CONF_FILE;
		if (args.length>0) {
			String s = args[0];
			if (s.startsWith("config=")) {
				s = s.substring(7);
				confFile = s;
			}
		}
		new MAIN_SHELL_VORLAGE_mitConfFile().run(confFile);
	}

	
	private void run(String confFile) {
		Display display = new Display();
		core = new BasisCore(display);
		// versuche conf über abs-path
		File f = new File(confFile);
		if (!f.exists()) {					// wenn nicht da -> da im aktuellen Verzeichnis
			f = new File(".", confFile);
		}
		ApplicationConfig conf = new ApplicationConfig(f);
		conf.loadConfigFile();
		
		shell = new Shell(display);
		shell.setText("DEV...");
		shell.setLayout(new FormLayout());
		
		core.mainShell = shell;
		core.appConfig = conf;
		
		Rectangle bounds = conf.getMainShellPosition(display.getMonitors());
		shell.setBounds(bounds);
		core.add2ShutdownShell(new IShutdownShell() {
			@Override
			public void shellShutdown(ApplicationConfig conf) {
				conf.setMainShellPosition(shell.getBounds());
			}
		});
		
		// höre auf Close der main-Shell:
		shell.addListener(SWT.Close, new Listener() {
			/** call kommt
			 * 1) vom User
			 * 2) vom Shutdown-Proc wenn alles rum ist
			 */
			@Override
			public void handleEvent(Event event) {
				core.triggerAppShutdown();
				if (!core.shellCanClose()) {
					event.doit = false;			// beim erste Durchgang: nöö, noch nicht
				}
			}
		});
		
		Composite boxOben = LAYOUT.layout_ObenLeiste(shell, 40, 3, FARBE.BLUE_5);
		LAYOUT.layout_button("Exit", 10, 5, 40, 25, boxOben, new IOnButtonClick() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});
		
		Composite boxUnten = new Composite(shell, SWT.NONE);
		boxUnten.setBackground(FARBE.YELLOW_5);
		LAYOUT.layout_useRestOfSpace(boxUnten, boxOben, 3);
		
		shell.open();
		core.runSwtLoop(shell);
	}
	
}













