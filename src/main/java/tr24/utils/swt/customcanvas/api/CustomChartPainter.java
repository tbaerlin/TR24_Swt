package tr24.utils.swt.customcanvas.api;

import org.eclipse.swt.graphics.Color;

/**
 * Helfer-Methoden zum Zeichnen
 */
public interface CustomChartPainter {

	/**
	 * rechnet die x-Koordinate in echte x-Pixel um
	 */
	int x2pixel(float x);

	/**
	 * rechnet die y-Koordinate in echte y-Pixel um
	 */
	int y2pixel(float y);

	/**
	 * Zeichne eine Linie, die Angaben sind x/y und werden intern in pixel-x/y umgerechnet
	 */
	void line(float x1, float y1, float x2, float y2, Color farbe);

	/**
	 * Zeichnet eine Kreis um den Punkt x/y
	 */
	void circle(float x, float y, int radiusPixels, Color farbe);
	
	/**
	 * Zeichnet ein Dreick (OrderMarker) an den Punkt x/y
	 */
	void marker(float x, float y, Color farbe, int markerSize);
	
	/**
	 * Sichtbarer linker X Wert
	 */
	public float xMin();
	/**
	 * Sichtbarer rechter X Wert
	 */
	public float xMax();
	/**
	 * Sichtbarer oberer Y Wert
	 */
	public float yMin();
	/**
	 * Sichtbarer untere Y Wert
	 */
	public float yMax();

	/**
	 * wieviele Pixel nimmt dieser x-Bereich ein
	 */
	public int xBreite(double xDiff);
	/**
	 * wieviele Pixel nimmt dieser y-Bereich ein
	 */
	public int yBreite(double yDiff);
	
}

