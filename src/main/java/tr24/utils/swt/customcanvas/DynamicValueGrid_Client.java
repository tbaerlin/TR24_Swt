package tr24.utils.swt.customcanvas;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import tr24.utils.common.Utils;
import tr24.utils.swt.FARBE;
import tr24.utils.swt.customcanvas.api.CustomChartPainter;
import tr24.utils.swt.customcanvas.api.ICustomChartClient;

/**
 * - Zeichnet ein Gitter auf das Custom-Chart
 * - die Wert-Abstände ergeben sich dynamisch! cooool
 */
public class DynamicValueGrid_Client implements ICustomChartClient {

	private static final double[] diffs = new double[] {
		0.00001, 0.0002, 0.0005,
		0.001, 0.002, 0.005,
		0.01, 0.02, 0.05,
		0.1, 0.2, 0.5,
		1, 2, 5,
		10, 20, 50,
		100, 200, 500,
		1000, 2000, 5000,
		10000, 20000, 50000, 
		100000, 200000, 500000,
		1000000
	};
	
	private final int anzahlLinien_X;
	private final int anzahlLinien_Y;
	private final Integer minXscale;
	private final Integer minYscale;

	/**
	 * constructor
	 * 
	 * @param minXscale - wenn angegeben, zB 5: gehe minimal auf diesen x-Abstand runter 
	 * 						und zeige alle x-Werte als GANZZAHL an. wenn null: erlaube alle Werte (Kommazahlen)
	 * 
	 */
	public DynamicValueGrid_Client(int anzahlLinien_X, int anzahlLinien_Y, Integer minXscale, Integer minYscale) {
		this.anzahlLinien_X = anzahlLinien_X;
		this.anzahlLinien_Y = anzahlLinien_Y;
		this.minXscale = minXscale;
		this.minYscale = minYscale;
	}
	
	/**
	 * Hier spielt die Musik: berechne den Abstand der Linien anhand des
	 * - Bereichs (max-min)
	 * - und der gewünschten ANZAHL von Linien, zB 20
	 */
	public double calcBreite(double min, double max, int zielAnzahl, Integer minBreite) {
		int idx = diffs.length-1;
		double bereich = max - min;
		double breite = 0;
		while (idx>=0) {
			breite = diffs[idx];
			int anzahl = (int) (bereich / breite);
			if (anzahl >= zielAnzahl) {
				break;
			}
			idx--;
		}
		if (minBreite!=null && breite < minBreite) {
			return minBreite;
		}
		return breite; 
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAreaChange(float xMin, float xMax, float yMin, float yMax) {
	}

	
	@Override
	public void paint(GC gc, CustomChartPainter painter, Rectangle pixBox) {

		// Zeichne die X-Linien
		int yMinP = pixBox.y+2;
		int yMaxP = pixBox.height-15;
		float xMin = painter.xMin();
		float xMax = painter.xMax();
		double xAbsVal = calcBreite(xMin, xMax, anzahlLinien_X, minXscale);		// Abstand in value-Koordinaten, nicht pixel
		double x = (Math.round(xMin / xAbsVal) * xAbsVal);						// "snap" das auf eine Linie, von da an dann per Breite
		int nachkommas = calcNackommas(xAbsVal);
		
		gc.setBackground(FARBE.WHITE);
		gc.setLineWidth(1);
		gc.setLineStyle(SWT.LINE_DOT);
		while (x<=xMax) {
			int xp = painter.x2pixel((float) x);
			gc.setForeground(FARBE.GRAY_1);
			gc.drawLine(xp, yMinP, xp, yMaxP);
			String label;
			if (minXscale!=null) {
				label = String.valueOf((int)x);
			} else {
				label = Utils.print(x, nachkommas);
			}
			int offset = 1 - label.length()*3;
			gc.setForeground(FARBE.GRAY_DARK_1);
			gc.drawString(label, xp+offset, yMaxP+1);
			x += xAbsVal;
		}
		
		// Zeichne die Y-Linien
		int xMinP = pixBox.x+15;
		int xMaxP = pixBox.width-2;
		float yMin = painter.yMin();
		float yMax = painter.yMax();
		double yAbsVal = calcBreite(yMin, yMax, anzahlLinien_Y, minYscale);		// Abstand in value-Koordinaten, nicht pixel
		double y = (Math.round(yMin / yAbsVal) * yAbsVal);						// "snap" das auf eine Linie, von da an dann per Breite
		nachkommas = calcNackommas(yAbsVal);

		gc.setLineWidth(1);
		gc.setLineStyle(SWT.LINE_DOT);
		while (y<=yMax) {
			int yp = painter.y2pixel((float) y);
			gc.setForeground(FARBE.GRAY_1);
			gc.drawLine(xMinP, yp, xMaxP, yp);
			String label;
			if (minYscale!=null) {
				label = String.valueOf((int)y);
			} else {
				label = Utils.print(y, nachkommas);
			}
			gc.setForeground(FARBE.GRAY_DARK_1);
			gc.drawString(label, 2, yp-7);
			y += yAbsVal;
		}
	}

	/**
	 * für die saubere Anzeige:
	 * - ich muss auf STRING runden, denn selbst (double) erzeugt ggf "1.50000000003" 
	 * @return
	 */
	private int calcNackommas(double range) {
		double exponent = Math.log10(range);		// 100 -> 2
		int n = (int) -Math.round(exponent);		// 0.0023 -> -2
		if (n<0) {
			n = 0;
		}
		return n;
	}

}













































