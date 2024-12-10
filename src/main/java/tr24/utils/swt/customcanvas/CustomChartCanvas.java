package tr24.utils.swt.customcanvas;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import tr24.algoutils.trading.Contract;
import tr24.utils.swt.FARBE;
import tr24.utils.swt.customcanvas.api.CustomChartPainter;
import tr24.utils.swt.customcanvas.api.ICustomChartClient;
import tr24.utils.swt.customcanvas.api.IMousePosCallback;

import java.util.*;
import java.util.List;

/**
 * Zeichenfläche für beliebige Dinge:
 * 
 * - User-Code hängt sich per {@link ICustomChartClient} rein
 * - und kann was zeichnen
 * - das Chart kann scrollen und skalieren
 */
public class CustomChartCanvas {

	private final Display display;
	private Canvas canvas;
	private MouseInteraction userDragCode;
	
	protected int mouseX, mouseY, oldMouseX, oldMouseY;
	protected MouseDragData drag = new MouseDragData();
	protected boolean mouseOnCanvas;

	private List<ICustomChartClient> clients = new ArrayList<ICustomChartClient>();
	private IMousePosCallback mousePosCallback = null;
	private ClientPainter painter;
	private Rectangle canvasBounds;

	private float defaultXMin=-200, defaultXMax=200;
	private int   defaultRandLinks = 0;
	private float defaultYMin=-200, defaultYMax=200;
	
	/**
	 * true wenn ein MouseInteraction-Dragging aktiv ist
	 */
	protected boolean userDragActive;
	
	/**
	 * flag: wird true wenn VOR einem Paint noch ein onAreaChange() call kommen muss
	 */
	private boolean need2CallClientUpdate;

	/**
	 * default = an, User kann disablen
	 */
	private boolean enabledRightButtonDrag = true;
	
	/**
	 * true: re-scale das Grid wenn sich die Canvas resize-t
	 */
	private boolean scaleOnResize = true;
	
	/**
	 * wenn gesetzt: male auf diese Bild
	 */
	private Image drawOnImage;
	
	
	
	/**
	 * Einhäng/Intercept - Interface für User-Code, um:
	 * - clicks zu erkennen
	 * - drags zu erkennen
	 */
	public static interface MouseInteraction {
		/**
		 * sobald die Maus gedrückt wird kommt diese call
		 * @param xp   - echte Pixel
		 * @param valX - umgerechnet in Werte-Bereich
		 * @param buttonCode - WELCHE Maus-Taste? 1=links, 2=Mitte, 3=rechts
		 * 
		 * @return true - wenn der User-Code übernimmt, d.h. das Chart wird sich NICHT VERSCHIEBEN,
		 *                sondern überlässt dem User-Code die Drag-Bewegung.
		 *                es kommen dann alle MouseMoves mit {@link #onMouseDrag(int, int, float, float)}.
		 *                Wenn der User die Maus wieder loslässt kommt abschliessend ein onMouseUp(int, int, float, float) <br>
		 *         		  Bei true macht das Chart auch sofort eine RE-DRAW
		 *         
		 *         false - wenn der User-Code nicht draggen will. Das Chart macht seine (normale) VERSCHIEBUNG       
		 *                 
		 */
		public boolean onMouseDown(int xp, int yp, float valX, float valY, int buttonCode);
		
		/**
		 * kommt nur wenn {@link #onMouseDown(int, int, float, float, int)} TRUE geliefert hat
		 * 
		 * @return true wenn ein redraw kommen soll
		 */
		public boolean onMouseDrag(int toXp, int toYp, float toValX, float toValY);
		
		/**
		 * kommt immer wenn die Maus losgelassen wird
		 * 
		 * @return true wenn ein re-draw kommen soll
		 */
		public boolean onMouseUp(int xp, int yp, float valX, float valY, int buttonCode);
		
		/**
		 * Höre auch auf alle Keyboard-Eingaben, 
		 * HIER kommt auch die "MIDDLE-MOUSE-BUTTON" an!!!
		 */
		public void onKey(String key);
	}
	
	/**
	 * der User-Code will die Maus-Clicks und -Drags auch mitbekommen
	 */
	public void setMouseInteraction(MouseInteraction userCode) {
		this.userDragCode = userCode;
	}
	
	/**
	 * constructor
	 */
	public CustomChartCanvas(Composite parent) {
		this(parent, FARBE.BLUE_6);
	}
	
	/**
	 * Soll das Grid an die Canvas-Änderung angepasst werden?
	 * - default ist off
	 */
	public void setRescaleOnResize(boolean onOff) {
		scaleOnResize = onOff;
	}
	
	/**
	 * Image-Erzeuger Version
	 * - code muss dann drawImage() aufrufen!
	 */
	public CustomChartCanvas(Image drawOnImage, Color bgColor) {
		this.drawOnImage = drawOnImage;
		display = null;
	}
	
	/**
	 * für die Image-Erzeuger-Version
	 * 
	 * - der GC muss von aussen gemanagt werden!
	 */
	public void drawImage(GC gc) {
		canvasBounds = drawOnImage.getBounds();		// muss VOR centerGrid kommen!
		painter = new ClientPainter();
		painter.centerGrid();
		painter.updateClients();
		
		// triggere das Zeichnen in allen registrierten Painter-Handlern:
		painter.gc = gc;
		
		for (ICustomChartClient cl : clients) {
			cl.paint(gc, painter, canvasBounds);
		}
	}
	
	/**
	 * constructor
	 */
	public CustomChartCanvas(Composite parent, Color bgColor) {
		// this.userDragCode = userDragCode;
		canvas = new Canvas(parent, SWT.BORDER | SWT.DOUBLE_BUFFERED);
		if (bgColor!=null) {
			canvas.setBackground(bgColor);
		}
		painter = new ClientPainter();
		// canvasBounds = new Rectangle(0, 0, 0, 0);
		this.display = parent.getDisplay();
		
		canvas.addPaintListener(new PaintListener() {
			
			@Override
			public void paintControl(PaintEvent e) {
				
				if (need2CallClientUpdate) {
					painter.updateClients();
					need2CallClientUpdate = false;
				}
				
            	int width = e.width;
            	int height = e.height;
				GC gc = e.gc;
				painter.gc = gc;
				for (ICustomChartClient cl : clients) {
					cl.paint(gc, painter, canvasBounds);
				}

				if (mouseOnCanvas) {
					boolean paintMousePos = true;
					if (mousePosCallback!=null) {
						float realX = painter.pix2x(mouseX);
						float realY = painter.pix2y(mouseY);
						paintMousePos = mousePosCallback.mouseOver(mouseX, mouseY, realX, realY, gc, painter, canvasBounds);
					}
					// zeige "x=100, y=120" wenn's der Client-Code nicht schon gemacht hat
					if (paintMousePos==true) {		
						//	String s = "X-Ray: x(" + f(painter.xMin) + ".." + f(painter.xMax) 
						//    + ") y(" + f(painter.yMin) + ".." + f(painter.yMax) + ")"; 
						String s = ""; 
						// Anzeige: wo ist die Maus in Grid-Koordinaten
						float x = (mouseX-painter.xOffset)/painter.xScale;
						float y = (mouseY-painter.yOffset)/painter.yScale;
						s += "  " + f(x) + "/" + f(y);
						gc.setForeground(FARBE.ALMOST_BLACK);
						gc.drawString(s, width/2, height-20, true);
					}
					
					// zeige Maus-Kreuz
					gc.setForeground(FARBE.RED_DARK_1);
					gc.setLineWidth(1);
					gc.setLineStyle(SWT.LINE_DOT);
					gc.drawLine(0, mouseY, width, mouseY);
					gc.drawLine(mouseX, 0, mouseX, height);
				}
			}
			private String f(float zahl) {
				// return String.valueOf(zahl);
				return Contract.print(zahl, 1);
			}
		});
		canvas.addListener(SWT.Resize, new Listener () {
		    public void handleEvent (Event e) {
		    	if (canvasBounds==null) {		// WICHTIG: beim ERSTEN Call: setzte die scales/offsets
			    	canvasBounds = canvas.getClientArea();
			    	painter.centerGrid();
		    	} else {		
		    		canvasBounds = canvas.getClientArea();		// Im Betrieb: scaliere das Grid mit der Canvas
		    		if (scaleOnResize) {
		    			painter.rescale();
		    		}
		    	}
		    }
		});
		
		canvas.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
            	// gab's ne Veränderung ?
            	if (oldMouseX!=e.x || oldMouseY!=e.y) {
            		mouseX = e.x;
            		mouseY = e.y;
            		
            		// liegt ein Dragging vor ?
            		if (drag.leftDown) {
            			int xDiff = mouseX - drag.lastX;
            			int yDiff = mouseY - drag.lastY;
            			drag.lastX = mouseX;
            			drag.lastY = mouseY;
            			// left-drag: VERSCHIEBE das Grid
            			painter.moveGrid(xDiff, yDiff);
            		}
            		if (drag.rightDown) {
            			int xDiff = mouseX - drag.lastX;
            			int yDiff = mouseY - drag.lastY;
            			drag.lastX = mouseX;
            			drag.lastY = mouseY;
            			// right-drag: SCALIERE das Grid
            			painter.scaleGrid(xDiff, yDiff);
            		}
            		if (userDragActive) {
            			userDragCode.onMouseDrag(e.x, e.y, painter.pix2x(e.x), painter.pix2y(e.y));
            			// true/false result hier egal, weil EH ein Redraw kommt
            		}
            		canvas.redraw();
            	}
			}
		});
		canvas.addMouseListener(new MouseListener() {
			@Override
			public void mouseDown(MouseEvent e) {
				
				// Sonder-Ding: wenn die CCC den Right-Scale selbst NICHT unterstützt
				// => melde auch ein Right-Drag an den Client
				boolean userRightDrag = !enabledRightButtonDrag && e.button==3;
				if (e.button==1 || userRightDrag) {		// Links oder user-right-drag
					if (userDragCode!=null) {
						int button = userRightDrag ? 3 : 1;
						userDragActive = userDragCode.onMouseDown(e.x, e.y, painter.pix2x(e.x), painter.pix2y(e.y), button);
						if (userDragActive) {
							canvas.redraw();
							return;		// bin fertig hier, KEIN Canvas-Verschieben einleiten!
						}
					}
					drag.leftDown = true;
//					drag.xStart = e.x;
//					drag.yStart = e.y;
					drag.lastX = e.x;
					drag.lastY = e.y;
				}
				if (e.button==3) {
					if (enabledRightButtonDrag) {		// rechts
						drag.rightDown = true;
//						drag.xStart = e.x;
//						drag.yStart = e.y;
						drag.lastX = e.x;
						drag.lastY = e.y;
					}
				}
				if (e.button==2 && !enabledRightButtonDrag) {
					if (userDragCode!=null) {
						if (userDragCode.onMouseDown(e.x, e.y, painter.pix2x(e.x), painter.pix2y(e.y), 2)) {
							canvas.redraw();
						}
					}
				}
			}
			@Override
			public void mouseUp(MouseEvent e) {
				
				boolean userRightDrag = !enabledRightButtonDrag && e.button==3;
				if (e.button==1 || userRightDrag) {
					if (userDragCode!=null) {
						int button = userRightDrag ? 3 : 1;
						boolean needRedraw = userDragCode.onMouseUp(e.x, e.y, painter.pix2x(e.x), painter.pix2y(e.y), button);
						userDragActive = false; 		// so oder so
						if (needRedraw) {
							canvas.redraw();
						}
					}
					drag.leftDown = false;
				}
				if (e.button==3 && enabledRightButtonDrag) {
					drag.rightDown = false;
				}
				// mittlere Taste gedrückt: alles Scale/Move reset:
				if (e.button==2) {
					if (enabledRightButtonDrag) {	// re-center macht auch nur Sinn, wenn right-scaling an ist
						painter.centerGrid();
						canvas.redraw();
					} else {
						userDragCode.onMouseUp(e.x, e.y, painter.pix2x(e.x), painter.pix2y(e.y), 2);
					}
				}
			}
			@Override
			public void mouseDoubleClick(MouseEvent e) {
			}
		});
		canvas.addMouseTrackListener(new MouseTrackListener() {
			@Override
			public void mouseHover(MouseEvent e) {
			}
			@Override
			public void mouseEnter(MouseEvent e) {
				mouseOnCanvas = true;
			}

			@Override
			public void mouseExit(MouseEvent e) {
				mouseOnCanvas = false;
				if (mousePosCallback!=null) {
					mousePosCallback.mouseExit();
				}
				canvas.redraw();
			}
		});
		canvas.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				String s = String.valueOf(e.character);
				if (userDragCode!=null) {
					userDragCode.onKey(s);
				}
			}
		});
	}

	/**
	 * UserCode kann hier das rechte-Maus-Scaling ausschalten
	 */
	public void setRightMouseButtonDraw(boolean onOff) {
		this.enabledRightButtonDrag = onOff;
	}
	
	public Control getControl() {
		return canvas;
	}

	public void redraw() {
		if (display.isDisposed()) { return; }
		if (display.getThread() == Thread.currentThread()) {
			canvas.redraw();
		} else {
			display.asyncExec(new Runnable() {
				public void run() {
					canvas.redraw();
				}
			});
		}
	}

	public void shutdown() {
	}

	
	/**
	 * Jemand will was zeichnen
	 */
	public void register(ICustomChartClient client) {
		clients.add(client);
	}
	
	/**
	 * es kann EINEN Handler geben, der ne Info zur aktuelle Maus-Position ausgibt
	 * - zB welcher Trade gerade unter der Maus ist
	 */
	public void setMousePositionCallback(IMousePosCallback callback) {
		this.mousePosCallback = callback;
	}

	/**
	 * setzt wie {@link #setXRange(float, float)} den Bereich
	 * ABER zusätzlich einen linken Rand (in Pixel)
	 * - das ist praktisch wenn zB links immer die y-Wert (Grid) angezeigt werden
	 */
	public void setXRange(float xMin, float xMax, int randLinks) {
		defaultXMin = xMin;
		defaultXMax = xMax;
		defaultRandLinks = randLinks;
		if (canvasBounds!=null) {			// NUR wenn's schon geht
			// rechne xScale und xOffset neu aus
			painter.xScale = (canvasBounds.width-10-randLinks) / (xMax-xMin);
			painter.xOffset = Math.round(canvasBounds.width-5 - xMax*painter.xScale) + randLinks; 
			painter.xMin = xMin;
			painter.xMax = xMax;
			need2CallClientUpdate = true;
		}
	}
	
	/**
	 * Zeige DIESEN Bereich an
	 */
	public void setXRange(float xMin, float xMax) {
		setXRange(xMin, xMax, defaultRandLinks);
	}
	
	/**
	 * Zeige DIESEN Bereich an
	 */
	public void setYRange(float yMin, float yMax) {
		defaultYMin = yMin;
		defaultYMax = yMax;
		if (canvasBounds!=null) {
			// rechne xScale und xOffset neu aus
			painter.yScale = -(canvasBounds.height-10) / (yMax - yMin);		// lasse Rand am Canvas
			painter.yOffset = Math.round(5 - yMax*painter.yScale);
			painter.yMin = yMin;
			painter.yMax = yMax;
			need2CallClientUpdate = true;
		}
	}

	/**
	 * Zeichen-callbacks von den Clients
	 * - hier lebt auch die x/y auf pixel-x/y Transformation
	 */
	public class ClientPainter implements CustomChartPainter {

		public GC gc;
		
		public float xMin, xMax, yMin, yMax;
		
		public float xScale = 1.0f;
		public float yScale = -1.0f;		// muss negativ sein, das in SWT 0/0 oben/links liegt
		public int xOffset;
		public int yOffset;

		public float xMin() { return xMin; }
		public float xMax() { return xMax; }
		public float yMin() { return yMin; }
		public float yMax() { return yMax; }
		
		/**
		 * die Canvas-Größe hat sich geändert:
		 * - update die Grid-Größe
		 */
		public void resize() {
			xMin = (canvasBounds.x-xOffset) / xScale;
			xMax = (canvasBounds.width-xOffset) / xScale;
			yMax = (canvasBounds.y-yOffset) / yScale;
			yMin = (canvasBounds.height-yOffset) / yScale;
			updateClients();
		}
		private void updateClients() {
			// informiere die Clients
			for (ICustomChartClient client : clients) {
				client.onAreaChange(xMin, xMax, yMin, yMax);
			}
		}

		/**
		 * scaliere das Gitter, das geht in x und y Richtung GETRENNT
		 * @param xDiff
		 * @param yDiff
		 */
		public void scaleGrid(int xDiff, int yDiff) {
			xScale *= (1.0f + xDiff*0.01f);
			yScale *= (1.0f - yDiff*0.01f);
			resize();
		}
		/**
		 * verschiebt das Gitter, kommt bei Drag-Bewegungen
		 * @param xDiff
		 * @param yDiff
		 */
		public void moveGrid(int xDiff, int yDiff) {
			xOffset += xDiff;
			yOffset += yDiff;
			resize();		// xMin... neu berechnen
		}
		/**
		 * reset das Gitter auf die Default-Werte (die vom User verändert worden können sein)
		 */
		public void centerGrid() {
			setXRange(defaultXMin, defaultXMax);
			setYRange(defaultYMin, defaultYMax);
			resize();
		}
		
		/**
		 * passe die scales an die aktuelle Grösse an
		 */
		public void rescale() {
			painter.xScale = (canvasBounds.width-10-defaultRandLinks) / (xMax-xMin);
			painter.xOffset = Math.round(canvasBounds.width-5 - xMax*painter.xScale) + defaultRandLinks; 
			painter.yScale = -(canvasBounds.height-10) / (yMax - yMin);		// lasse Rand am Canvas
			painter.yOffset = Math.round(5 - yMax*painter.yScale);
		}
		
		/**
		 * wieviele Pixel nimmt dieser x-Bereich ein
		 */
		@Override
		public int xBreite(double xDiff) {
			return (int) Math.round(xDiff*xScale);
		}
		/**
		 * wieviele Pixel nimmt dieser y-Bereich ein
		 */
		@Override
		public int yBreite(double yDiff) {
			return (int) Math.round(-yDiff*yScale);
		}
		
		/**
		 * rechnet die x-Koordinate in echte x-Pixel um
		 */
		@Override
		public int x2pixel(float x) {
			return Math.round(x*xScale+xOffset);
		}
		/**
		 * rechnet die y-Koordinate in echte y-Pixel um
		 */
		@Override
		public int y2pixel(float y) {
			return Math.round(y*yScale+yOffset);
		}

		public float pix2x(int xp) {
			return (xp-xOffset) / xScale;
		}
		public float pix2y(int yp) {
			return (yp-yOffset) / yScale;
		}
		
		/**
		 * Zeichne eine Linie, die Angaben sind x/y und werden intern in pixel-x/y umgerechnet
		 */
		@Override
		public void line(float x1, float y1, float x2, float y2, Color farbe) {
			gc.setForeground(farbe);
			int px1 = x2pixel(x1);
			int px2 = x2pixel(x2);
			int py1 = y2pixel(y1);
			int py2 = y2pixel(y2);
			gc.drawLine(px1, py1, px2, py2);
		}
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void circle(float x, float y, int radiusPixels, Color farbe) {
			gc.setForeground(farbe);
			int xp = x2pixel(x);
			int yp = y2pixel(y);
			gc.drawOval(xp-radiusPixels, yp-radiusPixels, radiusPixels*2, radiusPixels*2);
		}
		/**
		 * Zeichnet ein Dreick (OrderMarker) an den Punkt x/y
		 */
		@Override
		public void marker(float x, float y, Color farbe, int size) {
			gc.setBackground(farbe);
			int xp = x2pixel(x);
			int yp = y2pixel(y);
			gc.fillPolygon(new int[] { xp, yp, xp-size, yp+size, xp-size, yp-size });
		}
	}
	
	
	/**
	 * Kapselt die Maus-Drag-Bewegungen
	 * - singleton style: wird von den swt-Listenern befüllt und gelesen
	 */
	private class MouseDragData {
		public boolean leftDown = false;
		public boolean rightDown = false;
		// public int xStart, yStart;		// wo ging die Bewegung los
		/**
		 * wo war der letzte mouse-Punkt 
		 * - wichtig zum erkenne wie weit mit der aktuellen Bewegung gedraggt wurde
		 */
		public int lastX, lastY;
	}
}
































