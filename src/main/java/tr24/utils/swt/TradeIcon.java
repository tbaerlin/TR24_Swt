package tr24.utils.swt;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

/**
 * erzeugt das nette Icon *g*
 */
public class TradeIcon {

	/**
	 * call here!
	 */
	public Image buildIcon(Display display) {
		int GREEN = 1;
		int RED   = 2;
		Image image = new Image(display, 16, 16);
		GC gc = new GC(image);
		line(gc, 8, 7, GREEN);
		line(gc, 12, 7, RED);
		line(gc, 11, 8, GREEN);
		line(gc, 8, 5, RED);
		line(gc, 16, 12, GREEN);
		line(gc, 15, 13, GREEN);
		line(gc, 12, 11, RED);
		line(gc, 13, 8, RED);
		ImageData id = image.getImageData();
		image.dispose();
		id.transparentPixel = -256;
		Image image2 = new Image(display, id);		// neues Image erzeugen
		return image2;
	}
	
	
	private int x = 0;
	private void line(GC gc, int preis, int hoehe, int farbe) {
		gc.setBackground(farbe==1 ? FARBE.GREEN_DARK_5 : FARBE.RED_1 );
		gc.fillRectangle(x, 16-preis, 1, hoehe);
		x++;
		gc.setBackground(FARBE.GRAY_2);
		gc.fillRectangle(x, 17-preis, 1, hoehe);
		x++;
	}
	
}
