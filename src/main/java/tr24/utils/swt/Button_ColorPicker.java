package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * Helfer: ein farbiger Button
 * 
 * - click => color picker Dialog
 * - Btn ändert dann seine Farbe
 * 
 */
public class Button_ColorPicker extends Composite implements PaintListener {
	
	private Color myCol;
	private final Shell shell;

	/**
	 * welche Farbe ist's gerade?
	 */
	public Color getColor() {
		return myCol;
	}
	/**
	 * Setzt die Farbe
	 */
	public void setColor(Color c) {
		myCol = c;
		redraw();
	}
	
	/**
	 * constructor
	 * 
	 * @param shell - der Color-Dialog braucht ne Shell
	 * 
	 */
	public Button_ColorPicker(Shell shell, Composite parent, Color startCol, int x, int y, int w, int h) {
		super(parent, SWT.DOUBLE_BUFFERED);
		this.shell = shell;
		setBounds(x,y,w,h);
		this.myCol = startCol;
		addPaintListener(this);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if( e.button == 1/*links*/) {
					showDialog();
				}
			}
		});
	}
	protected void showDialog() {
		// Create the color-change dialog
		ColorDialog dlg = new ColorDialog(shell);
		// Set the selected color in the dialog from
		// user's selected color
		dlg.setRGB(myCol.getRGB());
		// Change the title bar text
		dlg.setText("Spread line color");
		// Open the dialog and retrieve the selected color
		RGB rgb = dlg.open();
		if (rgb != null) {
			myCol = new Color(getDisplay(), rgb);
			redraw();
		}
	}
	@Override
	public void paintControl(PaintEvent e) {
		GC gc = e.gc;
		gc.setBackground(myCol);
		Point p = getSize();
		gc.setForeground(FARBE.BLACK);
		gc.drawRectangle(0, 0, p.x-2, p.y-2);
		gc.setForeground(FARBE.WHITE);
		gc.drawRectangle(1, 1, p.x-4, p.y-4);
		gc.setBackground(myCol);
		gc.fillRectangle(2, 2, p.x-5, p.y-5);
	}

	
}	// ColorButton
