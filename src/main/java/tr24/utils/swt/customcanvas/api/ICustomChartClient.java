package tr24.utils.swt.customcanvas.api;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;


/**
 * Jemand will etwas auf dem XRay-Canvas zeichnen
 */
public interface ICustomChartClient {

	/**
	 * Zeichne dich, Client!
	 * 
	 * @param gc - direkter Zeichen-Zugriff
	 * @param painter - Hilfe: x/y Zeichen-Methoden und die xMin..xMax, yMin..yMax Werte
	 * @param pixBox  - die Canvas in Pixel
	 */
	void paint(GC gc, CustomChartPainter painter, Rectangle pixBox);

	/**
	 * kommt immer wenn sich der sichtbare Bereich ändert
	 * - der Painter-Client kann zB berechnen welche Elemente er überhaupt zeichnen muss
	 */
	void onAreaChange(float xMin, float xMax, float yMin, float yMax);
	
	
}
