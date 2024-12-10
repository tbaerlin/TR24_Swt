package tr24.utils.swt.customcanvas.api;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;


/**
 * Callback für zeige-was-gerade-unter-der-Maus-ist Handler
 */
public interface IMousePosCallback {

	/**
	 * die Maus ist gerade
	 * 
	 * @param xp - auf diesen Pixeln
	 * 
	 * @param realX - auf diesem ECHT-Wert
	 * 
	 * @return true wenn das Diagramm seine eigene (Standard) "x=120, y=-49" zeigen soll
	 */
	public boolean mouseOver(int xp, int yp, float realX, float realY, GC gc, CustomChartPainter painter, Rectangle pixBox);
	
	/**
	 * die Maus hat die Zeichenfläche verlassen
	 * - der Client-Code muss keine redraw machen, 
	 *   das kommt eh direkt nach dem mouseExit() call
	 */
	public void mouseExit();
}
