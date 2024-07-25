package tr24.utils.swt.gentable;


/**
 * Meta-Infos zu einer Spalte:
 * - welcher TYP bist du ?
 */
public enum ColumnType {

	DATETIME,		// komplettes Datum plus Zeit, zb für Entry-Time
	
	TIME,			// NUR Uhrzeit
	
	DATE,			// NUR Datum
	
	PRICE,			// ein Preis, 1.5340
	
	MONEY,			// ein Geld-Wert 120.50 €
	
	INT_VALUE,		// zB ein Abstand in pips
	
	LONG_VALUE,		// long
	
	FLOAT_VALUE,	// zB hit-ratio
	
	TEXT,			// ein Bezeichner, zB "FirstMover-Trade"
	
	PERCENTAGE,		// Anzeige von zB 73%
	
	USER_DEFINED	// für eigenen Ableitungen
}
