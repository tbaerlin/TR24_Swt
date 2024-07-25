package tr24.utils.swt;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Monitor;
import tr24.utils.common.FileUtil;
import tr24.utils.common.SortedProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


/**
 * Ausgelagerter Application-Zusammen-Bauer
 * 
 * - Idee: was die App können soll/muss steht irgendwo 
 *   (zB in einem config-file)
 * - Lade also die Kompomenten (ChartSourceTabelle, ConsumerTabelle, AlarmTabelle, usw)
 * - und lass die Komponeneten sich selber bauen
 * - und als Tab in der mainShell einhängen
 * 
 */
public class ApplicationConfig {

	private static final String SHELL_X = "shell.x";
	private static final String SHELL_Y = "shell.y";
	private static final String SHELL_HEIGHT = "shell.height";
	private static final String SHELL_WIDTH = "shell.width";

	private static final String CMD_LINE_FOCUS_HOTKEY = "commandline.hotkey";

	/**
	 * kann auch null sein
	 */
	private final BasisCore core;
	
	private SortedProperties props;
	private SortedProperties shellProps;

	private File configFile;
	private final String encoding;
	/**
	 * neu: speichere die Shell-x/y Dinge in einer EXTRA Datei
	 */
	private File shellFile;
	
	/**
	 * speichere das config File (und shell-file) NUR wenn sich auch echt was geändert hat
	 */
	private boolean shellChanged, propsChanged;
	
	/** true: keine Set() erlaubt! */
	private boolean readonly;
	private boolean shellOnly;
	
	/**
	 * Constructor: nehme das ABSOLUTE File als Config-File und eben NICHT den Registry-folder
	 */
	public ApplicationConfig(File absolutePathToConfigFile) {
		this(absolutePathToConfigFile, null);
	}
	
	public ApplicationConfig(File absolutePathToConfigFile, String encoding) {
		this.encoding = encoding;
		this.core = null;
		this.configFile = absolutePathToConfigFile;
	}


	
//	/**
//	 * constructor
//	 * @param configFile - den Namen des Files, der Folder wird aus der Registry geholt
//	 */
//	public ApplicationConfig(BasisCore centralOrNull, String configFileName) {
//		this.core = centralOrNull;
//		
//		//new ApplicationConfigByRegistry("r3-config").
//		//	setString("folder", "C:/Users/tbaer/Desktop/MyR3_TK2/config");
//		
//		// hole den Folder aus der Registry:
//		String folderName = new ApplicationConfigByRegistry("r3-config").getString("folder", null);
//		if (folderName==null) {
//			throw new ShittyCodeException("missing registry: 'r3-config.folder' !");
//		}
//		File folder = new File(folderName);
//		if (!folder.exists()) {
//			throw new ShittyCodeException("config-folder missing: " + folder);
//		}
//		
//		configFile = new File(folder, configFileName);		// kann's auch noch nicht geben
//	}


	/**
	 * direkter Zugriff, um zB nicht-standard-Werte auszulesen
	 */
	public Properties getRawProps() {
		return props;
	}
	
	/**
	 * beim shutdown der App:
	 * - merke Position und Größe der Shell
	 * - aber noch nicht speichern, da könnten ja noch mehr Daten dazukommen
	 */
	public void setMainShellPosition(Rectangle box) {
		shellProps.setProperty(SHELL_X, String.valueOf(box.x));
		shellProps.setProperty(SHELL_Y, String.valueOf(box.y));
		shellProps.setProperty(SHELL_WIDTH, String.valueOf(box.width));
		shellProps.setProperty(SHELL_HEIGHT, String.valueOf(box.height));
		shellChanged = true;
	}

	/**
	 * Speichert name.x, name.y, name.b und name.h
	 * @param shellName - key
	 */
	public void setShellPosition(String shellName, Rectangle box) {
		shellProps.setProperty(shellName + ".x", String.valueOf(box.x));
		shellProps.setProperty(shellName + ".y", String.valueOf(box.y));
		shellProps.setProperty(shellName + ".b", String.valueOf(box.width));
		shellProps.setProperty(shellName + ".h", String.valueOf(box.height));
		shellChanged = true;
	}
	
	/**
	 * Setze etwas in die *.shell Variante
	 */
	public void setStringPropShell(String key, String value) {
		shellProps.setProperty(key, value);
		shellChanged = true;
	}
	/**
	 * Setze etwas in die *.shell Variante
	 */
	public void setIntPropShell(String key, int zahl) {
		shellProps.setProperty(key, String.valueOf(zahl));
		shellChanged = true;
	}

	
	/**
	 * Lädt oder erzeugt das Config-File
	 * - Daten von Platte laden
	 * - lade auch das *.shell file
	 */
	public void loadConfigFile() {
		if (configFile.exists()) {
			props = new SortedProperties(configFile, encoding);
		} else {
			props = new SortedProperties();
		}
		_loadShellStuff();
	}

	/**
	 * Lade NUR die *.shell-Werte
	 */
	public void loadConfigShellOnly() {
		shellOnly = true;
		_loadShellStuff();
	}

	private void _loadShellStuff() {
		// *.shell file kann auch separat existieren
		String name = configFile.getName();
		shellFile = new File(configFile.getParent(), name+".shell"); 
		if (shellFile.exists()) {
			shellProps = new SortedProperties(shellFile, null);
		} else {
			shellProps = new SortedProperties();
			shellFile = null;
		}
	}

	
	/**
	 * Lese eine Integer-Config-Wert
	 */
	public int getIntProp(String key, int defaultValue) {
		String s = props.getProperty(key);
		if (s==null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	/**
	 * Lese eine Integer-Config-Wert aus dem shell-file
	 */
	public int getIntPropShell(String key, int defaultValue) {
		String s = shellProps.getProperty(key);
		if (s==null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}
	/**
	 * Lese einen String -Config-Wert aus dem shell-file
	 */
	public String getStringPropShell(String key, String defaultValue) {
		String s = shellProps.getProperty(key);
		if (s==null) {
			return defaultValue;
		}
		return s;
	}

	/**
	 * Lese einen String-Config-Wert
	 */
	public String getStringProp(String key, String defaultValue) {
		return props.getProperty(key, defaultValue);
	}
	

	/**
	 * Boolean Property lesen: true bei "true", "1", "TRUE"; sonst immer false
	 */
	public boolean getBoolProp(String key, boolean defaultValue) {
		String s = getStringProp(key, "false").toUpperCase();
		return (s.equals("TRUE") || s.equals("1"));
	}

	public void setStringProp(String key, String value) {
		if (readonly) {
			throw new IllegalArgumentException("is readonly");
		}
		props.setProperty(key, value);
		propsChanged = true;
	}
	
	/**
	 * für Einträge wie zB "file1=" ... "file123=": <br>
	 * - lösche alle Einträge
	 */
	public void removeAll(String startingWith) {
		if (startingWith==null || startingWith.length()==0) {
			return;
		}
		List<String> removeKeys = new ArrayList<String>(20);
		for (Object key : props.keySet()) {
			String s = (String) key;
			if (s.startsWith(startingWith)) {
				removeKeys.add(s);
			}
		}
		for (String s : removeKeys) {
			props.remove(s);
			propsChanged = true;
		}
	}
	
	/**
	 * @param monitors - die Liste der Monitore ist nötig um zu prüfen
	 *   ob die Shell-Position ok ist oder (per altes config-file zB) ausserhalb liegt
	 */
	public Rectangle getMainShellPosition(Monitor[] monitors) {
		// ermittle das maximale Monitor-Rechteck
		Rectangle r = new Rectangle(0, 0, 1, 1);
		for (Monitor m : monitors) {
			// r = r.union(m.getClientArea());
			r = r.union(m.getBounds());
		}
		int x = getIntPropShell(SHELL_X, -1);
		int y = getIntPropShell(SHELL_Y, -1);
		int width = getIntPropShell(SHELL_WIDTH, 200);
		int height = getIntPropShell(SHELL_HEIGHT, 200);
		Rectangle box = new Rectangle(x, y, width, height);
		
		// box muss INNERHALB des Monitor-Bereichs liegen
		if (r.contains(x, y) && r.contains(x+width, y+height)) {
			return box;		// jaaa, config-box liegt im Monitor-Bereich
		}
		// sonst: Default-Position
		return new Rectangle(r.x+r.width/2-300, r.y+r.height/2-200, 730, 400);
	}
	
	/**
	 * Liest eine Position einer Shell per Name
	 */
	public Rectangle getShellPosition(String shellName, Monitor[] monitors) {
		// ermittle das maximale Monitor-Rechteck
		Rectangle r = new Rectangle(0, 0, 1, 1);
		for (Monitor m : monitors) {
			r = r.union(m.getClientArea());
		}
		int x = getIntPropShell(shellName+".x", -1);
		int y = getIntPropShell(shellName+".y", -1);
		int width = getIntPropShell(shellName+".b", -1);
		int height = getIntPropShell(shellName+".h", -1);
		Rectangle box = new Rectangle(x, y, width, height);
		// box muss INNERHALB des Monitor-Bereichs liegen
		if (r.contains(x, y) && r.contains(x+width, y+height)) {
			return box;		// jaaa, config-box liegt im Monitor-Bereich
		}
		// sonst: Default-Position
		return new Rectangle(r.x+r.width/2-300, r.y+r.height/2-200, 730, 400);
	}
	

	/**
	 * "commandline.hotkey"
	 */
	public String getCommandLineHotKey() {
		String value = props.getProperty(CMD_LINE_FOCUS_HOTKEY);
		return value;
	}

	
	/**
	 * zum Schluss: config auf Platte schreiben
	 */
	public void saveConfig() {
		try {
			if (propsChanged && !shellOnly) {
				props.store(new FileOutputStream(configFile), null);
				propsChanged = false; 		// reset
			}
			if (shellChanged) {
				if (shellFile==null) {
					if (configFile!=null) {
						String name = configFile.getName();
						shellFile = new File(configFile.getParent(), name+".shell");
					}
				}
				if (shellFile!=null) {
					shellProps.store(new FileOutputStream(shellFile), null);
					shellChanged = false;	// reset
				}
			}
		} catch (Exception e) {
			if (core!=null) {
				core.logger().error("Error writing config file", e);
			}
		}
	}

	/**
	 * Liefert den config-Folder
	 */
	public File getFolder() {
		return configFile.getParentFile();
	}

	/**
	 * Pfad zum config-file, absolute
	 */
	public File getFile() {
		return configFile;
	}

	/**
	 * Hole alle Einträge die mit etwas bestimmtem anfangen
	 */
	public List<String> getAllStartingWith(String prefix) {
		List<String> result = new ArrayList<String>();
		for (Object key : props.keySet()) {
			String s = (String) key;
			if (s.startsWith(prefix)) {
				result.add(s);
			}
		}
		return result;
	}
	
	
	/**
	 * Lese alle Zeilen (ohne Kommentare): <br>
	 * - die ist config nötig die MEHRFACH-Keys haben
	 */
	public List<String> getRawLines() {
		List<String> result = new ArrayList<String>(40);
		try {
			List<String> lines = FileUtil.readCharacterFileIntoArray(configFile);
			for (String s : lines) {
				if (s.length()==0 || s.startsWith("#")) {
					continue;
				}
				result.add(s);
			}
		} catch (IOException e) {
			e.printStackTrace();
			core.showCriticalError("Error reading raw lines from config file: " + configFile);
		}
		return result;
	}

	/**
	 * true: verbiete set()-Methoden <br>
	 * - die shell-size()-setter gehen aber immer
	 */
	public void setReadonly(boolean ro) {
		this.readonly = ro;
	}

	
}




































