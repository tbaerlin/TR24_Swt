package tr24.utils.swt;


/**
 * Sicht des User-Code auf die Info-Table
 */
public interface IProgressInfoTable {

	
	/**
	 * User-Code holt sich so eine Zeile
	 */
	public interface IInfoRow {

		/**
		 * Zeige das Alter des Vorgangs zusätzlich in der Status-Spalte
		 * 
		 * @eturn (chainable)
		 */
		public IInfoRow showClock(boolean onOff);
		
		/**
		 * Update Anzeige in Spalte "status"
		 */
		public void update(String status);
		
		/**
		 * Vorgang fertig, lösche Zeile
		 */
		public void remove();
		
	}
	
	
	/**
	 * Erzeuge eine neue Zeile
	 */
	public IInfoRow show(String context, String status);
	
	
}
