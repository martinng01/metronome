import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

//https://stackoverflow.com/questions/62609789/change-thumb-and-color-of-jslider

public class MySliderUI extends BasicSliderUI {
	private final int TRACK_HEIGHT = 3;
	private final int TRACK_WIDTH = 3;
	private final int TRACK_ARC = 5;
	private final Dimension THUMB_SIZE = new Dimension(15, 15);
	private final RoundRectangle2D.Float trackShape = new RoundRectangle2D.Float();
	private final Color THUMB_COLOUR = Color.decode("#94d2bd");
	private final Color TRACK_COLOUR = Color.LIGHT_GRAY;
	private final Color SELECTED_TRACK_COLOUR = THUMB_COLOUR.darker().darker();

	public MySliderUI(JSlider slider) {
		super(slider);
	}
	

	@Override
	protected void calculateTrackRect() {
		super.calculateTrackRect();
		if (isHorizontal()) {
			trackRect.y = trackRect.y + (trackRect.height - TRACK_HEIGHT) / 2;
			trackRect.height = TRACK_HEIGHT;
		} else {
			trackRect.x = trackRect.x + (trackRect.width - TRACK_WIDTH) / 2;
			trackRect.width = TRACK_WIDTH;
		}
		trackShape.setRoundRect(trackRect.x, trackRect.y, trackRect.width, trackRect.height, TRACK_ARC, TRACK_ARC);
	}

	@Override
	protected void calculateThumbLocation() {
		super.calculateThumbLocation();
		if (isHorizontal()) {
			thumbRect.y = trackRect.y + (trackRect.height - thumbRect.height) / 2;
		} else {
			thumbRect.x = trackRect.x + (trackRect.width - thumbRect.width) / 2;
		}
	}

	@Override
	protected Dimension getThumbSize() {
		return THUMB_SIZE;
	}

	private boolean isHorizontal() {
		return slider.getOrientation() == JSlider.HORIZONTAL;
	}

	@Override
	public void paint(final Graphics g, final JComponent c) {
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		super.paint(g, c);
	}

	@Override
	public void paintThumb(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		Rectangle t = thumbRect;

		g2d.setPaint(THUMB_COLOUR);
		g2d.fillOval(t.x, t.y, t.width, t.height);
	}

	@Override
	public void paintTrack(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		Shape clip = g2d.getClip();

		g2d.setPaint(TRACK_COLOUR);
		g2d.fill(trackShape);

		if (isHorizontal()) {
			int thumbPos = thumbRect.x + thumbRect.width / 2;
			g2d.clipRect(0, 0, thumbPos, slider.getHeight());
		} else {
			int thumbPos = thumbRect.y + thumbRect.height / 2;
			g2d.clipRect(0, thumbPos, slider.getWidth(), slider.getHeight() - thumbPos);
		}
        g2d.setColor(SELECTED_TRACK_COLOUR);
        g2d.fill(trackShape);
        g2d.setClip(clip);
	}
	
	@Override
	public void paintHorizontalLabel(Graphics g, int value, Component label) {
		Graphics2D g2d = (Graphics2D) g;
		
		int xPos = xPositionForValue(value);
		if (value >= 100) {
			xPos -= 10;
		} else if (value >= 10) {
			xPos -= 5;
		}
		
		g2d.setColor(THUMB_COLOUR.darker());
		g2d.drawString(Integer.toString(value), xPos, yPositionForValue(value) + 5);
	}
	
	@Override
	public void paintVerticalLabel(Graphics g, int value, Component label) {
		Graphics2D g2d = (Graphics2D) g;
		
		g2d.setColor(THUMB_COLOUR.darker());
		g2d.drawString(Integer.toString(value), xPositionForValue(value) - 8, yPositionForValue(value) + 5);
	}
}

