package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

/**
  * Horizontaler Sash für ZWEI Dinge (oben und unten)
  * - der Balken in der Mitte zeichnet "^" und "^andersrum"
  *
  */
public class HorizontalSash {

     private static final int breite = 8;
     private final Control topDing;
     private final Composite parent;
     
	 private Sash sash;
	 
	 /**
	  * Wenn User-Code passiv informiert werden möchte
	  * wenn sich was geändert hat
	  */
	 public interface IHorizontalSashChangeListener {
		 /**
		  * Call in SWT
		  * 
		  * @param curAufteilung - 0..100[%]
		  */
		 public void onChange(int curAufteilung);
	 }
	 
     /**
      * constructor - mit Listener
      */
     public HorizontalSash(final Composite parent, Control topDing, Control bottomDing, int initialAufteilung, final int minTop, final int minBottom)
     {
    	 this(parent, topDing, bottomDing, initialAufteilung, minTop, minBottom, null);
     }
     
     /**
      * constructor
      * - setzt ein {@link FormLayout} in den parent
      * @param initialAufteilung 0..100
      *
      * @param minTop    - minimale Höhe in px für obere Seite
      * @param minBottom - minimale Höhe in px für untere Seite
      */
     public HorizontalSash(final Composite parent, Control topDing, Control bottomDing,
             int initialAufteilung, final int minTop, final int minBottom,
             final IHorizontalSashChangeListener changeListener)
     {
         this.parent = parent;
         this.topDing = topDing;
         parent.setLayout(new FormLayout());

         sash = new Sash(parent, SWT.HORIZONTAL);

         Cursor cur = new Cursor(parent.getDisplay(), SWT.CURSOR_SIZENS);
         sash.setCursor(cur);

         FormData d = new FormData ();
         d.top = new FormAttachment (0, 0);
         d.bottom = new FormAttachment (sash, 0);
         d.left = new FormAttachment (0, 0);
         d.right = new FormAttachment (100, 0);
         topDing.setLayoutData(d);

         final FormData sashData = new FormData ();
         sashData.top    = new FormAttachment(initialAufteilung, -breite/2+1);
         sashData.bottom = new FormAttachment(initialAufteilung, breite/2);
         sashData.left   = new FormAttachment (0, 0);
         sashData.right  = new FormAttachment (100, 0);
         sash.setLayoutData(sashData);
         sash.setBackground(FARBE.GRAY_3);

         d=new FormData ();
         d.top    = new FormAttachment(sash, 0);
         d.bottom = new FormAttachment(100, 0);
         d.left = new FormAttachment(0, 0);
         d.right = new FormAttachment(100, 0);
         bottomDing.setLayoutData(d);

         // das Sash-Moven machen wir hier so:
         sash.addListener (SWT.Selection, new Listener () {
             public void handleEvent (Event e) {
                 Rectangle sashRect = sash.getBounds();
                 Rectangle shellRect = parent.getClientArea();
                 int yMax = shellRect.height - sashRect.height - minBottom;
                 e.y = Math.max(Math.min(e.y, yMax), minTop);
                 if (e.y != sashRect.y) {
                	 sashData.top 	 = new FormAttachment(0, e.y);
                	 sashData.bottom = new FormAttachment(0, e.y+breite);
                	 parent.layout();
                 }
                 if (changeListener!=null) {
                	changeListener.onChange(getCurrentMiddle()); 
                 }
             }
         });

         // zeichne die hoch/runter-Pfeile
         /*sash.addPaintListener(new PaintListener() {
             @Override
             public void paintControl(PaintEvent e) {
                 GC gc = e.gc;
                 int x = e.width/2;
                 int y = e.height/2;
                 // gc.setAntialias(SWT.ON);
                 gc.setForeground(FARBE.BLACK);
                 gc.setLineWidth(2);
                 gc.drawLine(x-10, y+3, x-6, y-3);
                 gc.drawLine(x-6, y-3, x-2, y+3);
                 
                 gc.drawLine(x+2, y-3, x+6, y+3);
                 gc.drawLine(x+6, y+3, x+10, y-3);
             }
         });*/
     }

     /**
      * wo ist die Mitte gerade, call muss in SWT kommen
      * 
      * @return - wo ist das Sash in % 0..100 (von TOP gemessen)
      */
     public int getCurrentMiddle() {
         int top = (topDing.getSize().y+10) * 100;
         int alles = parent.getClientArea().height;
         int perc = top / alles;
         if (perc<=0 || perc>=100) {
             return 50;                    // sicher ist sicher
         }
         return perc;
     }
     
     public void setLayoutData(FormData fd) {
    	 sash.setLayoutData(fd);
     }
     
}


