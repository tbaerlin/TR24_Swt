package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Text;
import tr24.utils.common.DateUtility;
import tr24.utils.swt.api.IOnButtonClick;

import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.List;
import java.util.function.*;


/**
 * Alles was nicht in FileUtil oder StringUtil passt
 */
public class SwtUtils {

	private static DecimalFormat sizeFormatter = new DecimalFormat("#,##0.##");
	private static String endings[] = new String[] { " byte", " KB", " MB"," GB" };

	/**
	 * für dummy-Secret: test mit SHA1 (idee: machineID ist irgendwas, SHA1 ist immer gleich lang)
	 */
	private static MessageDigest sha1Digest;
	static {
		try {
			sha1Digest =  MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	/**
	 * erzeuge SHA1
	 */
	public static String string2sha1(String value) {
		try {
			synchronized (sha1Digest) {
				sha1Digest.reset();
				sha1Digest.update(value.getBytes("utf8"));
				String sha1 = String.format("%040x", new BigInteger(1, sha1Digest.digest()));
				return sha1;
			}
		} catch (UnsupportedEncodingException e) { return "error";}
	}


	/**
	 * weil's ja sonst keiner macht
	 * 
	 * @return empty int[] wenn list==null
	 */
	public static int[] toIntArray(List<Integer> list) {
		if (list==null) {
			return new int[0];
		}
		int n = list.size();
		int[] result = new int[n];
		for (int i=0; i<n; i++) {
			result[i] = list.get(i);
		}
		return result;
	}
	/**
	 * weil's ja sonst keiner macht
	 * 
	 * @return empty float[] wenn list==null
	 */
	public static float[] toFloatArray(List<Float> list) {
		if (list==null) {
			return new float[0];
		}
		int n = list.size();
		float[] result = new float[n];
		for (int i=0; i<n; i++) {
			result[i] = list.get(i);
		}
		return result;
	}
	/**
	 * Double-to-float: Weil's ja sonst keiner macht
	 * 
	 * @return empty float[] wenn list==null
	 */
	public static float[] toFloatArrayFromDouble(List<Double> list) {
		if (list==null) {
			return new float[0];
		}
		int n = list.size();
		float[] result = new float[n];
		for (int i=0; i<n; i++) {
			result[i] = (float)((double)list.get(i));
		}
		return result;
	}
	
	/**
	 * List-von-Daten-Object to native float[]
	 */
	public static <T> float[] toFloatArray(List<T> input, Function<T, Float> converter) {
		int n = input.size();
		float[] result = new float[n];
		for (int i=0; i<n; i++) {
			result[i] = converter.apply(input.get(i));
		}
		return result;
	}
	/**
	 * Native-Array-von-Daten-Object to native float[]
	 */
	public static <T> float[] toFloatArray(T[] input, Function<T, Float> converter) {
		int n = input.length;
		float[] result = new float[n];
		for (int i=0; i<n; i++) {
			result[i] = converter.apply(input[i]);
		}
		return result;
	}
	
	
	/** weil's ja sonst keiner macht
	 * 
	 * @return empty String[] wenn list==null
	 */
	public static String[] toStringArray(List<String> list) {
		if (list==null) {
			return new String[0];
		}
		int n = list.size();
		String[] result = new String[n];
		for (int i=0; i<n; i++) {
			result[i] = list.get(i);
		}
		return result;
	}
	
	/**
	 * Object oder Object[] to List : nice
	 */
	public static <T> List<T> toList(@SuppressWarnings("unchecked") T... elements) {
		return Arrays.asList(elements);
	}
	/**
	 * Object oder Object[] to List <br>
	 *   <b>OHNE NULL-Elemente</b>
	 * @return im Extrem-Fall: leere Liste
	 */
	public static <T> List<T> toListNoNulls(@SuppressWarnings("unchecked") T... elements) {
		List<T> res = new ArrayList<>();
		if (elements==null) {
			return res;
		}
		for (T el : elements) {
			if (el!=null) {
				res.add(el);
			}
		}
		return res;
	}
	
	/**
	 * Spart ein paar Zeichen code
	 */
	public static <T> T[] toArray(List<T> list, Class<T> clz) {
		@SuppressWarnings("unchecked")
		T[] res = (T[]) Array.newInstance(clz, list.size());
		return list.toArray(res);
	}
	
	
	/**
	 * gibt die Dauer als String aus:
	 * 
	 * - 4m 34s
	 * - 2h 2m
	 * - 7d 3h
	 * 
	 * - Für genauere Ausgabe: {@link DateUtility#howLong(long, int)}
	 */  
	public static String formatTimeToHumanReadable(int secs) {
		return DateUtility.howLong(secs, 2);
	}
	
	/**
	 * Zahl mit Nachkommas to String
	 * @param zahl
	 * @param nachkommas
	 */
	static public String print(double zahl, int nachkommas) {
		if (Double.isNaN(zahl)|| zahl==Double.POSITIVE_INFINITY || zahl==Double.NEGATIVE_INFINITY) {
			return "NaN";
		}
		BigDecimal z = null;
		try {
			z = new BigDecimal(zahl);
		} catch (Exception e) {
			e.printStackTrace();
		}
		z = z.setScale(nachkommas, BigDecimal.ROUND_HALF_UP);
		return z.toString();
	}
	
	public static float round(float zahl, int nachkommas) {
		BigDecimal z = new BigDecimal(zahl);
		z = z.setScale(nachkommas, BigDecimal.ROUND_HALF_UP);
		return z.floatValue();
	}

	
	/**
	 * return sth like "1,43 MB" or "6,22 KB" or "977 byte"
	 */
	public static synchronized String sizeTo4711kb(long size) {
		int c = 0;
		double d = size;
		while (d>1000 && c<3) {
			d = d/1024;
			c++;
		}
		String s = sizeFormatter.format(d);
		return s + endings[c];
	}
	
	/**
	 * return sth like "1,2Mrd" or "4.2M" or "6,22k" or "977"
	 */
	public static synchronized String sizeTo123M(long n) {
		double d = n;
		if (d >= 1000*1000*1000) {
			d = Math.round(d/(100*1000*1000))/10D;
			return sizeFormatter.format(d)+"Mrd";
		}
		if (d >= 1000*1000) {
			d = Math.round(d/(100*1000))/10D;
			return sizeFormatter.format(d)+"M";
		}
		if (d >= 1000) {
			d = Math.round(n/100)/10D;
			return sizeFormatter.format(d)+"k";
		}
		return sizeFormatter.format(n);
	}

	
	/**
	 * @return InetAddress.getLocalHost().getHostName() oder null
	 */
	public static String computerName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return null;
		}
	}


	/**
	 * Wer ist gerade eingeloggt
	 * - genauer: der Process-Owner 
	 * 
	 * @return oder null
	 */
	public static String userName() {
		Properties x = System.getProperties();
		return (String) x.get("user.name");
	}


	/**
	 * @param ex
	 * 
	 * @param maxDepth: wenn >0: maxiaml so viele Stack-Level
	 * 
	 * @return Leerstring wenn ex==null
	 */
	public static String stackTrace2String(Throwable ex, int maxDepth) {
		return stackTrace2String(ex, maxDepth, "\n");
	}

	/**
	 * @param ex
	 * 
	 * @param maxDepth: wenn >0: maxiaml so viele Stack-Level
	 * 
	 * @return Leerstring wenn ex==null
	 */
	public static String stackTrace2String(Throwable ex, int maxDepth, String separator) {
		if (ex==null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		StackTraceElement[] trace = ex.getStackTrace();
		int n = trace.length;
		if (maxDepth>0) {
			n = Math.min(n, maxDepth);
		}
		for (int i=0; i<n; i++) {
			StackTraceElement x = trace[i];
			sb.append(x);
			sb.append(separator!=null ? separator : "\n");
		}
		return sb.toString();
	}

	/**
	 *  Liefere eine Stack-Trace-Punkt, zB "x.y.z.ThisClass.doSomething(), line=123"  oder "n/a" bei Fehler
	 */
	public static String stackTraceMethod(int methodPosition) {
		try {
			StackTraceElement[] trace = Thread.currentThread().getStackTrace();
			return trace[methodPosition].toString();
		} catch (Exception e) {
			return "n/a";
		}
	}
	
	
	/**
	 * Idee: 
	 * - im Framework knallt's irgendwo
	 * - ich will aber die ERSTE Zeile sehen in "MEINEN" Code
	 * - also filtere, anhand 'frameWorkClass'
	 *   a) alles was java-intern ist   		=> warte BIS die 'frameWorkClass' kommt
	 *   b) alles was vom Framework kommt		=> dann warte auf die ERSTE Zeile nach 'frameWorkClass'
	 */
	public static StackTraceElement[] filterStackTrace(String frameWorkClass, StackTraceElement[] orgStack) {
		if (orgStack==null || orgStack.length==0) {
			return new StackTraceElement[0];		// return leer, aber eben NIE NULL
		}
		int n = orgStack.length;
		int i=0;
		int mode = 0;			// Zu Beginn: warte bis 'frameWorkClass' kommt
		while (i<n) {			// gehe alle durch, filtere...
			String s = orgStack[i++].getClassName();
			boolean isFrmWorkLine = (s.contains(frameWorkClass));
			if (mode==0) {
				if (isFrmWorkLine) {
					mode = 1;
				}
			} else {
				if (!isFrmWorkLine) {
					break;			// höre mit dem Filtern auf sobald was vom "User-Code" kommt
				}
			}
		}
		if (i==n) {			// Achtung: ich kann auch "drüber-gelaufen" sein
			return new StackTraceElement[0];
		}
		StackTraceElement[] res = new StackTraceElement[n-i];		// soviele noch übrig
		System.arraycopy(orgStack, i, res, 0, (n-i));
		return res;
	}
	
	
	/**
	 * TODO: check ob das auch so funktioniert!!
	 * 
	 *  Zerlege eine Message in Zeilen
	 *  
	 *  - Trenner sind Leerzeichen
	 *  
	 *  @param org       - input string
	 *  @param maxPerRow - schneide Zeilen ab die länger sind
	 *  @param maxRows   - maximal so viele Zeilen
	 *  
	 *  @param returnAsList : <br><b>true</b>: die Zeilen kommen EINZELN in einem String[]-Array <br>
	 *                        <b>false</b>: es kommt ein Sting[0], die Zeilen sind per <b>\n</b> getrennt.
	 *  
	 *  @return ein ready-to-rumble String-array der Länge x oder 0
	 *  
	 */
	@Deprecated
	private static String[] splitByBlanks(String org, int maxPerRow, int maxRows, boolean returnAsList) {
		int i = -1;
		int startIdx = 0;
		int lastBlank = 0;
		char c;
		int n = org.length();
		List<String> temp = new ArrayList<String>(maxRows);
		do { 
			i++;
			c = (i==n) ? ' ' : org.charAt(i);		// hänge künstliches Leerzeichen hinten an
			if (c==' ') {
				if ((i-startIdx) < maxPerRow) {
					lastBlank = i;		// merken
					continue;
				}
				// hier: ich bin zu lang: 
				if (lastBlank==startIdx) {		// prüfe Sonderfall: super-langes-Wort
					lastBlank = Math.min(i, (startIdx+maxPerRow));
				}
				String s = org.substring(startIdx, lastBlank);
				// check super langes Wort
				if (s.length()>maxPerRow) {
					s = s.substring(0, maxPerRow);
				}
				// fertig wenn's zu viele Zeilen gibt
				if (temp.size()==maxRows) {
					if (returnAsList) {
						return temp.toArray(new String[temp.size()]);
					} else {
						return new String[] { toNewLine(temp) };
					}
				}
				temp.add(s);
				startIdx = (++lastBlank);			// setze neuen Anfang da hin
			}
		} while (i<n);
		// wenn's noch "Rest" gibt
		if (lastBlank==startIdx) {		// prüfe Sonderfall: super-langes-Wort
			lastBlank = Math.min(i, (startIdx+maxPerRow));
		}
		if (startIdx < lastBlank) {
			String s = org.substring(startIdx, lastBlank);
			// check super langes Wort
			if (s.length()>maxPerRow) {
				s = s.substring(0, maxPerRow);
			}
			temp.add(s);
		}
		if (returnAsList) {
			return temp.toArray(new String[temp.size()]);
		} else {
			return new String[] { toNewLine(temp) };
		}
	}

	/**
	 * Wandle eine Liste von String
	 * 
	 * @return in einen "ersteZeile \n zweite Zeile \n ..."
	 */
	public static String toNewLine(Collection<String> list) {
		return toLineWithSeparator(list, "\n");
	}

	/**
	 * Wandle eine Liste<String> in EINEN String mit frei wählbarem Separator
	 * 
	 * @return in einen "ersteZeile \n zweite Zeile \n ..."
	 */
	public static String toLineWithSeparator(Collection<String> list, String sep) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String tok : list) {
			if (first) {
				first = false;
			} else {
				sb.append(sep);
			}
			sb.append(tok);
		}
		return sb.toString();
	}
	
	/**
	 * Wandle eine Liste<etwas> in EINEN String mit frei wählbarem Separator
	 * 
	 * @return in einen "ersteZeile \n zweite Zeile \n ..."
	 */
	public static String toLineWithSeparator(Object[] list, String sep) {
		StringBuilder sb = new StringBuilder();
		int n = list.length;
		for (int i=1; i<=n; i++) {
			Object o = list[i-1];
			String s = (o!=null) ? o.toString() : "<null>";
			sb.append(s);
			if (i==n) {
				return sb.toString();
			}
			sb.append(sep);
		}
		return sb.toString();
	}
	
	/**
	 * Wandle eine Liste<von-irgendwas> in EINEN String mit frei wählbarem Separator
	 * 
	 * @param objToString - wenn null: nehme obj.toString()
	 * 
	 * @return in einen "ersteZeile \n zweite Zeile \n ..."
	 */
	public static <T> String toLineWithSeparator(Collection<T> listOfObjs, String sep, Function<T, String> objToString) {
		StringBuilder sb = new StringBuilder();
		int n = listOfObjs.size();
		int i=0; 
		for (T o : listOfObjs) {
			if (o==null) {
				continue;
			}
			String string = objToString!=null ? objToString.apply(o) : o.toString();
			sb.append(string);
			if (++i==n) {
				return sb.toString();
			}
			sb.append(sep);
		}
		return sb.toString();
	}
	/**
	 * ARRAY-Version
	 */
	public static <T> String toLineWithSeparator(T[] arrayOfObjs, String sep, Function<T, String> objToString) {
		StringBuilder sb = new StringBuilder();
		int n = arrayOfObjs.length;
		int i=0; 
		for (T o : arrayOfObjs) {
			if (o==null) {
				continue;
			}
			String string = objToString!=null ? objToString.apply(o) : o.toString();
			sb.append(string);
			if (++i==n) {
				return sb.toString();
			}
			sb.append(sep);
		}
		return sb.toString();
	}

	
	/**
	 * Convertiere eine Liste in eine andere (ähnlich MAP-Function)
	 * 
	 * @param skipNulls - true: wenn die 'converter'-Func NULL liefert: füge NICHT in out-Liste => FILTER
	 */
	public static <IN, OUT> List<OUT> convertList(Collection<IN> inputList, boolean skipNulls, Function<IN, OUT> converter) {
		List<OUT> result = new ArrayList<OUT>(inputList.size());
		for (IN in : inputList) {
			OUT out = converter.apply(in);
			if (out==null && skipNulls) 
				continue;
			result.add( out );
		}
		return result;
	}

	
	public static class LAYOUT {
		/**
		 * Helfer: Platziere ein {@link Composite} oben in der Shell <br>
		 * (Parent muss FormLayout haben!)
		 * 
		 * @param rand - 0 oder mehr: Rand link+oben+rechts
		 * 
		 * @param parent - Shell oder eine andere Box
		 * 
		 * @return - die fertig ge-layout-ete Box
		 */
		public static Composite layout_ObenLeiste(Composite parent, int height, int rand, Color farbeOrNull) {
			rand = Math.max(0, rand);		// nicht <0 !
			Composite com = new Composite(parent, SWT.None);
			FormData fd = new FormData();
			fd.top = new FormAttachment(0, rand);
			fd.left = new FormAttachment(0, rand);
			fd.right = new FormAttachment(100, -rand);
			fd.bottom = new FormAttachment(0, height);
			com.setLayoutData(fd);
			
			if (farbeOrNull!=null) {
				com.setBackground(farbeOrNull);
			}
			return com;
		}
		/**
		 * Platiere die BOX in den Rest-Unten-Bereich
		 * - die obere Begrenzung ist die die boxOben
		 * 
		 * @param element - Das Ding soll platziert werden
		 * @param boxOben - das Ding soll unterhalt der boxOben liegen
		 * @parma rand    - 0 oder mehr
		 */
		public static void layout_useRestOfSpace(Control element, Composite boxOben, int rand) {
			rand = Math.max(0, rand);
			FormData fd = new FormData();
			fd.left = new FormAttachment(0, rand);
			fd.right = new FormAttachment(100, -rand);
			fd.top = new FormAttachment(boxOben, rand);
			fd.bottom = new FormAttachment(100, -rand);
			element.setLayoutData(fd);
		}
		/**
		 * Platiere die BOX "dynamisch mittig" , d.h <br>
		 * - FIXER Abstand/Rand nach oben
		 * - FIXER Abstand/Rand nach unten
		 * 
		 * @param element - Das Ding soll platziert werden
		 * @param absTop  - Abstand (px) nach oben, 0 oder mehr
		 * @param borderLeftRight - 0 oder mehr
		 */
		public static void layout_fixTopFixBottom(Control element, int absTop, int absBottom, int borderLeftRight) {
			absTop = Math.max(0, absTop);
			absBottom = Math.max(0, absBottom);
			borderLeftRight = Math.max(0, borderLeftRight);
			FormData fd = new FormData();
			fd.left = new FormAttachment(0, borderLeftRight);
			fd.right = new FormAttachment(100, -borderLeftRight);
			fd.top = new FormAttachment(0, absTop);
			fd.bottom = new FormAttachment(100, -absBottom);
			element.setLayoutData(fd);
		}


		/**
		 * Plaziere ein {@link Label}
		 * - die Breite wird geschätzt berechnet
		 * 
		 * @param text
		 * @param parent
		 * @param x
		 * @param y
		 * 
		 * @return das Label 
		 */
		public static Label layout_showLabel(String text, Composite parent, int x, int y) {
			Label lab = new Label(parent, SWT.NONE);
			lab.setText(text);
			int width = text.length() * 6 + 5;
			lab.setBounds(x, y, width, 17);
			lab.setBackground(parent.getBackground());
			return lab;
		}
		/**
		 * Plaziere ein {@link Label}
		 * - die Breite wird ECHT über GC gemessen
		 * @param text
		 * @param parent
		 * @param x
		 * @param y
		 * @return das Label 
		 */
		public static Label layout_showLabel(String text, Composite parent, int x, int y, GC gc) {
			Label lab = new Label(parent, SWT.NONE);
			lab.setText(text);
			int width = gc.textExtent(text).x;
			lab.setBounds(x, y, width+2, 18);
			lab.setBackground(parent.getBackground());
			return lab;
		}
		/**
		 * Plaziere ein {@link Label} mit bekannter Breite
		 * 
		 * @param text
		 * @param parent
		 * @param x
		 * @param y
		 * 
		 * @return das Label 
		 */
		public static Label layout_showLabel(String text, Composite parent, int x, int y, int w) {
			Label lab = new Label(parent, SWT.NONE);
			lab.setText(text);
			lab.setBounds(x, y, w, 18);
			lab.setBackground(parent.getBackground());
			return lab;
		}
		/**
		 * Plaziere ein {@link Label} mit bekannter Breite, für FORMLAYOUT
		 * 
		 * @param text
		 * @param parent
		 * @param x
		 * @param y
		 * 
		 * @return das Label 
		 */
		public static Label layoutForm_showLabel(String text, Composite parent, int x, int y, int w) {
			Label lab = new Label(parent, SWT.NONE);
			lab.setText(text);
			FormData fd = new FormData();
			fd.left = new FormAttachment(0, x);
			fd.top = new FormAttachment(0, y);
			fd.right = new FormAttachment(0, x+w);
			fd.bottom = new FormAttachment(0, y+20);
			lab.setLayoutData(fd);
			lab.setBackground(parent.getBackground());
			return lab;
		}


		/**
		 * Erzeuge und plaztiere ein Edit-Text-Feld
		 */
		public static Text layout_showEditField(String text, Composite parent, int x, int y, int width) {
			Text txt = new Text(parent, SWT.BORDER);
			txt.setText(text!=null ? text : "");
			txt.setBounds(x, y, width, 20);
			return txt;
		}
		
		/**
		 * Erzeuge und plaztiere ein Edit-Text-Feld FÜR EIN FORMLAYOUT
		 */
		public static Text layoutForm_showEditField(String text, Composite parent, int x, int y, int width) {
			Text txt = new Text(parent, SWT.BORDER);
			txt.setText(text!=null ? text : "");
			FormData fd = new FormData();
			fd.left = new FormAttachment(0, x);
			fd.top  = new FormAttachment(0, y);
			fd.right = new FormAttachment(0,x+width);
			fd.bottom = new FormAttachment(0, y+20);
			txt.setLayoutData(fd);
			return txt;
		}

		/**
		 * Super praktisch: Lege einen Button an, FORMLAYOUT
		 */ 
		public static Button layoutForm_button(String text, int x, int y, int w, int h, Composite parent, final IOnButtonClick onClick) {
			Button btn = new Button(parent, SWT.PUSH);
			btn.setText(text);
			FormData fd = new FormData();
			fd.left = new FormAttachment(0, x);
			fd.top  = new FormAttachment(0, y);
			fd.right = new FormAttachment(0,x+w);
			fd.bottom = new FormAttachment(0, y+h);
			btn.setLayoutData(fd);
			btn.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					onClick.widgetSelected(e);
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					onClick.widgetSelected(e);
				}
			});
			return btn;
		}

		/**
		 * Super praktisch: Lege einen Button an
		 * 
		 * @param onClick - kann auch null bleiben
		 */ 
		public static Button layout_button(String text, int x, int y, int w, int h, Composite parent, final IOnButtonClick onClick) {
			Button btn = new Button(parent, SWT.PUSH);
			btn.setText(text);
			btn.setBounds(x, y, w, h);
			if (onClick!=null) {
				btn.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						onClick.widgetSelected(e);
					}
					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
						onClick.widgetSelected(e);
					}
				});
			}
			return btn;
		}
		/**
		 * Super praktisch: Lege eine CheckBox an
		 * 
		 * @param onClick - null-able
		 */ 
		public static Button layout_checkbox(String text, int x, int y, int w, int h, Composite parent, final IOnButtonClick onClick) {
			Button btn = new Button(parent, SWT.CHECK);
			btn.setText(text);
			btn.setBounds(x, y, w, h);
			if (onClick!=null) {
				btn.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						onClick.widgetSelected(e);
					}
					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
						onClick.widgetSelected(e);
					}
				});
			}
			return btn;
		}
		
		/**
		 * Super praktisch: Lege eine CheckBox an, FORM-LAYOUT
		 * 
		 * @param onClick - null-able
		 */ 
		public static Button layoutForm_checkbox(String text, int x, int y, int w, int h, Composite parent, final IOnButtonClick onClick) {
			Button btn = new Button(parent, SWT.CHECK);
			btn.setText(text);
			FormData fd = new FormData();
			fd.left = new FormAttachment(0, x);
			fd.top  = new FormAttachment(0, y);
			fd.right = new FormAttachment(0, x+w);
			fd.bottom = new FormAttachment(0, y+h);
			btn .setLayoutData(fd);
			if (onClick!=null) {
				btn.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						onClick.widgetSelected(e);
					}
					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
						onClick.widgetSelected(e);
					}
				});
			}
			return btn;
		}
		
		/**
		 * bei FormLayout: Hänge das Element <br>:
		 * - links: fix an x/y <br>
		 * - rechts: dyn an rechte Kante <br>
		 * - fixe Höhe
		 * 
		 * @param elem
		 * @param x
		 * @param y
		 * @param minusRight - so viel dynamisch vom rechten Rand weg, muss minus sein
		 * @param height - höhe des Elements
		 */
		public static void attach_fixL_dynR(Control elem, int x, int y, int minusRight, int height) {
			FormData fd = new FormData();
			fd.left = new FormAttachment(0, x);
			fd.top  = new FormAttachment(0, y);
			fd.right = new FormAttachment(100, minusRight);
			fd.bottom = new FormAttachment(0, y+height);
			elem.setLayoutData(fd);
		}
		/**
		 * bei FormLayout: Hänge das Element <br>:
		 * - links fix an x/y <br>
		 * - dyn an rechte Kante <br>
		 * - dyn an untere Kante <br>
		 * 
		 * @param elem
		 * @param x
		 * @param y
		 * @param minusRight - so viel dynamisch vom rechten Rand weg, muss minus sein
		 * @param minusBottom - so viel vom unteren Rand weg, muss minus sein
		 */
		public static void attach_fixL_dynR_dynBottom(Control elem, int x, int y, int minusRight, int minusBottom) {
			FormData fd = new FormData();
			fd.left = new FormAttachment(0, x);
			fd.top  = new FormAttachment(0, y);
			fd.right = new FormAttachment(100, minusRight);
			fd.bottom = new FormAttachment(100, minusBottom);
			elem.setLayoutData(fd);
		}
		/**
		 * bei FormLayout: Hängt das Element (zB einen Button) 
		 *  ans RECHTE Ende mit fixer Breite und fester Höhe
		 *  
		 * @param elem
		 * @param minusRight - rechte Kante, Abstand nach rechts
		 * @param y - fixe y-Position
		 * @param width - fixe Breite des Elements
		 * @param height - fixe Höhe des Elements
		 */
		public static void attach_dynR(Control elem, int minusRight, int y, int width, int height) {
			FormData fd = new FormData();
			fd.left = new FormAttachment(100, minusRight-width);
			fd.top  = new FormAttachment(0, y);
			fd.right = new FormAttachment(100, minusRight);
			fd.bottom = new FormAttachment(0, y+height);
			elem.setLayoutData(fd);
		}
		/**
		 * Hängt das Element:
		 * - linksbündig, rechtsbündig, bottombündig
		 * - mit Rand
		 * - und fixem Abstand von oben
		 *  
		 * @param elem
		 * @param topAbs - 100px vom oberen Rand
		 * @param padding - 10px links, unten und rechts
		 */
		public static void attach_fillBottom(Control elem, int topAbs, int padding) {
			FormData fd = new FormData();
			fd.left = new FormAttachment(0, padding);
			fd.top  = new FormAttachment(0, topAbs);
			fd.right = new FormAttachment(100, -padding);
			fd.bottom = new FormAttachment(100, -padding);
			elem.setLayoutData(fd);
		}
		
		/**
		 * trotz FormLayout: fixe Position von links/oben aus
		 */
		public static void attach_fixed(Control elem, int x, int y, int w, int h) {
			FormData fd = new FormData();
			fd.left = new FormAttachment(0, x);
			fd.top  = new FormAttachment(0, y);
			fd.right = new FormAttachment(0, x+w);
			fd.bottom = new FormAttachment(0, y+h);
			elem.setLayoutData(fd);
		}
		
		
	}	// LAYOUT

	
	/**
	 * Zerlegt einen String nach 'splitAt' Zeichen an einem Leerzeichen
	 * - sucht ein Leerzeichen in der Nähe der split-Position
	 * - wenn keins gefunden: split hart an der Position
	 * 
	 * Ergebnis: Der Multi-String wird nicht der schönste sein, aber der KOMPAKTESTE
	 * 
	 * @param splitAt - nach zB 20 Zeichen
	 * @param maxRows - limitiere die Ausgabe auf 5 Zeilen
	 */
	static public String[] splitString_toArray(String input, int splitAt, int maxRows) {
		List<String> temp = splitString(input, splitAt, maxRows);
		return temp.toArray(new String[temp.size()]);
	}
	
	/**
	 * Zerlegt einen String nach 'splitAt' Zeichen an einem Leerzeichen
	 * - sucht ein Leerzeichen in der Nähe der split-Position
	 * - wenn keins gefunden: split hart an der Position
	 * 
	 * Ergebnis: EIN neuer String mit '\n' als Trenner
	 * 
	 * @param splitAt - nach zB 20 Zeichen
	 * @param maxRows - limitiere die Ausgabe auf 5 Zeilen
	 */
	static public String splitString_toNewLine(String input, int splitAt, int maxRows) {
		List<String> temp = splitString(input, splitAt, maxRows);
		StringBuilder sb = new StringBuilder();
		int n=temp.size()-1;
		for (int i=0; i<=n; i++) {
			sb.append(temp.get(i));
			if (i<n) {
				sb.append('\n');
			}
		}
		return sb.toString();
	}
	
	private static List<String> splitString_OLD(String input, int splitAt, int maxRows) {
		List<String> temp = new ArrayList<String>(maxRows);
		int idx;		// wo gugg ich gerade hin
		int startIdx = 0;
		int backTest = 10;
		if (splitAt < 10) {
			backTest = 0;				// keine Blank-Suche wenn split zu klein
		}
		while (true) {
			// gehe auf den nächsten wunsch-split-punkt
			idx = startIdx + splitAt;
			
			// check! wenn ich drüber bin
			if (idx >= input.length()) {
				String rest = input.substring(startIdx, input.length());		// schneide Rest raus
				temp.add(rest);
				break;
			}
			
			// Jetzt finde einen passenden SplitPoint
			int min = idx-backTest;		// maximal 10 chars zurück suche ich ein Blank
			char c = 0;
			while (idx > min) {
				c = input.charAt(idx);
				if (c==' ') {		// Blank gefunden -> hier wird geschnitten
					break;
				}
				idx--;		// sonst: such weiter vorne
			}
			String line;
			if (c==' ') {
				line = input.substring(startIdx, idx);		// schneide Linie
				startIdx = idx+1;		// es geht dann weiter beim Zeichen NACH dem Blank
			} else {
				// Schnitt liegt IN einem Wort -> mache einen hard-cut -> erzeuge eine Zeile mit 'splitAt' Zeichen
				idx = idx + backTest;
				line = input.substring(startIdx, idx);		// schneide Linie
				startIdx = idx;		// es geht dann weiter beim Zeichen nach dem Schnitt (mitten in einem Wort)
			}
			temp.add(line);
			if (temp.size()==maxRows) {		// schon fertig?
				break;		
			}
		}	// über alles
		return temp;
	}

	
	/**
	 * NEUE VERSION: kann auch \n im 'input' berücksichtigen
	 * 
	 * INTERN: 
	 * - gehe Zeichen für Zeichen durch und versuche 
	 * - die aktuelle Ziel-Line zu füllen
	 * - CarrigeReturn beginnt sofort eine neue Ziel-Line
	 * - wenn's zu lang wird: versuche das Wort in die nächsten Ziel-Line zu packen
	 * - Sonderfall: wenn ein Wort über 'maxChars' rausgeht: Dann splitte
	 * 
	 */
	public static List<String> splitString(String input, int maxChars, int maxRows) {
		List<String> result = new ArrayList<String>();
		int n = input.length();
		int idx = 0;
		int curLineStart = 0;
		int curLineLen = 0;
		int savePoint = 0;			// Zählung ist idx+1, damit's auch die 0 gibt!
		
		while (idx<n) {
			char c = input.charAt(idx);
			// System.err.println("test [" + c + "]");
			// Unterscheidung: wenn noch Platz ist:
			if (curLineLen < maxChars) {
				if (c==' ') {			// Blank: die aktuelle Zeile endet hier (oder später)
					savePoint = idx + 1;
					idx++;
					curLineLen++;
					continue;
				}
				if (c=='\n') {			// CarriageReturn erzwingt sofort den Start der nächsten Zeile
					String line = input.substring(curLineStart, idx);		// Zeile läuft bis zum CarRet
					// System.err.println("cut (" + line + ")");
					result.add(line);
					if (result.size()==maxRows) { return result; }
					curLineStart = idx+1;				// weiter bei nächsten Zeichen!
					curLineLen = 0;
					savePoint = 0;
					idx = curLineStart;
					continue;
				}
				// hier: normales Zeichen
				idx++;
				curLineLen++;
				continue;				// weiter...
			}
			// hier: die aktuelle Zeile ist voll:
			
			// Versuch 1: Gehe zurück bis zum letzen Blank=SavePoint
			if (savePoint>0) {
				String line = input.substring(curLineStart, savePoint-1);		// schneide raus bis zum SavePoint! (ohne Blank)
				// System.err.println("cut (" + line + ")");
				result.add(line);
				if (result.size()==maxRows) { return result; }
				// Bereite nächste Zeile vor:
				curLineStart = savePoint +1-1;		// die nächste Zeile fängt nach dem Blank an
				curLineLen = 0;
				savePoint = 0;
				idx = curLineStart;					// fange beim letzten SavePoint an
			} else {
				// Autsch: es GAB noch gar kein BLANK in der Zeile, d.h. wir haben ein Wort, das LÄNGER ist als 'maxChars' !!
				// Daher: Schneide trotzdem und führe das Wort in der nächsten Zeile fort
				String line = input.substring(curLineStart, idx);		// schneide raus bis zum aktuellen Zeichen!
				// System.err.println("cut (" + line + ")");
				result.add(line);
				if (result.size()==maxRows) { return result; }
				// Bereite nächste Zeile vor:
				curLineStart = idx;					// nächste Zeile fängt beim AKTUELLEN Zeichen an, weil das ja zu lang war
				curLineLen = 0;
				savePoint = 0;
				idx = curLineStart;
			}
		}
		// out of input: erstelle die letzte Zeile
		String line = input.substring(curLineStart, idx);		// schneide bis zum Ende
		// System.err.println("cut (" + line + ")");
		result.add(line);
		
		return result;
	}

	/**
	 * Kopiere was in die Zwischenablage
	 */
	public static void textToClipboard(Display display, String text) {
		if (display==null || text==null || text.length()==0) {
			return;
		}
		Object[] data = { text };
		Transfer[] dataTypes = { TextTransfer.getInstance() };
		new Clipboard(display).setContents(data, dataTypes);
	}
	
	
	
	/**
	 * Helfer: Schreibt die Felder des 'obj' als String raus <br>
	 * - kann zB bequem ein Parameter-Obj in einen String wandeln: "pro=10, con=20, ...2
	 * 
	 * @param obj - wenn null: return "obj==null"
	 * @param delimeter - Trennzeichen, zB ", " oder "\t"
	 */
	public static String fieldsToString(Object obj, String delimeter) {
		if (obj==null) {
			return "obn==null";
		}
		StringBuilder sb = new StringBuilder();
		Field[] fields = obj.getClass().getDeclaredFields();
		for (Field f : fields) {
			try {
				String name = f.getName();
				if (f.isAccessible()==false) {		// lese auf private-stuff
					f.setAccessible(true);
				}
				sb.append(name);	sb.append('=');
				Object value = f.get(obj);
				sb.append(value);	sb.append(delimeter);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		String s = sb.toString();
		if (s.length()>delimeter.length()) {
			s = s.substring(0, s.length()-delimeter.length());
		}
		return s;
	}
	
	/**
	 * Liefert die unteren 'count' bytes als "00101010 10101010", jedes Byte durch Leerzeichen getrennt
	 */
	public static String toBits(long zahl, int countBytes) {
		// so machen: ich erzeuge ein char[] und gehe von links/nieder nach rechts/höher-wertige-Bits
		char[] x = new char[countBytes*8+(countBytes-1)];		// Platz für Leerzeichen
		int pos=x.length-1;
		for (int c=1; c<=countBytes; c++) {
			for (int i=0; i<8; i++) {
				long test = zahl & 1L;
				/*log(Long.toBinaryString(zahl));
				log(Long.toBinaryString(1L));
				log(Long.toBinaryString(test)); */
				if (test > 0) {			// 0000 0001
					x[pos--] = '1';
				} else {
					x[pos--] = '0';
				}
				zahl = zahl >>> 1;
			}
			if (c<countBytes) {
				x[pos--] = ' ';
			}
		}
		return new String(x);
	}
	
	/**
	 * Liefert die unteren 'size' bits als 00101010101, also zb 11 Bits, keine Leerstellen
	 */
	public static String toBitString(long zahl, int sizeBits) {
		// so machen: ich erzeuge ein char[] und gehe von links/nieder nach rechts/höher-wertige-Bits
		char[] x = new char[sizeBits];
		for (int i=sizeBits-1; i>=0; i--) {
			long test = zahl & 1L;		// teste unterstes Bit
			if (test > 0) {
				x[i] = '1';
			} else {
				x[i] = '0';
			}
			zahl = zahl >>> 1;
		}
		return new String(x);
	}
	
	/**
	 * Liefert die unteren 'count' bytes als 00101010, Bytes durch Leerzeichen getrennt
	 */
	public static String toBits(byte[] data, int countBytes) {
		// so machen: ich erzeuge ein char[] und gehe von links/nieder nach rechts/höher-wertige-Bits
		char[] x = new char[countBytes*8+(countBytes-1)];		// Platz für Leerzeichen
		int pos=x.length-1;
		for (int c=1; c<=countBytes; c++) {
			int b = data[c-1];
			for (int i=0; i<8; i++) {
				int test = b & 0x1;
				/*log(Long.toBinaryString(zahl));
				log(Long.toBinaryString(1L));
				log(Long.toBinaryString(test)); */
				if (test > 0) {			// 0000 0001
					x[pos--] = '1';
				} else {
					x[pos--] = '0';
				}
				b = b >>> 1;
			}
			if (c<countBytes) {
				x[pos--] = ' ';
			}
		}
		return new String(x);
	}

	/**
	 * Liefert bits von einem Byte
	 */
	public static String toBits(byte value) {
		return toBits(new byte[]{value}, 1);
	}


	
	/**
	 * TESTS
	 *
	public static void main(String[] args) {
		//String s = "heute ist ein schöner Tag und esgibtauchabundzumallangeWörter die OhneTrennzeichenAuskommen müssen   ";
		String s = "heute: \nSchöner Tag \n und esgibtauchabundzumallangeWörter die OhneTrennzeichenAuskommen müssen   ";
		List<String> list = splitString(s, 16, 5);
		System.out.println("123456789_123456789_123456789_123456789_");
		for (String x : list) {
			System.out.println(x);
		}
		System.out.println("--------------");
//		s = splitString_toNewLine(s, 16, 5);
//		System.out.println(s);
		
		// test fieldsToString
		Object x = new SimpleTradeInfo(new TimeUnit(), 999);
		System.out.println(x + " => " + fieldsToString(x, ", "));
	} */

	/**
	 * Liefere den Wert, min oder max
	 */
	public static int inRange(int value, int min, int max) {
		if (value < min) {
			return min;
		}
		if (value > max) {
			return max;
		}
		return value;
	}

	
	/**
	 * Ermittle die Anzahl der nötigen Nachkomma-Stellen für einen Schrittweite, zB tic-Size
	 * BSPs <br>
	 *   0.01 => 2  <br>
	 *   0.25 => 2  <br>
	 *   0.5  => 1  <br>
	 *   1,0  => 1  <br>
	 *   1,25  => 2  <br>
	 *   0.015625 => 6 <br>
	 *   0.03125  => 5
	 */
	public static int calcNachkommaAnzahlForDelta(float stepSize) {
		DecimalFormat nf = new DecimalFormat("0.0000000", new DecimalFormatSymbols(Locale.US));		// damit's immer Deciaml-PUNKT wird

		int maxStellen, p;
		// init maxStellen
		int ohneNach = (int) stepSize;
		int inv = (int) (1 / (stepSize-ohneNach));		// 0.01 => 100
		String s = String.valueOf(inv-1);
		// System.out.println(s);
		maxStellen = s.length();
		
		// Idee: teste einfach mehrere Zahlen durch: 'nf' macht immer trailing-0-en 
		// Ich gehe also nach dem '.' los und suche die erste '0'
		// damit umgehe ich auch elegant Rundungs-Probleme "1.8000002"
		double x = 0f;
		for (int i=0; i<10; i++) {
			x = x + stepSize;
			s = nf.format(x);
			// System.out.println(s + "    ->    ");
			// jetzt check die nicht-nuller hinterm '.'
			int start = s.indexOf('.');
			p = start + maxStellen; 			// fange erst nach 'maxStellen' an zu suchen
			int first0 = s.length();
			while (p < first0) {
				char c = s.charAt(p);
				if (c=='0') {
					first0 = p;
					break;
				}
				p++;
			}
			p = first0 - start - 1;
			if (p>maxStellen) {
				maxStellen = p;
			}
		}
		return maxStellen;
	}


	
	static private final ThreadLocal<char[]> tempForNumberForSep = new ThreadLocal<>();
	
	
	/**
	 * Helper: Erzeugt aus -1234.56 => "-1 234.56" wenn sep=' '
	 * 		wenn die Zahl klein-mit-viel-Nachkomma ist => kick die vielen Nachkommas
	 */
	public static String formatNumberWithSep(double number, char sep) {
		
		// A) Check auf viele Nachkommas
		if (number <= 1000) {
			String s = String.valueOf(number);
			int i = s.indexOf('.');
			if ((s.length()-i) > 3) {
				DecimalFormat df = new DecimalFormat("#.##");
				return df.format(number);
				// return String.format("%.2g%n", 0.912385);
			}
		}
		
		
		// brauche Temp-Ding -> lege das im Thread ab
		char[] tempChars = tempForNumberForSep.get();
		if (tempChars==null) {
			tempChars = new char[30];
			tempForNumberForSep.set(tempChars);  		// create on demand
		}
		
		// hänge Minus ab: damit's vor dem '-' kein Leerzeichen gibt
		boolean isMinus = (number<0);
		if (isMinus) {
			number = -number;
		}
		
		long ganz = (long)number;				// -12345
		String s;
		int i = 29, sI;
		if (ganz != number) {				// wenn's Nachkommas hat: kopiere den ".56" schon mal rüber
			s = String.valueOf(number);		// da kommt immer was mit "xx.0" raus!
			int n = s.length();
			sI = n-1;
			int p = s.lastIndexOf('.');
			for (int j=n-1; j>=p; j--) {
				tempChars[i--] = s.charAt(j);
			}
			sI = p-1;		// zeigt auf's erste Zeichen, ganz rechts bei GANZ oder erstes-links-vom-Komma
		} else {
			s = String.valueOf(ganz);
			sI = s.length()-1;
		}
		
		// jetzt gehe in Dreier-Gruppen durch:
		int count=0;
		for (int j=sI; j>=0; j--) {
			tempChars[i--] = s.charAt(j);
			if ((++count)==3 && (j>0)/*es muss noch etwas davor geben!*/) {
				tempChars[i--] = sep;
				count=0;
			}
		}
		
		// Minus?
		if (isMinus) {
			tempChars[i--] = '-';
		}
		
		return new String(tempChars, i+1, (29-i));
	}

	
	/** 
	 * Zerlege einen String in eine ArrayList
	 * 
	 * @return leere-Liste bei s='' oder s==null
	 */ 
	public static List<String> stringToList(String s, char sep) {
		List<String> result = new ArrayList<String>(50);
		if (s==null) return result;
		int n=s.length();
		if (n==0) return result;
		
		int i = 0;
		int curStart = 0;
		
		// start: gehe von Links auf das erste nicht-sep-Zeichen
		while (i<n) {
			if (s.charAt(i)!=sep) {
				break;
			}
			i++;
		}
		curStart = i;
		boolean inText = true;		// Start: wir stehen schon auf einem Text
		
		// "jetzt alle durch-gehen "
		while (i<=n) {
			char c = (i==n) ? sep : s.charAt(i);		// netter Trick: hänge künstlichen Sep an Text
			if (inText) {		// warte auf Text-Ende = erster Sep
				if (c==sep) {
					String text = s.substring(curStart, i);		// schneide raus
					result.add(text);							// und sammle
					inText = false;
				}
			} else {
				if (c!=sep) {		// warte auf nächsten Text
					curStart = i;
					inText = true;
				}
			}
			i++;
		}	// über alle Zeichen
		
		// Keine "Nach-arbeit" nötig
		
		return result;
	}	// string -> List<String>

	/**
	 * Splitte "a,b,c,d" in ein Tokens, return Set<String>
	 */
	public static Set<String> stringToSet(String tokens, char sep) {
		return new HashSet<String>(stringToList(tokens, sep));
	}

	/**
	 * Helfer: Zerlege 'input' anhand von 'sep', jedes Token ist ein Key=Value, mit Trenner 'kvSplit', streame diese (key,value) an 'code'  <br> <br>
	 * 
	 * "k1=val1,k2=val2,k3=val3, ..."  : sep=','   kvSplit='='
	 */
	public static void keyValueStringSplit(String input, char sep, char kvSplit, BiConsumer<String, String> code) {
		
		if (input==null || input.length()==0) return;			// sicher ist sicher
		
		boolean waitForSplit = true;
		boolean waitForSep = false;
		int n = input.length();
		int i = 0;
		int tokStartIdx = 0;
		String curKey = null;
		
		while (true) {
			char c = input.charAt(i++);
			if (waitForSplit) {
				if (c==kvSplit) {
					curKey = input.substring(tokStartIdx, i-1);			// schneide raus
					tokStartIdx = i;									// da geht der nächste String los
					waitForSep = true;		waitForSplit = false;		// jetzt warte auf den nächsten Seperator
					continue;
				}
			}
			if (waitForSep) {
				if (c==sep) {
					String value = input.substring(tokStartIdx, i-1);		// schneide raus
					tokStartIdx = i;
					waitForSep = false;		waitForSplit = true;		// jetzt warte wieder auf den nächsten Split
					// melde noch:
					code.accept(curKey, value);
					continue;
				}
			}
			
			// nach EOF
			if (i==n) {
				String value = input.substring(tokStartIdx);			// Letztes Token muss ein 'value' sein
				// melde noch:
				code.accept(curKey, value);
			break;														// jetzt fertig
			}
		}	// über alle Chars
	}

	
	
	
	
	
	
	
	/**
	 * TESTs
	 */
	public static void main(String[] args) {
		// System.out.println(Utils.calcNachkommaAnzahlForDelta(0.00005f));
		// check von hinten, ab wann sich's unterscheidet
		
		/*
		System.out.println(formatNumberWithSep( -9_456_789.3141, ' '));
		System.out.println(formatNumberWithSep( 0, ' ')); 
		System.out.println(formatNumberWithSep( -1f, ' ')); 
		System.out.println(formatNumberWithSep( 1024, ' ')); 
		System.out.println(formatNumberWithSep( 1024.60, ' '));
		*/
		
		stringToListTests("a");
		stringToListTests(" a b c");
		stringToListTests(" a ");
		stringToListTests("  ");
		stringToListTests(" GC SI PL   x y  ");
		
	}

	/**
	 * teste die {@link #stringToList(String, char)} Methode
	 */
	private static void stringToListTests(String input) {
		System.out.println("["+input + "] => " + stringToList(input, ' '));
	}


	/**
	 * <code> "03,06,9,12" => int[] { 3,6,9, 12 } </code>
	 */
	public static int[] toIntArrayFromString(String intList, String sep) {
		String[] toks = intList.split(sep);
		int[] result = new int[toks.length];
		for (int i=0; i<toks.length; i++) {
			result[i] = Integer.parseInt(toks[i].trim());
		}
		return result;
	}
	
	/**
	 * Liefere einen Map-Key oder lege ihn an
	 * @param buildFirstEntry - insert-Methode
	 */
	public static <K, V> V  MAP_GET(Map<K, V> map, K key, Supplier<V> buildFirstEntry) {
		if (key==null) {
			throw new NullPointerException("MAP_KEY: key==null");
		}
		V val = map.get(key);
		if (val!=null) {
			return val;
		}
		// lege an per callback
		val = buildFirstEntry.get();
		map.put(key, val);
		return val;
	}
	/**
	 * Liefere einen Map-Key oder lege ihn an <br>
	 * VERSION MIT <b>Constructor-Ref<b> wenn der Constructor den KEY auf-nimmt
	 * 
	 * @param constWithKeyParam - zB ein <code> public MyValWrapper(String myKey)..</code> 
	 */
	public static <K, V> V  MAP_GET_C(Map<K, V> map, K key, Function<K,V> constWithKeyParam) {
		if (key==null) {
			throw new NullPointerException("MAP_KEY: key==null");
		}
		V val = map.get(key);
		if (val!=null) {
			return val;
		}
		// lege an:
		val = constWithKeyParam.apply(key);
		map.put(key, val);
		return val;
	}
	/**
	 * Gehe alle Key=>Value Dinge durch -> rufe alle Elemente einzeln auf, ggf SORTIERT
	 * 
	 * @param map - wenn null: mache nichts
	 * @param onEveryEntry - wenn null: mache nichts
	 */
	public static <K extends Comparable<K>, V> void MAP_FOREACH(Map<K, V> map, boolean sortKeys, BiConsumer<K, V> onEveryEntry) {
		if (map==null || onEveryEntry==null) {
			return;
		}
		List<K> keys = new ArrayList<>(map.keySet());
		if (sortKeys) {
			Collections.sort(keys);
		}
		for (K key : keys) {
			V val = map.get(key);
			onEveryEntry.accept(key, val);
		}
	}
	/**
	 * Liefere einen Map-Key oder lege ihn an <br>
	 * VERSION MIT <b>Constructor-Ref<b> und LEER-Constructor, zb <code>new ArrayList()</code>
	 *
	public static <K, V> V  MAP_GET_2(Map<K, V> map, K key, Supplier<V> constEmpty) {
		if (key==null) {
			throw new NullPointerException("MAP_KEY: key==null");
		}
		V val = map.get(key);
		if (val!=null) {
			return val;
		}
		// lege an:
		val = constEmpty.get();
		map.put(key, val);
		return val;
	}*/
	
	/**
	 * - Füge das elem ist die Liste ein, <br>
	 * - wenn die Liste noch null ist: ERZEUGE sie
	 * - wenn Element null ist (und Liste auch null) => null kommt zurück
	 */
	public static <T> List<T> LIST_ADD(List<T> listOrNull, T thisElement) {
		if (thisElement==null) {
			return listOrNull;
		}
		if (listOrNull==null) {
			listOrNull = new ArrayList<T>();
		}
		listOrNull.add(thisElement);
		return listOrNull;
	}

	/**
	 * array[] add to List<>
	 */
	public static <T> void listAdd(List<T> list, T[] addThese) {
		if (addThese==null || addThese.length==0 || list==null) {
			return;
		}
		for (T t : addThese) list.add(t);
	}

	
	/**
	 * Baut einen StringKey-to-Idx: 
	 */
	public static StringKeyToIdx STRINGKeyIdx(int initialValue) {
		return new StringKeyToIdx(initialValue);
	}

	/**
	 * sortiere schnell mal (gut für Eclipse's Expressions-Tab)
	 */
	public static List<String> sorted(Collection<String> input) {
		List<String> result = new ArrayList<>(input);
		Collections.sort(result);
		return result;
	}
	
	/**
	 * Jeder neue Key gibt einen (+1)-idx
	 */
	public static class StringKeyToIdx {
		
		private int nextIdx;
		private final Map<String, Integer> map = new HashMap<String, Integer>();
		
		StringKeyToIdx(int initialValue) {
			this.nextIdx = initialValue;
		}
		
		/**
		 * Liefere den Idx zum Key, auto-inc(+1) bei jedem neuem Key
		 */
		public int keyIdx(String key) {
			Integer i = map.get(key);
			if (i==null) {
				i = nextIdx++;
				map.put(key, i);
			}
			return i;
		}
		/**
		 * Liefere den Idx zum Key, bei first-time-Key liefere (-idx)
		 */
		public int keyIdxFirstNegative(String key) {
			Integer i = map.get(key);
			if (i==null) {
				i = nextIdx++;
				map.put(key, i);
				return -i;
			}
			return i;
		}
	}

	/**
	 * Ermittle das Maximum von etwas aus den Elementen einer Liste <br>
	 * - die Mapper-Funktion muss einen VERGLEICHs-INTEGER liefern
	 * 
	 * @return WERT: den grössten Vergleichs-INTEGER oder null wenn die Liste leer war 
	 */
	public static <T> Integer MAX_value(List<T> elements, Function<T, Integer> elToInt) {
		if (elements==null || elements.size()==0) {
			return null;
		}
		int max = Integer.MIN_VALUE;
		for (int i=0; i<elements.size(); i++) {
			int v = elToInt.apply( elements.get(i) );
			if (v>max) max=v;
		}
		return max;
	}
	/**
	 * Ermittle den Listen-INDEX des größten Elements <br>
	 * - die Mapper-Funktion muss einen VERGLEICHs-INTEGER liefern
	 * 
	 * @return idx oder (-1) wenn Liste leer war 
	 */
	public static <T> int MAX_index(List<T> elements, Function<T, Long> elToLong) {
		if (elements==null || elements.size()==0) {
			return -1;
		}
		long max = Long.MIN_VALUE;
		int maxAtIdx = 0;
		for (int i=0; i<elements.size(); i++) {
			long v = elToLong.apply( elements.get(i) );
			if (v>max) {
				max=v;
				maxAtIdx = i;
			}
		}
		return maxAtIdx;
	}
	/**
	 * Ermittle das Listen-ELEMENT mit dem größten Wert <br>
	 * - die Mapper-Funktion muss einen VERGLEICHs-LONG liefern
	 * 
	 * @return Element oder null (wenn Liste leer war)
	 */
	public static <T> T MAX_element(List<T> elements, Function<T, Long> elToLong) {
		if (elements==null || elements.size()==0) {
			return null;
		}
		long max = Long.MIN_VALUE;
		T found = null;
		for (int i=0; i<elements.size(); i++) {
			T e = elements.get(i);
			long v = elToLong.apply( e );
			if (v>max) {
				max=v;
				found = e;
			}
		}
		return found;
	}
	/**
	 * Ermittle das Listen-ELEMENT mit dem größten Wert <br>
	 * - die Mapper-Funktion muss einen VERGLEICHs-LONG liefern
	 * 
	 * @return Element oder null (wenn Liste leer war)
	 */
	public static <T> T MAX_element(T[] elements, Function<T, Long> elToLong) {
		if (elements==null || elements.length==0) {
			return null;
		}
		long max = Long.MIN_VALUE;
		T found = null;
		for (int i=0; i<elements.length; i++) {
			T e = elements[i];
			long v = elToLong.apply( e );
			if (v>max) {
				max=v;
				found = e;
			}
		}
		return found;
	}
	
	
	/**
	 * Ermittle den Listen-Index anhand einer Suche-Func (erster Treffer zählt)
	 *  
	 * @param elFound - liefert TRUE wenn das Such-Kriterium passt
	 *  
	 * @return 0..n oder (-1)
	 */
	public static <T> int LIST_Index(List<T> elements, Function<T, Boolean> elFound) {
		if (elements==null || elements.size()==0) {
			return -1;
		}
		for (int i=0; i<elements.size(); i++) {
			T e = elements.get(i);
			boolean hit = elFound.apply( e );
			if (hit) {
				return i;
			}
		}
		return -1;
	}
	
	
	/**
	 * Summiere etwas auf, jedes Listen-Element liefert eine int-Zahl
	 */
	public static <T> int list_SUM(List<T> elements, Function<T, Integer> el2Summand) {
		if (elements==null || elements.size()==0) {
			return 0;
		}
		int sum = 0;
		for (int i=0; i<elements.size(); i++) {
			int add = el2Summand.apply( elements.get(i) );
			sum += add;
		}
		return sum;
	}
	
	/**
	 * Zähle wie oft "etwas" in einer List vorkommt
	 * @return 0 wenn die Liste null oder leer ist
	 */
	public static <T> int count(List<T> elements, Function<T, Boolean> checkPresence) {
		if (elements==null || elements.size()==0) {
			return 0;
		}
		int count=0;
		for (int i=0; i<elements.size(); i++) {
			boolean yes = checkPresence.apply( elements.get(i) );
			if (yes) count++;
		}
		return count;
	}

	/**
	 * Zerlege einen String in Tokens (anhand Trenn-Zeichen)
	 * @return Liste von etwas(s), LEER wenn irgendwas klemmt
	 */
	public static <T> List<T> tokenize(String tokenString, String separator, Function<String, T> stringToVal) {
		List<T> result = new ArrayList<>();
		try {
			String[] toks = tokenString.split(separator);
			for (String s : toks) {
				T val = stringToVal.apply(s);
				result.add(val);
			}
		} catch (Throwable th) {
			System.err.println("Utils.tokenize: error with tokenString [" + tokenString + "] = " + th);
		}
		return result;
	}
	

	/**
	 * KICKE alle Elemente aus der Liste, die NICHT in keep... enthalten sind
	 */
	public static <T> void listKeepOnly(Collection<T> list, @SuppressWarnings("unchecked") T... onlyThese) {
		List<T> keepCol = Arrays.asList(onlyThese);
		list.retainAll(keepCol);
	}

	
	/**
	 * Extrahiere KEY-Elemente aus einer Liste, liefere ein unique-Set
	 * 
	 * @param useThisResultSetOrNull - zb ein Sorted-Set
	 */
	public static <T,K> Set<K> listToKeySet(Collection<T> list, Set<K> useThisResultSetOrNull, Function<T, K> keyExtractFunc) {
		Set<K> keySet = (useThisResultSetOrNull!=null ? useThisResultSetOrNull : new HashSet<>(list.size()/3) );
		for (T t : list) {
			K key = keyExtractFunc.apply(t);
			keySet.add(key);
		}
		return keySet;
	}
	
	
	/**
	 * Erzeuge String, der 'useLength' Platz braucht, fülle mit Leerzeichen
	 * 
	 * @param useOrNull - Verwende diesen StringBuilder
	 * 
	 * @return org-String wenn der schon zu lang ist
	 */
	public static String withLength(String s, int useSpace, StringBuilder useOrNull) {
		int n = s.length();
		if (n>useSpace) return s;
		StringBuilder sb;
		if (useOrNull!=null) {
			sb = useOrNull;
			sb.setLength(0);	 		// reset
		} else {
			sb = new StringBuilder();
		}
		sb.append(s);
		for (int i=n; i<useSpace; i++) {
			sb.append(' ');
		}
		return sb.toString();
	}

	
	/**
	 * Ersetze Sonderzeichen für HTML
	 */
	public static String escapeToHtml(String label) {
		if (label==null || label.length()==0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<label.length(); i++) {
			char c = label.charAt(i);
			switch (c) {
				case 'ä':			sb.append("&auml;");		break;
				case 'ö':			sb.append("&ouml;");		break;
				case 'ü':			sb.append("&uuml;");		break;
				case 'Ä':			sb.append("&Auml;");		break;
				case 'Ö':			sb.append("&Ouml;");		break;
				case 'Ü':			sb.append("&Uuml;");		break;
				case '&':			sb.append("&amp;");			break;
				case '<':			sb.append("&lt;");			break;
				case '>':			sb.append("&gt;");			break;
				default:			sb.append(c);				break;
			}
		}
		return sb.toString();
	}

	
	
	/**
		Zeichen 	Unicode
		------------------------------
		Ä, ä 		\u00c4, \u00e4
		Ö, ö 		\u00d6, \u00f6
		Ü, ü 		\u00dc, \u00fc
		ß 			\u00df
		
		@return LEER-String wenn input==null
	 */
	public static String escapeToUnicode(String input) {
		if (unicodeMap==null) {			// hier kein SYNC, im super-race-Fall wird's halt zweimal erzeugt
			unicodeMap = new HashMap<Character, String>(10);
			unicodeMap.put('Ä', "\\u00c4");
			unicodeMap.put('ä', "\\u00e4");
			unicodeMap.put('Ö', "\\u00d6");
			unicodeMap.put('ö', "\\u00f6");
			unicodeMap.put('Ü', "\\u00dc");
			unicodeMap.put('ü', "\\u00fc");
			unicodeMap.put('ß', "\\u00df");
		}
		
		if (input==null || input.length()==0) return "";
		StringBuilder sb = new StringBuilder(input.length()+20);
		for (int i=0; i<input.length(); i++) {
			char c = input.charAt(i);
			String rep = unicodeMap.get(c);
			if (rep!=null) {
				sb.append(rep);
			} else {
				sb.append(c);	
			}
		}
		return sb.toString();
	}	
	private static Map<Character, String> unicodeMap;	// on-demand


	/**
	 * Filter-Loop: Liefere i=0...(n-1) an die Func
	 * 
	 * @param newListValues - func bekommt Index, muss NULL oder Wert liefern
	 */
	public static <T> List<T> filterByIndex(int n, Function<Integer, T> newListValues) {
		List<T> result = new ArrayList<>();
		for (int i=0; i<n; i++) {
			T val = newListValues.apply(i);
			if (val!=null) {
				result.add(val);
			}
		}
		return result;
	}

	/**
	 * Filtere die Elemente einer Liste/Collection: <b>List[] => newList[less objs, same type]</b>
	 * 
	 * @param predicate - liefert <b>true</b>: Element <b>bleibt</b> in Result-Liste erhalten
	 * @return neue Liste, ggf leer
	 */
	public static <T> List<T> FILTER(Collection<T> input, Function<T, Boolean> predicate) {
		List<T> result = new ArrayList<>();
		input.forEach(e->{
			if (predicate.apply(e)) {
				result.add(e);
			}
		});
		return result;
	}
	
	/**
	 * Filter'e und liefere gleich ein anderes Object  <b>List[] => newList[less objs, other type]</b>
	 * 
	 * @param filterConverter - bekommt jedes input-Element [T], muss liefern: <br>
	 *   <b>null</b> - nicht nehmen <br>
	 *   <b>Object [N]</b> - nehmen
	 *   
	 * @return neue Liste &lt;N&gt; , idr kleiner als input, ggf leer
	 */
	public static <T,N> List<N> FILTER_toOBJ(Collection<T> input, Function<T, N> filterConverter) {
		List<N> result = new ArrayList<>();
		for (T t : input) {
			N newObj = filterConverter.apply(t);
			if (newObj!=null) {
				result.add(newObj);
			}
		}
		return result;
	}
	

	/**
	 * Baue eine Lookup-Map aus einer Liste, KEY und Element-TYPE sind genersich
	 */
	public static <K,T> Map<K, T> listToMap(List<T> elements, Function<T, K> keyProducer) {
		Map<K, T> result = new HashMap<>(elements.size());
		for (T e : elements) {
			K key = keyProducer.apply(e);
			result.put(key, e);
		}
		return result;
	}

	/**
	 * Aus-dünnen: Gehe alle (key,value)-Elemente der Map durch und 
	 * LÖSCHE wenn die inner-Func <code>true</code> liefert
	 */
	public static <K,V> void removeFromMap(Map<K, V> map, BiFunction<K, V, Boolean> checker) {
		List<K> keys = new ArrayList<>(map.keySet());
		for (K k : keys) {
			V val = map.get(k);
			boolean remove = checker.apply(k, val);
			if (remove) {
				map.remove(k);
			}
		}
		
	}

	/** 
	 * Close stream if possible, never throw anything
	 */
	public static void closeStream(FileInputStream is) {
		if (is==null) return;
		try {
			is.close();
		} catch (Throwable th) { };
	}

	
	/**
	 * siehe SORT_MultiLevel: jedes Element muss diese temp-Info-Obj füllen
	 */
	public static class MultiLevelSortInfo<E> {
		
		final E el;
		Comparable<?> obj;
		int level;
		boolean reverseOrder;
		
		public MultiLevelSortInfo(E el) {
			this.el = el;
		}
		/**
		 * auf welchem Level soll was verglichen werden
		 * 
		 * @param ding  : für Level-Gleichheit: Vergleiche diese Objs miteinander <br>
		 *        			ACHTUNG: <code>null</code> ist ZULÄSSSIG, dann gewinnt das Object mit !=null
		 *        
		 * @param level : 1 = best, dann absteigend  (0 ist nicht erlaubt!)
		 * 
		 * @param reverseOrder - false: normales Comparable-Ordnung. <br>
		 * 						 true: zb der grössere DayKey gewinnt
		 *        
		 */
		public void set(Comparable<?> ding, int level, boolean reverseOrder) {
			this.reverseOrder = reverseOrder;
			if (level<=0) {
				throw new IllegalArgumentException("level must be >= 1");
			}
			this.obj = ding;   this.level = level;
		}
	}
	
	@FunctionalInterface
	public static interface IMultiLevelSort_OnEveryElement<T> {
		/**
		 * callback auf jedes Element der Liste
		 */
		public void getInfo(T listElement, MultiLevelSortInfo<T> info);
	}
	
	/**
	 * SORT Helfer für <b> mehr-stufiges Sortieren </b>
	 * 
	 * - Jedes Element der Liste muss ZWEI Infos liefern <br>
	 *  - das {@link Comparable} Ding				<br>
	 *  - der LEVEl auf dem es läuft: 1..n
	 *  
	 * @param inputList
	 * @return NEUE Liste, sortiert
	 */
	public static <T> List<T> SORT_MultiLevel(Collection<T> inputList, IMultiLevelSort_OnEveryElement<T> sortDingProvider) {
		if (inputList==null) {					// sicher ist sicher
			return new ArrayList<>();
		}
		if (sortDingProvider==null) {
			throw new NullPointerException("SORT_MultiLevel: callback==null!");
		}
		
		// 1) baue temp-Infos auf
		final int n = inputList.size();
		@SuppressWarnings("unchecked")
		final MultiLevelSortInfo<T>[] infos = new MultiLevelSortInfo[n]; 
		
		int i=0;
		for (T el : inputList) {
			infos[i] = new MultiLevelSortInfo<T>(el);		// ich muss dann in 2) auf der INFO-Liste sortieren, und DAHER die El-REF mit-nehmen!
			sortDingProvider.getInfo(el, infos[i]);
			i++;
		}
		
		// 2) Sortiere alles anhand der Infos [level + comparable]
		Arrays.sort(infos, new Comparator<MultiLevelSortInfo<T>>() {
			/**
			 * HIER steckt jetzt die Leve-Compare-Logik: Beim VERGLEICH zweier Objs mache:
			 * 1) Level hat prio: das Object mit dem niedrigeren level (1=best) gewinnt
			 * 2) bei Level-GLEICH-heit: jetzt vergleiche die Comparable-Dinge
			 */
			@SuppressWarnings("unchecked")
			@Override
			public int compare(MultiLevelSortInfo<T> o1, MultiLevelSortInfo<T> o2) {
				
				// 1) Level-Vergleiche
				if (o1.level < o2.level) {
					return -1;
				}
				if (o1.level > o2.level) {
					return 1;
				}
				
				// 2) bei gleich-Level: das Compare-Ding entscheidet. Achtung: kann null sein
				Comparable c1 = o1.obj, c2 = o2.obj;
				
				if (c1!=null) {
					if (c2==null) {
						// Achtung: die 'reverseOrder'-Info lebt ja in jedem Info-Object, könnte also (wenn der User-Code Käse eingibt!)
						// auch unterschiedlich sein... Mir egal hier, ich nehem mal o1
						return (o1.reverseOrder ? 1: -1);		// c1 gewinnt		
					}
					// hier: beide gesetzt: 
					int comp = c1.compareTo(c2);
					return (o1.reverseOrder ? -comp : comp);
				} else {
					if (c2!=null) {		// c1==null
						return (o1.reverseOrder ? -1 : 1);			// c2 gewinnt
					}
					// hier: BEIDE null
					return 0;
				}
			}
		});
		
		// 3) return: baue NEUE Liste:
		List<T> result = new ArrayList<>(n);
		for (MultiLevelSortInfo<T> in : infos) {			// infos[] ist jetzt sortiert und hält die REFs
			result.add(in.el);
		}
		
		return result;
	}


	/**
	 * Laufe durch die Element der 'input' Liste und liefere ein native-Array des convertierten-outputs
	 */
	public static <T, E> T[] MAP_LIST(Class<T> retType, List<E> input, Function<E, T> converter) {
		int n = input.size();
		@SuppressWarnings("unchecked")
		T[] res = (T[]) Array.newInstance(retType, n);
		int i=0; 
		for (E e : input) {
			T use = converter.apply(e);
			res[i++] = use;
		}
		return res;
	}
	
	/**
	 * Laufe durch die Element der 'input' Liste und liefere ein native-Array des convertierten-outputs
	 */
	public static <T, E> T[] MAP_ARRAY(Class<T> retType, E[] input, Function<E, T> converter) {
		int n = input.length;
		@SuppressWarnings("unchecked")
		T[] res = (T[]) Array.newInstance(retType, n);
		int i=0; 
		for (E e : input) {
			T use = converter.apply(e);
			res[i++] = use;
		}
		return res;
	}

	/**
	 * Liste rein, neue Liste raus
	 * @return null wenn input oder mapFunc null sind, ggf <b>leere Liste</b>
	 */
	public static <T,N> List<N> MAP(Collection<T> input, Function<T, N> mapFunc) {
		if (input==null || mapFunc==null) {
			return null;
		}
		int n = input.size();
		List<N> result = new ArrayList<>(n);
		for (T t : input) {
			N nObj = mapFunc.apply(t);
			result.add(nObj);
		}
		return result;
	}
	
	
	/**
	 * Convertiere (MAP-Function) ein Array in ein anderes <br>
	 * <b> ACHTUNG: wenn 'input' LEER ist dann kommt hier NULL !! <b>
	 * 
	 * @return NULL ggf, ACHTUNG!!
	 */
	public static <T, E> T[] MAP_ARRAY(E[] input, Function<E, T> converter) {
		int n = input.length;
		if (n==0) {
			return null;
		}
		// Fahre mal den ersten Convert-Schritt => ich brauche den <T>
		T erg = converter.apply(input[0]);		// for now: hier darf kein null kommen!
		@SuppressWarnings("unchecked")
		T[] res = (T[]) Array.newInstance(erg.getClass(), n);		// NICE: Erzeuge Result-Array aus diesem Typ
		res[0] = erg;
		// jetzt Rest:
		for (int i=1; i<n; i++) {
			T use = converter.apply(input[i]);
			res[i] = use;
		}
		return res;
	}
	/**
	 * MAP auf Array mit Index: inner-param = (idx, elem) => (return T)
	 */
	public static <T, E> T[] MAP_ARRAY_IDX(E[] input, BiFunction<Integer, E, T> converter) {
		int n = input.length;
		if (n==0) {
			return null;
		}
		// Fahre mal den ersten Convert-Schritt => ich brauche den <T>
		T erg = converter.apply(0, input[0]);		// for now: hier darf kein null kommen!
		@SuppressWarnings("unchecked")
		T[] res = (T[]) Array.newInstance(erg.getClass(), n);		// NICE: Erzeuge Result-Array aus diesem Typ
		res[0] = erg;
		// jetzt Rest:
		for (int i=1; i<n; i++) {
			T use = converter.apply(i, input[i]);
			res[i] = use;
		}
		return res;
	}


	/**
	 * Gruppiere Elemente einer Liste nach einem Key <br>
	 *  => Ergebnis ist eine MAP mit {key1=[List], key2=[List], ...}
	 *  
	 *  HIER muss die Map schon reingegeben werden => gut weil ich APPEND-en kann 
	 *  
	 *  @param map - APPEND darein, wenn gesetzt, oder <b>null</b> => dann wird eine neue Map angelegt
	 *  
	 *  @return die MAP (gleiche Instanz) oder eben eine NEU ERZEUGTE
	 */
	public static <K, T> Map<K, List<T>> GROUP_intoMAP(Map<K, List<T>> map, List<T> flatList, Function<T, K> keyProducer) {
		if (map==null) {
			map = new HashMap<K, List<T>>();
		}
		if (flatList==null || flatList.size()==0) {
			return map;
		}
		for (T e : flatList) {
			K key = keyProducer.apply(e);
			List<T> eList = map.get(key);
			if (eList==null) {
				eList = new ArrayList<T>();
				map.put(key, eList);
			}
			eList.add(e);
		}
		return map;
	}

	/**
	 * Liefere Liste oder leere Liste
	 */
	public static <T> List<T> NOT_NULL(List<T> testList) {
		return (testList!=null) ? testList : new ArrayList<>();
	}


	/**
	 * Liefere den Sub-String bis zum 'sep' oder den ganzen String
	 */
	public static String subStringUntil(String input, String sep) {
		if (input==null || input.length()==0) {
			return input;
		}
		if (sep==null || sep.length()==0) {
			return input;
		}
		int i = input.indexOf(sep);
		if (i>=0) {
			return input.substring(0, i);
		}
		return input;
	}


	/**
	 * Erzeuge eine unique-Liste von etwas
	 * <br>
	 * Sortiere das Ergebnis (wenn möglich)
	 */
	@SuppressWarnings("unchecked")
	public static <T, U> U[] UNIQUE(Collection<T> list, Function<T, U> objToUniqueDing) {
		if (list==null || objToUniqueDing==null) {
			return null;
		}
		Set<U> uniqueSet = new HashSet<>();
		for (T t : list) {
			U unique = objToUniqueDing.apply(t);		// wandle Listen-Element in "ding", da wir unique() sammeln
			uniqueSet.add(unique);
		}
		// return set als Liste
		int n = uniqueSet.size();
		int i=0;
		U[] result = null;			// erzeuge dymamisch, cool:
		U dummy = null;
		for (U u : uniqueSet) {
			if (dummy==null) {
				dummy = u;
				result = (U[]) Array.newInstance(u.getClass(), n);
			}
			result[i++] = u;
		}
		
		// Sortiere wenn möglich:
		if (dummy!=null) {			// Leere Liste => dummy = null
			if (dummy instanceof Comparable<?>) {
				Arrays.sort(result);
			}
		}
		
		return result;
	}


	/**
	 * Erstelle einen Merge-Iterator
	 * 
	 * @param list - diese Liste soll element-weise liefern
	 * @param peekNextIsHit - interne Func: ist diese Element [T] (das nächst-mögliche) ein Treffer für den Key [K] : ja/nein
	 * @param <HitKey> - Key-Type (zB DayKey) nach dem das nächste Element der Liste ausgewählt wird
	 */
	public static <ListElement, HitKey> MergeIterator<ListElement, HitKey> mergeIter(Collection<ListElement> list, HitKey keyExample, BiPredicate<ListElement, HitKey> peekNextIsHit) {
		return new MergeIterator<ListElement, HitKey>(list, peekNextIsHit);
	}
	
	/**
	 * Hilfs-Dings für das Abgleichen von zwei Listen:
	 * - ein Driver (Code-Loop) geht Dinge durch, zB Datums-Werte (DayKey)
	 * - eine Liste von Objs ist nach DayKey sortiert, hat aber NICHT FÜR JEDEN Tag einen Eintrag
	 * - Ziel: Iterator liefert immer DANN (und nur dann) den nächsten Eintrag wenn der Tag stimmt
	 *   - sonst: null
	 */
	public static class MergeIterator<T, K> {
		private final BiPredicate<T, K> peekNextIsHit;
		private Iterator<T> iterator;		// wenn null: Liste ist gar nicht da oder leer, WIRD auch null wenn nichts mehr da
		private T nextCandi = null;
		
		/**
		 * constructor
		 * @param T - element-Typ der Liste, zB eine Bar/Row
		 * @param K - Key nach dem gesucht wird, zB DayKey
		 */
		public MergeIterator(Collection<T> list, BiPredicate<T, K> peekNextIsHit) {
			if (peekNextIsHit==null) {
				throw new IllegalArgumentException("peek-Func is null");
			}
			this.peekNextIsHit = peekNextIsHit;
			if (list==null || list.size()==0) {
				iterator = null;
			} else {
				iterator = list.iterator();
			}
		}
		/**
		 * Liefer den nächsten Eintrag oder null
		 */
		public T next(K key) {
			if (iterator==null) {
				return null;				// Nichts mehr da
			}
			if (nextCandi==null) {
				if (iterator.hasNext()) {
					nextCandi = iterator.next();
				} else {
					iterator = null;		// EOF => null
					return null;
				}
			}
			// hier: nextCandi ist immer gesetzt: Jetzt prüfe ob's der 'key' stimmt

			Boolean hit = peekNextIsHit.test(nextCandi, key);
			T ret = null;
			if (hit) {
				ret = nextCandi;
				nextCandi = null;			// reset; Beim nächsten 'next()' kommt der nächste Candi dran
			}
			return ret;
		}
		
	}	// Helfer		
	

	/**
	 * Helfer: <br>
	 *  int-Zähler der final sein muss (zB für inner-Lambdas)
	 */
	public static class IndexWrapper {
		public int idx=0;
	}
	
	
	/**
	 * Listen-Splitter: Zerlege die 'input'-Liste in beliebig viele TEIL-Listen
	 * 
	 * @param elemToIdx - Mapper: Bekommt jedes input[]-Element, muss liefern: <br> 
	 *      (-1) für: Element T soll NICHT gesammelt werden
	 *      (0..n): Sammle in dieser Teil-Liste
	 * 
	 * @return Array mit Teil-Listen, Index-Bereich ist das was die split-Func liefert
	 *   Hinweis: wenn ein Index gar nicht vorkommt, dann hat das Ergebnis-Array da eben eine null-Liste
	 */
	public static <T> List<T>[] LIST_SPLIT(List<T> input, Function<T, Integer> elemToIdx) {
		
		// 1) Sammle in temp-Map:
		Map<Integer, List<T>> sammler = new HashMap<>();
		int maxIdx = 0;
		for (T elem : input) {
			int listIdx = elemToIdx.apply(elem);		// mappe auf (-1) oder 0..n
			if (listIdx<0) {
				continue;
			}
			List<T> teil = sammler.get(listIdx);
			if (teil==null) {
				teil = new ArrayList<>();
				sammler.put(listIdx, teil);
			}
			teil.add(elem);				// verteile auf Unter-Listen
			if (listIdx > maxIdx) {
				maxIdx = listIdx;		// führe mit...
			}
		}
		
		// 2) baue fixes Output-native-Array:
		@SuppressWarnings("unchecked")
		List<T>[] array = (List<T>[]) Array.newInstance(List.class, maxIdx+1);		// muss ich hier so machen...
		
		// 2.1) füge ein was da ist, hihi: der glieferte "idx-Stream" kann auch Löcher haben
		for (int idx : sammler.keySet()) {
			array[idx] = sammler.get(idx);
		}
		
		return array;
		
	}


	/**
	 * Liefere ein Array, das eins länger wurde
	 * 
	 * @param array - nice: kann <b>null</b> sein, dann return T[0]=newElem
	 * @param newElem - muss gesetzt sein
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] arrayAdd(T[] array, T newElem) {
		if (newElem==null) {
			throw new NullPointerException("newElem==null");	
		}
		
		// pre-check: Liste ist leer
		if (array==null) {
			T[] newArray = (T[]) Array.newInstance(newElem.getClass(), 1);
			newArray[0] = newElem;
			return newArray;
		}
		
		int n = array.length;
		T[] newArray = (T[]) Array.newInstance(newElem.getClass(), n+1);
		System.arraycopy(array, 0, newArray, 0, n);
		newArray[n] = newElem;
		return newArray;
	}


	/**
	 * - Splitte einen String nach Trennzeichen
	 * - füttere jeden den ge-TRIM-ten Teil an den callback
	 * - Token 'eat' sind ge-String.trim()-t
	 * 
	 * @param sep - ready for reg-ex, also zb "\\t"
	 */
	public static void splitStream(String input, String sep, Consumer<String> eat) {
		if (input==null || input.length()==0 || eat==null) {
			return;
		}
		if (sep==null) sep = ",";			// just in case
		
		String toks[] = input.split(sep);
		for (String s : toks) {
			s = s.trim();
			eat.accept(s);
		}
	}


	/**
	 * split'e, dann return ge-TRIM-t
	 * 
	 * @param sep - ready for reg-ex, also zb "\\t"
	 */
	public static List<String> splitAndStream(String input, String sep) {
		if (input==null || input.length()==0 || sep==null) {
			return new ArrayList<>();
		}
		
		String toks[] = input.split(sep);
		List<String> res = new ArrayList<>(toks.length);
		for (String s : toks) {
			res.add(s.trim());
		}
		return res;
	}


	/**
	 * Zerlege eine Liste in n sub-Listen, teile nach 'split' Einträgen <br>
	 * - die letzte Sub-Liste kann kürzer sein
	 * - wenn gesamt-LEN < split: es gibt nur EINE Liste
	 * 
	 * @return null wenn input null ist
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T[]> splitIntoSubLists(T[] input, int split, Class<T> tClz) {
		if (input==null) {
			return null;
		}
		final int len = input.length;
		if (split<=1) {
			split = len;
		}
		int x = len / split + 1;
		List<T[]> result = new ArrayList<>(x);
		int start = 0;
		boolean run=true;
		while (run) {						// Bsp: len=25, split=10
			int max = start + split;		// [start,max] = [0, 10]
			// int copyTo;
			if (max>=len) {					// wenn Ende erreicht oder drüber
				max = len;
				run = false;
			}
			T[] sub = (T[]) Array.newInstance(tClz, max-start);
			System.arraycopy(input, start, sub, 0, sub.length);
			result.add(sub);
			start += split;
		}
		return result;
	}

	/**
	 * Zerlege eine Liste in n sub-Listen, teile nach 'split' Einträgen <br>
	 * - die letzte Sub-Liste kann kürzer sein
	 * - wenn gesamt-LEN < split: es gibt nur EINE Liste
	 * 
	 * @return null wenn input null ist
	 */
	public static <T> List<List<T>> splitIntoSubLists(Collection<T> input, int split) {
		if (input==null) {
			return null;
		}
		final int len = input.size();
		if (split<=1) {
			split = len;
		}
		int x = len / split + 1;
		List<List<T>> result = new ArrayList<>(x);
		int start = 0;
		boolean run=true;
		Iterator<T> it = input.iterator();
		while (run) {						// Bsp: len=25, split=10
			int max = start + split;		// [start,max] = [0, 10]
			// int copyTo;
			if (max>=len) {					// wenn Ende erreicht oder drüber
				max = len;
				run = false;
			}
			int subLen = max-start;
			List<T> sub = new ArrayList<T>(subLen);
			for (int j=0; j<subLen; j++) {
				sub.add(it.next());
			}
			result.add(sub);
			start += split;
		}
		return result;
	}
	
	/**
	 * Strings to Enum-Obj Convertert: Baue Enum für jeden Eintrag in input
	 * 
	 * @param <T> - Enum-Type
	 * @param <E> - throws this exception type
	 * @param inputList - "VWD,CME,MRCI" - Trenner ist <b>Komma</b>
	 * @param exceptionBuilder - bei Enum-Fehler: Knalle diese Exception raus:
	 *  
	 * @return neue Enum-Liste, Länge so wie Einträge in 'input', <b>null</b> wenn input==null
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Enum<T>, E extends Exception> T[] listToEnum(Class<T> enumClz, String inputList, Function<String, E> exceptionBuilder) throws E {
		if (inputList==null) {
			return null;
		}
		// über alle Inputs:
		String[] toks = inputList.split(",");
		T[] result = (T[]) Array.newInstance(enumClz, toks.length);
		for (int i=0; i<toks.length; i++) {
			String t = toks[i];
			try {
				Enum<T> en = Enum.valueOf(enumClz, t);
				result[i] = (T) en;
			} catch (Exception e) {
				E ex = exceptionBuilder.apply(t);
				throw ex;
			}
		}
		return result;
	}

	
	public interface BiConsumerWithIndex<T, U> {
	    void accept(int idx, T t, U u);
	}


	/**
	 * Gehe parallel über beiden Listen <br>
	 * ACHTUNG: wenn follow.len < driver.len : dann kommen null-T2-items !
	 * 
	 * @param driverArray - diese Liste gibt den idx=0..n vor
	 * 
	 * @param code - params(3): ( idx, item(driver), item(follow) )
	 */
	public static <T1, T2> void FOREACH2(T1[] driverArray, List<T2> followList, BiConsumerWithIndex<T1, T2> code) {
		int n2 = followList!=null ? followList.size() : 0;
		for (int i=0; i<driverArray.length; i++) {
			T2 t2 = (i<n2) ? followList.get(i) : null;
			code.accept(i, driverArray[i], t2);
		}
	}


	/**
	 * über alle Listen-Elemente, mit (idx, el)
	 */
	public static <T> void FOREACH_IDX(List<T> list, BiConsumer<Integer, T> perIdxAndEl) {
		if (list==null || list.size()==0) {
			return;
		}
		int n = list.size();
		for (int i=0; i<n; i++) {
			perIdxAndEl.accept(i, list.get(i));
		}
	}


	/**
	 * Wandle eine Zahlen-Array in einen String <br>
	 * 
	 * - MISSING-VALUE Handling: wenn ein Wert NaN oder +/-Infinity ist: <br>
	 *   a) lasse Wert weg (missingVal=null) <br>
	 *   b) setze zB "-"
	 *
	 * @param digits - wenn (>=0) : Runde auf soviele Stellen, bei (0) schreibe OHNE Komma; (-1): nix runden
	 * 
	 * @return "" wenn vals==null oder length==0
	 *
	public static String arrayToString(float[] vals, String separator, String missingVal, int digits) {
		if (vals==null || vals.length==0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		boolean needSep = false;
		for (int i=0; i<vals.length; i++) {
			float v = vals[i];
			if (Float.isFinite(v)) {		// wenn gültig => schreibe raus
				if (needSep) {
					sb.append(separator);
				} else {
					needSep = true;
				}
				String s;
				if (digits==0) {
					s = String.valueOf(Math.round(v));
				} else if (digits>0) {
					s = Contract.print(v, digits);
				} else {
					s = String.valueOf(v);		// volle 3.1415927
				}
				sb.append(s);
			} else {
				if (missingVal!=null) {
					if (needSep) {
						sb.append(separator);
					} else {
						needSep = true;
					}
					sb.append(missingVal);
				} // else: Skip value
			}
		}
		return sb.toString();
	}*/


	/**
	 * Flatten alles in eine String-Liste
	 * 
	 * @param valOrList - frisst alles, auch null-Werte; kann auch MEHRDIMENSIONALE Array[][] !
	 * 
	 * @return String[]-Liste ggf leer, nie null
	 */
	public static String[] flatten(Object... valOrList) {
		if (valOrList==null || valOrList.length==0) {
			return new String[0];
		}
		final List<String> res = new ArrayList<>();

		for (Object o : valOrList) {		// gehe alles durch, mach aus allem toString()
			_flattenVal(o, res);
		}
		return res.toArray(new String[res.size()]);
	}

	
	private static void _flattenVal(Object o, List<String> res) {
		if (o==null) {
			res.add(null);
			return;
		}
		if (o instanceof Iterable) {	// a) List, Collection, Set, Map, ...
			Iterator<?> it = ((Iterable<?>)o).iterator();
			while (it.hasNext()) {
				Object val = it.next();
				if (val!=null) {
					res.add(val.toString());
				} else {
					res.add(null);
				}
			}
			return;
		}
		if (o.getClass().isArray()) {	// b) native array
			Object[] ar = (Object[])o;
			for (Object val : ar) {
				_flattenVal(val, res);
			}
			return;
		}
		res.add(o.toString());		// c) default
	}


	/**
	 * Praktisch: Zerlege "-1.0,1.7,3.7,-2.0,-1.4,0.6,-0.9,-0.7,0.6,-0.5,-0.4,0.5,-1.0,-2.0,-2.9" in float[]
	 * 
	 * @return null wenn irgendwas klemmt => knallt also NIE
	 */
	public static float[] string2Floats(String stringOfFloats, char sep) {
		if (stringOfFloats==null || stringOfFloats.length()==0) {
			return null;
		}
		String[] toks = stringOfFloats.split(String.valueOf(sep));
		int n = toks.length;
		float[] res = new float[n];
		try {
			for (int i=0; i<n; i++) {
				res[i] = Float.parseFloat(toks[i]);
			}
		} catch (NumberFormatException e) {
			return null;
		}
		return res;
	}

	
	/**
	 * wie oft kommt char in string vor ?
	 * 
	 * @return 0 bei Knaller
	 */
	public static int countInString(String line, char thisChar) {
		if (line==null || line.length()==0) {
			return 0;
		}
		int c = 0;
		for (int i=0; i<line.length(); i++) {
			if (line.charAt(i)==thisChar) {
				c++;
			}
		}
		return c;
	}


	/**
	 * Zerlege eine String von ELementen, zB "ZC,ZW,ZR,ZO,ZS,ZM,ZL,DC,KE", per Sepeartor, zB ","
	 * und jage jedes Ding durch die Consumer-Func
	 */
	public static void foreachString(String elementString, String separator, Consumer<String> applyFunc) {
		if (elementString==null || elementString.length()==0) {
			return;
		}
		String toks[] = elementString.split(separator);
		for (String t : toks) {
			applyFunc.accept(t);
		}
	}

}























































