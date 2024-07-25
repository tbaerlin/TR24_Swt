package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import java.math.BigDecimal;


/**
 * Nice-Helfer für GUI-Dinge:
 * - editierbares Feld, das FLOAT-Zahlen in einem BEREICH anzeigen kann (mit Step-Größe)
 * 
 * - Bsp: User kann Risk-Euros eintippen oder mouse-wheel-en
 */
public class EditField_ScrollValues_float {

	private final Color normal;
	private Text txt;

	private double curValidValue;
	protected boolean error;
	
	/**
	 * User-Code will mitkriegen wenn sich was geändert hat
	 */
	public interface IValueChange_float {
		/**
		 * kommt NUR wenn auch ein gültiger Wert eintragen wurde
		 * - und nach jedem Scroll, logo!
		 */
		public void onNewValidValue(float value);
	}
	
	
	/**
	 * constructor
	 * 
	 * @param callback - null-able
	 */
	public EditField_ScrollValues_float(Composite parent, int x, int y, int w, int h, final IValueChange_float callback, 
										final float minVal, final float maxVal, float pStep, float initValue) 
	{
		// this.callback = callback;
		txt = new Text(parent, SWT.BORDER);
		txt.setBounds(x, y, w, h);
		normal = FARBE.WHITE;
		txt.setBackground(normal);
		
		this.curValidValue = initValue;
		txt.setText(anzeige());
		final double step = pStep;		// intern arbeite ich mit double!

		// Verabeite Mouse-Wheel
		txt.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				boolean isUp = e.count < 0;		// hoch oder runter reicht mir hier
				double newV;
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
					txt.setText(anzeige());
					if (callback!=null) {
						callback.onNewValidValue((float)newV);
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
				float testV;
				try {
					testV = Float.parseFloat(txt.getText());
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
						callback.onNewValidValue((float)curValidValue);
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


	protected String anzeige() {
		BigDecimal z = new BigDecimal(curValidValue);
		z = z.setScale(4, BigDecimal.ROUND_HALF_UP);
		float anzeige = z.floatValue();
		return String.valueOf(anzeige);
	}
	
}















