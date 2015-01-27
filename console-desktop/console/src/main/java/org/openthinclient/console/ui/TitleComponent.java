package org.openthinclient.console.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import com.levigo.util.log.Logger;
import com.levigo.util.log.LoggerFactory;

public class TitleComponent extends JComponent {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory
			.getLogger(TitleComponent.class);
	private static final BufferedImage TITLE_LOGO;
	private static final BufferedImage TITLE_BACKGROUND;
	private static final Dimension PREFERRED_SIZE;

	static {

		BufferedImage logo;
		BufferedImage backgroundSlice;

		try {
			logo = ImageIO.read(TitleComponent.class
					.getResource("otc_toplogo.png"));

			backgroundSlice = new BufferedImage(2, logo.getHeight(),
					logo.getType());

			// the first two columns of pixels will be our background image.
			Graphics2D g2d = backgroundSlice.createGraphics();
			g2d.drawImage(logo, null, 0, 0);
			g2d.dispose();
		} catch (Exception e) {
			LOG.error("Failed to load title logo.", e);
			logo = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			backgroundSlice = new BufferedImage(1, 1,
					BufferedImage.TYPE_INT_ARGB);
		}
		TITLE_LOGO = logo;
		PREFERRED_SIZE = new Dimension(TITLE_LOGO.getWidth(),
				TITLE_LOGO.getHeight());
		TITLE_BACKGROUND = backgroundSlice;
	}

	private String title;
	private transient GlyphVector preparedTitle;

	public TitleComponent() {
		// initializing some visual defaults
		setFont(new Font("dialog", Font.PLAIN, 20));
		setForeground(Color.WHITE);

	}

	public TitleComponent(String title) {
		this();
		this.title = title;
		preparedTitle = null;
	}

	@Override
	public void setFont(Font font) {
		super.setFont(font);
		preparedTitle = null;
	}

	@Override
	public Dimension getPreferredSize() {

		GlyphVector glv = getPreparedTitle();

		if (glv != null) {

			Rectangle2D visualBounds = glv.getVisualBounds();
			// adding a 10 pixel border to the end.
			int width = (int) (PREFERRED_SIZE.width + visualBounds.getX() + visualBounds
					.getWidth()) + 10;
			return new Dimension(width, PREFERRED_SIZE.height);

		} else {
			return PREFERRED_SIZE;
		}
	}

	@Override
	public Dimension getMinimumSize() {
		// the preferred size that we calculate will be the minimum size, as
		// there will be no additional space in the computation.
		return getPreferredSize();
	}

	public void setTitle(String title) {
		this.title = title;

	}

	public String getTitle() {
		return title;
	}

	protected GlyphVector getPreparedTitle() {
		if (preparedTitle == null && title != null
				&& title.trim().length() != 0) {

			preparedTitle = getFont().createGlyphVector(
					new FontRenderContext(new AffineTransform(), true, true),
					title);
		}
		return preparedTitle;
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2d = (Graphics2D) g;

		AffineTransform transform = g2d.getTransform();

		// render the background
		double scaleX = getSize().getWidth() / TITLE_BACKGROUND.getWidth();
		g2d.scale(scaleX, 1);
		g2d.drawImage(TITLE_BACKGROUND, null, 0, 0);

		// paint the actual foreground image
		// reset the transform to have an unmodified g2d
		g2d.setTransform(transform);
		// draw the title image
		g2d.drawImage(TITLE_LOGO, null, 0, 0);

		GlyphVector glv = getPreparedTitle();
		if (glv != null) {
			g2d.setColor(getForeground());

			Rectangle2D visualBounds = glv.getVisualBounds();

			final int x = TITLE_LOGO.getWidth();
			final float y = (float) ((TITLE_LOGO.getHeight() / 2) - visualBounds.getHeight()
					/ 2 - visualBounds.getY());
			g2d.drawGlyphVector(glv, x, y);
		}

	}
}
