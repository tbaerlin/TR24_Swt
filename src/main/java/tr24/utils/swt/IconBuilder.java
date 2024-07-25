package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import tr24.utils.common.FileUtil;


/**
 * -ich habe viiieeele Apps, es wird zu voll auf der Task-Leiste
 * -also: eine App kann sich einfach ein voll-farbiges Icon holen
 * 
 * 
 * @author tbaer
 *
 */
public class IconBuilder {

	/**
	 * Baut eine Icon mit einem Text, zB "!"
	 */
	public Image buildTextIcon(Display display, Color bgColor, String text, Color txtColor, int fontSize) {
		Image image = new Image(display, 16, 16);
		GC gc = new GC(image);
		gc.setAntialias(SWT.ON);
		gc.setBackground(bgColor);
		gc.fillRectangle(1, 1, 14, 14);
		gc.setForeground(txtColor);
		gc.setFont(new Font(display, "Arial", fontSize, SWT.BOLD));
		// wie gross ist der Text?
		Point box = gc.stringExtent(text);
		int x = 8 - box.x/2;
		int y = 8 - box.y/2;
		gc.drawString(text, x, y, true);
		
		ImageData id = image.getImageData();
		image.dispose();
		gc.dispose();
		id.transparentPixel = -256;
		Image image2 = new Image(display, id);		// neues Image erzeugen
		return image2;
	}
	
	/**
	 * baut eine Voll-Farb-Icon
	 */
	public void setIconForShell(Shell shell, Color fuellFarbe) {
		shell.setImage(buildIcon(shell.getDisplay(), fuellFarbe));
	}
	
	/**
	 * baut ein Zwei-Farben-Icon
	 */
	public void setIconForShell(Shell shell, Color farbe1, Color farbe2) {
		shell.setImage(buildIconTwoColors(shell.getDisplay(), farbe1, farbe2));
	}
	
	public Image buildBarIcon(Display display, Color bgOrNull) {
		return new BarIconBuilder().buildIcon(display, bgOrNull);
	}
	
	/**
	 * Erzeuge ein EINFARBIGES Rechteck
	 */
	private Image buildIcon(Display display, Color fuellFarbe) {
		Image image = new Image(display, 16, 16);
		
		GC gc = new GC(image);
		gc.setForeground(FARBE.GRAY_2);
		gc.drawRectangle(0, 0, 15, 15);
		gc.setBackground(fuellFarbe);
		gc.fillRectangle(1, 1, 14, 14);
		ImageData id = image.getImageData();
		image.dispose();
		id.transparentPixel = -256;
		Image image2 = new Image(display, id);		// neues Image erzeugen
		return image2;
	}

	/**
	 * erzeuge ein 2x2-Grid mit den beiden Farben
	 */
	private Image buildIconTwoColors(Display display, Color farbe1, Color farbe2) {
		Image image = new Image(display, 16, 16);
		
		GC gc = new GC(image);
		gc.setForeground(farbe1);
		gc.drawRectangle(0, 0, 15, 15);
		gc.setBackground(farbe1);
		gc.fillRectangle(1, 1, 14, 14);
		gc.setBackground(farbe2);
//		gc.fillRectangle(1, 1, 8, 8);
//		gc.fillRectangle(8, 8, 12, 12);
		gc.fillRectangle(1, 8, 7, 7);
		gc.fillRectangle(8, 1, 7, 7);

		ImageData id = image.getImageData();
		image.dispose();
		id.transparentPixel = -256;
		Image image2 = new Image(display, id);		// neues Image erzeugen
		return image2;
	}

	/**
	 * erzeuge Icon mit Kreisen
	 */
	public Image buildIconWithCircels(Display display, Color bgColor, Color circleColor) {
		Image image = new Image(display, 16, 16);
		GC gc = new GC(image);
		gc.setAntialias(SWT.ON);

		// fill bg
		gc.setBackground(bgColor);
		gc.fillRectangle(0, 0, 15, 15);
		// Kreise
		gc.setForeground(circleColor);
		gc.setLineWidth(2);
		gc.drawOval(1, 0, 8, 8);
		gc.drawOval(7, 2, 8, 8);
		gc.drawOval(2, 6, 8, 8);
		
		ImageData id = image.getImageData();
		image.dispose();
		id.transparentPixel = -256;
		Image image2 = new Image(display, id);		// neues Image erzeugen
		return image2;
	}

	/**
	 * erzeuge Icon mit guten Trading-Hinweis
	 */
	public Image buildIconWithTradingHint(Display display, Color bgColor, Color lineColor) {
		Image image = new Image(display, 16, 16);
		GC gc = new GC(image);
		gc.setAntialias(SWT.ON);

		// fill bg
		gc.setBackground(bgColor);
		gc.fillRectangle(0, 0, 15, 15);
		// Kreise
		gc.setForeground(lineColor);
		gc.setLineWidth(2);
		gc.drawLine(1, 6, 2, 14);
		gc.drawLine(2, 14, 6, 8);
		gc.drawLine(6, 8, 11, 13);
		gc.drawLine(11, 13, 14, 0);
		
		ImageData id = image.getImageData();
		image.dispose();
		id.transparentPixel = -256;
		Image image2 = new Image(display, id);		// neues Image erzeugen
		return image2;
	}
	
	/**
	 * Helfer: baut ein schönen Icon *g*
	 */
	private class BarIconBuilder {
		
		private int x = 0;
		
		public Image buildIcon(Display display, Color bgOrNull) {
			int GREEN = 1;
			int RED   = 2;
			Image image = new Image(display, 16, 16);
			GC gc = new GC(image);
			if (bgOrNull!=null) {
				gc.setBackground(bgOrNull);
				gc.fillRectangle(0, 0, 15, 15);
			}
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
		private void line(GC gc, int preis, int hoehe, int farbe) {
			gc.setBackground(farbe==1 ? FARBE.GREEN_DARK_5 : FARBE.RED_1 );
			gc.fillRectangle(x, 16-preis, 1, hoehe);
			x++;
			gc.setBackground(FARBE.GRAY_2);
			gc.fillRectangle(x, 17-preis, 1, hoehe);
			x++;
		}
	}
	
	/**
	 * Setze ein Bild von Platte
	 */
	static public void setIconFromImage(Shell forThisShell, Class location, String imgName) {
		try {
			String path = FileUtil.getFileInPackageByClass(location, imgName);
			Image img = new Image(forThisShell.getDisplay(), path);
			forThisShell.setImage(img);
		} catch (Exception e) {
			System.err.println("Error in setIconImage: " + e);
			e.printStackTrace();
		}
	}

	
}






















