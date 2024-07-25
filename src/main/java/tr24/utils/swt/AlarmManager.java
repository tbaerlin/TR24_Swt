package tr24.utils.swt;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import tr24.utils.common.ThreadUtil;
import tr24.utils.swt.api.IAlarmManager;
import tr24.utils.swt.api.IShutdownHook;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Singleton-Helfer:
 *  
 * - kann die {@link AlarmDialog}e verwalten
 * 
 * - es kann eine MAXIMALE Anzahl gleichzeitig offenere Alarms geben
 * - re-PING alle 60s zentral wenn noch was offen ist

 * 
 * @author tbaer
 *
 */
public class AlarmManager implements IShutdownHook, IAlarmManager {

	private final BasisCore core;
	private final Image imageOrNull;
	private final int maxConcurrentOpen;

	/**
	 * wann darf frühestens der nächste Ping kommen [millies]
	 */
	private long nextPingAt = 0;
	
	/**
	 * n..0 Runter-Zähler
	 */
	private AtomicInteger pingsToPlay = new AtomicInteger(0);
	
	/**
	 * FIFO der wartenden...
	 */
	private Queue<AlarmWait> waitingList = new LinkedList<AlarmWait>();
	
	private static class AlarmWait {
		public final String headline;
		public final String message;
		public final Color colOrNull;
		public AlarmWait(String headline, String message, Color colOrNull) {
			this.headline = headline;
			this.message = message;
			this.colOrNull = colOrNull;
		}
	}
	
	private int openDialogCounter = 0;
	private PingDelayer delayer;
	
	private boolean active = true;
	private File soundFile;
	
	/**
	 * flag: muss true werden solange ein killAll läuft
	 */
	private boolean killingActive = false;
	
	/**
	 * Sound?
	 */
	private boolean soundOn = true;
	
	
	/**
	 * constructor, sollte nur vom Basis-Core verwendet werden
	 */
	AlarmManager(BasisCore core, Image imageOrNull, int maxConcurrentOpen, File soundFile_KannAuchSpaeterGesetztWerden) {
		this.core = core;
		this.imageOrNull = imageOrNull;
		this.maxConcurrentOpen = maxConcurrentOpen;
		this.delayer = new PingDelayer();
		core.add2Shutdown(this);
		if (soundFile_KannAuchSpaeterGesetztWerden!=null) {
			if (!soundFile_KannAuchSpaeterGesetztWerden.exists()) {
				soundFile_KannAuchSpaeterGesetztWerden = null;
			}
		}
		this.soundFile = soundFile_KannAuchSpaeterGesetztWerden;
		
		// höre ZENTRAL auf "AlarmDialog is wieder zuuu"
		AlarmDialog.register(new AlarmDialog.IAlarmDialogCloseListener() {
			@Override
			public void onClosed(AlarmDialog ad) {
				onDialogClosed();
			}
		});
	}
	
	/**
	 * Spiele DIESEN Sound
	 */
	@Override
	public void setSoundFile(File soundFile) {
		this.soundFile = null;
		if (soundFile!=null && soundFile.exists()) {
			this.soundFile = soundFile;
		}
	}

	@Override
	public boolean isSoundOn() {
		return soundOn;
	}
	
	@Override
	public void shutdown() {
		active = false;
	}
	
	/**
	 * spiele den Sound HIER ab
	 */
    private void playSound() {
    	if (soundFile==null) {
    		return;				// nur wenn's geht
    	}
    	AudioInputStream audioInputStream = null;
        try { 
            audioInputStream = AudioSystem.getAudioInputStream(soundFile);
        } catch (UnsupportedAudioFileException e1) {
            e1.printStackTrace();
            return;
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        } 
 
        AudioFormat format = audioInputStream.getFormat();
        SourceDataLine auline = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
 
        try { 
            auline = (SourceDataLine) AudioSystem.getLine(info);
            auline.open(format);
        } catch (LineUnavailableException e) { 
            e.printStackTrace();
            return;
        } catch (Exception e) { 
            e.printStackTrace();
            return;
        } 
        auline.start();
        int nBytesRead = 0;
        int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb 
        byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
 
        try { 
            while (nBytesRead != -1) { 
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
                if (nBytesRead >= 0) 
                    auline.write(abData, 0, nBytesRead);
            } 
        } catch (IOException e) { 
            e.printStackTrace();
            return;
        } finally { 
            auline.drain();
            auline.close();
        } 
	}

	
	/**
	 * Loop für:
	 * a) Warte-Liste der "spiele-PING"s
	 * b) RE-Ping nach 60sec
	 * 
	 * Idee: 
	 * - wenn sehr schnell viele Alarms reinkommen will ich NICHT ein Dauerfeuer-ge-pinge haben
	 * - sondern die Pings sollen 2sec-zeitversetzt kommen
	 * - ich merke mir also WIEVIELE Pings ich rausblasen muss und triggere das dann HIER im Thread
	 */
	private class PingDelayer extends Thread {
		
		/**
		 * wann wurde zuletzt ge-ping-t? [millies]
		 * - daran kann ich erkennen, dass 60sec rum sind
		 * - und ich RE-Pingen muss
		 */
		private long lastPingPlayed;
		
		/**
		 * constructor
		 */
		public PingDelayer() {
			setName("AlarmManager.PingDelayer");
			setDaemon(true);
			start();
		}
		@Override
		public void run() {
			lastPingPlayed = System.currentTimeMillis();		// muss auf jetzt setzen, sonst kommt gleich ein RE-Ping
			while (active) {
				ThreadUtil.sleepUnhandled(200);
				long now = System.currentTimeMillis();
				
				// a) Arbeite die austehenden Pings ab
				if (pingsToPlay.get() > 0) {
					if (now >= nextPingAt) {		// wenn ich "darf":
						// trickle: ich spiele den Sound in DIESEM Thread
						// damit WARTET alles bis das Ding rum ist und die Pause ist immer min 2sec
						if (soundOn) {
							playSound();		// kann nicht knallen
						}
						lastPingPlayed = System.currentTimeMillis();
						nextPingAt = lastPingPlayed + 2000;			// nächster erst in 2sec
						int x = pingsToPlay.decrementAndGet();	// einer weniger to go
						System.err.println("$$ sounds to go " + x);
					}
				} else {	// wenn es gerade NICHT zu pingen gibt:

					// b) RE-Ping nach 1min:
					if (openDialogCounter>0) {		// nur WENN noch was offen ist
						long waited = now - lastPingPlayed;
						if (waited > 60*1000) {
							if (soundOn) {
								playSound();		// kann nicht knallen
							}
							lastPingPlayed = System.currentTimeMillis();
						}
					}
				}
			}
		}
	}	// PingDelayer

	
	/**
	 * Zeige einen neuen Alarm
	 * - oder packe ihn auf die Warte-Liste
	 */
	@Override
	public synchronized void addAlarm(final String headline, final String message, final Color colOrNull) {
		// synchronized wegen 'openDialogCounter' -> siehe onDialogClose()
		
		if (openDialogCounter >= maxConcurrentOpen) {
			waitingList.add(new AlarmWait(headline, message, colOrNull));
			// System.out.println(headline + ":" + message + " must WAIT, size = " + waitingList.size());
			return;			// kann sonst nichts machen
		}
		
		// Zeige Dialog
		if (!core.isSwtThread()) {
			core.asyncExec(new Runnable() {
				public void run() {
					new AlarmDialog(headline, message, core.getDisplay(), imageOrNull, colOrNull);
				}
			});
		} else {
			new AlarmDialog(headline, message, core.getDisplay(), imageOrNull, colOrNull);
		}
		int x = pingsToPlay.incrementAndGet();		// eins hoch
		// Race-condition-check beim killAll(): 
		if (x==0) {		// pingToPlay kann -1 sein, dann ist's jetzt "nur" 0
			pingsToPlay.set(1);
		}
		System.out.println("&& addAlarm: pings to play="+pingsToPlay.get());
		openDialogCounter++;
		
		// System.out.println("Pings to go " + pingsToPlay.get());
	}
	
	/**
	 * kommt immer wenn ein Dialog zu ging
	 * 
	 * - praktisch: call kommt im SWT
	 */
	protected synchronized void onDialogClosed() {
		if (killingActive) {
			return;
		}
		openDialogCounter--;		// deswegen synchronized
		
		// gibt es wartende, die jetzt dran sind
		if (openDialogCounter < maxConcurrentOpen) {
			AlarmWait wait = waitingList.poll(); 		// hole ersten, oder null
			if (wait!=null) {
				// System.err.println("show waiting. " + waitingList.size() + " still waiting...");
				addAlarm(wait.headline, wait.message, wait.colOrNull);
			}
		}
		pingsToPlay.decrementAndGet();		// einer weniger zu spielen
	}

	/**
	 * Lösche ALLE !
	 * a) gerade offenen
	 * b) noch zu zeigenden AlarmDialoge
	 */
	@Override
	public synchronized void killAllAlarms() {
		killingActive = true;
		pingsToPlay.set(0);
		AlarmDialog.closeAllDialogs();
		waitingList.clear();
		openDialogCounter = 0;
		killingActive = false;
	}
	
	/**
	 * Schaltet die Pings global an/aus
	 */
	@Override
	public void turnSound(boolean OnOff) {
		soundOn = OnOff;
	}
	
	
	
	
	private static boolean blaseAlarmeRaus;
	
	/**
	 * test
	 */
	public static void main(String[] args) {
		Display display = new Display();
		final BasisCore core = new BasisCore(display);
		Shell shell = new Shell(display);
		shell.setBounds(1, 1, 120, 120);
		blaseAlarmeRaus = true;
		
		final Random ran = new Random();
		
		File sound = new File("T:/PROJECTS/R3-LivElems/ding.wav");
		core.presentAlarmManager(FARBE.RED_3, sound, 5);
		
		final IAlarmManager am = core.getAlarmManager();
		
		final Button onOff = new Button(shell, SWT.CHECK);
		onOff.setText("alarm!");
		onOff.setBounds(1, 1, 50, 20);
		onOff.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				blaseAlarmeRaus = onOff.getSelection();		// schalte Alarm-Erzeugen an/aus
				// System.err.println("Alarm erzeugen ist " + blaseAlarmeRaus);
			}
		});
		onOff.setSelection(blaseAlarmeRaus);

		final Button killAll = new Button(shell, SWT.PUSH);
		killAll.setText("Kill All");
		killAll.setBounds(1, 30, 50, 20);
		killAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// NICHT SO AlarmDialog.closeAllDialogs();
				am.killAllAlarms();
			}
		});
		
		final Button soundBtn = new Button(shell, SWT.CHECK);
		soundBtn.setText("sound?");
		soundBtn.setSelection(am.isSoundOn());		// true by default
		soundBtn.setBounds(1, 60, 50, 20);
		soundBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean sOnOff = soundBtn.getSelection();
				am.turnSound(sOnOff);
			}
		});
		
		shell.open();
		
		
		Thread th = new Thread(new Runnable() {
			public void run() {
				while (true) {
					ThreadUtil.sleepUnhandled(ran.nextInt(10)*100);		// warte etwas
					if (!blaseAlarmeRaus) {
						continue;
					}
					core.asyncExec(new Runnable() {
						public void run() {
							String msg = "" + ran.nextDouble();
							am.addAlarm("Alarm", msg, null);
						}
					});
				}
			}
		});
		th.setDaemon(true);
		th.start();
		
		core.runSwtLoop(shell);
	}

	
}





















