/*******************************************************************************
 * openthinclient.org ThinClient suite
 * 
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 ******************************************************************************/
package org.openthinclient.console;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Utilities;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class LdifActionProgressBar {
	private JProgressBar progressBar;
	private JDialog progressDialog = new JDialog(ConsoleFrame.getINSTANCE(), true);

	public LdifActionProgressBar() {
		this.progressBar = new JProgressBar(0, 100);
	}

	public void startProgress() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				loadDialog();
			}
		});
	}

	public void loadDialog() {
		final CellConstraints cc = new CellConstraints();
		Font f = UIManager.getFont("TitledBorder.font"); //$NON-NLS-1$
		f = f.deriveFont(Font.BOLD, AffineTransform.getScaleInstance(1.5, 1.5));

		progressDialog.setResizable(false);

		final JPanel panel = new JPanel(new FormLayout("f:p:g", "p,p,p"));
		panel.setPreferredSize(new Dimension(300, 100));
		progressDialog.setContentPane(panel);
		progressDialog.setIconImage(Utilities.loadImage(
				"org/openthinclient/console/icon.png", true));

		panel.setBorder(Borders.DIALOG_BORDER);

		final JLabel label = new JLabel(Messages.getString("pleasewait"));
		label.setFont(f);
		label.setForeground(new Color(50, 50, 150));
		panel.add(label, cc.xy(1, 1));
		final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		panel.setLocation(new Double(dim.getWidth()).intValue() / 2 - 150,
				new Double(dim.getHeight()).intValue() / 2 - 150);
		panel.setSize(new Dimension(300, 100));
		progressBar.setIndeterminate(true);
		panel.add(progressBar, cc.xy(1, 2));

		progressDialog.setContentPane(panel);
		progressDialog.pack();

		// center dialog box
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		progressDialog.setLocation(
				(screenSize.width - progressDialog.getWidth()) / 2,
				(screenSize.height - progressDialog.getHeight()) / 2);
		progressDialog.getSize();
		progressDialog.setIconImage(Utilities.loadImage(
				"org/openthinclient/console/icon.png", true));

		progressDialog.setVisible(true);
	}

	public void finished() {
		progressDialog.setVisible(false);
		progressDialog.dispose();
	}

	public void finished(String title, String text) {
		progressDialog.setVisible(false);
		progressDialog.dispose();
		final DialogDescriptor descriptor = new DialogDescriptor(text, title, true,
				new Object[]{DialogDescriptor.OK_OPTION}, DialogDescriptor.OK_OPTION,
				DialogDescriptor.DEFAULT_OPTION, null, new ActionListener() {
					public void actionPerformed(ActionEvent e) {
					}
				});
		descriptor.setClosingOptions(new Object[]{DialogDescriptor.OK_OPTION});
		final Dialog readyDialog = DialogDisplayer.getDefault().createDialog(
				descriptor);
		readyDialog.setIconImage(Utilities.loadImage(
				"org/openthinclient/console/icon.png", true));
		readyDialog.setVisible(true);
	}

}
