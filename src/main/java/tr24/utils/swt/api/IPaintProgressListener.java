package tr24.utils.swt.api;

/**
 * Neue/andere Version des Progress-Monitors,
 * Beispiel: 
 *  
 *  - User trigger das "Verlinke AC xyz mit Chart-#3" in der GUI
 *  - das System pustet alle seither aufgelaufenen paint-cmds an JN7
 *  - und das DAUERT ein paar Sekunden!
 *  - die GUI will einen Progress-Monitor anzeigen
 *  
 *  => Diese Interface entkoppelt die GUI von der Stelle an der die 
 *     "ja, bin 2 Schritte weiter"-Info entsteht
 *  
 * @author tbaer
 *
 */
public interface IPaintProgressListener {

	/**
	 * Muss am Anfang kommen:
	 * - die ausführende Schleife weiss: ich werte 250 Steps machen
	 */
	public void init(int totalSteps);
	
	/**
	 * Bin jetzt 'inc' Schritte weiter, kann 1 sein oder ganze Blöcke
	 */
	public void next(int inc);
	
	/**
	 * Ich habe fertig
	 */
	public void done();
	
}
