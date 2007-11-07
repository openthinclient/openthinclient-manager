package org.openthinclient.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

/**
 * @author breitbach
 * 
 */
public class ApplicationSplash extends JWindow {

	private static final int MAX_SPLASH_VISIBILITY = 6000;

	/**
	 * Constructor with an image specified.
	 * 
	 * @param parent
	 * @param image
	 */
	public ApplicationSplash(Frame parent, Image image) {
		super(parent);

		// put it all together
		getContentPane().setLayout(new BorderLayout());

		getContentPane().setBackground(Color.darkGray);

		// image
		getContentPane().add(buildBody(image), BorderLayout.CENTER);
		pack();

		// If there is a parent, center above parent. Else center on screen.
		if (parent != null && parent.isVisible()) {
			setLocationRelativeTo(parent);
		} else {
			Dimension dScreen = Toolkit.getDefaultToolkit().getScreenSize();
			setLocation((int) (dScreen.getWidth() - getSize().getWidth()) / 2,
					(int) ((dScreen.getHeight() - getSize().getHeight()) / 2) - 30);
		}
		setVisible(true);
	}

	/**
	 * sets the window visible or not, depending on the fFlag
	 */
	@Override
	public void setVisible(boolean fFlag) {
		super.setVisible(fFlag);
		if (fFlag) {
			new Thread("Splash dispose timer") {
				/*
				 * @see java.lang.Thread#run()
				 */
				@Override
				public void run() {
					try {
						Thread.sleep(MAX_SPLASH_VISIBILITY);
					} catch (InterruptedException e) {
					}
					dispose();
				}
			}.start();
		}
	}

	/**
	 * Builds the body of the Panel.
	 * 
	 * @param image the image
	 * 
	 * @return the j panel
	 */
	public static JPanel buildBody(Image image) {
		JPanel lBody = new JPanel(new BorderLayout());
		lBody.add(new JLabel(new ImageIcon(image)), BorderLayout.NORTH);

		// version label
		// Font leviFont = new Font("SansSerif", Font.PLAIN, 11);
		// JLabel versionLabel = new JLabel("Version "
		// + ApplicationSplash.class.getPackage().getImplementationVersion());
		// versionLabel.setForeground(Color.black);
		// versionLabel.setFont(leviFont);
		// versionLabel.setBorder(new EmptyBorder(0, 5, 0, 0));
		// versionLabel.setBackground(Color.black);
		// lBody.add(versionLabel, BorderLayout.SOUTH);
		//
		// lBody.setBorder(BorderFactory.createLineBorder(
		// Color.black, 1));

		return lBody;
	}
}
