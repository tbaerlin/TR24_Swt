package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import tr24.utils.common.TimeUnit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Wrapper für einen Alarm:
 * - non-modaler Dialog
 * - mit Message
 * - und OK Button
 * 
 * @author tbaer
 */
public class AlarmDialog {

	/**
	 * zentralesr statischer Callback
	 */
	public interface IAlarmDialogCloseListener {
		public void onClosed(AlarmDialog ad);
	}

	private static IAlarmDialogCloseListener _callback = null;
	
	public static void register(IAlarmDialogCloseListener callback) {
		_callback = callback;
	}
	
	/**
	 * die Alarm-Boxen laufen von links oben nach rechts unten
	 */
	private static int startAlarmPos = 50;
	private static int nextAlarmIncrement = 50;
	
	private static Integer maxHeight = null;
	private static int pos;
	private static float ratio;
	private Shell shell;
	
	/**
	 * Der Alarm kann einen eigenen Ping haben (für jedes Popup ein anderes, wenn's der User so will)
	 */
	private File pingFile;
	/**
	 * wann wurde das File zuletzt abgespielt
	 */
	private long lastPing;
	
	private static final List<AlarmDialog> dialogList = new ArrayList<AlarmDialog>();
	private static Display theDisplay = null;
	
	/**
	 * true: jede Minute: ping
	 */
	protected boolean pingReAlarm = true;
	
	protected boolean bigSize = false;
	
	/**
	 * Schliesst alle Dialoge
	 */
	public static void closeAllDialogs() {
		if (theDisplay==null) { return; }
		theDisplay.asyncExec(new Runnable() {
			public void run() {
				for (AlarmDialog ad : dialogList) {
					ad.close();
				}
				dialogList.clear();
			}
		});
	}
	
	public static void moveAllPopupsToMonitor2() {
		if (theDisplay==null) { return; }
		theDisplay.asyncExec(new Runnable() {
			public void run() {
				// bestimme den NICHT-Primären
				Monitor[] monitors = theDisplay.getMonitors();
				if (monitors.length==1) { return; }
				int idx = 0;
				if (theDisplay.getPrimaryMonitor()==monitors[0]) {
					idx = 1;
				}
				Monitor m2 = monitors[idx];			// daaa sollen die Popups hin
				int m2X = m2.getClientArea().x;
				int m2Y = m2.getClientArea().y;
				
				for (AlarmDialog ad : dialogList) {
					if (ad.shell.isDisposed()) {
						continue;			// PRÜFEN!
					}
					Monitor popupM = ad.shell.getMonitor();		// wo ist das Popup gerade
					Point p = ad.shell.getLocation();
					// Berechne die x-Position des Popus in seinem aktuellen Monitor
					Rectangle box = popupM.getClientArea();
					int relativX = p.x - box.x;
					int relativY = p.y - box.y;
					// und rechne um, so dass es relativ-x/y gleich erscheint auf dem neuen Monitor, besonders Y ist wichtig wenn der 2. Monitor kleiner ist!
					int newX = m2X + relativX;
					int newY = m2Y + relativY;
					ad.shell.setLocation(newX, newY);
					ad.shell.forceActive();			// zeige das Popup
				}
			}
		});
	}
	

	/**
	 * Erzeuge und zeige einen Alarm-Dialog
	 * 
	 * @param imageOrNull - shell-image
	 */
	AlarmDialog(String title, String message, Display display, Image imageOrNull) {
		this(title, message, display, imageOrNull, null);
	}

	/**
	 * Erzeuge und zeige einen Alarm-Dialog, Zugriff nur über Core oder AlarmManager
	 * 
	 * @param imageOrNull - shell-image
	 * @param bgCol       - Hintergrund-Farbe, null => rot
	 */
	AlarmDialog(String title, String message, Display display, Image imageOrNull, Color bgCol) {
		if (bgCol==null) {
			bgCol = FARBE.RED_2;
		}
		if (maxHeight==null) {
			Monitor m = display.getPrimaryMonitor();
			Rectangle clientArea = m.getClientArea();
			maxHeight = clientArea.height - 200;
			ratio = (float)clientArea.height / clientArea.width;
			pos = startAlarmPos;		// start-Wert
		}
		
		/*
		 * Lerne:
		 * - parent = display => zeigt Icon in TaskBar an
		 * - SWT.CLOSE => zeigt Title mit Close-X an
		 */
		shell = new Shell(display, SWT.CLOSE | SWT.RESIZE);
		shell.setBackground(bgCol);
		shell.setText(title);
		shell.setSize(285, 160);
		if (imageOrNull!=null) {
			shell.setImage(imageOrNull);
		}
		
		// Position von links oben nach rechts unten durch-rutschen lassen
		int yPos = (int) (pos * ratio);
		shell.setLocation(pos, yPos);
		pos += nextAlarmIncrement;
		if (yPos >= maxHeight) {
			pos = startAlarmPos; 
		}
		shell.setLayout(new FormLayout());
		
		FormData fd;
		
		Label lbl = new Label(shell, SWT.NONE);
		lbl.setBackground(FARBE.GRAY_5);
		lbl.setAlignment(SWT.CENTER);
		lbl.setText("\n" + message);
		// lbl.setBackground(FARBE.YELLOW_1);
		fd = new FormData();
		fd.left = new FormAttachment(0, 5);
		fd.top  = new FormAttachment(0, 5);
		fd.right = new FormAttachment(100, -5);
		fd.bottom = new FormAttachment(100, -34-16-2);
		lbl.setLayoutData(fd);
		
		Button ok = new Button(shell, SWT.FLAT);
		// Zeige die Uhrzeit mit an
		String okTxt = new TimeUnit().toStringFull() + "    - OK -";
		ok.setText(okTxt);
		fd = new FormData();
		fd.left = new FormAttachment(0, 5);
		fd.top  = new FormAttachment(100, -33);
		fd.right = new FormAttachment(100, -5);
		fd.bottom = new FormAttachment(100, -3);
		ok.setLayoutData(fd);
		
		ok.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		// Schliesse auch bei ESC
		ok.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
			}
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.ESC) {
					shell.close();
				}
			}
		});

		// ping-no-more
		Button pnm = new Button(shell, SWT.CHECK);
		pnm.setText("PNM");
		pnm.setBackground(bgCol);
		fd = new FormData();
		fd.left = new FormAttachment(100, -50);
		fd.top  = new FormAttachment(100, -34-16);
		fd.right = new FormAttachment(100, -5);
		fd.bottom = new FormAttachment(100, -34);
		pnm.setLayoutData(fd);
		pnm.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pingReAlarm = !pingReAlarm;
			}
		});
		
		// zeige den Dialog grösser -> für Exceptions
		Button bigger = new Button(shell, SWT.CHECK);
		bigger.setText("Big");
		bigger.setBackground(bgCol);
		fd = new FormData();
		fd.left = new FormAttachment(100, -40-50);
		fd.top  = new FormAttachment(100, -34-16);
		fd.right = new FormAttachment(100, -5-50);
		fd.bottom = new FormAttachment(100, -34);
		bigger.setLayoutData(fd);
		bigger.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!bigSize) {
					shell.setSize(350, 250);
					bigSize = true;
				} else {
					shell.setSize(285, 160);
					bigSize = false;
				}
			}
		});
		
		
		// wenn die Shell zugeht: lösche den Sound-Eintrag
		shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				if (_callback!=null) {
					_callback.onClosed(AlarmDialog.this);
				}
			}
		});

		// fertig, jetzt zeigen:
		shell.open();
		
		// dirty for now: alle Dialoge merken, so dass sie von aussen ge-close-t werden können
		theDisplay = display;		// muss auch statisch bekannt sein
		dialogList.add(this);
	}

	/**
	 * Schliesse den Dialog
	 */
	public void close() {
		if (!shell.isDisposed()) {
			shell.close();
			shell.dispose();
		}
	}

	
	
}














