package tr24.utils.swt.api;


/**
 * Ähnlich zum {@link IShutdownHook}:
 *
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
     */
	public void shellShutdown();
	
}













