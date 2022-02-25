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
import java.awt.Cursor;
import java.awt.Dimension;

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
import javax.swing.Box;
import javax.swing.BoxLayout;

/**
 * @author martinng01
 */

public class Metronome {

	private static final int MIN_BPM = 0;
	private static final int MAX_BPM = 300;
	private static final int EXTENDED_BPM = 800;
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
	private static JToggleButton tglbtnBeatSound, tglbtnIndvBeats, tglbtnExtendBPM;
	private static MySlider bpmSlider, measureSlider, volSlider;
	private static JLabel bpmLabel, lblTimer, lblHighBeat, lblLowBeat, lblVolume, lblBeats;
	private static JPanel beatsPanel, extensionPanel, highLowBeatSoundPanel, indvBeatSoundPanel;
	private static MyComboBox highBeatSelector, lowBeatSelector;

	private static int bpm = DEFAULT_BPM;
	private static int maxBPM = MAX_BPM;
	private static int beatsPerMeasure = 4;
	private static int beatCount = 1;
	private static int timerValue = 0;
	private static boolean isPlaying = false;
	private static boolean isModifyingBeatSound = false;
	private static boolean isIndivBeats = false;
	private static List<JLabel> beatList = new ArrayList<JLabel>();
	private static List<MyComboBox> beatComboBoxList = new ArrayList<MyComboBox>();
	private static List<ClickSound> indvBeatSounds = new ArrayList<ClickSound>();
	private static ClickSound highSound = ClickSound.CLICKHIGH;
	private static ClickSound lowSound = ClickSound.CLICKLOW;
	private static ScheduledFuture<?> beepHandler, timerHandler;
	private static ScheduledExecutorService scheduler;
	private static PracticeTimer practiceTimer;

	public static class MetronomeBeep implements Runnable {
		@Override
		public void run() {
			if (!isIndivBeats) {
				if (beatCount == 1) {
					AudioHandler.play(highSound.getID());
				} else {
					AudioHandler.play(lowSound.getID());
				}
			} else {
				AudioHandler.play(indvBeatSounds.get(beatCount - 1).getID());
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

			/**
			 * Changes opacity of tap button per unit time, resets the memory of past taps
			 * if button is not pressed within 2 seconds
			 */
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
				reset();
			}

			public void restartCountdown() {
				transparency = 255;
			}

		}

		/**
		 * Calculates and sets new BPM based on delays of previous taps
		 */
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
		} else if (newBpm >= maxBPM) {
			bpm = maxBPM;
		} else {
			bpm = newBpm;
		}
		bpmLabel.setText(Integer.toString(bpm));
		bpmSlider.setValue(bpm);
	}

	/**
	 * Paints the beat circles on the screen. Adds the high/low ticks to list of
	 * individual beat sounds
	 */
	public static void setNumBeats(int beats) {
		beatsPerMeasure = beats;

		indvBeatSounds.clear();
		beatList.clear();
		beatsPanel.removeAll();
		beatsPanel.repaint();

		for (int i = 0; i < beats; i++) {
			int xPos = Math.round((500 - beats * 60) / (beats + 1)) * (i + 1) + i * 60;

			beatList.add(new JLabel());
			beatList.get(i).setBounds(xPos, 30, 60, 60);
			beatList.get(i).setOpaque(false);
			beatList.get(i).setIcon(offBeatIcon);
			beatsPanel.add(beatList.get(i));

			if (i == 0) {
				indvBeatSounds.add(highSound);
			} else {
				indvBeatSounds.add(lowSound);
			}
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

	/**
	 * Schedule a metronome tick at rate of BPM
	 */
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

	/**
	 * Adds the dropdown menus for the individual beat sounds
	 */
	public static void layoutBeatSounds(int numBeats) {
		indvBeatSoundPanel.removeAll();
		indvBeatSoundPanel.revalidate();
		indvBeatSoundPanel.repaint();

		indvBeatSoundPanel.add(Box.createVerticalGlue());
		for (int i = 0; i < numBeats; i++) {
			JLabel beatLabel = new JLabel(Integer.toString(i + 1));
			beatLabel.setForeground(textColour);
			beatLabel.setMaximumSize(new Dimension(180, 14));
			beatLabel.setPreferredSize(new Dimension(180, 14));

			MyComboBox comboBox = new MyComboBox();
			comboBox.setID(i);
			comboBox.setMaximumSize(new Dimension(180, 20));
			comboBox.setPreferredSize(new Dimension(180, 20));
			comboBox.setSize(180, 20);
			comboBox.setSelectedItem(indvBeatSounds.get(i).name());
			comboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					indvBeatSounds.set(comboBox.getID(), ClickSound.valueOf((String) comboBox.getSelectedItem()));
				}
			});

			beatComboBoxList.add(comboBox);

			indvBeatSoundPanel.add(beatLabel);
			indvBeatSoundPanel.add(Box.createVerticalStrut(3));
			indvBeatSoundPanel.add(comboBox);
			indvBeatSoundPanel.add(Box.createVerticalGlue());
		}
		indvBeatSoundPanel.add(Box.createVerticalStrut(30));
	}

	private static void initialize() {
		frmMetronome = new JFrame();
		frmMetronome.setTitle("Metronome");
		frmMetronome.setBounds(100, 100, 500, 500);
		frmMetronome.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmMetronome.getContentPane().setLayout(null);
		frmMetronome.getContentPane().setBackground(frmMetronomeColour);
		frmMetronome.setResizable(false);

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
		measureSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				if (isPlaying) pause();
				setNumBeats(measureSlider.getValue());
				layoutBeatSounds(beatsPerMeasure);
			}
		});

		bpmSlider = new MySlider();
		bpmSlider.setBounds(0, 416, 484, 45);
		bpmSlider.setPaintLabels(true);
		bpmSlider.setMinorTickSpacing(10);
		bpmSlider.setMajorTickSpacing(50);
		bpmSlider.setValue(DEFAULT_BPM);
		bpmSlider.setMaximum(maxBPM);
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

		bpmTap = new MyButton("Tap");
		bpmTap.setBounds(211, 371, 60, 34);
		bpmTap.setForeground(Color.WHITE);
		bpmTap.setFont(new Font("Tahoma", Font.BOLD, 11));
		bpmTap.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				TapTempo.tap();
			}
		});

		bpmSub = new MyButton();
		bpmSub.setBounds(150, 371, 51, 34);
		bpmSub.setFont(new Font("Tahoma", Font.PLAIN, 20));
		bpmSub.setIcon(subtractBPMIcon);
		bpmSub.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setBpm(bpm - 1);
			}
		});

		bpmAdd = new MyButton();
		bpmAdd.setBounds(281, 371, 51, 34);
		bpmAdd.setFont(new Font("Tahoma", Font.PLAIN, 20));
		bpmAdd.setIcon(plusBPMIcon);
		bpmAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setBpm(bpm + 1);
			}
		});

		bpmLabel = new JLabel(Integer.toString(bpm));
		bpmLabel.setBounds(142, 164, 200, 97);
		bpmLabel.setFont(new Font("Tahoma", Font.BOLD, 80));
		bpmLabel.setForeground(textColour);
		bpmLabel.setHorizontalAlignment(SwingConstants.CENTER);

		btnPlay = new MyButton();
		btnPlay.setIcon(playIcon);
		btnPlay.setFont(new Font("Tahoma", Font.BOLD, 23));
		btnPlay.setBounds(150, 306, 182, 54);
		btnPlay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (isPlaying) {
					pause();
				} else {
					play();
				}
			}
		});

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

		lblTimer = new JLabel("0:00");
		lblTimer.setHorizontalAlignment(SwingConstants.CENTER);
		lblTimer.setFont(new Font("Tahoma", Font.BOLD, 30));
		lblTimer.setForeground(textColour);
		lblTimer.setBounds(11, 190, 122, 54);

		btnTimerSub = new MyButton();
		btnTimerSub.setBounds(12, 241, 51, 29);
		btnTimerSub.setIcon(subtractTimerIcon);
		btnTimerSub.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isPlaying) pause();

				if (timerValue != 0) {
					timerValue -= 15;
					updateTimer(timerValue);
				}
			}
		});

		btnTimerAdd = new MyButton();
		btnTimerAdd.setBounds(81, 241, 51, 29);
		btnTimerAdd.setIcon(plusTimerIcon);
		btnTimerAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isPlaying) pause();
				timerValue += 15;
				updateTimer(timerValue);
			}
		});

		lblVolume = new JLabel();
		lblVolume.setBounds(379, 377, 32, 32);
		lblVolume.setIcon(softVolIcon);
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

		lblBeats = new JLabel();
		lblBeats.setBounds(428, 377, 32, 32);
		lblBeats.setIcon(beatsIcon);
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

		// Beat Sound Modifier Panel

		tglbtnBeatSound = new JToggleButton();
		tglbtnBeatSound.setBorderPainted(false);
		tglbtnBeatSound.setBounds(7, 10, 40, 34);
		tglbtnBeatSound.setOpaque(false);
		tglbtnBeatSound.setIcon(menuIcon);
		tglbtnBeatSound.setFocusable(false);
		tglbtnBeatSound.setContentAreaFilled(false);
		tglbtnBeatSound.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		tglbtnBeatSound.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isModifyingBeatSound = !isModifyingBeatSound;

				if (isModifyingBeatSound) {
					frmMetronome.setSize(700, 500);
					extensionPanel.setVisible(true);
					tglbtnBeatSound.setIcon(menuSelectedIcon);
				} else {
					frmMetronome.setSize(500, 500);
					extensionPanel.setVisible(false);
					tglbtnBeatSound.setIcon(menuIcon);
				}
			}
		});

		extensionPanel = new JPanel();
		extensionPanel.setBorder(new MatteBorder(0, 2, 0, 0, Color.LIGHT_GRAY));
		extensionPanel.setBackground(frmMetronomeColour);
		extensionPanel.setBounds(485, 0, 200, 461);
		extensionPanel.setLayout(null);
		extensionPanel.setVisible(false);

		highBeatSelector = new MyComboBox();
		highBeatSelector.setBounds(10, 70, 180, 20);
		highBeatSelector.setSelectedItem(highSound.name());
		highBeatSelector.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				highSound = ClickSound.valueOf((String) highBeatSelector.getSelectedItem());
			}
		});

		lblHighBeat = new JLabel("High beat");
		lblHighBeat.setHorizontalAlignment(SwingConstants.CENTER);
		lblHighBeat.setForeground(textColour);
		lblHighBeat.setBounds(10, 50, 180, 14);

		lblLowBeat = new JLabel("Low beat");
		lblLowBeat.setHorizontalAlignment(SwingConstants.CENTER);
		lblLowBeat.setForeground(textColour);
		lblLowBeat.setBounds(10, 200, 180, 14);

		lowBeatSelector = new MyComboBox();
		lowBeatSelector.setBounds(10, 220, 180, 20);
		lowBeatSelector.setSelectedItem(lowSound.name());
		lowBeatSelector.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				lowSound = ClickSound.valueOf((String) lowBeatSelector.getSelectedItem());
			}
		});

		indvBeatSoundPanel = new JPanel();
		indvBeatSoundPanel.setOpaque(false);
		indvBeatSoundPanel.setVisible(false);
		indvBeatSoundPanel.setBounds(0, 87, 200, 374);
		indvBeatSoundPanel.setLayout(new BoxLayout(indvBeatSoundPanel, BoxLayout.Y_AXIS));

		highLowBeatSoundPanel = new JPanel();
		highLowBeatSoundPanel.setOpaque(false);
		highLowBeatSoundPanel.setBounds(0, 87, 200, 374);
		highLowBeatSoundPanel.setLayout(null);

		tglbtnIndvBeats = new JToggleButton("Change individual beats");
		tglbtnIndvBeats.setBounds(10, 11, 180, 32);
		tglbtnIndvBeats.setBorder(new MatteBorder(2, 2, 2, 2, Color.LIGHT_GRAY));
		tglbtnIndvBeats.setOpaque(false);
		tglbtnIndvBeats.setFocusable(false);
		tglbtnIndvBeats.setContentAreaFilled(false);
		tglbtnIndvBeats.setForeground(textColour);
		tglbtnIndvBeats.setBackground(frmMetronomeColour.brighter().brighter().brighter());
		tglbtnIndvBeats.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		tglbtnIndvBeats.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				if (!isIndivBeats) {
					tglbtnIndvBeats.setBorder(new MatteBorder(2, 2, 2, 2, Color.GRAY));
				}
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				if (!isIndivBeats) {
					tglbtnIndvBeats.setBorder(new MatteBorder(2, 2, 2, 2, Color.LIGHT_GRAY));
				}
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
			}

		});
		tglbtnIndvBeats.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isIndivBeats = !isIndivBeats;

				if (isIndivBeats) {
					tglbtnIndvBeats.setBorder(new MatteBorder(2, 2, 2, 2, Color.GRAY));
					tglbtnIndvBeats.setOpaque(true);
					highLowBeatSoundPanel.setVisible(false);
					indvBeatSoundPanel.setVisible(true);

					layoutBeatSounds(beatsPerMeasure);
				} else {
					tglbtnIndvBeats.setOpaque(false);
					highLowBeatSoundPanel.setVisible(true);
					indvBeatSoundPanel.setVisible(false);
				}
			}
		});

		tglbtnExtendBPM = new JToggleButton("Extend BPM Range");
		tglbtnExtendBPM.setOpaque(false);
		tglbtnExtendBPM.setForeground(new Color(148, 210, 189));
		tglbtnExtendBPM.setFocusable(false);
		tglbtnExtendBPM.setContentAreaFilled(false);
		tglbtnExtendBPM.setBorder(new MatteBorder(2, 2, 2, 2, Color.LIGHT_GRAY));
		tglbtnExtendBPM.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		tglbtnExtendBPM.setBackground(frmMetronomeColour.brighter().brighter().brighter());
		tglbtnExtendBPM.setBounds(10, 54, 180, 32);
		tglbtnExtendBPM.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				if (maxBPM == MAX_BPM) {
					tglbtnExtendBPM.setBorder(new MatteBorder(2, 2, 2, 2, Color.GRAY));
				}
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				if (maxBPM == MAX_BPM) {
					tglbtnExtendBPM.setBorder(new MatteBorder(2, 2, 2, 2, Color.LIGHT_GRAY));
				}
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
			}

		});
		tglbtnExtendBPM.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isPlaying) pause();

				maxBPM = maxBPM == MAX_BPM ? EXTENDED_BPM : MAX_BPM;
				bpmSlider.setMaximum(maxBPM);

				if (maxBPM == MAX_BPM) {
					tglbtnExtendBPM.setOpaque(false);
				} else {
					tglbtnExtendBPM.setOpaque(true);
				}
			}
		});

		highLowBeatSoundPanel.add(lowBeatSelector);
		highLowBeatSoundPanel.add(highBeatSelector);
		highLowBeatSoundPanel.add(lblHighBeat);
		highLowBeatSoundPanel.add(lblLowBeat);

		extensionPanel.add(tglbtnIndvBeats);
		extensionPanel.add(tglbtnExtendBPM);
		extensionPanel.add(indvBeatSoundPanel);
		extensionPanel.add(highLowBeatSoundPanel);

		frmMetronome.getContentPane().add(extensionPanel);
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
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
		private int id = 0;

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

		public int getID() {
			return id;
		}

		public void setID(int id) {
			this.id = id;
		}
	}
}
