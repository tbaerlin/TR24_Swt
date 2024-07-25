package tr24.utils.swt;
/**
 * int width x height
 */
public class ImgSize {
	
	public final int w, h;
	
	/**
	 * Gesetzt wenn erzeugt über string
	 */
	public final String error;
	
	/**
	 * direkt Ctor, 'error' ist hier immer null
	 */
	public ImgSize(int w, int h) {
		this.w = w;
		this.h = h;
		this.error = null;
	}
	
	/**
	 * erzeuge from String
	 */
	public ImgSize(String fromCode) {
		int _w = 0, _h = 0;
		String _error = null;
		try {
			String[] toks = fromCode.split("x");
			if (toks.length!=2) {
				_error = "invalid ImgSize: " + fromCode;
			} else {
				_w = Integer.parseInt(toks[0]);
				_h = Integer.parseInt(toks[1]);;
			}
		} catch (Exception e) {
			_error = "Error with ImgSize: " + fromCode + ": " + e;
		} finally {
			this.w = _w;  this.h = _h;
			this.error = _error;
		}
	}
	
	/**
	 * "320x200"
	 */
	@Override
	public String toString() {  return w + "x" + h; };
}











