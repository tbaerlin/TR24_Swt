package tr24.utils.swt.gentable;

import tr24.algoutils.IContractPrice;
import tr24.utils.common.DataType;
import tr24.utils.common.DayKey;
import tr24.utils.common.TimeUnit;
import tr24.utils.common.Utils;

import java.util.Date;



/**
 * Wrapper für eine Spalte (Meta-Info) 
 * 
 * - kennt die Breite, Label, Ausrichtung
 * - kann den Anzeige-String aus den Roh-Daten erzeugen (zB Date -> 24.12.2010) 
 * - kann Sortieren
 * - für jeden Standard-Type existiert eine eigene Klasse
 * 
 * - IDEE: Kann Überschreiben/Erweitert werden für was-auch-immer-anzeigen
 */
public abstract class ColumnHandler {

	public static Integer useNachkommaStellen = null;
	
	/**
	 * dirty: kann geändert werden, zB auf leer-String oder "-"
	 */
	public static String NA = "n/a";
	
	private ColumnType type;
	public String label;
	public int width;
	public int columnIdx;
	public int swtAlignment;
	public boolean sortable;

	/**
	 * hier steht zB
	 * - wie lange noch bis es losgeht
	 * - "running"
	 */
	public String status;
	
	
	
	protected ColumnHandler(ColumnType type, String label, int width, int columnIdx, int swtAlignment, boolean sortable) {
		this.type = type;
		this.label = label;
		this.width = width;
		this.columnIdx = columnIdx;
		this.swtAlignment = swtAlignment;
		this.sortable = sortable;
	}

	/**
	 * Erzeuge einen String aus den Daten für die Anzeige in der Tabelle
	 * - jede konkrete Type-Klasse implementiert ihre Version
	 */
	abstract public String render(Object data);
	
	/**
	 * für die Sortierung in einer Spalte:
	 */
	abstract public int sortCompare(Object cell1, Object cell2);
	
	@Override
	public String toString() {
		return type + "(\"" + label + "\")";
	}

	public ColumnType type() {
		return type;
	}
	
	/**
	 * Erzeuge eine neue PRICE-Spalte
	 * - der user-code MUSS @link PRICE_Type#renderPriceNullAllowed(float, Contract) verwenden!!
	 * @param showZeroPrice - false: zeige nicht für 0.0 
	 */
	public static ColumnHandler PRICE(int dataIdx, String label, int width, int swtAlignment, boolean sortable, boolean showZeroPrice) {
		ColumnHandler ttc = new PRICE_Type(dataIdx, label, width, swtAlignment, sortable, showZeroPrice);
		return ttc;
	}
	
	/**
	 * Erzeuge eine neue MONEY-Spalte: +350.00€  oder 25$ , mit/ohne Nachkommas
	 * - der user-code MUSS @link MONEY_Type#renderMoney(float, Contract) verwenden!!
	 */
	public static ColumnHandler MONEY(int dataIdx, String label, int width, 
			int swtAlignment, boolean sortable) 
	{
		ColumnHandler ttc = new MONEY_Type(dataIdx, label, width, swtAlignment, sortable);
		return ttc;
	}
	
	/**
	 * Erzeuge eine neue TRADE-Spalte: 23 t
	 */
	public static ColumnHandler TRADE(int dataIdx, String label, int width, 
			int swtAlignment, boolean sortable) 
	{
		ColumnHandler ttc = new TRADE_Type(dataIdx, label, width, swtAlignment, sortable);
		return ttc;
	}
	
	/**
	 * Erzeuge eine neue DATe-Spalte: <br>
	 * - NUR Datum 24.12.2010
	 */
	public static ColumnHandler DATE(int dataIdx, String label, int width, int swtAlignment, boolean sortable) {
		ColumnHandler ttc = new Date_Type(dataIdx, label, width, swtAlignment, sortable);
		return ttc;
	}
	
	/**
	 * Erzeuge eine neue DATETIME-Spalte: <br>
	 * - Datum PLUS Uhrzeit
	 * <br>
	 * <br>
	 * verwende {@link #TIME(int, String, int, int, boolean, boolean)} für NUR-Uhrzeit
	 */
	public static ColumnHandler DATETIME(int dataIdx, String label, int width, int swtAlignment, boolean sortable) {
		ColumnHandler ttc = new DateTime_Type(dataIdx, label, width, swtAlignment, sortable);
		return ttc;
	}
	
	/**
	 * Erzeuge eine NUR-Uhrzeit Spalte: <br>
	 * - verwendet {@link #DATETIME(int, String, int, int, boolean)} für komplettes Datum
	 */
	public static ColumnHandler TIME(int dataIdx, String label, int width, int swtAlignment, boolean sortable, boolean showSeconds) {
		ColumnHandler ttc = new Time_Type(dataIdx, label, width, swtAlignment, sortable, showSeconds);
		return ttc;
	}
	
	/**
	 * Erzeuge eine neue TEXT-Spalte
	 */
	public static ColumnHandler TEXT(int dataIdx, String label, int width, int swtAlignment, boolean sortable) {
		ColumnHandler ttc = new Text_Type(dataIdx, label, width, swtAlignment, sortable);
		return ttc;
	}
	/**
	 * Erzeuge eine neue FLOAT-Spalte
	 */
	public static ColumnHandler FLOAT(int dataIdx, String label, int width, int swtAlignment, boolean sortable) {
		ColumnHandler ttc = new Float_Type(dataIdx, label, width, swtAlignment, sortable);
		return ttc;
	}

	/**
	 * Erzeuge eine neue INT-Spalte
	 */
	public static ColumnHandler INT(int dataIdx, String label, int width, int swtAlignment, boolean sortable) {
		ColumnHandler ttc = new Int_Type(dataIdx, label, width, swtAlignment, sortable, false);
		return ttc;
	}
	
	/**
	 * Erzeuge eine neue INT-Spalte mit Tausender-Trennzeichen <br>
	 * - gut für Anzeige von Lot-Size: 150.000
	 */
	public static ColumnHandler INT_THOUSAND(int dataIdx, String label, int width, int swtAlignment, boolean sortable) {
		ColumnHandler ttc = new Int_Type(dataIdx, label, width, swtAlignment, sortable, true);
		return ttc;
	}
	
	/**
	 * Erzeuge eine neue LONG-Spalte
	 */
	public static ColumnHandler LONG(int dataIdx, String label, int width, int swtAlignment, boolean sortable) {
		ColumnHandler ttc = new Long_Type(dataIdx, label, width, swtAlignment, sortable);
		return ttc;
	}
	
	/**
	 * Erzeuge eine neue PERCENTAGE-Spalte
	 */
	public static ColumnHandler PERCENTAGE(int dataIdx, String label, int width, int swtAlignment, boolean sortable) {
		ColumnHandler ttc = new Percentage_Type(dataIdx, label, width, swtAlignment, sortable);
		return ttc;
	}

	/**
	 * Klasse um DATETIME-Spalten zu handeln: <br> 
	 * Datum MIT Uhrzeit
	 */
	public static class DateTime_Type extends ColumnHandler {

		public DateTime_Type(int colIdx, String label, int width, int swtAlignment, boolean sortable) {
			super(ColumnType.DATETIME, label, width, colIdx, swtAlignment, sortable);
		}
		
		@Override
		public String render(Object data) {
			if (data instanceof TimeUnit) {
				TimeUnit x = (TimeUnit) data;
				return x.toStringFull();
			} 
			if (data instanceof Date) {
				Date d = (Date) data;
				TimeUnit x = new TimeUnit(d.getTime());
				return x.toStringFull();
			}
			return NA;
		}
		
		@Override
		public int sortCompare(Object cell1, Object cell2) {
			
			if (cell1==null) {
				return cell2==null ? 0 : -1;
			}
			if (cell2==null) {
				return cell1==null ? 0 : 1;
			}
			
			TimeUnit t1, t2;
			if (cell1 instanceof TimeUnit) {
				t1 = (TimeUnit) cell1;
			} else if (cell1 instanceof Date) {
				t1 = new TimeUnit(((Date) cell1).getTime()); 
			} else {
				return 0;
			}
			if (cell2 instanceof TimeUnit) {
				t2 = (TimeUnit) cell2;
			} else if (cell2 instanceof Date) {
				t2 = new TimeUnit(((Date) cell2).getTime()); 
			} else {
				return 0;
			}
			return t1.compareTo(t2);
		}

	}
	
	/**
	 * Klasse um DATE-Spalten zu handeln: <br> 
	 * NUR Datum
	 */
	public static class Date_Type extends ColumnHandler {

		public Date_Type(int colIdx, String label, int width, int swtAlignment, boolean sortable) {
			super(ColumnType.DATE, label, width, colIdx, swtAlignment, sortable);
		}
		
		@Override
		public String render(Object data) {
			if (data instanceof DayKey) {
				DayKey dk = (DayKey) data;
				return dk.ddMMyyyy();
			}
			if (data instanceof TimeUnit) {
				TimeUnit x = (TimeUnit) data;
				return x.toStringDateOnly();
			} 
			if (data instanceof Date) {
				Date d = (Date) data;
				TimeUnit x = new TimeUnit(d.getTime());
				return x.toStringDateOnly();
			}
			return NA;
		}
		
		@Override
		public int sortCompare(Object cell1, Object cell2) {
			
			if (cell1==null) {
				return cell2==null ? 0 : -1;
			}
			if (cell2==null) {
				return cell1==null ? 0 : 1;
			}
			
			TimeUnit t1, t2;
			if (cell1 instanceof TimeUnit) {
				t1 = (TimeUnit) cell1;
			} else if (cell1 instanceof Date) {
				t1 = new TimeUnit(((Date) cell1).getTime()); 
			} else {
				return 0;
			}
			if (cell2 instanceof TimeUnit) {
				t2 = (TimeUnit) cell2;
			} else if (cell2 instanceof Date) {
				t2 = new TimeUnit(((Date) cell2).getTime()); 
			} else {
				return 0;
			}
			return t1.compareTo(t2);
		}

	}
	
	/**
	 * Klasse um NUR-UHRZEIT-Spalten zu handeln: <br>
	 * mit oder ohne Sekunden 
	 */
	public static class Time_Type extends ColumnHandler {

		private final boolean showSeconds;

		public Time_Type(int colIdx, String label, int width, int swtAlignment, boolean sortable, boolean showSeconds) {
			super(ColumnType.DATETIME, label, width, colIdx, swtAlignment, sortable);
			this.showSeconds = showSeconds;
		}
		
		@Override
		public String render(Object data) {
			if (data instanceof TimeUnit) {
				TimeUnit x = (TimeUnit) data;
				if (showSeconds) {
					return x.toStringShort();
				} else {
					return x.toStringHourMin();
				}
			} 
			if (data instanceof Date) {
				Date d = (Date) data;
				TimeUnit x = new TimeUnit(d.getTime());
				if (showSeconds) {
					return x.toStringShort();
				} else {
					return x.toStringHourMin();
				}
			}
			return NA;
		}
		
		@Override
		public int sortCompare(Object cell1, Object cell2) {
			
			if (cell1==null) {
				return cell2==null ? 0 : -1;
			}
			if (cell2==null) {
				return cell1==null ? 0 : 1;
			}
			
			TimeUnit t1, t2;
			if (cell1 instanceof TimeUnit) {
				t1 = (TimeUnit) cell1;
			} else if (cell1 instanceof Date) {
				t1 = new TimeUnit(((Date) cell1).getTime()); 
			} else {
				return 0;
			}
			if (cell2 instanceof TimeUnit) {
				t2 = (TimeUnit) cell2;
			} else if (cell2 instanceof Date) {
				t2 = new TimeUnit(((Date) cell2).getTime()); 
			} else {
				return 0;
			}
			return t1.compareTo(t2);
		}

	}
	
	
	/**
	 * Klasse um TIME-Spalten zu handeln
	 */
	public static class Text_Type extends ColumnHandler {

		public Text_Type(int colIdx, String label, int width, int swtAlignment, boolean sortable) {
			super(ColumnType.TEXT, label, width, colIdx, swtAlignment, sortable);
		}
		@Override
		public String render(Object data) {
			if (data==null) {
				return "";
			}
			String x = (String) data;
			return x;
		}
		@Override
		public int sortCompare(Object cell1, Object cell2) {
			String s1 = (String) cell1;
			if (s1==null) {
				return -1;
			}
			String s2 = (String) cell2;
			if (s2==null) {
				return 0;
			}
			return s1.compareTo(s2);
		}
	}
	
	/**
	 * Klasse um FLOAT-Spalten zu handeln
	 */
	public static class Float_Type extends ColumnHandler {
		
		public Integer nachkommastellen;
		
		public Float_Type(int colIdx, String label, int width, int swtAlignment, boolean sortable) {
			super(ColumnType.FLOAT_VALUE, label, width, colIdx, swtAlignment, sortable);
			if (useNachkommaStellen!=null) {
				nachkommastellen = useNachkommaStellen;
			}
		}
		@Override
		public String render(Object data) {
			if (data==null) {
				return NA;
			}
			Float x = (Float) data;
			String s;
			if (nachkommastellen!=null) {
				s = Utils.print(x, nachkommastellen);
			} else {
				s = String.valueOf(x);
			}
			return s.replace('.', ',');
		}
		/**
		 * muss mit NULL umgehen können!
		 */
		@Override
		public int sortCompare(Object cell1, Object cell2) {
			Float f1 = (Float) cell1;
			if (f1==null) {
				return -1;		// erste null -> zweiter ist größer
			}
			Float f2 = (Float) cell2;
			if (f2==null) {
				return 0;		
			}
			return f1.compareTo(f2);
		}
	}
	
	/**
	 * Klasse um INT-Spalten zu handeln
	 */
	public static class Int_Type extends ColumnHandler {
		
		private final boolean show1000;
		
		public Int_Type(int colIdx, String label, int width, int swtAlignment, boolean sortable, boolean show1000) {
			super(ColumnType.INT_VALUE, label, width, colIdx, swtAlignment, sortable);
			this.show1000 = show1000;
		}
		@Override
		public String render(Object data) {
			if (data==null) {
				return NA;
			}
			Integer x = (Integer) data;
			if (!show1000) {
				return String.valueOf(x);
			} else {
				if (x<1000 && x>-1000) {
					return String.valueOf(x);
				} else {
					int r = x % 1000;
					if (r<10) {
						return (x / 1000) + ".00" + r; 
					} 
					if (r<100) {
						return (x / 1000) + ".0" + r; 
					}
					return (x / 1000) + "." + r; 
				}
			}
		}
		/**
		 * muss mit NULL umgehen können!
		 */
		@Override
		public int sortCompare(Object cell1, Object cell2) {
			Integer i1 = (Integer) cell1;
			if (i1==null) {
				return -1;
			}
			Integer i2 = (Integer) cell2;
			if (i2==null) {
				return 0;		// statt 1!! TRICKLE: damit kommen leere Felder immer unten, keine Ahnung wieso genau, aber es funzt *g*
			}
			return i1.compareTo(i2);
		}
	}

	/**
	 * Klasse um LONG-Spalten zu handeln
	 */
	public static class Long_Type extends ColumnHandler {
		
		public Long_Type(int colIdx, String label, int width, int swtAlignment, boolean sortable) {
			super(ColumnType.LONG_VALUE, label, width, colIdx, swtAlignment, sortable);
		}
		@Override
		public String render(Object data) {
			if (data==null) {
				return NA;
			}
			Long x = (Long) data;
			return String.valueOf(x);
		}
		@Override
		public int sortCompare(Object o1, Object o2) {
			Long i1 = (Long) o1;
			if (i1==null) {
				return -1;
			}
			Long i2 = (Long) o2;
			if (i2==null) {
				return 0;
			}
			return i1.compareTo(i2);
		}
	}

	/**
	 * Klasse um PERCANTAGE-Spalten zu handeln
	 * - kann mit FLOAT UND INTEGER Werten umgehen
	 */
	public static class Percentage_Type extends ColumnHandler {
		
		private Integer nachkommastellen = null;
		
		public Percentage_Type(int colIdx, String label, int width, int swtAlignment, boolean sortable) {
			super(ColumnType.PERCENTAGE, label, width, colIdx, swtAlignment, sortable);
			if (useNachkommaStellen!=null) {
				nachkommastellen = useNachkommaStellen;
			}
		}
		/**
		 * super-special hier:
		 * - wenn eine INTEGER zahl kommt: 0..100%
		 * - wenn eine FLOAT Zahl kommt: mal 100 => 0..100%
		 */
		@Override
		public String render(Object data) {
			if (data instanceof Float) {
				float f = (Float) data * 100.0f;
				if (nachkommastellen!=null) {
					return Utils.print(f, nachkommastellen) + "%";
				} else {
					return String.valueOf(f) + "%";
				}
			}
			if (data instanceof Integer) {
				Integer x = (Integer) data;
				return x + "%";
			}
			return NA;		// sonst
		}
		@Override
		public int sortCompare(Object cell1, Object cell2) {
			Float f1 = null, f2 = null;
			if (cell1 instanceof Float) {
				f1 = (Float) cell1 * 100.0f;
			} else if (cell1 instanceof Integer) {
				f1 = 1.0f * (Integer) cell1;
			}
			if (cell2 instanceof Float) {
				f2 = (Float) cell2 * 100.0f;
			} else if (cell1 instanceof Integer) {
				f2 = 1.0f * (Integer) cell2;
			}
			if (f1==null) {
				return -1;
			}
			if (f2==null) {
				return 0;
			}
			return f1.compareTo(f2);
		}
	}

	
	/**
	 * Klasse um PRICE-Spalten zu handeln
	 * - diese Variante ERWARTET den call auf 
	 */
	public static class PRICE_Type extends ColumnHandler {
		
		private final boolean showZeroPrice;
		
		public PRICE_Type(int colIdx, String label, int width, int swtAlignment, boolean sortable, boolean showZeroPrice) {
			super(ColumnType.PRICE, label, width, colIdx, swtAlignment, sortable);
			this.showZeroPrice = showZeroPrice;
		}
		
		@Override
		public String render(Object data) {
			throw new RuntimeException("PRICE_Type: use renderPrice(float, Contract) !!");
		}
		
		/**
		 * Spezielle Render-Methode für Preise
		 * @return leer-string für null-price
		 */
		public String renderPriceNullAllowed(Float thisPrice, IContractPrice contract) {
			if (thisPrice==null) {
				return "";
			}
			float x = thisPrice;
			if (x==0.0f && !showZeroPrice) {
				return "";
			}
			return contract.print(thisPrice);
		}
		@Override
		public int sortCompare(Object cell1, Object cell2) {
			Float f1 = (Float) cell1;
			if (f1==null) {
				return -1;
			}
			Float f2 = (Float) cell2;
			if (f2==null) {
				return 0;
			}
			return f1.compareTo(f2);
		}
	}
	
	/**
	 * Klasse um MONEY-Spalten zu rendern:
	 * - Formatierung mit/ohne Nachkommas
	 * - zeige das WÄHRUNGS-Symbol mit an
	 */
	public static class MONEY_Type extends ColumnHandler {
		
		public MONEY_Type(int colIdx, String label, int width, int swtAlignment, boolean sortable) {
			super(ColumnType.MONEY, label, width, colIdx, swtAlignment, sortable);
		}
		
		@Override
		public String render(Object data) {
			throw new RuntimeException("MONEY_TYPE: use renderMoney(float, Contract) !!");
		}
		
		public String renderMoney(float thisPrice, IContractPrice priceToCurrencyString, boolean showNachkommas) {
			if (thisPrice==0) {
				return "";
			}
			return priceToCurrencyString.printMoney(thisPrice, showNachkommas);
		}
		
		@Override
		public int sortCompare(Object cell1, Object cell2) {
			Float f1 = (Float) cell1;
			Float f2 = (Float) cell2;
			return f1.compareTo(f2);
		}
	}
	
	/**
	 * Klasse um TRADE-Spalten zu handeln
	 */
	public static class TRADE_Type extends ColumnHandler {
		
		public TRADE_Type(int colIdx, String label, int width, int swtAlignment, boolean sortable) {
			super(ColumnType.INT_VALUE, label, width, colIdx, swtAlignment, sortable);
		}
		@Override
		public String render(Object data) {
			if (data==null) {
				return "";
			}
			Integer x = (Integer) data;
			return x + " t";
		}
		@Override
		public int sortCompare(Object cell1, Object cell2) {
			Integer i1 = (Integer) cell1;
			Integer i2 = (Integer) cell2;
			return i1.compareTo(i2);
		}
	}
	
	/**
	 * Erzeugt Spalten aus {@link DataType}
	 */
	public static ColumnHandler byType(DataType convertFrom, int dataIdx, String label, int width, int swtAlignment, boolean sortable) {
		switch (convertFrom) {
			case FLOAT:  		return ColumnHandler.FLOAT(dataIdx, label, width, swtAlignment, sortable);
			case INT:    		return ColumnHandler.INT(dataIdx, label, width, swtAlignment, sortable);
			case STRING: 		return ColumnHandler.TEXT(dataIdx, label, width, swtAlignment, sortable);
			case ENUM:	 		return ColumnHandler.TEXT(dataIdx, label, width, swtAlignment, sortable);
			case PERCENTAGE: 	return ColumnHandler.PERCENTAGE(dataIdx, label, width, swtAlignment, sortable);
			case TIME: 			return ColumnHandler.DATETIME(dataIdx, label, width, swtAlignment, sortable);
			case MONEY:			return ColumnHandler.MONEY(dataIdx, label, width, swtAlignment, sortable);
			case TRADES:		return ColumnHandler.TRADE(dataIdx, label, width, swtAlignment, sortable);
		}
		return null;		// sonst
	}

}



