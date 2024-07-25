package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Helfer: eine Status-Bar
 * 
 * - kann über StatusMessage-Objects befüllt werden
 * - Konzept: wenn mehrere Sender eine StatusMessage absetzen 
 *   dann wird immer die zuletzt gesetzte/verändertet abgesetzt
 * => es kann also eine lang-laufenden "Load long stuff 1" von einer 
 *   "Load schnell mal was 2" unterbrochen werden. Und sobald die 2 rum ist
 *   wird die 1 wieder angezeigt
 */
public class StatusBar {

	public static final StatusBar DUMMY = new StatusBarDummy();
	
	private Label labelLeft;
	private Label labelRight;
	private final Display display;
	
	/**
	 * zeigt auf die jüngste Message = die lezte in der Liste
	 */
	private StatusMessageImpl pointer;
	
	/**
	 * default constructor wegen Dummy-Version
	 */
	protected StatusBar() {
		display = null;
	} 
	
	/**
	 * constructor
	 */
	public StatusBar(Shell parent, Display display) {
		this(parent, null, display);
	}
	
	public StatusBar(Composite parent, FormData d, Display display) {
		this.display = display;
		
		Composite box = new Composite(parent, SWT.NONE);
		FillLayout fillLayout = new FillLayout(SWT.HORIZONTAL);
		fillLayout.marginHeight = 2;
	    fillLayout.marginWidth = 2;
	    fillLayout.spacing = 10;
		box.setLayout(new FillLayout(SWT.HORIZONTAL));
		
		if (d==null) {
			d = new FormData();
			d.left = new FormAttachment(0);
			d.bottom = new FormAttachment(100);
			d.right = new FormAttachment(100);
		}
		box.setLayoutData(d);
		
		labelLeft = new Label(box, SWT.BORDER);
// 		label.setSize(100, 130);
		labelLeft.setText("..");
		labelLeft.setBackground(FARBE.GRAY_4);
		
		labelRight = new Label(box, SWT.BORDER | SWT.RIGHT);
		labelRight.setBackground(FARBE.GRAY_4);
		labelRight.setText("...");
		
		pointer = null;
	}

	/**
	 * Zeigt auf der rechten Seite
	 */
	public void showPermaText(final String rightSideText) {
		if (display.getThread() == Thread.currentThread ()) {
			labelRight.setText(rightSideText);
			return;
		}
		display.asyncExec(new Runnable() {
			public void run() {
				labelRight.setText(rightSideText);
			}
		});
	}
	
	/**
	 * Zeige DIESEN Text (links), unterscheide hier:
	 * - wenn der CALLING-Thread schon der SWT-Thread ist => zeig's gleich an
	 * - sonst wie üblich SWT-Thread-wrap-en
	 */
	private void show(final String msg) {
		if (display.getThread() == Thread.currentThread ()) {
			labelLeft.setText(msg);
			return;
		}
		display.asyncExec(new Runnable() {
			public void run() {
				if (labelLeft.isDisposed()) {
					return;
				}
				labelLeft.setText(msg);
			}
		});
	}
	
	/**
	 * Setzt den Text und liefert eine Ref, so dass
	 * - update() und  
	 * - kill() geht
	 * 
	 * eine neue Message wird eine 'ältere' zeitweise überschreiben
	 */
	public synchronized StatusMessage setMessage(String thisText) {
		StatusMessageImpl sm = new StatusMessageImpl(thisText);
		
		// füge an Liste an:
		sm.prev = pointer;
		pointer = sm;
		
		return sm;
	}
	
	/**
	 * Aufr�umen nach einem kill():
	 * - wenn die calling-Message NICHT die letzte ist: mache nichts, es wird ja gerade was anderes angezeigt
	 * - eine ältere Message kann ERST wieder angezeigt werden wenn die jüngste activ=false ist
	 */
	private synchronized void cleanMessages(StatusMessageImpl caller) {
		if (pointer==null && caller!=pointer) {		// pointer==null: sicher ist sicher
			return;
		}
		StatusMessageImpl cur = pointer;		// fange mit der letzten an
		String showThis = "";
		while (cur!=null) {
			if (!cur.activ) {
				StatusMessageImpl temp = cur;
				cur = cur.prev;		// nach dem ersten Konten wird cur==null
				temp.prev = null;	// Verlinkung l�sen
			} else {
				showThis = cur.msg;
				break;
			}
		}
		pointer = cur;				// die Liste hört bei der letzen aktiven auf oder ist leer
		show(showThis);				// Zeige die letzte gültige Msg an oder ""
	}
	
	/**
	 * Zeigt eine veränderte Msg an 
	 * - aber NUR wenn's die jüngste = oberste ist
	 */
	private void updateMsg(StatusMessageImpl sm) {
		if (sm!=pointer) {
			return;
		}
		show(sm.msg);
	}

	
	/**
	 * eine Message
	 * - wird initial gesetzt
	 * - bleibt so lange bestehehn bis der kill() kommt
	 * - kann unterwegs beliebig geändert werden
	 * 
	 */
	public interface StatusMessage {
		/**
		 * die neue Message wird angezeigt (sofern's nicht eine 'jängere' Message gibt)
		 */
		public void update(String newMessage);
		
		/**
		 * lösche die Message
		 * - die Status-Bar zeigt dann entweder nichts mehr an, oder die n�chst 'ältere'
		 */
		public void kill();
	}
	
	
	private class StatusMessageImpl implements StatusMessage {

		public String msg;
		
		public boolean activ;
		
		/**
		 * linked-list
		 */
		public StatusMessageImpl prev;
		
		/**
		 * constructor
		 */
		public StatusMessageImpl(String thisText) {
			msg = thisText;
			activ = true;
			show(msg);
		}
		@Override
		public void update(String newMessage) {
			this.msg = newMessage;
			updateMsg(this);
		}
		@Override
		public void kill() {
			if (!activ) { return;  /* sicher ist sicher */ }
			activ = false;
			cleanMessages(this);
		}
		
		@Override
		public String toString() {
			return msg + "/" + activ;
		}
		
	}

	/**
	 * statischer Dummy, gilt solange bis die echte da ist
	 * -> nötig wegen Race-conditions
	 */
	private static class StatusBarDummy extends StatusBar  {
		@Override
		public void showPermaText(String rightSideText) {
		}
		
		@Override
		public synchronized StatusMessage setMessage(String thisText) {
			return new StatusMessage() {
				@Override
				public void update(String newMessage) {
				}
				@Override
				public void kill() {
				}
			};
		}
	}


}











