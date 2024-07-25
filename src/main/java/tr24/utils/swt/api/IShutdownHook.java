package tr24.utils.swt.api;

/**
 * Jede Komponente kann sich informieren lassen,
 * wenn die App geschlossen wird
 */
public interface IShutdownHook {
	public void shutdown();
}
