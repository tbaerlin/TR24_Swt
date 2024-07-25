package tr24.utils.swt;

import org.eclipse.swt.graphics.Color;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Farb-Konstanten
 * 
 * gute Quelle ist: http://www.tayloredmktg.com/rgb/
 * 
 * @author tbaer
 */
public class FARBE {

	private static List<Color> allColors = new ArrayList<Color>();
	
	/**
	 * Die Farben können auch über red.green.blue angesprochen werden
	 * - verwende sync'ed Version, da put/get beliebig kommen können
	 */
	private static Map<Integer, Color> colByCode = new Hashtable<Integer, Color>();
	
	public static Color BLACK = rgb(0, 0, 0);
	public static Color WHITE = rgb(255, 255, 255);
	
	public static Color BLACK_RED_ish  = rgb(100, 0, 0);
	public static Color BLACK_RED_ish2 = rgb(60, 0, 0);
	public static Color BLACK_RED_ish3 = rgb(40, 0, 0);
	public static Color BLACK_GREEN_ish  = rgb(0, 70, 0);
	public static Color BLACK_GREEN_ish2 = rgb(0, 50, 0);
	public static Color BLACK_GREEN_ish3 = rgb(0, 40, 0);
	public static Color BLACK_BLUE_ish  = rgb(0, 0, 100);
	public static Color BLACK_BLUE_ish2 = rgb(0, 0, 60);
	public static Color BLACK_BLUE_ish3 = rgb(0, 0, 40);

	public static Color WHITE_BLUE_ish  = rgb(250, 250, 255);
	public static Color WHITE_RED_ish   = rgb(255, 250, 250);
	public static Color WHITE_GREEN_ish = rgb(250, 255, 250);
	
	public static Color ALMOST_BLACK = rgb(50, 50, 50);
	
	public static Color BLUE_1 = rgb(0, 0, 255);
	public static Color BLUE_2 = rgb(50, 50, 255);
	public static Color BLUE_3 = rgb(100, 100, 255);
	public static Color BLUE_4 = rgb(150, 150, 255);
	public static Color BLUE_5 = rgb(200, 200, 255);
	public static Color BLUE_6 = rgb(230, 230, 255);
	
	public static Color BLUE_GRAY = rgb(200, 200, 230);
	
	public static Color BLUE_DARK_1 = rgb(0, 0, 20);
	public static Color BLUE_DARK_2 = rgb(50, 50, 40);
	public static Color BLUE_DARK_3 = rgb(0, 64, 128);
	public static Color BLUE_DARK_4 = rgb(70, 90, 140);
	
	public static Color GRAY_1 = rgb(159, 159, 159);
	public static Color GRAY_2 = rgb(191, 191, 191);
	public static Color GRAY_3 = rgb(207, 207, 207);
	public static Color GRAY_4 = rgb(223, 223, 223);
	public static Color GRAY_5 = rgb(239, 239, 239);
	public static Color GRAY_6 = rgb(245, 245, 245);
	public static Color GRAY_7 = rgb(250, 250, 250);
	
	public static Color GRAY_DARK_1 = rgb(100, 100, 100);
	public static Color GRAY_DARK_2 = rgb(80, 80, 80);
	public static Color GRAY_DARK_3 = rgb(60, 60, 60);
	
	public static Color RED_1 = rgb(255, 0, 0);
	public static Color RED_2 = rgb(255, 50, 50);
	public static Color RED_3 = rgb(255, 100, 100);
	public static Color RED_4 = rgb(255, 150, 150);
	public static Color RED_5 = rgb(255, 200, 200);
	public static Color RED_6 = rgb(255, 230, 230);
	public static Color RED_BLACK_1 = rgb(180, 40, 40);
	
	public static Color GREEN_1 = rgb(0, 255, 0);
	public static Color GREEN_2 = rgb(50, 255, 50);
	public static Color GREEN_3 = rgb(100, 255, 100);
	public static Color GREEN_4 = rgb(150, 255, 150);
	public static Color GREEN_5 = rgb(200, 255, 200);
	public static Color GREEN_6 = rgb(220, 255, 230);
	public static Color GREEN_BLACK_1 = rgb(40, 150, 40);

	public static Color GREEN_DARK_1 = rgb(27, 223, 35);
	public static Color GREEN_DARK_2 = rgb(38, 207, 45);
	public static Color GREEN_DARK_3 = rgb(47, 191, 53);
	public static Color GREEN_DARK_4 = rgb(59, 159, 63);
	public static Color GREEN_DARK_5 = rgb(62, 111, 64);
	public static Color GREEN_DARK_6 = rgb(7,  95, 11);
	
	public static Color RED_DARK_1 = rgb(239, 0, 10);
	public static Color RED_DARK_2 = rgb(239, 14, 23);
	public static Color RED_DARK_3 = rgb(223, 27, 35);
	public static Color RED_DARK_4 = rgb(191, 47, 53);
	public static Color RED_DARK_5 = rgb(159, 19, 25);
	public static Color RED_DARK_6 = rgb(130, 10, 20);

	public static Color YELLOW_1 = rgb(229, 255, 0);
	public static Color YELLOW_2 = rgb(237, 255, 79);
	public static Color YELLOW_3 = rgb(247, 255, 175);
	public static Color YELLOW_4 = rgb(250, 255, 207);
	public static Color YELLOW_5 = rgb(252, 255, 223);
	
	public static Color YELLOW_DARK_1 = rgb(237, 239, 0);
	public static Color YELLOW_DARK_2 = rgb(221, 223, 0);
	public static Color YELLOW_DARK_3 = rgb(189, 191, 0);
	public static Color YELLOW_DARK_4 = rgb(158, 159, 0);
	public static Color YELLOW_DARK_5 = rgb(126, 127, 0);

	public static Color ORANGE_1 = rgb(255, 165, 0);
	public static Color ORANGE_2 = rgb(239, 133, 29);
	public static Color ORANGE_3 = rgb(191, 121, 0);
	public static Color GOLD     = rgb(255, 215, 0);
	
	public static Color BROWN_1 = rgb(205, 133, 63);
	public static Color BROWN_2 = rgb(160, 82, 45);
	public static Color BROWN_3 = rgb(139, 125, 107);
	
	public static Color PINK      = rgb(255, 20, 147);
	public static Color VIOLETT_1 = rgb(238, 130, 238);
	public static Color MAGENTA   = rgb(168, 37, 115);
	
	public static Color SandyBrown = rgb(244, 164, 96);
	
	public static Color CH_TKC = rgb(250, 128, 114);
	public static Color CH_Wellington = rgb(218, 165, 32);
	public static Color CH_Tokyo = rgb(128, 0, 128);
	public static Color CH_London = rgb(0, 255, 255);
	public static Color CH_NewYork = rgb(0, 0, 205);
	
	public static Color CYAN_1 = rgb(128, 255, 255);
	

	/**
	 * color from '#b4ac10',   #fff geht NICHT!
	 * 
	 * @return WHITE bei allen Fehlern
	 */
	public static Color hex(String hexString) {
		if (hexString.startsWith("#")==false) {
			return WHITE;
		}
		String s = hexString.substring(1);
		if (s.length()!=6) {
			return WHITE;
		}
		int x = Integer.parseInt(s, 16);
//		System.out.println(hexString);
//		System.out.println((x >> 16) + ", " + Integer.parseInt(s.substring(0, 2), 16));
//		System.out.println(((x & 65535) >> 8)  + ", " + Integer.parseInt(s.substring(2, 4), 16));
//		System.out.println((x & 255)  + ", " + Integer.parseInt(s.substring(4), 16));
		return rgb( (x>>16), ((x & 65535 ) >> 8), (x & 255) );
	}
	
	public static Color rgb(int r, int g, int b) {
		assert (r>=0 && r<=255) : "red invalid [" + r + "]";
		assert (g>=0 && g<=255) : "green invalid [" + g + "]";
		assert (b>=0 && b<=255) : "blue invalid [" + b + "]";
		
		Color c = new Color(null, r, g, b);
		allColors.add(c);
		int x = r*1000000 + g*1000 + b;
		colByCode.put(x, c);
		return c;
	}
	
	private static final Random random = new Random();
	
	/**
	 * Hole eine Farbe über (red*1000000 + green*1000 + blue)
	 * 
	 * @return null wenn's keine bekannte ist
	 */
	public static Color colByRGBCode(int code) {
		return colByCode.get(code);
	}
	
	/**
	 * Fügt diese Farbe in den Cache ein
	 * - User code kann HIER Color-Objects cachen
	 */
	public static void cacheColByRGBCode(Color col) {
		int x = col.getRed()*1000000 + col.getGreen()*1000 + col.getBlue();
		colByCode.put(x, col);
	}

	public static Color randomColor() {
		int index = random.nextInt(allColors.size());
		return allColors.get(index);
	}
	
	public static Color randomHelleFarbeFuerHintergrund() {
		int r = random.nextInt(155) + 100;
		int g = random.nextInt(155) + 100;
		int b = random.nextInt(155) + 100;
		return rgb(r, g, b);
	}

	public static final ColorMap MAP1 = new ColorMap(1);
	public static final ColorMap MAP2 = new ColorMap(2);
	
	public static class ColorMap {
		public Color[] map;
		public ColorMap(int i) {
			if (i==1) {
				map = new Color[] { GREEN_DARK_1, RED_1, BLUE_1, ORANGE_1, YELLOW_1, GRAY_2, RED_5, GREEN_4, BLUE_4 };
			}
			if (i==2) {
				map = new Color[] { BLUE_1, RED_DARK_1, GREEN_DARK_3, ORANGE_1, GRAY_1, CH_Wellington, CH_Tokyo, BLUE_3, RED_3, GREEN_3, YELLOW_3 };
			}
		}
		/**
		 * hole die Farbe 0..n, dann wieder von vorn
		 */
		public Color get(int idx) {
			if (idx>=map.length) {
				idx = idx % map.length;				// modulo
			}
			return map[idx];
		}
	}

	/**
	 * ich merke mir: Yellow, YELLOW und yellow
	 */
	private static Map<String, Color> colByPossibleNames;
	
	/**
	 * Hole eine Farbe über ihren Feld-Namen
	 * 
	 * @param colName - YELLOW_3, geht auch "Yellow_3" oder "yelLoW_3"
	 * 
	 * @return null wenn's die Farbe/Feld nicht gibt
	 */
	public static Color byName(String colName) {
		if (colByPossibleNames==null) {			// baue beim ersten Call
			colByPossibleNames = new HashMap<String, Color>(100);
			try {
				Field[] list = FARBE.class.getFields();
				for (Field f : list) {
					if (f.getType().equals(Color.class)) {
						String name = f.getName();
						Color c = (Color) f.get(null);
						colByPossibleNames.put(name, c);
						colByPossibleNames.put(name.toUpperCase(), c);
						colByPossibleNames.put(name.toLowerCase(), c);
					}
				}
			} catch (Exception e) { }
		}
		
		// Versuch 1:
		Color c = colByPossibleNames.get(colName);
		if (c!=null) {
			return c;
		}
		// Versuch 2:
		c = colByPossibleNames.get(colName.toUpperCase());
		if (c!=null) {
			return c;
		}
		// Versuch 3:
		c = colByPossibleNames.get(colName.toLowerCase());
		if (c!=null) {
			return c;
		}
		return null;
	}
	
	/**
	 * Liefert eine dunklere Version dieser Farbe
	 */
	public static Color darker(Color thanThis) {
		int r = (int)(0.8f * thanThis.getRed());
		int g = (int)(0.8f * thanThis.getGreen());
		int b = (int)(0.8f * thanThis.getBlue());
		return rgb(r, g, b);
	}
	/**
	 * Liefert eine hellere Version dieser Farbe
	 * 
	 * @param prozent 10, 20, 30, ...
	 */
	public static Color brigther(Color thanThis, int prozent) {
		int add = 255*prozent/100;
		int r = Math.min(255, add+thanThis.getRed());
		int g = Math.min(255, add+thanThis.getGreen());
		int b = Math.min(255, add+thanThis.getBlue());
		return rgb(r, g, b);
	}

	/**
	 * Mische zwei Farben
	 * @param col1
	 * @param anteilCol1 - zB 0.9 = 90%
	 * @param col2
	 */
	public static Color mix(Color col1, float anteilCol1, Color col2) {
		float f2 = 1-anteilCol1;	// 0.1f
		int r = Math.round(col1.getRed()*anteilCol1 + col2.getRed()*f2);
		int g = Math.round(col1.getGreen()*anteilCol1 + col2.getGreen()*f2);
		int b = Math.round(col1.getBlue()*anteilCol1 + col2.getBlue()*f2);
		return rgb(r,g,b);
	}

	/**
	 * Erzeuge Farbe aus "255,120,23"
	 * 
	 * @return bei allen möglichen Fehlern
	 */
	public static Color fromString(String rgbString) {
		if (rgbString==null || rgbString.length()==0) {
			return null;
		}
		try {
			String[] toks = rgbString.split(",");
			return rgb(Integer.parseInt(toks[0]), Integer.parseInt(toks[1]), Integer.parseInt(toks[2]));
		} catch (Exception e) {
			return null;
		}
	}
	
	
	
	
}



















