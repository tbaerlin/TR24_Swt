package tr24.utils.swt.customcanvas.api;

/**
 * Zugriff auf ein CustomChart
 */
public interface ICustomChart {
	
	/**
	 * Der User code muss wissen was/wie er zeichnen will
	 */
	void register(ICustomChartClient client);
	
	/**
	 * jetzt geht's los
	 */
	void show();
	
	/**
	 * show mit: setzte diesen Anzeige-Bereich
	 * @param xMin
	 * @param xMax
	 * @param yMin
	 * @param yMax
	 */
	public void show(float xMin, float xMax, float yMin, float yMax);

}
