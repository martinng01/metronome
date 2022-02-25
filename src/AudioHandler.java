import java.net.URL;
import java.util.Vector;

import kuusisto.tinysound.*;

//Uses TinySound library

public class AudioHandler {
	private static Vector<Sound> soundList = new Vector<Sound>();
	private static double volume = 0.5;

	public static void init() {
		TinySound.init();

		for (ClickSound clickSound : ClickSound.values()) {
			try {
				if (clickSound.getFileName() == "") {
					soundList.add(null);
					continue;
				}
				
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
		if (soundList.elementAt(id) == null) {
			return;
		}
		soundList.elementAt(id).play(volume);
	}

	public static void setVolume(double vol) {
		volume = vol;
	}
}
