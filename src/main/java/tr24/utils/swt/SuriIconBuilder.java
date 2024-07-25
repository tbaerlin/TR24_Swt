package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.InputStream;


/**
 * Helfer: liefert ein 32x32 Suri-Kopf-Icon
 */
public class SuriIconBuilder {

	
	/**
	 * Liefer das Bild
	 * @param bgColorOrNull - wenn gesetzt: fülle Hintergrund
	 * @param alpha         - Deckkraft: 0..255, empfohlen: unter 100
	 */
	public Image getSuriImage(Display display, Color bgColorOrNull, int alpha) {
		// String s = FileUtil.getFileInPackageByClass(getClass(), "Suri-32x32.bmp");
		// Image img = new Image(display, s);
		
		// soll auch per JAR gehen:
		@SuppressWarnings("rawtypes")
		Class cl = getClass();
		Package package1 = cl.getPackage();
		String pack = package1.getName().replace('.', '/');		// muss fully bauen
		InputStream is = cl.getClassLoader().getResourceAsStream(pack + "/Suri-32x32.bmp");
		Image img = new Image(display, is);

		if (bgColorOrNull!=null) {
			// test: male ne Farbe drüber
			GC gc = new GC(img);
			gc.setForeground(bgColorOrNull);
			gc.setAlpha(alpha);
			ImageData id = img.getImageData();
			for (int x=0; x<32; x++) {
				for (int y=0; y<32; y++) {
					int p = id.getPixel(x, y);
					int red = (p>> 16) & 0xff;
					int green = (p>> 8) & 0xff;
					int blue = (p) & 0xff;
					
					// test: übermale wenn hell genug
					int sum = red + green + blue;
					if (sum >= 3*220) {
						gc.drawPoint(x, y);
					}
				}
			}
			gc.dispose();
		}
		
		return img;
	}
	
	/**
	 * test...
	 */
	public static void main(String[] args) {
		// zeige Shell mit Bild
		BasisCore core = new BasisCore(new Display());
		Shell shell = new Shell(core.display, SWT.CLOSE);
		shell.setSize(400, 400);
		shell.setImage(new SuriIconBuilder().getSuriImage(core.display, FARBE.RED_2, 50));
		shell.open();
		core.runSwtLoop(shell);
	}
	
}

















