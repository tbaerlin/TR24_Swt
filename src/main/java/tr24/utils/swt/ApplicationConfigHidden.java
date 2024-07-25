package tr24.utils.swt;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import tr24.utils.common.ShittyCodeException;
import tr24.utils.swt.api.IShutdownHook;

import java.io.*;
import java.util.HashMap;
import java.util.Map;


/**
 * Schreibt/Liest String und int-Werte in eine Hashtable
 * - die als bin-dump auf die Platte geht
 * - also nicht so einfach les-bar
 * 
 * Konzept:
 * - ich kann Werte beliebig of reinpusten (also zB auch bei SWT-Resizes)
 * - und erst bei APP-Shutdown wird gespeichert 
 * - oder wenn User das halt so an-triggert
 *
 */
public class ApplicationConfigHidden implements IShutdownHook {

	private final File cfgFile;
	private Map<String, String> data;
	
	/**
	 * true? Speichern am Ende
	 */
	private boolean needSaving;

	
	/**
	 * constructor
	 * 
	 * @param cfgFile - da rein, Datei muss noch nicht da sein, wird gleich angelegt wenn noch nicht da
	 * @param core    - hängt sich für shutdown() rein, WENN gesetzt <br>
	 *                  wenn <b>null</b>: User-code muss selber save() rufen
	 */
	@SuppressWarnings("unchecked")
	public ApplicationConfigHidden(File cfgFile, BasisCore core) {
		this.cfgFile = cfgFile;
		if (core!=null) {
			core.add2Shutdown(this);
		}
		if (cfgFile==null) {
			throw new ShittyCodeException("cfgFile is null");
		}
		File parent = cfgFile.getParentFile();
		if (parent.exists()==false) {
			parent.mkdirs();		// muss da sein, sonst anlegen
		}
		if (cfgFile.exists()==false) {
			data = new HashMap<String, String>();
			// lege schon mal an:
			System.out.println("initial write: " + cfgFile);
			saveFile();
		} else {
			// lese alles schon mal ein
			try {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cfgFile));
				int v = ois.readInt();
				if (v!=10) {
					throw new Exception("Invalid config file version");
				}
				data = (Map<String, String>) ois.readObject();
			} catch (Exception e) {
				e.printStackTrace();
				throw new ShittyCodeException("Error reading config file: " + cfgFile + ": " + e);
			}
		}
	}
	
	@Override
	public void shutdown() {
		saveFile();
	}

	/**
	 * Speichere File wenn sich was geändert hat
	 */
	public void saveFile() {
		if (needSaving==false) {
			return;
		}
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cfgFile));
			int version = 10;
			oos.writeInt(version);		// immer for now
			oos.writeObject(data);		// dann pure hashtable
			oos.flush();
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ShittyCodeException("Error saving cfg file: " + cfgFile);
		}
	}

	/**
	 * Hole ein "x/y/w/h"
	 * 
	 * @return null wenn nicht da
	 */
	public Rectangle getRectangle(String key) {
		try {
			String[] split = data.get(key).split(",");		// knallt wenn key nicht da
			int x = Integer.parseInt(split[0]);
			int y = Integer.parseInt(split[1]);
			int w = Integer.parseInt(split[2]);
			int h = Integer.parseInt(split[3]);
			return new Rectangle(x, y, w, h);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Schreibe "x/y/w/h"
	 */
	public void setRectangle(String key, Rectangle box) {
		if (box==null) {
			return;
		}
		data.put(key, box.x + "," + box.y + "," + box.width + "," + box.height);
		needSaving = true;
	}

	/**
	 * Schreibe key=value
	 */
	public void setString(String key, String value) {
		if (value==null) {
			data.remove(key);
		} else {
			data.put(key, value);
		}
		needSaving = true;
	}

	/**
	 * Lese key=>value
	 * @return defValue wenn key nicht da ist
	 */
	public String getString(String key, String defValue) {
		String s = data.get(key);
		if (s==null) {
			return defValue;
		}
		return s;
	}
	
	public void setInt(String key, int value) {
		data.put(key, String.valueOf(value));
		needSaving = true;
	}
	
	public int getInt(String key, int defValue) {
		try {
			return Integer.parseInt(data.get(key));		// kann aus zwei Gründen knallen
		} catch (Exception e) {
			return defValue;
		}
	}
	
	public void setFloat(String key, float value) {
		data.put(key, String.valueOf(value));
		needSaving = true;
	}
	
	public float getFloat(String key, float defValue) {
		try {
			return Float.parseFloat(data.get(key));		// kann aus zwei Gründen knallen
		} catch (Exception e) {
			return defValue;
		}
	}
	
	public void setBool(String key, boolean trueOrFalse) {
		data.put(key, Boolean.toString(trueOrFalse));
		needSaving = true;
	}
	public boolean getBool(String key, boolean defValue) {
		String s = data.get(key);
		if (s==null) {
			return defValue;
		}
		return Boolean.parseBoolean(s);
	}

	/**
	 * Schreibe "255,102,140"
	 */
	public void setColor(String key, Color col) {
		if (col==null) {
			return;
		}
		String s = col.getRed() + "," + col.getGreen() + "," + col.getBlue();
		data.put(key, s);
		needSaving = true;
	}
	
	public Color getColor(String key, Color defColor) {
		try {
			String[] split = data.get(key).split(",");		// knallt wenn key nicht da
			int r = Integer.parseInt(split[0]);
			int g = Integer.parseInt(split[1]);
			int b = Integer.parseInt(split[2]);
			return FARBE.rgb(r, g, b);
		} catch (Exception e) {
			return defColor;
		}
	}

	
	/**
	 * setze String default-Wert NUR WENN key noch nicht da ist
	 */
	public void setDefault(String key, String value) {
		String s = data.get(key);
		if (s==null) {
			data.put(key, value);
			needSaving = true;
		}
	}
	/**
	 * setze boolean default-Wert NUR WENN key noch nicht da ist
	 */
	public void setDefault(String key, boolean value) {
		String s = data.get(key);
		if (s==null) {
			setBool(key, value);
			needSaving = true;
		}
	}
	/**
	 * setze int default-Wert NUR WENN key noch nicht da ist
	 */
	public void setDefault(String key, int value) {
		String s = data.get(key);
		if (s==null) {
			setInt(key, value);
			needSaving = true;
		}
	}
	/**
	 * setze float default-Wert NUR WENN key noch nicht da ist
	 */
	public void setDefault(String key, float value) {
		String s = data.get(key);
		if (s==null) {
			setFloat(key, value);
			needSaving = true;
		}
	}
	/**
	 * setze COLOR default-Wert NUR WENN key noch nicht da ist
	 */
	public void setDefault(String key, Color value) {
		String s = data.get(key);
		if (s==null) {
			setColor(key, value);
			needSaving = true;
		}
	}

	/**
	 * setze BOX wenn noch nicht da
	 */
	public void setDefault(String key, Rectangle box) {
		String s = data.get(key);
		if (s==null) {
			setRectangle(key, box);
			needSaving = true;
		}
	}

	
}







