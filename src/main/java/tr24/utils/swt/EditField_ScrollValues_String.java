package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;


/**
 * Nice-Helfer für GUI-Dinge:
 * - editierbares Feld, das eine FIXE MENGE an String-Werte zulässt
 * - Bsp: User kann Contracts schnell auswählen oder von Hand eintippen
 */
public class EditField_ScrollValues_String {

	private final Color normal;
	private final String[] validValues;
	// private final IValueChange callback;		// null-able
	private Text txt;

	private int curIdx;
	private String curTxt;
	protected boolean error;
	
	/**
	 * User-Code will mitkriegen wenn sich was geändert hat
	 */
	public interface IValueChange_String {
		/**
		 * kommt NUR wenn auch ein gültiger Wert eintragen wurde
		 * - und nach jedem Scroll, logo!
		 */
		public void onNewValidValue(String value);
	}
	
	
	/**
	 * constructor
	 * 
	 * @param callback - null-able
	 */
	public EditField_ScrollValues_String(Composite parent, int x, int y, int w, int h, String[] pValidValues, String initValue, final IValueChange_String callback) {
		this.validValues = pValidValues;
		// this.callback = callback;
		txt = new Text(parent, SWT.BORDER);
		txt.setBounds(x, y, w, h);
		txt.setText(initValue);
		normal = FARBE.WHITE;
		txt.setBackground(normal);
		
		// ermittle den aktuellen wo-stehe-ich ?
		this.curIdx = find(initValue);
		if (curIdx<0) {
			curIdx = 0;
		}
		this.curTxt = initValue;
		
		// Verabeite Mouse-Wheel
		txt.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				boolean isUp = e.count < 0;		// hoch oder runter reicht mir hier
				if (isUp) {
					curIdx--;
					if (curIdx<0) {
						curIdx = validValues.length-1;
					}
				} else {
					curIdx++;
					if (curIdx>=validValues.length) {
						curIdx = 0;
					}
				}
				String newV = validValues[curIdx];
				if (error) {
					txt.setBackground(normal);
					error = false;
				}
				txt.setText(newV);
				if (callback!=null) {
					callback.onNewValidValue(newV);
				}
			}
		});
		
		// Verabeite auch direkte User-Eingaben:
		txt.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}
			// höre auf key-hoch
			@Override
			public void keyReleased(KeyEvent e) {
				String now = txt.getText();
				if (!now.equals(curTxt)) {		// NUR wenn sich was geändert hat = blende Cursor-Dinge aus
					curTxt = now;
					int newIdx = find(now);		// hat User was gültiges eingegeben ?
					if (newIdx>=0) {
						curIdx = newIdx;
						if (error) {
							txt.setBackground(normal);
							error = false;
						}
						if (callback!=null) {
							callback.onNewValidValue(now);
						}
					} else {
						// wenn nicht: zeige Text in rot
						txt.setBackground(FARBE.RED_6);
						error = true;
					}
				}
			}
		});
		
		// bei Focus: markiere gleich alles => User kann direkt los-tippen
		txt.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
			}
			@Override
			public void focusGained(FocusEvent e) {
				txt.setSelection(0, 1000);
			}
		});
	}

	
	private int find(String v) {
		for (int i=0; i<validValues.length; i++) {
			if (validValues[i].equals(v)) {
				return i;
			}
		}
		return -1;
	}
	
	
	
}















