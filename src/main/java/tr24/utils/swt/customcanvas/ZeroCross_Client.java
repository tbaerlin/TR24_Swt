package tr24.utils.swt.customcanvas;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import tr24.utils.swt.FARBE;
import tr24.utils.swt.customcanvas.api.CustomChartPainter;
import tr24.utils.swt.customcanvas.api.ICustomChartClient;

/**
 * Zeichnet das 0/0 Kreue in der Mitte
 * - nur zu Dev-Zwecken
 */
public class ZeroCross_Client implements ICustomChartClient {

	/**
	 * Zeichne das Kreuz
	 */
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAreaChange(float xMin, float xMax, float yMin, float yMax) {
	}

	@Override
	public void paint(GC gc, CustomChartPainter painter, Rectangle pixBox)
	{
		int x = painter.x2pixel(0);
		int y = painter.y2pixel(0);		// Mitte in echten pixel-x/y
		gc.setLineWidth(2);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setForeground(FARBE.ALMOST_BLACK);
		gc.drawLine(pixBox.x, y, pixBox.width, y);
		gc.drawLine(x, pixBox.y, x, pixBox.height);
	}

}
