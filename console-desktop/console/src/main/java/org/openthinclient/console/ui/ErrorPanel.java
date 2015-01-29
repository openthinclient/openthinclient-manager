package org.openthinclient.console.ui;

import java.awt.Color;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXHyperlink;
import org.openthinclient.console.ConsoleFrame;
import org.openthinclient.console.Messages;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.levigo.util.swing.IconManager;

public class ErrorPanel extends JPanel {
	private final String title;
	private final String subtitle;
	private final String detail;
	private final Throwable t;

	/**
	 * 
	 */
	private static final String BOTTOM_INDENT = "8dlu"; //$NON-NLS-1$
	/**
	 * 
	 */
	private static final String LEFT_INDENT = "10dlu"; //$NON-NLS-1$

	public ErrorPanel(String title, String subtitle, String detail, Throwable t) {
		this.title = title;
		this.subtitle = subtitle;
		this.detail = detail;
		this.t = t;

		final FormLayout layout = new FormLayout("pref, 3dlu, fill:default:grow", //$NON-NLS-1$
				"2dlu, top:default, 2dlu, default, 0dlu, fill:default:grow, 0dlu"); //$NON-NLS-1$
		setLayout(layout);
		setOpaque(false);

		final Color foreground = UIManager
				.getColor("OptionPane.errorDialog.border.background");

		final CellConstraints cc = new CellConstraints();

		// error icon
		final JLabel icon = new JLabel(IconManager.getInstance(ConsoleFrame.class,
				"icons").getIcon("tree.error"));
		add(icon, cc.xy(1, 2));

		if (null == subtitle && null != t && null != t.getLocalizedMessage())
			subtitle = t.getLocalizedMessage();

		final JLabel titleLabel = new JLabel("<html><b>" + title + "</b>"
				+ (null != subtitle ? "<p>" + subtitle : "") + "</html>");
		titleLabel.setForeground(foreground);
		add(titleLabel, cc.xy(3, 2));

		JLabel detailLabel = null;
		if (null != detail || null != t) {
			final StringBuilder sb = new StringBuilder();
			if (null != detail)
				sb.append(detail);
			if (null != t) {
				if (sb.length() > 0)
					sb.append("\n");
				final StringWriter sw = new StringWriter();
				t.printStackTrace(new PrintWriter(sw));
				sb.append(sw.toString());
			}

			String d = sb.toString();
			d = d.replaceAll("\n", "<br>\n");
			detailLabel = new JLabel("<html>" + d + "</html>");
			detailLabel.setForeground(foreground);

			if (UIFactory.DEBUG)
				setBorder(BorderFactory.createEtchedBorder());

			final FormLayout collapsibleLayout = new FormLayout(LEFT_INDENT
					+ ", fill:default:grow", "fill:default:grow, " + BOTTOM_INDENT); //$NON-NLS-1$ //$NON-NLS-2$
			final JXCollapsiblePane collapsible = new JXCollapsiblePane();
			collapsible.setAnimated(false);
			collapsible.getContentPane().setLayout(collapsibleLayout);
			collapsible.getContentPane().add(detailLabel, cc.xy(2, 1));
			collapsible.setOpaque(false);
			collapsible.setCollapsed(true);
			((JPanel) collapsible.getContentPane()).setOpaque(false);
			((JPanel) collapsible.getComponent(0)).setOpaque(false);

			if (UIFactory.DEBUG)
				((JPanel) collapsible.getContentPane()).setBorder(BorderFactory
						.createEtchedBorder());

			final Action toggleAction = collapsible.getActionMap().get(
					JXCollapsiblePane.TOGGLE_ACTION);
			// // use the collapse/expand icons from the JTree UI
			toggleAction.putValue(JXCollapsiblePane.COLLAPSE_ICON, UIManager
					.getIcon("Tree.expandedIcon")); //$NON-NLS-1$
			toggleAction.putValue(JXCollapsiblePane.EXPAND_ICON, UIManager
					.getIcon("Tree.collapsedIcon")); //$NON-NLS-1$

			final JXHyperlink link = new JXHyperlink(toggleAction);
			link.setText(Messages.getString("ErrorPanel.details"));
			link.setOpaque(false);
			link.setFocusPainted(false);

			link.setUnclickedColor(foreground);
			link.setClickedColor(foreground);

			add(link, cc.xy(3, 4));
			add(collapsible, cc.xy(3, 6));
		}
	}
}
