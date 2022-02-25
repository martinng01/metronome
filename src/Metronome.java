import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;

import java.awt.Font;
import javax.swing.JSlider;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.Color;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.ImageIcon;
import javax.swing.border.MatteBorder;

public class Metronome {

	private static final int MIN_BPM = 0;
	private static final int MAX_BPM = 300;
	private static final int DEFAULT_BPM = 100;

	private static final Color frmMetronomeColour = Color.decode("#001219");
	private static final Color textColour = Color.decode("#94d2bd");
	private static final Color tapTempoColour = Color.decode("#ee9b00");

	private static final ImageIcon playIcon = new ImageIcon(Metronome.class.getResource("/icons/icons8_play_32px_1.png"));
	private static final ImageIcon pauseIcon = new ImageIcon(Metronome.class.getResource("/icons/icons8_pause_32px_1.png"));
	private static final ImageIcon plusBPMIcon = new ImageIcon(Metronome.class.getResource("/icons/icons8_plus_math_24px.png"));
	private static final ImageIcon subtractBPMIcon = new ImageIcon(Metronome.class.getResource("/icons/icons8_subtract_24px.png"));
	private static final ImageIcon plusTimerIcon = new ImageIcon(Metronome.class.getResource("/icons/icons8_plus_math_20px.png"));
	private static final ImageIcon subtractTimerIcon = new ImageIcon(Metronome.class.getResource("/icons/icons8_subtract_20px.png"));
	private static final ImageIcon menuIcon = new ImageIcon(Metronome.class.getResource("/icons/icons8_menu_32px.png"));
	private static final ImageIcon menuSelectedIcon = new ImageIcon(Metronome.class.getResource("/icons/icons8_menu_32px_1.png"));
	private static final ImageIcon offBeatIcon = new ImageIcon(Metronome.class.getResource("/icons/icons8_circled_thin_64px_1.png"));
	private static final ImageIcon onBeatIcon = new ImageIcon(Metronome.class.getResource("/icons/icons8_filled_circle_64px_2.png"));
	private static final ImageIcon loudVolIcon = new ImageIcon(Metronome.class.getResource("/icons/icons8_audio_32px.png"));
	private static final ImageIcon softVolIcon = new ImageIcon(Metronome.class.getResource("/icons/icons8_speaker_32px.png"));
	private static final ImageIcon muteVolIcon = new ImageIcon(Metronome.class.getResource("/icons/icons8_mute_32px.png"));
	private static final ImageIcon beatsIcon = new ImageIcon(Metronome.class.getResource("/icons/icons8_music_32px.png"));

	private static JFrame frmMetronome;
	private static MyButton bpmTap, bpmSub, bpmAdd, btnPlay, btnTimerSub, btnTimerAdd;
	private static JToggleButton tglbtnBeatSound;
	private static MySlider bpmSlider, measureSlider, volSlider;
	private static JLabel bpmLabel, lblTimer, lblHighBeat, lblLowBeat, lblVolume, lblBeats;
	private static JPanel beatsPanel, beatSoundPanel;
	private static MyComboBox highBeatSelector, lowBeatSelector;

	private static int bpm = DEFAULT_BPM;
	private static int beatsPerMeasure = 4;
	private static int beatCount = 1;
	private static int timerValue = 0;
	private static boolean isPlaying = false;
	private static boolean isModifyingBeatSound = false;
	private static List<JLabel> beatList = new ArrayList<JLabel>();
	private static ClickSound highSound = ClickSound.CLICKHIGH;
	private static ClickSound lowSound = ClickSound.CLICKLOW;
	private static ScheduledFuture<?> beepHandler, timerHandler;
	private static ScheduledExecutorService scheduler;
	private static PracticeTimer practiceTimer;

	public static class MetronomeBeep implements Runnable {
		@Override
		public void run() {
			if (beatCount == 1) {
				AudioHandler.play(highSound.getID());
			} else {
				AudioHandler.play(lowSound.getID());
			}

			beatList.get(beatCount - 1).setIcon(onBeatIcon);

			if (beatCount == 1) {
				beatList.get(beatsPerMeasure - 1).setIcon(offBeatIcon);
			} else {
				beatList.get(beatCount - 2).setIcon(offBeatIcon);
			}

			if (beatCount >= beatsPerMeasure) {
				beatCount = 1;
			} else {
				beatCount++;
			}
		}
	}

	public static class PracticeTimer implements Runnable {
		private int timer;
		private int timerOriginal;
		private boolean isTimerRunning = false;

		public PracticeTimer(int timerOriginal) {
			this.timerOriginal = timerOriginal;
			this.timer = timerOriginal;
		}

		@Override
		public void run() {
			timer--;
			updateTimer(timer);

			if (timer == 0) {
				pause();
			}

			if (isTimerRunning == false) {
				isTimerRunning = true;
			}
		}

		public void interrupt() {
			timer = timerOriginal;
			isTimerRunning = false;

			updateTimer(timer);
			timerHandler.cancel(true);
		}

		public boolean isActive() {
			return isTimerRunning;
		}
	}

	public static class TapTempo {
		private static List<Long> tapTime = new ArrayList<Long>();
		private static Thread countdownThread;
		private static CountdownRunnable countdownRunnable = new CountdownRunnable();
		private static boolean isCountingDown = false;
		private static long prevTime;

		public static class CountdownRunnable implements Runnable {
			private final int countdownTime = 2000; // 2000 milliseconds, 2 seconds
			private final int numIterations = 30;
			private int transparency = 255;

			@Override
			public void run() {
				isCountingDown = true;

				while (isCountingDown) {
					bpmTap.setBackground(new Color(tapTempoColour.getRed(), tapTempoColour.getGreen(), tapTempoColour.getBlue(), transparency));
					transparency -= 255 / numIterations;
					frmMetronome.repaint();

					try {
						Thread.sleep(countdownTime / numIterations);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					if (transparency < 0) isCountingDown = false;
				}

				// If user does not press tapTempo button again within 2 seconds
				reset();
			}

			public void restartCountdown() {
				transparency = 255;
			}

		}

		public static void tap() {
			if (isPlaying) pause();
			if (!isCountingDown) isCountingDown = true;
			bpmTap.setOpaque(true);

			if (countdownThread == null || !countdownThread.isAlive()) {
				countdownRunnable = new CountdownRunnable();
				countdownThread = new Thread(countdownRunnable);
				countdownThread.start();
			} else {
				countdownRunnable.restartCountdown();
			}

			if (prevTime == 0) {
				prevTime = System.currentTimeMillis();
				return;
			}

			long currentTime = System.currentTimeMillis();
			tapTime.add(currentTime - prevTime);
			prevTime = currentTime;

			float totalElapsed = 0;
			float delay;

			if (tapTime.size() < 4) {
				for (long time : tapTime) {
					totalElapsed += (float) time;
				}
				delay = (float) ((totalElapsed / (float) tapTime.size()) / 1000.0);
			} else {
				for (int i = 1; i <= 4; i++) { // Only consider last 4 taps
					totalElapsed += (float) tapTime.get(tapTime.size() - i);
				}
				delay = (float) ((totalElapsed / 4.0) / 1000.0);
			}

			int newBpm = (int) Math.round(60 / delay);

			setBpm(newBpm);
		}

		public static void reset() {
			isCountingDown = false;
			bpmTap.setOpaque(false);
			tapTime.clear();
			prevTime = 0;
			bpmTap.setText("Tap");
		}
	}

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					initialize();
					frmMetronome.setVisible(true);
					setNumBeats(4);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		AudioHandler.init();
		scheduler = Executors.newScheduledThreadPool(2);
	}

	public static void setBpm(int newBpm) {
		if (newBpm <= MIN_BPM) {
			bpm = MIN_BPM;
		} else if (newBpm >= MAX_BPM) {
			bpm = MAX_BPM;
		} else {
			bpm = newBpm;
		}
		bpmLabel.setText(Integer.toString(bpm));
		bpmSlider.setValue(bpm);
	}

	public static void setNumBeats(int beats) {
		beatList.clear();
		beatsPanel.removeAll();
		beatsPanel.repaint();

		for (int i = 0; i < beats; i++) {
			int xPos = Math.round((500 - beats * 60) / (beats + 1)) * (i + 1) + i * 60;

			beatList.add(new JLabel());
			// beatList.get(i).setBorder(new EtchedBorder(EtchedBorder.RAISED, null, null));
			beatList.get(i).setBounds(xPos, 30, 60, 60);
			beatList.get(i).setFont(new Font("Tahoma", Font.PLAIN, 30));
			beatList.get(i).setHorizontalAlignment(SwingConstants.CENTER);
			beatList.get(i).setOpaque(false);
			beatList.get(i).setIcon(offBeatIcon);
			beatsPanel.add(beatList.get(i));
		}
	}

	public static void pause() {
		isPlaying = false;

		beepHandler.cancel(true);

		if (practiceTimer != null) {
			practiceTimer.interrupt();
			practiceTimer = null;
		}

		beatCount = 1;
		for (int i = 0; i < beatList.size(); i++) {
			beatList.get(i).setIcon(offBeatIcon);
		}

		btnPlay.setIcon(playIcon);
	}

	public static void play() {
		beepHandler = scheduler.scheduleAtFixedRate(new MetronomeBeep(), 0, Math.round((60.0 / bpm) * 1000), TimeUnit.MILLISECONDS);

		if (timerValue > 0) {
			practiceTimer = new PracticeTimer(timerValue);
			timerHandler = scheduler.scheduleAtFixedRate(practiceTimer, 0, 1, TimeUnit.SECONDS);
		}

		isPlaying = true;
		btnPlay.setIcon(pauseIcon);
	}

	public static void updateTimer(int time) {
		int min = (int) Math.floor(time / 60);
		int sec = time - (min * 60);

		if (sec == 0) {
			lblTimer.setText(min + ":00");
		} else if (sec >= 1 && sec <= 9) {
			lblTimer.setText(min + ":0" + sec);
		} else {
			lblTimer.setText(min + ":" + sec);
		}
	}

	private static void initialize() {
		frmMetronome = new JFrame();
		frmMetronome.setTitle("Metronome");
		frmMetronome.setBounds(100, 100, 500, 500);
		frmMetronome.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmMetronome.getContentPane().setLayout(null);
		frmMetronome.getContentPane().setBackground(frmMetronomeColour);

		measureSlider = new MySlider();
		measureSlider.setVisible(false);
		measureSlider.setPaintLabels(true);
		measureSlider.setValue(4);
		measureSlider.setMinimum(2);
		measureSlider.setSnapToTicks(true);
		measureSlider.setMajorTickSpacing(1);
		measureSlider.setMaximum(6);
		measureSlider.setOrientation(SwingConstants.VERTICAL);
		measureSlider.setBounds(426, 181, 50, 200);
		measureSlider.keepAliveOnHover();

		bpmSlider = new MySlider();
		bpmSlider.setBounds(0, 416, 484, 45);
		bpmSlider.setPaintLabels(true);
		bpmSlider.setMinorTickSpacing(10);
		bpmSlider.setMajorTickSpacing(50);
		bpmSlider.setValue(DEFAULT_BPM);
		bpmSlider.setMaximum(MAX_BPM);

		bpmTap = new MyButton("Tap");
		bpmTap.setBounds(211, 371, 60, 34);
		bpmTap.setForeground(Color.WHITE);
		bpmTap.setFont(new Font("Tahoma", Font.BOLD, 11));

		bpmSub = new MyButton();
		bpmSub.setBounds(150, 371, 51, 34);
		bpmSub.setFont(new Font("Tahoma", Font.PLAIN, 20));
		bpmSub.setIcon(subtractBPMIcon);

		bpmAdd = new MyButton();
		bpmAdd.setBounds(281, 371, 51, 34);
		bpmAdd.setFont(new Font("Tahoma", Font.PLAIN, 20));
		bpmAdd.setIcon(plusBPMIcon);

		bpmLabel = new JLabel(Integer.toString(bpm));
		bpmLabel.setBounds(142, 164, 200, 97);
		bpmLabel.setFont(new Font("Tahoma", Font.BOLD, 80));
		bpmLabel.setForeground(textColour);
		bpmLabel.setHorizontalAlignment(SwingConstants.CENTER);

		btnPlay = new MyButton();
		btnPlay.setIcon(playIcon);
		btnPlay.setFont(new Font("Tahoma", Font.BOLD, 23));
		btnPlay.setBounds(150, 306, 182, 54);

		beatsPanel = new JPanel();
		beatsPanel.setBounds(-10, 38, 484, 97);
		beatsPanel.setBackground(frmMetronomeColour);
		beatsPanel.setLayout(null);

		volSlider = new MySlider();
		volSlider.setVisible(false);
		volSlider.setOrientation(SwingConstants.VERTICAL);
		volSlider.setValue(5);
		volSlider.setMaximum(10);
		volSlider.setBounds(370, 226, 50, 155);
		volSlider.keepAliveOnHover();

		lblTimer = new JLabel("0:00");
		lblTimer.setHorizontalAlignment(SwingConstants.CENTER);
		lblTimer.setFont(new Font("Tahoma", Font.BOLD, 30));
		lblTimer.setForeground(textColour);
		lblTimer.setBounds(11, 190, 122, 54);

		btnTimerSub = new MyButton();
		btnTimerSub.setBounds(12, 241, 51, 29);
		btnTimerSub.setIcon(subtractTimerIcon);

		btnTimerAdd = new MyButton();
		btnTimerAdd.setBounds(81, 241, 51, 29);
		btnTimerAdd.setIcon(plusTimerIcon);

		lblVolume = new JLabel();
		lblVolume.setBounds(379, 377, 32, 32);
		lblVolume.setIcon(softVolIcon);

		lblBeats = new JLabel();
		lblBeats.setBounds(428, 377, 32, 32);
		lblBeats.setIcon(beatsIcon);

		// Beat Sound Modifier

		tglbtnBeatSound = new JToggleButton();
		tglbtnBeatSound.setBorderPainted(false);
		tglbtnBeatSound.setFont(new Font("Tahoma", Font.PLAIN, 12));
		tglbtnBeatSound.setBounds(7, 10, 40, 34);
		tglbtnBeatSound.setBorder(new MatteBorder(1, 1, 1, 1, (Color) Color.WHITE));
		tglbtnBeatSound.setOpaque(false);
		tglbtnBeatSound.setIcon(menuIcon);
		tglbtnBeatSound.setFocusable(false);
		tglbtnBeatSound.setContentAreaFilled(false);

		beatSoundPanel = new JPanel();
		beatSoundPanel.setBorder(new MatteBorder(0, 2, 0, 0, (Color) new Color(255, 255, 255)));
		beatSoundPanel.setBackground(frmMetronomeColour);
		beatSoundPanel.setBounds(485, 0, 200, 461);
		beatSoundPanel.setLayout(null);

		highBeatSelector = new MyComboBox();
		highBeatSelector.setBounds(10, 114, 180, 20);
		highBeatSelector.setSelectedItem(highSound.name());

		lblHighBeat = new JLabel("High beat");
		lblHighBeat.setHorizontalAlignment(SwingConstants.CENTER);
		lblHighBeat.setForeground(textColour);
		lblHighBeat.setBounds(10, 96, 180, 14);

		lblLowBeat = new JLabel("Low beat");
		lblLowBeat.setHorizontalAlignment(SwingConstants.CENTER);
		lblLowBeat.setForeground(textColour);
		lblLowBeat.setBounds(10, 282, 180, 14);

		lowBeatSelector = new MyComboBox();
		lowBeatSelector.setBounds(10, 298, 180, 20);
		lowBeatSelector.setSelectedItem(lowSound.name());

		beatSoundPanel.add(highBeatSelector);
		beatSoundPanel.add(lblHighBeat);
		beatSoundPanel.add(lblLowBeat);
		beatSoundPanel.add(lowBeatSelector);

		frmMetronome.getContentPane().add(beatSoundPanel);
		frmMetronome.getContentPane().add(tglbtnBeatSound);
		frmMetronome.getContentPane().add(btnTimerAdd);
		frmMetronome.getContentPane().add(btnTimerSub);
		frmMetronome.getContentPane().add(lblTimer);
		frmMetronome.getContentPane().add(volSlider);
		frmMetronome.getContentPane().add(beatsPanel);
		frmMetronome.getContentPane().add(btnPlay);
		frmMetronome.getContentPane().add(bpmLabel);
		frmMetronome.getContentPane().add(bpmAdd);
		frmMetronome.getContentPane().add(bpmSub);
		frmMetronome.getContentPane().add(bpmTap);
		frmMetronome.getContentPane().add(bpmSlider);
		frmMetronome.getContentPane().add(measureSlider);
		frmMetronome.getContentPane().add(lblVolume);
		frmMetronome.getContentPane().add(lblBeats);

		bpmAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setBpm(bpm + 1);
			}
		});
		bpmSub.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setBpm(bpm - 1);
			}
		});
		bpmTap.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				TapTempo.tap();
			}
		});
		btnPlay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (isPlaying) {
					pause();
				} else {
					play();
				}
			}
		});
		btnTimerAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isPlaying) pause();
				timerValue += 15;
				updateTimer(timerValue);
			}
		});
		btnTimerSub.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isPlaying) pause();

				if (timerValue != 0) {
					timerValue -= 15;
					updateTimer(timerValue);
				}
			}
		});

		tglbtnBeatSound.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isModifyingBeatSound = !isModifyingBeatSound;

				if (isModifyingBeatSound) {
					frmMetronome.setSize(700, 500);
					beatSoundPanel.setVisible(true);
					tglbtnBeatSound.setIcon(menuSelectedIcon);
				} else {
					frmMetronome.setSize(500, 500);
					beatSoundPanel.setVisible(false);
					tglbtnBeatSound.setIcon(menuIcon);
				}
			}
		});

		highBeatSelector.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				highSound = ClickSound.valueOf((String) highBeatSelector.getSelectedItem());
			}
		});

		lowBeatSelector.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				lowSound = ClickSound.valueOf((String) lowBeatSelector.getSelectedItem());
			}
		});

		bpmSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				frmMetronome.repaint();
				if (isPlaying) pause();

				if (bpmSlider.getValue() == 0) {
					setBpm(1);
				} else {
					setBpm(bpmSlider.getValue());
				}
			}
		});
		measureSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				if (isPlaying) pause();

				beatsPerMeasure = measureSlider.getValue();
				setNumBeats(measureSlider.getValue());
			}
		});
		volSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				frmMetronome.repaint();
				double volume = volSlider.getValue() / 10.0;
				AudioHandler.setVolume(volume);
				if (volume > 0.8) {
					lblVolume.setIcon(loudVolIcon);
				} else if (volume == 0.0) {
					lblVolume.setIcon(muteVolIcon);
				} else {
					lblVolume.setIcon(softVolIcon);
				}
			}
		});
		lblVolume.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				volSlider.setVisible(true);
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				volSlider.setVisible(false);
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
			}

		});
		lblBeats.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				measureSlider.setVisible(true);
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				measureSlider.setVisible(false);
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
			}

		});
	}

	public static class MySlider extends JSlider {
		private static final long serialVersionUID = 5864883254392379038L;

		public MySlider() {
			setFocusable(false);
			setUI(new MySliderUI(this));
			setBackground(frmMetronomeColour);
		}

		public void keepAliveOnHover() {
			addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent e) {
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					setVisible(true);
				}

				@Override
				public void mouseExited(MouseEvent e) {
					setVisible(false);
				}

				@Override
				public void mousePressed(MouseEvent e) {
				}

				@Override
				public void mouseReleased(MouseEvent e) {
				}

			});
			addMouseMotionListener(new MouseMotionListener() {
				@Override
				public void mouseMoved(MouseEvent e) {
				}

				@Override
				public void mouseDragged(MouseEvent e) {
					repaint();
				}
			});
		}
	}

	public static class MyButton extends JButton {
		private static final long serialVersionUID = -818352889910348393L;
		private final Color BTN_COLOUR = Color.WHITE;
		private final Color BTN_HOVER_COLOUR = Color.LIGHT_GRAY;

		public MyButton(String text) {
			super(text);
			init();
		}

		public MyButton() {
			init();
		}

		private void init() {
			setBorder(new MatteBorder(1, 1, 1, 1, (Color) Color.WHITE));
			setOpaque(false);
			setFocusable(false);
			setContentAreaFilled(false);
			addMouseListener(new MouseListener() {

				@Override
				public void mouseClicked(MouseEvent arg0) {
				}

				@Override
				public void mouseEntered(MouseEvent arg0) {
					setBorder(new MatteBorder(1, 1, 1, 1, BTN_HOVER_COLOUR));
				}

				@Override
				public void mouseExited(MouseEvent arg0) {
					setBorder(new MatteBorder(1, 1, 1, 1, BTN_COLOUR));
				}

				@Override
				public void mousePressed(MouseEvent arg0) {
				}

				@Override
				public void mouseReleased(MouseEvent arg0) {
				}

			});
		}
	}
	
	public static class MyComboBox extends JComboBox<String> {
		private static final long serialVersionUID = -2502900049691909854L;
		
		public MyComboBox() {
			setFocusable(false);
			setModel(new DefaultComboBoxModel<String>(ClickSound.getNames()));
			setBackground(frmMetronomeColour);
			setForeground(textColour);
			setRenderer(new DefaultListCellRenderer() {
				// https://stackoverflow.com/questions/64541587/lookandfeel-blocking-jcombobox-background-change

				private static final long serialVersionUID = 5382877703364914637L;
				
				@Override
				public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

					JComponent comp = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

					list.setBackground(frmMetronomeColour);
					list.setForeground(textColour);
					list.setOpaque(false);

					return comp;
				}
			});
		}
	}
}
