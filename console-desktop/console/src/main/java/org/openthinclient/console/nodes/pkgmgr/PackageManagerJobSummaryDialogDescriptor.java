package org.openthinclient.console.nodes.pkgmgr;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Utilities;
import org.openthinclient.console.ui.TitleComponent;

public class PackageManagerJobSummaryDialogDescriptor extends DialogDescriptor {

	public static void show(String title, List<String> warnings) {
		Dialog dialog = DialogDisplayer.getDefault().createDialog(
				new PackageManagerJobSummaryDialogDescriptor(title, warnings));
		final Dimension screenSize = Toolkit.getDefaultToolkit()
				.getScreenSize();
		dialog.setLocation((screenSize.width - dialog.getWidth()) / 2,
				(screenSize.height - dialog.getHeight()) / 2);

		dialog.setIconImage(Utilities.loadImage(
				"org/openthinclient/console/icon.png", true));

		dialog.setVisible(true);
	}
	
	public PackageManagerJobSummaryDialogDescriptor(String title,
			List<String> warnings) {
		super(createDialogBody(title, warnings), title, true,
				new Object[] { DialogDescriptor.OK_OPTION }, null,
				DialogDescriptor.BOTTOM_ALIGN, null, new ActionListener() {
					public void actionPerformed(ActionEvent e) {
					}
				});
	}

	protected static JPanel createDialogBody(String title, List<String> warnings) {
		JPanel body = new JPanel();

		body.setLayout(new BorderLayout());
		body.add(new TitleComponent(title), BorderLayout.NORTH);

		// creating the actual content panel with the details
		final JPanel summaryPanel = new JPanel();
		summaryPanel.setLayout(new BorderLayout(10, 10));
		summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		summaryPanel.add(new JLabel("Summary: " + warnings.size()
				+ " warning(s)"), BorderLayout.NORTH);
		JScrollPane scrollPane = new JScrollPane(new JList(warnings.toArray()));
		scrollPane.setPreferredSize(new Dimension(200, 200));
		scrollPane.setMinimumSize(scrollPane.getPreferredSize());
		summaryPanel.add(scrollPane);

		body.add(summaryPanel);
		return body;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				PackageManagerJobSummaryDialogDescriptor descriptor = new PackageManagerJobSummaryDialogDescriptor(
						"Update Packages", Arrays.<String> asList("Warning 1",
								"Warning 2", "Warning 3"));

				Dialog dialog = DialogDisplayer.getDefault().createDialog(
						descriptor);
				final Dimension screenSize = Toolkit.getDefaultToolkit()
						.getScreenSize();
				dialog.setLocation((screenSize.width - dialog.getWidth()) / 2,
						(screenSize.height - dialog.getHeight()) / 2);

				dialog.setIconImage(Utilities.loadImage(
						"org/openthinclient/console/icon.png", true));

				dialog.setVisible(true);
				
			}
		});
	}

	public static void main3(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				String packageName = "My Package!";
				String licenseText = "Some strange license!";

				final JTextArea jta = new JTextArea(licenseText);
				jta.setFocusable(false);
				jta.setLineWrap(true);
				jta.setWrapStyleWord(true);

				final JScrollPane scrollPane = new JScrollPane(jta);

				final JButton okButton = new JButton("I Accept");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
					}
				});

				final DialogDescriptor descriptor = new DialogDescriptor(
						scrollPane, "License Agreement for Package "
								+ packageName, true, new Object[] {
								DialogDescriptor.CANCEL_OPTION, okButton },
						null, DialogDescriptor.BOTTOM_ALIGN, null,
						new ActionListener() {
							public void actionPerformed(ActionEvent e) {
							}
						});
				descriptor.setClosingOptions(new Object[] { okButton,
						DialogDescriptor.CANCEL_OPTION });

				final Dialog licenseDialog = DialogDisplayer.getDefault()
						.createDialog(descriptor);
				licenseDialog.setPreferredSize(new Dimension(640, 480));
				licenseDialog.setMinimumSize(new Dimension(640, 480));
				licenseDialog.pack();

				final Dimension screenSize = Toolkit.getDefaultToolkit()
						.getScreenSize();
				licenseDialog.setLocation(
						(screenSize.width - licenseDialog.getWidth()) / 2,
						(screenSize.height - licenseDialog.getHeight()) / 2);

				licenseDialog.setIconImage(Utilities.loadImage(
						"org/openthinclient/console/icon.png", true));

				licenseDialog.setVisible(true);

				// return descriptor.getValue() == okButton;
			}
		});
	}

}
