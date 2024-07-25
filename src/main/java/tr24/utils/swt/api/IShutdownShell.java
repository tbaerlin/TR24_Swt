package tr24.utils.swt.api;


import tr24.utils.swt.ApplicationConfig;

/**
 * Ähnlich zum {@link IShutdownHook}:
 * - kommt wenn der Shutdown-Process läuft
 * - alles Shell/Fenster können sich beim Core registrieren
 * - und bekommen HIER einen call
 * - Die Shell wird idR ihre Position im Config-File speichern
 * 
 * @author tbaer
 *
 */
public interface IShutdownShell {

	/**
	 * Call für die Shell, kommt in SWT
	 * 
	 * @param conf - gesetzt wenn die Anwendung eine config im Core hinterlegt hat
	 */
	public void shellShutdown(ApplicationConfig conf);
	
}













