package org.openthinclient.console.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
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

	@Override
	public Dimension getPreferredSize() {
		return PREFERRED_SIZE;
	}

	@Override
	public Dimension getMinimumSize() {
		return PREFERRED_SIZE;
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

	}

//	public static void main(String[] args) {
//		SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
//				JFrame frame = new JFrame();
//				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//				frame.getContentPane().setLayout(new BorderLayout());
//				frame.getContentPane().add(new TitleComponent(),
//						BorderLayout.NORTH);
//				frame.pack();
//				frame.setVisible(true);
//			}
//		});
//
//	}

}
