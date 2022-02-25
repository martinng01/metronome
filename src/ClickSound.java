public enum ClickSound
{
	CLICKHIGH("ClickHigh.wav", 0),
	CLICKLOW("ClickLow.wav", 1),
	SNARE("Snare.wav", 2),
	KICKDRUM("KickDrum.wav", 3),
	SILENCE("", 4);

	private String fileName;
	private int id;
	
    private ClickSound(String fileName, int id) 
    { 
    	this.fileName = fileName;
    	this.id = id;
    }
    	
    public String getFileName()
    {
    	return fileName;
    }
    
    public int getID()
    {
    	return id;
    }
    
    public static String[] getNames()
    {
    	ClickSound[] clickSounds = values();
    	String[] soundNames = new String[clickSounds.length];
    	
    	for (int i = 0; i < soundNames.length; i++)
    	{
    		soundNames[i] = clickSounds[i].name();
    	}
    	
    	return soundNames;
    }
}

