package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import tr24.utils.swt.SwtUtils.LAYOUT;
import tr24.utils.swt.api.IOnButtonClick;

/**
 * Tester für den coolen Layout-Algo
 */
public class MAIN_SHELL_VORLAGE_2 {

	private BasisCore core;
	private Shell shell;


	public static void main(String[] args) {
		new MAIN_SHELL_VORLAGE_2().run();
	}

	
	private void run() {
		Display display = new Display();
		core = new BasisCore(display);
		shell = new Shell(display);
		shell.setText("DEV...");
		core.mainShell = shell;
		shell.setBounds(100, 10, 500, 500);
		shell.setLayout(new FormLayout());
		
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













