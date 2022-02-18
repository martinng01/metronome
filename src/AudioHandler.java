import java.net.URL;
import java.util.Vector;

import audiocue.AudioCue;

public class AudioHandler
{
	private static Vector<AudioCue> audioCueList = new Vector<AudioCue>();
	private static double volume = 0.5;
	
	public static void init()
	{
		for (ClickSound clickSound : ClickSound.values())
		{
			try
	    	{
				URL url = AudioHandler.class.getClassLoader().getResource(clickSound.getFileName());
				
				AudioCue audioCue = AudioCue.makeStereoCue(url, 3);
				audioCue.open();

				audioCueList.add(audioCue);
			}
			catch (Exception e)
	    	{
	    		e.printStackTrace();
	    	}
		}
	}
	
	public static void play(int id)
	{
		audioCueList.elementAt(id).play(volume);
	}
	
	public static void setVolume(double vol)
	{
		volume = vol;
	}
}
