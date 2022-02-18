import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Font;
import javax.swing.JSlider;
import javax.swing.JButton;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.Color;
import javax.swing.border.EtchedBorder;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JComboBox;

public class Metronome {

	private static JFrame frmMetronome;
	private static JButton bpmTap;
	private static JButton bpmSub;
	private static JButton bpmAdd;
	private static JButton btnPlay;
	private static JButton btnTimerSub;
	private static JButton btnTimerAdd;
	private static JToggleButton tglbtnBeatSound;
	private static JToggleButton tglbtnStress;
	private static JSlider bpmSlider;
	private static JSlider measureSlider;
	private static JSlider volSlider;
	private static JLabel bpmLabel;
	private static JLabel measureLabel;
	private static JLabel volLabel;
	private static JLabel lblTimer;
	private static JPanel beatsPanel;
	private static JPanel beatSoundPanel;
	private static JComboBox<String> highBeatSelector;
	private static JLabel lblHighBeat;
	private static JLabel lblLowBeat;
	private static JComboBox<String> lowBeatSelector;
	
	private static final int MIN_BPM = 0;
	private static final int MAX_BPM = 300;
	private static final int DEF_BPM = 100;
	private static int bpm = DEF_BPM;
	private static int beatsPerMeasure = 4;
	private static int beatCount = 1;
	private static int timer = 0;
	private static int timerOriginal = 0;
	private static boolean isPlaying = false;
	private static boolean isTimerRunning = false;
	private static boolean stressFirstBeat = true;
	private static boolean isModifyingBeatSound = false;
	private static Object lockObject = new Object();
	private static Object timerObject = new Object();
	private static Thread metronomeThread;
	private static Thread timerThread;
	private static List<JLabel> beatList = new ArrayList<JLabel>();
	private static List<Long> tapTime = new ArrayList<Long>();
	private static ClickSound highSound = ClickSound.CLICKHIGH;
	private static ClickSound lowSound = ClickSound.CLICKLOW;


	public static void main(String[] args)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					initialize();
					frmMetronome.setVisible(true);
					setNumBeats(4);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
		
		AudioHandler.init();
		
		Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{
				while(true)
				{
					long delay = Math.round((60.0 / bpm) * 1000);
					
					if (!isPlaying)
					{
						synchronized (lockObject)
						{
							try
							{
								lockObject.wait();
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}
					}
					else
					{
						try
						{
							tick();
							Thread.sleep(delay);
						}
						catch (InterruptedException e)
						{
							
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				}
			}
		};
		
		Runnable timerRunnable = new Runnable()
		{
			@Override
			public void run()
			{
				while (true)
				{
					if (!isPlaying)
					{
						synchronized (timerObject)
						{
							try
							{
								timerObject.wait();
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}
					}
					else if (isTimerRunning)
					{
						try
						{
							Thread.sleep(1000);
							timer--;
							updateTimer();
							
							if (timer == 0)
							{
								isTimerRunning = false;
								timerOriginal = 0;
								pause();
							}
						} 
						catch (InterruptedException e)
						{
							
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				}
			}
		};
		
		metronomeThread = new Thread(runnable);
		metronomeThread.start();
		
		timerThread = new Thread(timerRunnable);
		timerThread.start();
	}
	
	public static void setBpm(int newBpm)
	{	
		if (newBpm <= MIN_BPM)
		{
			bpm = MIN_BPM;
		}
		else if (newBpm >= MAX_BPM)
		{
			bpm = MAX_BPM;
		}
		else
		{
			bpm = newBpm;
		}
		bpmLabel.setText(Integer.toString(bpm));
		bpmSlider.setValue(bpm);
	}
	
	public static void tick()
	{
		try
		{
			playClick();
			beatList.get(beatCount - 1).setBackground(new Color(255, 215, 0));
			if (beatCount == 1)
			{
				beatList.get(beatsPerMeasure - 1).setBackground(Color.LIGHT_GRAY);
			}
			else
			{
				beatList.get(beatCount - 2).setBackground(Color.LIGHT_GRAY);
			}
			
			if (beatCount >= beatsPerMeasure)
			{
				beatCount = 1;
			}
			else
			{
				beatCount++;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void playClick()
	{
		if (beatCount == 1 || !stressFirstBeat)
		{
			AudioHandler.play(highSound.getID());
		}
		else
		{
			AudioHandler.play(lowSound.getID());
		}
	}
	
	public static void setNumBeats(int beats)
	{
		beatList.clear();
		beatsPanel.removeAll();
		beatsPanel.repaint();
		
		for (int i = 0; i < beats; i++)
		{
			int xPos = Math.round((500 - beats * 60) / (beats + 1)) * (i+1) +  i * 60;
			
			beatList.add(new JLabel(Integer.toString(i + 1)));
			beatList.get(i).setBorder(new EtchedBorder(EtchedBorder.RAISED, null, null));
			beatList.get(i).setBackground(Color.LIGHT_GRAY);
			beatList.get(i).setBounds(xPos, 30, 60, 60);
			beatList.get(i).setFont(new Font("Tahoma", Font.PLAIN, 30));
			beatList.get(i).setHorizontalAlignment(SwingConstants.CENTER);
			beatList.get(i).setOpaque(true);
			beatsPanel.add(beatList.get(i));
		}
	}

	public static void pause()
	{
		isPlaying = false;
		if (isTimerRunning)
		{
			timerThread.interrupt();
			timer = timerOriginal;
			updateTimer();
			isTimerRunning = false;
		}
		
		beatCount = 1;
		for (int i = 0; i < beatList.size(); i++)
		{
			beatList.get(i).setBackground(Color.LIGHT_GRAY);
		}
		
		btnPlay.setBackground(new Color(50, 205, 50));
		btnPlay.setText("START");
	}
	
	public static void updateTimer()
	{
		int min = (int) Math.floor(timer / 60);
		int sec = timer - (min * 60);
		
		if (sec == 0)
		{
			lblTimer.setText(min + ":00");
		}
		else if (sec >= 1 && sec <= 9)
		{
			lblTimer.setText(min + ":0" + sec);
		}
		else
		{
			lblTimer.setText(min + ":" + sec);
		}
	}
	
	private static void initialize()
	{
		frmMetronome = new JFrame();
		frmMetronome.setTitle("Metronome");
		frmMetronome.setBounds(100, 100, 500, 500);
		frmMetronome.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmMetronome.getContentPane().setLayout(null);
		
		measureSlider = new JSlider();
		measureSlider.setValue(4);
		measureSlider.setMinimum(2);
		measureSlider.setPaintLabels(true);
		measureSlider.setPaintTicks(true);
		measureSlider.setSnapToTicks(true);
		measureSlider.setMajorTickSpacing(1);
		measureSlider.setMaximum(6);
		measureSlider.setOrientation(SwingConstants.VERTICAL);
		measureSlider.setBounds(423, 126, 51, 279);
		frmMetronome.getContentPane().add(measureSlider);
		
		bpmSlider = new JSlider();
		bpmSlider.setBounds(0, 416, 484, 45);
		frmMetronome.getContentPane().add(bpmSlider);
		bpmSlider.setPaintLabels(true);
		bpmSlider.setMinorTickSpacing(10);
		bpmSlider.setMajorTickSpacing(50);
		bpmSlider.setValue(DEF_BPM);
		bpmSlider.setMaximum(MAX_BPM);
		bpmSlider.setPaintTicks(true);
		
		bpmTap = new JButton("Tap");
		bpmTap.setBounds(211, 371, 60, 34);
		frmMetronome.getContentPane().add(bpmTap);
		bpmTap.setFont(new Font("Tahoma", Font.BOLD, 11));
		
		bpmSub = new JButton("-");
		bpmSub.setBounds(150, 371, 51, 34);
		frmMetronome.getContentPane().add(bpmSub);
		bpmSub.setFont(new Font("Tahoma", Font.PLAIN, 20));
		
		bpmAdd = new JButton("+");
		bpmAdd.setFocusable(false);
		bpmAdd.setBounds(281, 371, 51, 34);
		frmMetronome.getContentPane().add(bpmAdd);
		bpmAdd.setFont(new Font("Tahoma", Font.PLAIN, 20));
		
		bpmLabel = new JLabel(Integer.toString(bpm));
		bpmLabel.setBounds(142, 164, 200, 97);
		frmMetronome.getContentPane().add(bpmLabel);
		bpmLabel.setFont(new Font("Tahoma", Font.BOLD, 80));
		bpmLabel.setHorizontalAlignment(SwingConstants.CENTER);
		
		measureLabel = new JLabel("Beats Per Measure");
		measureLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
		measureLabel.setBounds(394, 96, 90, 34);
		frmMetronome.getContentPane().add(measureLabel);
		
		btnPlay= new JButton("START");
		btnPlay.setFocusable(false);
		btnPlay.setFont(new Font("Tahoma", Font.BOLD, 23));
		btnPlay.setBackground(new Color(50, 205, 50));
		btnPlay.setBounds(150, 306, 182, 54);
		frmMetronome.getContentPane().add(btnPlay);
		
		beatsPanel = new JPanel();
		beatsPanel.setBounds(0, 0, 484, 97);
		frmMetronome.getContentPane().add(beatsPanel);
		beatsPanel.setLayout(null);
		
		volSlider = new JSlider();
		volSlider.setName("");
		volSlider.setOrientation(SwingConstants.VERTICAL);
		volSlider.setValue(5);
		volSlider.setMaximum(10);
		volSlider.setBounds(377, 272, 34, 137);
		frmMetronome.getContentPane().add(volSlider);
		
		volLabel = new JLabel("Volume");
		volLabel.setHorizontalAlignment(SwingConstants.CENTER);
		volLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		volLabel.setBounds(367, 256, 46, 14);
		frmMetronome.getContentPane().add(volLabel);
		
		lblTimer = new JLabel("0:00");
		lblTimer.setHorizontalAlignment(SwingConstants.CENTER);
		lblTimer.setFont(new Font("Tahoma", Font.BOLD, 30));
		lblTimer.setBounds(11, 190, 122, 54);
		frmMetronome.getContentPane().add(lblTimer);
		
		btnTimerSub = new JButton("-");
		btnTimerSub.setFont(new Font("Tahoma", Font.PLAIN, 20));
		btnTimerSub.setBounds(12, 241, 51, 29);
		frmMetronome.getContentPane().add(btnTimerSub);
		
		btnTimerAdd = new JButton("+");
		btnTimerAdd.setFont(new Font("Tahoma", Font.PLAIN, 20));
		btnTimerAdd.setBounds(81, 241, 51, 29);
		frmMetronome.getContentPane().add(btnTimerAdd);
		
		//Beat Sound Modifier
		
		tglbtnBeatSound = new JToggleButton("Change Sound");
		tglbtnBeatSound.setFont(new Font("Tahoma", Font.PLAIN, 12));
		tglbtnBeatSound.setBounds(11, 124, 122, 34);
		frmMetronome.getContentPane().add(tglbtnBeatSound);
		
		beatSoundPanel = new JPanel();
		beatSoundPanel.setBackground(Color.LIGHT_GRAY);
		beatSoundPanel.setBounds(485, 0, 200, 461);
		frmMetronome.getContentPane().add(beatSoundPanel);
		beatSoundPanel.setLayout(null);
		beatSoundPanel.setVisible(true);
		
		tglbtnStress = new JToggleButton("Stress first beat");
		tglbtnStress.setFont(new Font("Tahoma", Font.BOLD, 13));
		tglbtnStress.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		tglbtnStress.setSelected(true);
		tglbtnStress.setBounds(10, 11, 180, 34);
		beatSoundPanel.add(tglbtnStress);
		
		highBeatSelector = new JComboBox<String>(ClickSound.getNames());
		highBeatSelector.setBounds(10, 114, 180, 20);
		highBeatSelector.setSelectedItem(highSound.name());
		beatSoundPanel.add(highBeatSelector);
		
		lblHighBeat = new JLabel("High beat");
		lblHighBeat.setHorizontalAlignment(SwingConstants.CENTER);
		lblHighBeat.setBounds(10, 96, 180, 14);
		beatSoundPanel.add(lblHighBeat);
		
		lblLowBeat = new JLabel("Low beats");
		lblLowBeat.setHorizontalAlignment(SwingConstants.CENTER);
		lblLowBeat.setBounds(10, 282, 180, 14);
		beatSoundPanel.add(lblLowBeat);
		
		lowBeatSelector = new JComboBox<String>(ClickSound.getNames());
		lowBeatSelector.setBounds(10, 298, 180, 20);
		lowBeatSelector.setSelectedItem(lowSound.name());
		beatSoundPanel.add(lowBeatSelector);
		
		bpmAdd.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				setBpm(bpm + 1);
			}
		});
		bpmSub.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				setBpm(bpm - 1);
			}
		});
		bpmTap.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				tapTime.add(System.currentTimeMillis());
				bpmTap.setText(Integer.toString(8 - tapTime.size()));
				
				if (tapTime.size() == 8)
				{
					float totalElapsed = 0;
					
					for (int i = 1; i < tapTime.size() ; i++)
					{
						totalElapsed += (tapTime.get(i) - tapTime.get(i - 1));
					}
					
					float delay = (float) ((totalElapsed / 7.0) / 1000.0);
					
					int newBpm = (int) Math.round(60 / delay);
					setBpm(newBpm);
					
					tapTime.clear();
					
					bpmTap.setText("Tap");
				}
			}
		});
		btnPlay.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				if (isPlaying)
				{
					pause();
				}
				else
				{
					isPlaying = true;
					if (timer > 0)
					{
						isTimerRunning = true;
					}
					btnPlay.setBackground(new Color(220, 20, 60));
					btnPlay.setText("STOP");
					
					synchronized (lockObject)
					{
						lockObject.notify();
					}
					synchronized (timerObject)
					{
						if (timer > 0)
						{
							timerObject.notify();
						}
					}
				}
			}
		});
		tglbtnStress.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				stressFirstBeat = !stressFirstBeat;
			}
		});
		btnTimerAdd.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (isPlaying)
				{
					pause();
				}
				timerOriginal += 15;
				timer = timerOriginal;
				updateTimer();
			}
		});
		btnTimerSub.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (isPlaying)
				{
					pause();
				}
				
				if (timerOriginal != 0)
				{
					timerOriginal -= 15;
					timer = timerOriginal;
					updateTimer();
				}
			}
		});
		
		tglbtnBeatSound.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				isModifyingBeatSound = !isModifyingBeatSound;
				
				if (isModifyingBeatSound)
				{
					frmMetronome.setSize(700, 500);
					beatSoundPanel.setVisible(true);
				}
				else
				{
					frmMetronome.setSize(500, 500);
					beatSoundPanel.setVisible(false);
				}
			}
		});
		
		highBeatSelector.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				highSound = ClickSound.valueOf((String)highBeatSelector.getSelectedItem());
			}
		});
		
		lowBeatSelector.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				lowSound = ClickSound.valueOf((String)lowBeatSelector.getSelectedItem());
			}
		});
		
		bpmSlider.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent arg0)
			{
				if (isPlaying)
				{
					pause();
					metronomeThread.interrupt();
				}
				
				if (bpmSlider.getValue() == 0)
				{
					setBpm(1);
				}
				else
				{
					setBpm(bpmSlider.getValue());
				}
			}
		});
		measureSlider.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent arg0)
			{
				pause();
				
				beatsPerMeasure = measureSlider.getValue();
				setNumBeats(measureSlider.getValue());
			}
		});
		volSlider.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent arg0)
			{
				AudioHandler.setVolume(volSlider.getValue() / 10.0);
			}
		});
	}
}
