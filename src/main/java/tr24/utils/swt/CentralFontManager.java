package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import tr24.utils.swt.api.IShutdownShell;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton: Verwaltet FONTs
 * 
 * - create on-demand
 * - dispose am Ende
 * 
 */
public class CentralFontManager implements IShutdownShell {

	private final Display display;
	private final String fontName;
	
	private final Map<String, Font> fontCache = new HashMap<>(10);

	

	/**
	 * constructor
	 * 
	 * @param core - nullable: Dann MUSS der Caller selber {@link #shellShutdown(ApplicationConfig)} rufen!!
	 * 
	 * @param defaultFontName - Arial, Consolas, ...
	 */
	public CentralFontManager(Display display, BasisCore core, String defaultFontName) {
		this.display = display;
		if (core!=null) {
			core.add2ShutdownShell(this);
		}
		this.fontName = (defaultFontName!=null ? defaultFontName : "Arial");
	}

	@Override
	public void shellShutdown(ApplicationConfig conf) {
		for (Font f : fontCache.values()) {
			f.dispose();
		}
	}

	/**
	 * API: Hole/erzeuge einen Font
	 * - call muss in SWT kommen
	 */
	public Font getFont(int fontSize, boolean isBold) {
		if (fontSize<5) {
			fontSize = 5;		// sicher ist sicher
		}
		String key = fontSize + (isBold?"_bold":"_normal");
		Font font = fontCache.get(key);
		if (font==null) {
			int style = (isBold ? SWT.BOLD : SWT.NORMAL);
			font = new Font(display, fontName, fontSize, style);
			System.err.println("FontCache: add [" + key + "]");
			fontCache.put(key, font);
		}
		return font;
	}
	
	
}








