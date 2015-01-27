package org.openthinclient.console;

import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.apache.log4j.Logger;

import com.jgoodies.looks.Options;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;

/**
 * Helper class used to install a bunch of UI defaults.
 */
class UIDefaults {
	private static final Logger LOGGER = Logger.getLogger(UIDefaults.class);

	private UIDefaults() {
		// don't instantiate
	}

	/**
	 * Install the defaults.
	 */
	public static void install(Component c) {
		configureUI();
		SwingUtilities.updateComponentTreeUI(c);
	}

	private static void configureUI() {
		Options.setDefaultIconSize(new Dimension(18, 18));

		// Set font options
		UIManager.put(Options.USE_SYSTEM_FONTS_APP_KEY, Boolean.TRUE);
		// Options.setGlobalFontSizeHints(FontSizeHints.MIXED);
		// PlasticLookAndFeel.setFontPolicy(FontPolicies.getLooks1xWindowsPolicy());

		Options.setUseNarrowButtons(false);
		Options.setTabIconsEnabled(true);
		// ClearLookManager.setMode(ClearLookMode.OFF);
		// ClearLookManager.setPolicy(ClearLookManager.getPolicy().getClass()
		// .getName());

		// Swing Settings
		final LookAndFeel selectedLaf = new PlasticXPLookAndFeel();
		// LookAndFeel selectedLaf = new ExtWindowsLookAndFeel();
		// LookAndFeel selectedLaf = new PlasticLookAndFeel();
		// LookAndFeel selectedLaf = new Plastic3DLookAndFeel();

		if (selectedLaf instanceof PlasticLookAndFeel) {
			PlasticLookAndFeel.setPlasticTheme(PlasticLookAndFeel
					.createMyDefaultTheme());
			PlasticLookAndFeel
					.setTabStyle(PlasticLookAndFeel.TAB_STYLE_DEFAULT_VALUE);
			PlasticLookAndFeel.setHighContrastFocusColorsEnabled(false);

		} else if (selectedLaf.getClass() == MetalLookAndFeel.class)
			MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());

		// Workaround caching in MetalRadioButtonUI
		final JRadioButton radio = new JRadioButton();
		radio.getUI().uninstallUI(radio);
		final JCheckBox checkBox = new JCheckBox();
		checkBox.getUI().uninstallUI(checkBox);

		try {
			UIManager.setLookAndFeel(selectedLaf);
		} catch (final Throwable e) {
			LOGGER.error("Can't change L&F: ", e); //$NON-NLS-1$
		}

		// keep me up to date, if the LAF changes
		UIManager.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals("lookAndFeel"))
					overrideLAFDefaults();
			}
		});

		overrideLAFDefaults();
	}

	private static void overrideLAFDefaults() {
		UIManager.put("SplitPane.border", BorderFactory.createEmptyBorder());
		UIManager.put("SplitPaneDivider.border", BorderFactory.createEmptyBorder());
	}
}
