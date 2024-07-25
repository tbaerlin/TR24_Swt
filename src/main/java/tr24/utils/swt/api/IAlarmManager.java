package tr24.utils.swt.api;

import org.eclipse.swt.graphics.Color;

import java.io.File;

/**
 * Zugriff auf den Alarm-Manager, um zB killAllAlarms() zu rufen
 */
public interface IAlarmManager {

	public void turnSound(boolean OnOff);

	public void killAllAlarms();

	public void addAlarm(final String headline, final String message, final Color colOrNull);

	public void setSoundFile(File soundFile);

	public boolean isSoundOn();
	
}
