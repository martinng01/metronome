import java.net.URL;
import java.util.Vector;

import kuusisto.tinysound.*;

public class AudioHandler {
	private static Vector<Sound> soundList = new Vector<Sound>();
	private static double volume = 0.5;

	public static void init() {
		TinySound.init();

		for (ClickSound clickSound : ClickSound.values()) {
			try {
				URL url = AudioHandler.class.getClassLoader().getResource(clickSound.getFileName());
				Sound sound = TinySound.loadSound(url);
				soundList.add(sound);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void play(int id) {
		soundList.elementAt(id).play(volume);
	}

	public static void setVolume(double vol) {
		volume = vol;
	}
	
	//TODO: invoke this method when metronome window is closed
	public static void close() {
		TinySound.shutdown();
	}
}
