package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Button für Farb-Auswahl:
 * - Button zeigt die aktuelle Farbe
 * - dann gibt's ein Farb-Gitter (Popup-Menu style)
 * 
 * siehe {@link #main(String[])} für Bsp
 * 
 * @author tbaer
 *
 */
public class CoolColorPicker {
	
	private static final int MAP_WIDTH  = 19;
	private static final int MAP_HEIGHT = 11;
	
	private static final int[][][] colMapData = new int[][][] {
		{ {250,250,250}, {245,245,245}, {238,238,238}, {224,224,224}, {189,189,189}, {158,158,158}, {117,117,117}, {97,97,97}, {66,66,66}, {33,33,33}, {0,0,0} },
		{ {255,235,237}, {255,205,210}, {238,154,154}, {229,115,115}, {238,83,79}, {244,66,54}, {229,57,53}, {211,48,43}, {198,40,39}, {182,28,28}, {166,0,0} },
		{ {251,228,236}, {249,187,208}, {244,143,177}, {240,98,146}, {236,64,122}, {234,30,99}, {216,26,96}, {194,23,91}, {173,20,87}, {137,14,79}, {113,0,45} },
		{ {243,229,246}, {225,190,232}, {207,147,217}, {185,104,199}, {170,71,188}, {156,40,177}, {142,36,170}, {122,31,162}, {106,27,154}, {74,20,140}, {38,0,118} },
		{ {238,232,246}, {208,196,232}, {179,157,219}, {150,117,206}, {126,87,194}, {103,59,183}, {93,53,176}, {81,45,167}, {69,40,159}, {48,27,146}, {6,0,124} },
		{ {232,234,246}, {197,202,232}, {158,168,219}, {121,134,204}, {92,107,192}, {63,81,181}, {57,73,171}, {48,62,159}, {40,53,147}, {26,35,126}, {0,0,100} },
		{ {228,242,253}, {187,222,250}, {144,202,248}, {100,181,246}, {66,165,246}, {33,150,243}, {29,137,228}, {25,118,211}, {21,100,192}, {14,71,161}, {0,33,141} },
		{ {225,245,254}, {179,229,252}, {129,213,250}, {79,194,248}, {40,182,246}, {3,169,245}, {3,155,230}, {2,136,209}, {2,119,189}, {0,87,156}, {0,53,136} },
		{ {223,247,249}, {178,235,242}, {128,222,234}, {77,208,226}, {37,198,218}, {0,188,213}, {0,172,194}, {0,152,166}, {0,130,143}, {1,96,100}, {0,64,68} },
		{ {224,242,242}, {178,223,220}, {128,203,196}, {76,182,172}, {38,165,154}, {0,151,136}, {0,136,122}, {0,121,106}, {0,105,91}, {0,76,63}, {0,40,25} },
		{ {232,246,233}, {200,230,202}, {165,214,167}, {128,199,131}, {102,187,106}, {76,176,80}, {67,160,71}, {57,142,61}, {47,125,50}, {28,94,32}, {0,62,0} },
		{ {241,247,233}, {221,237,200}, {197,225,166}, {174,213,130}, {156,204,102}, {139,194,74}, {125,179,67}, {104,159,57}, {84,139,46}, {51,105,30}, {9,75,0} },
		{ {249,251,230}, {240,244,194}, {230,238,155}, {221,231,118}, {212,224,86}, {205,220,57}, {192,202,51}, {176,180,43}, {158,158,36}, {129,119,22}, {103,91,0} },
		{ {255,253,232}, {255,250,195}, {255,245,156}, {255,241,118}, {255,238,88}, {255,235,60}, {253,215,52}, {250,192,46}, {249,168,37}, {244,127,22}, {242,99,0} },
		{ {254,248,224}, {255,236,178}, {255,224,131}, {255,213,79}, {255,201,40}, {254,193,7}, {255,178,0}, {255,159,0}, {255,142,1}, {255,111,0}, {255,81,0} },
		{ {255,242,223}, {255,224,178}, {255,204,128}, {255,182,77}, {255,168,39}, {255,151,0}, {251,140,0}, {246,124,1}, {239,108,0}, {230,81,0}, {226,47,0} },
		{ {251,233,231}, {255,204,187}, {255,171,145}, {255,138,102}, {255,113,67}, {254,87,34}, {245,81,30}, {230,74,25}, {215,67,21}, {191,54,12}, {179,16,0} },
		{ {239,235,232}, {215,204,200}, {188,171,164}, {160,136,126}, {140,110,99}, {121,85,71}, {109,77,66}, {93,64,56}, {77,52,47}, {62,38,34}, {24,0,0} },
		{ {235,239,242}, {207,216,221}, {176,191,198}, {144,164,173}, {120,144,156}, {96,125,139}, {84,111,122}, {70,90,101}, {54,71,79}, {39,50,56}, {0,8,16} }		
	};
	
	
	/**
	 * User-Code kann AKTIV informiert werden wenn sich was gändert hat
	 */
	public interface IColorPickedListener {
		/**
		 * call kommt in SWT, bitte schnell beenden
		 */
		public void onColorPicked(Color color);
	}
	
	/**
	 * wird on-demand gebaut
	 */
	private static Color[][] colMap;
	
	private final Display display;
	private final Composite btnBox;
	IColorPickedListener lisOrNull;
	private Point size = new Point(10, 10);
	Color curBtnCol;		// die wird auch nach aussen gezeigt
	boolean mouseOverButton;
	
	Shell curDialog;
	Color selectedCol;		// null oder gesetzt
	int lastI=-1, lastJ=-1;
	int rahmenI, rahmenJ;
	
	/**
	 * Button kann on/off sein
	 */
	private boolean enabled;
	
	/**
	 * singleton field!
	 */
	static private Pattern patDisabled;
	
	/**
	 * Liefert die aktuelle Farbe (passives Abfragen) <br>
	 * - geht auch in NICHT-SWT
	 */
	public Color getPickedColor() {
		return curBtnCol;
	}
	
	/**
	 * Änder die aktuelle Farbe von aussen
	 * - call muss in SWT sein, Box wird direkt NEU gezeichnet
	 */
	public void setColor(Color col) {
		if (col==null) {
			curBtnCol = FARBE.BLACK;
			return;
		}
		curBtnCol = col;
		btnBox.redraw();
	}
	
	/**
	 * constructor
	 * 
	 * @param lisOrNull - user-code kann aktiv informiert werden, oder über {@link #getPickedColor()}
	 */
	public CoolColorPicker(Composite parent, Color startColor, IColorPickedListener lisOrNull) {
		
		curBtnCol = startColor!=null ? startColor : FARBE.BLACK;		// sicher ist sicher
		this.lisOrNull = lisOrNull;
		display = parent.getDisplay();
		btnBox = new Composite(parent, 0);
		enabled = true;		// starte so
		
		// erkenne Mouse-drüber/runter
		btnBox.addMouseTrackListener(new MouseTrackListener() {
			@Override
			public void mouseHover(MouseEvent e) {
			}
			@Override
			public void mouseExit(MouseEvent e) {
				mouseOverButton = false;
				btnBox.redraw();
			}
			@Override
			public void mouseEnter(MouseEvent e) {
				mouseOverButton = true;
				btnBox.redraw();
			}
		});
		
		// malen:
		btnBox.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				// System.out.println("paint btn box, active = " + isAcitve);
				GC gc = e.gc;
				gc.setBackground(curBtnCol);
				gc.fillRoundRectangle(0, 0, size.x, size.y, 3, 3);
				if (enabled==false) {			// off-button: male ein Gitter drüber
					gc.setBackgroundPattern(getPattern());
					gc.fillRoundRectangle(0, 0, size.x, size.y, 3, 3);
				}
				gc.setForeground(FARBE.BLACK);
				gc.drawRectangle(0, 0, size.x-1, size.y-1);
				
				if (mouseOverButton && enabled==true) {
					gc.setAntialias(SWT.ON);
					gc.setForeground(FARBE.WHITE);
					gc.drawRoundRectangle(3, 3, size.x-7, size.y-7, 3, 3);
				}
			}
		});
		
		// erkenne Click
		btnBox.addMouseListener(new MouseAdapter() {
			/**
			 * click wenn Maus wieder hochging
			 */
			@Override
			public void mouseUp(MouseEvent e) {
				if (enabled==false) {
					return;					// ignore
				}
				// rechne gleich in absolut um => öffne die Shell immer direkt an der Maus -> gut
				Point clickPoint = btnBox.toDisplay(e.x, e.y);
				startPopupShell(clickPoint);
			}
		});
	}
	
	/**
	 * Listener kann auch später reinkommen
	 */
	public void setListener(IColorPickedListener iColorPickedListener) {
		lisOrNull = iColorPickedListener;
	}
	

	
	public void setBounds(int x, int y, int w, int h) {
		btnBox.setBounds(x, y, w, h);
		size.x = w;
		size.y = h;
	}
	
	/**
	 * Picker-Button kann enabled/disabled sein <br>
	 * - call bitte in SWT
	 * 
	 */
	public void setEnabled(boolean onOff) {
		enabled = onOff;
		btnBox.redraw();
	}
	
	/**
	 * on-demand: Erzeuge Pattern für enabled=false Button
	 * - erzeug's STATISCH nur einmal
	 */
	protected Pattern getPattern() {
		if (patDisabled==null) {
			patDisabled = new Pattern(display, 0, 0, 2, 2, FARBE.BLACK, 100, FARBE.WHITE, 100);
		}
		return patDisabled;
	}



	
	
	void startPopupShell(Point clickPoint) {
		initColors();
		
		final int fac = 20;
		final int randX = 10;
		
		// Berechne die x-Werte für die beiden Vergleichs-Boxen
		int w = MAP_WIDTH*fac;
		final int x1 = randX+10;
		final int x2 = w/2-10 + randX;
		final int x3 = w/2+10 + randX;
		final int x4 = randX + w-10;
		
		// System.err.println("open shell at " + clickPoint);
		final Shell dialog = new Shell(display, SWT.NONE);
		int x = clickPoint.x - 5;
		int y = clickPoint.y - 80;
		dialog.setBounds(x, y, MAP_WIDTH*fac + 2*randX, MAP_HEIGHT*fac + 70);

		// Nice: Male einen Rahmen um die "start-Farbe"
		setRahmen();
		
		// Idee: schliesse das Ding DIREKT wieder wenn der Focus weg-geht: User klickt irgendwo anders
		dialog.addFocusListener(new FocusListener() {			// ===> GUUUT: Das mit "delayed shell.close" FUNZT !!
			@Override
			public void focusLost(FocusEvent e) {
				triggerClose();
			}
			@Override
			public void focusGained(FocusEvent e) { }
		});
		
		/**
		 * ich male die Farb-Matrix selber!
		 */
		dialog.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				// System.err.println("PAINT grid");
				GC gc = e.gc;
				// gc.setAntialias(SWT.ON);
				
				// oben: zeige select-Farbe vor weissem und schwarzem Hintergrund
				gc.setBackground(FARBE.WHITE);
				gc.fillRectangle(x1, 10, (x2-x1), 40);
				gc.setBackground(FARBE.BLACK);
				gc.fillRectangle(x3, 10, (x4-x3), 40);
				
				if (selectedCol!=null) {
					gc.setBackground(selectedCol);
					gc.fillRoundRectangle(x1+15,  20, (x2-x1)-30, 20, 4, 4);
					gc.fillRoundRectangle(x3+15,  20, (x4-x3)-30, 20, 4, 4);
				}
				
				for (int i=0; i<MAP_WIDTH; i++) {
					int x = randX + i*fac;
					for (int j=0; j<MAP_HEIGHT; j++) {
						int y = 60 + j*fac;
						gc.setBackground(colMap[i][j]);
						gc.fillRectangle(x, y, fac, fac);
					}
				}
				gc.setForeground(FARBE.GRAY_1);
				gc.drawRectangle(randX-2, 60-2, MAP_WIDTH*fac+2, MAP_HEIGHT*fac+2);
				
				// Rahmen um die Start-Farbe?
				if (rahmenI>=0) {
					int x = randX + rahmenI*fac;
					int y = 60 + rahmenJ*fac;
					gc.setForeground(FARBE.WHITE);
					gc.setLineWidth(3);
					// gc.drawRoundRectangle(x-4, y-4, fac+8, fac+8, 3, 3);
					gc.drawRectangle(x-1, y-1, fac+1, fac+1);
				}
			}
		});
		
		/**
		 * Ändere die zu-wählende-Farbe bei Maus-Bewegung
		 * - re-mappe maus.x/y auf Grid-i/j
		 */
		dialog.addMouseMoveListener(new MouseMoveListener() {

			@Override
			public void mouseMove(MouseEvent e) {
				int i = (e.x-randX) / fac;
				int j = (e.y-60) / fac;
				boolean ausserhalb = (e.x<10 || e.y<60 || i>=MAP_WIDTH || j>=MAP_HEIGHT);
				if (ausserhalb) {
					i = -1;  j=-1;
				}
				// Reagiere nur bei WECHSEL!
				if (i!=lastI || j!=lastJ) {
					if (i>=0 && j>=0) {
						selectedCol = colMap[i][j];
						// System.err.println("CHANGE Col");
					} else {
						selectedCol = null;
						// System.err.println("RESET Col");
					}
					lastI = i;
					lastJ = j;
					// nice: ich kann redraw mit clipping machen: am col-Grid ändert sich NIIIEE was
					dialog.redraw(x1, 10, x4, 50, false); 
				}
			}
		});
		
		// bei Click: Übernehme die Farbe, schlisse den Dialog
		dialog.addMouseListener(new MouseAdapter() {
			/**
			 * Reagiere DIREKT auf mouse-DOWN Click
			 */
			@Override
			public void mouseDown(MouseEvent e) {
				if (selectedCol!=null) {
					curBtnCol = selectedCol;
					triggerClose();
					btnBox.redraw();			// der soll die neue Farbe anzeigen
					if (lisOrNull!=null) {
						lisOrNull.onColorPicked(curBtnCol);		// feuere Info raus
					}
					return;
				}
				// reagiere auf JEDEN Click, also auch ausserhalb des Grids
				triggerClose();
			}
		});
		
		dialog.open();
		curDialog = dialog;
	}

	private void setRahmen() {
		rahmenI = rahmenJ = -1;		// erst mal nööö
		int r = curBtnCol.getRed();
		int g = curBtnCol.getGreen();
		int b = curBtnCol.getBlue();
		// suche nur GENAUEN-Match auf unsere Farben. UserCode kann ja auch ne beliebig andere Farbe rein-geben
		for (int i=0; i<MAP_WIDTH; i++) {
			for (int j=0; j<MAP_HEIGHT; j++) {
				int data[] = colMapData[i][j];
				if (data[0]==r && data[1]==g && data[2]==b) {
					rahmenI = i;
					rahmenJ = j;
					return;
				}
			}
		}
	}

	/**
	 * ich halte mir hier die Liste der Color-Objs
	 * - die ausgewählte Farbe wird auch an den Client-Code geliert
	 */
	private void initColors() {
		if (colMap==null) {		// nice: call kommt single-style, das SWT-Thread!
			colMap = new Color[MAP_WIDTH][MAP_HEIGHT];
			for (int i=0; i<MAP_WIDTH; i++) {
				for (int j=0; j<MAP_HEIGHT; j++) {
					int[] data = colMapData[i][j];
					colMap[i][j] = new Color(display, data[0], data[1], data[2]);
				}
			}
		}
	}

	/**
	 * mach das mal "eine SWT-Runde später"
	 */
	void triggerClose() {
		display.asyncExec(new Runnable() {
			public void run() {
				// System.err.println("close dialog again!");
				if (curDialog!=null && curDialog.isDisposed()==false) {			// sicher ist sicher
					curDialog.close();
					curDialog = null;
				}
			}
		});
	}
	
	
	
	/**
	 * DEV / TEST
	 */
	public static void main(String[] args) {
		Display display = new Display();
		BasisCore core = new BasisCore(display);
		Shell shell = new Shell(display);
		shell.setText("DEV Cool Color Picker");
		shell.setBounds(300, 700, 200, 160);
		core.mainShell = shell;
		
		final CoolColorPicker ccp = new CoolColorPicker(shell, FARBE.YELLOW_1, null);
		ccp.setBounds(10, 10, 40, 25);
		ccp.setListener( new IColorPickedListener() {
			@Override
			public void onColorPicked(Color color) {
				System.out.println("MAIN-test-code: neue Farbe: " + color + ",  get-Versuch: " + ccp.getPickedColor()); 
			}
		});
		
		// test: dis-able
		ccp.setEnabled(false);
		
		
		shell.open();
		core.runSwtLoop(shell);
	}

		
	
}

































