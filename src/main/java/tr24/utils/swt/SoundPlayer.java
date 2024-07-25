package tr24.utils.swt;

import tr24.utils.common.AbstractQueue;
import tr24.utils.swt.api.IShutdownHook;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;


/**
 * Helfer: spielt einen Sound ab
 * 
 * - alle spiel-das(file) laufen HINTER-EINANDER ab
 * - es sei denn: playNow(file)
 * 
 * @author tbaer
 *
 */
public class SoundPlayer implements IShutdownHook {

	private AbstractQueue<File> soundWaitingList;
	
	/**
	 * constructor
	 */
	public SoundPlayer(BasisCore core) {
		soundWaitingList = new AbstractQueue<File>(false) {
			@Override
			protected void handleEvent(File file) {
				// hier kommen die sound-files der Reihe nach an
				playThisFile(file);
			}
		};
		core.add2Shutdown(this);
	}
	
	@Override
	public void shutdown() {
		soundWaitingList.shutdownQueue();
	}
	
	/**
	 * Play this sound (queued)
	 */
	public void play(File thisFile) {
		if (thisFile!=null) {
			if (!thisFile.exists()) {
				System.err.println("SoundPlayer: file not found: " + thisFile);
				return;
			}
			soundWaitingList.push(thisFile);
		}
	}
	
	/**
	 * play the sound file NOW
	 */
	public void playNow(final File thisFile) {
		Thread player = new Thread() {
			@Override
			public void run() {
				playThisFile(thisFile);
			}
		};
		player.start();
	}

	
    private void playThisFile(File pingF) {
    	AudioInputStream audioInputStream = null;
        try { 
            audioInputStream = AudioSystem.getAudioInputStream(pingF);
        } catch (UnsupportedAudioFileException e1) { 
            e1.printStackTrace();
            return;
        } catch (IOException e1) { 
            e1.printStackTrace();
            return;
        } 
 
        AudioFormat format = audioInputStream.getFormat();
        SourceDataLine auline = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
 
        try { 
            auline = (SourceDataLine) AudioSystem.getLine(info);
            auline.open(format);
        } catch (LineUnavailableException e) { 
            e.printStackTrace();
            return;
        } catch (Exception e) { 
            e.printStackTrace();
            return;
        } 
        auline.start();
        int nBytesRead = 0;
        int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb 
        byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
 
        try { 
            while (nBytesRead != -1) { 
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
                if (nBytesRead >= 0) 
                    auline.write(abData, 0, nBytesRead);
            } 
        } catch (IOException e) { 
            e.printStackTrace();
            return;
        } finally { 
            auline.drain();
            auline.close();
        } 
	}

	
}














