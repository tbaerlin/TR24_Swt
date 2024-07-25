package tr24.utils.swt.api;

/**
 * wenn ein Prozess/Task/Routine WEISS, dass es länger gehen wird (einige Sekunden bis Minuten)
 * - kann der Prozess einen Progress-Balken anzeigen
 * - wir machen das zentral: 
 * - der Prozess holt sich den Monitor vom Core
 * - der liefert einen oder auch nicht (der Prozess muss auf null prüfen!)
 * - der Prozess kann den Monitor mit von..bis und bin-jetzt-hier füttern
 * - der Core kümmert sich um die komplette Anzeige
 * 
 * @author tbaer
 */
public interface IProgressMonitor {

	/**
	 * gehe einen Step hoch
	 */
	public void step();
	
	/**
	 * wenn's nicht 1-um-1 hochgeht
	 */
	public void step(int nowWeAreHere);

	/**
	 * ich habe fertig
	 */
	public void done();
	
}
