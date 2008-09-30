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
package org.openthinclient.console.ui;

import java.awt.Dialog;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.nodes.Node;
import org.openide.util.Utilities;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.console.DetailView;
import org.openthinclient.console.Messages;
import org.openthinclient.console.util.ChildValidator;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;
import com.jgoodies.validation.util.DefaultValidationResultModel;
import com.jgoodies.validation.view.ValidationResultViewFactory;

/**
 * @author bohnerne
 */
public class DirObjectEditPanel extends JPanel {

	private final ChildValidator validator = new ChildValidator();
	private final ValidationResultModel vrm = new DefaultValidationResultModel();

	public DirObjectEditPanel(DetailView detailView) {
		setLayout(new FormLayout("p:g", "p, 3dlu, f:p:g, 3dlu, p, 3dlu")); //$NON-NLS-1$ //$NON-NLS-2$
		final CellConstraints cc = new CellConstraints();

		final JComponent headerComponent = detailView.getHeaderComponent();
		if (null != headerComponent) {
			headerComponent.setBorder(BorderFactory
					.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
							getBackground().darker()), headerComponent.getBorder()));
			validator.addValidatorFrom(headerComponent);
			add(headerComponent, cc.xy(1, 1));
		}

		add(ValidationResultViewFactory.createReportIconAndTextPane(vrm), cc.xy(1,
				5));

		final JComponent mainComponent = detailView.getMainComponent();
		validator.addValidatorFrom(mainComponent);

		add(mainComponent, cc.xy(1, 3));

		setPreferredSize(new Dimension(800, 600));
	}

	/**
	 * @param node
	 * @param dirObject
	 * @return
	 * 
	 */
	public boolean doEdit(DirectoryObject dirObject, Node node) {
		final JButton okButton = new JButton(Messages.getString("OK")); //$NON-NLS-1$
		final JButton cancelButton = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$

		final String name = node.getDisplayName().replace("()", "").trim();
		final String simpleClassName = dirObject.getClass().getSimpleName();
		// String title =
		// MessageFormat.format(Messages.getString("DirObjectEditPanel.title"), new
		// Object[]{ //$NON-NLS-1$
		// Messages.getString("types.singular." + simpleClassName), //$NON-NLS-1$
		// dirObject.getName()});
		final String title = MessageFormat.format(Messages
				.getString("DirObjectEditPanel.title"), new Object[]{ //$NON-NLS-1$
				Messages.getString("types.singular." + simpleClassName), //$NON-NLS-1$
						name});

		final DialogDescriptor descriptor = new DialogDescriptor(this, title, true,
				new Object[]{okButton, cancelButton}, okButton,
				DialogDescriptor.BOTTOM_ALIGN, null, null);

		doValidate(okButton);

		final Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
		dialog.setIconImage(Utilities.loadImage(
				"org/openthinclient/console/icon.png", true));

		dialog.setSize(830, 600);

		final PropertyChangeListener pcl = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				doValidate(okButton);
			}
		};

		dirObject.addPropertyChangeListener(pcl);
		dialog.setVisible(true);
		dirObject.removePropertyChangeListener(pcl);

		return descriptor.getValue() == okButton;
	}

	/**
	 * @param okButton
	 */
	private void doValidate(final JButton okButton) {
		final ValidationResult validate = validator.validate();
		okButton.setEnabled(!validate.hasErrors());
		DirObjectEditPanel.this.revalidate();
		vrm.setResult(validate);
	}

}
