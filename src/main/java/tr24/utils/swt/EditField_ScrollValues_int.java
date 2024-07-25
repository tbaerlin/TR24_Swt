package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;


/**
 * Nice-Helfer für GUI-Dinge:
 * - editierbares Feld, das GANZ-Zahlen in einem BEREICH anzeigen kann (mit Step-Größe)
 * 
 * - Bsp: User kann Risk-Euros eintippen oder mouse-wheel-en
 */
public class EditField_ScrollValues_int {

	private final Color normal;
	private Text txt;

	private int curValidValue;
	protected boolean error;
	
	/**
	 * User-Code will mitkriegen wenn sich was geändert hat
	 */
	public interface IValueChange_int {
		/**
		 * kommt NUR wenn auch ein gültiger Wert eintragen wurde
		 * - und nach jedem Scroll, logo!
		 */
		public void onNewValidValue(int value);
	}
	
	
	/**
	 * constructor
	 * 
	 * @param callback - null-able
	 */
	public EditField_ScrollValues_int(Composite parent, int x, int y, int w, int h, final IValueChange_int callback, final int minVal, final int maxVal, final int step, int initValue) {
		// this.callback = callback;
		txt = new Text(parent, SWT.BORDER);
		txt.setBounds(x, y, w, h);
		normal = FARBE.WHITE;
		txt.setBackground(normal);
		
		this.curValidValue = initValue;
		txt.setText(String.valueOf(curValidValue));

		// Verabeite Mouse-Wheel
		txt.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				boolean isUp = e.count < 0;		// hoch oder runter reicht mir hier
				int newV;
				if (!isUp) {
					newV = Math.min(curValidValue+step, maxVal);
				} else {
					newV = Math.max(curValidValue-step, minVal);
				}
				if (newV != curValidValue) {
					curValidValue = newV;
					if (error) {
						txt.setBackground(normal);
						error = false;
					}
					txt.setText(String.valueOf(curValidValue));
					if (callback!=null) {
						callback.onNewValidValue(newV);
					}
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
				int testV;
				try {
					testV = Integer.parseInt(txt.getText());
				} catch (Exception ex) {
					error = true;
					txt.setBackground(FARBE.RED_6);
					return;
				}
				if (testV != curValidValue) {
					curValidValue = testV;
					if (error) {
						txt.setBackground(normal);
						error = false;
					}
					if (callback!=null) {
						callback.onNewValidValue(curValidValue);
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
	
}















