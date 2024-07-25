package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

/**
 * Vertikales Sash für ZWEI Dinge (links und rechts)
 * - der Balken in der Mitte zeichnet "<" und ">"

 * 
 * @author tbaer
 *
 */
public class TobySash {

	private static final int breite = 8;
	private final Control leftDing;
	private final Composite parent;
	
	private ISashMoveListener listener;
	
	/**
	 * some-Code will mitkriegen wenn User den Sash verschiebt
	 */
	public interface ISashMoveListener {
		/**
		 * Liefert aktiv das gleiche wie getCurrentMiddle()
		 */
		public void onNewMiddle(int middlePerc);
	}

	/**
	 * Höre auf Sash-Verschieber
	 */
	public void setListener(ISashMoveListener listener) {
		this.listener = listener;
	}

	/**
	 * constructor
	 * - setzt ein {@link FormLayout} in den parent
	 * @param initialAufteilung 0..100 
	 * 
	 * @param minLeft  - minimale Breite in px für linke Seite
	 * @param minRight - minimale Breite in px für rechte Seite
	 */
	public TobySash(final Composite parent, Control leftDing, Control rightDing, 
			int initialAufteilung, final int minLeft, final int minRight) 
	{
		this.parent = parent;
		this.leftDing = leftDing;
		parent.setLayout(new FormLayout());
		
		final Sash sash = new Sash(parent, SWT.VERTICAL);
		
		Cursor cur = new Cursor(parent.getDisplay(), SWT.CURSOR_SIZEW);
		sash.setCursor(cur);
		
		FormData d = new FormData ();
		d.left = new FormAttachment (0, 0);
		d.right = new FormAttachment (sash, 0);
		d.top = new FormAttachment (0, 0);
		d.bottom = new FormAttachment (100, 0);
		leftDing.setLayoutData(d);
		
		final FormData sashData = new FormData ();
		sashData.left = new FormAttachment (initialAufteilung, -breite/2+1);
		sashData.right = new FormAttachment(initialAufteilung, breite/2);
		sashData.top = new FormAttachment (0, 0);
		sashData.bottom = new FormAttachment (100, 0);
		sash.setLayoutData(sashData);
		
		d=new FormData ();
		d.left = new FormAttachment (sash, 0);
		d.right = new FormAttachment (100, 0);
		d.top = new FormAttachment (0, 0);
		d.bottom = new FormAttachment (100, 0);
		rightDing.setLayoutData(d);
	
		// das Sash-Moven machen wir hier so:
		sash.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				Rectangle sashRect = sash.getBounds ();
				Rectangle shellRect = parent.getClientArea ();
				int right = shellRect.width - sashRect.width - minRight;
				e.x = Math.max (Math.min (e.x, right), minLeft);
				if (e.x != sashRect.x)  {
					sashData.left = new FormAttachment (0, e.x);
					sashData.right = new FormAttachment(0, e.x+10);
					parent.layout();
				}
				if (listener!=null) {
					listener.onNewMiddle(getCurrentMiddle());
				}
			}
		});
		
		// zeichne die links/rechts-Pfeile
		sash.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				GC gc = e.gc;
				int y = e.height/2;
				gc.drawString("<", 1, y-8);
				gc.drawString(">", 1, y+8);
			}
		});
	}
	
	/**
	 * wo ist die Mitte gerade, call muss in SWT kommen
	 * @return - wo ist das Sash in % 0..100
	 */
	public int getCurrentMiddle() {
		int left = (leftDing.getSize().x+10) * 100;
		int alles = parent.getClientArea().width;
		int perc = left / alles;
		if (perc<=0 || perc>=100) {
			return 50;					// sicher ist sicher
		}
		return perc;
	}
}


