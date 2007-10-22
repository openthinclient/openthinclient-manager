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
 *******************************************************************************/
package org.openthinclient.console.nodes.pkgmgr;

import java.awt.Font;
import java.awt.geom.AffineTransform;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
//import javax.swing.table.JTableHeader;

import org.openide.nodes.Node;
import org.openide.windows.TopComponent;
import org.openthinclient.console.AbstractDetailView;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class PackageManagementView extends AbstractDetailView{
	private static PackageManagementView detailView;

	public static PackageManagementView getInstance() {

		if (null == detailView){
			detailView = new PackageManagementView();
		}
		return detailView;
	}

	private Node[] nodes;
	private TopComponent tc;
	public JComponent getMainComponent() {
		Font f = UIManager.getFont("TitledBorder.font"); //$NON-NLS-1$
		f = f.deriveFont(Font.BOLD, AffineTransform.getScaleInstance(1.5, 1.5));
		DefaultFormBuilder dfb = new DefaultFormBuilder(new FormLayout(
		"f:p:g"));
		for(Node node:nodes){
			PackageDetailView pdv=PackageDetailView.getInstance();
			pdv.init(node.getChildren().getNodes(), tc);
			JLabel jlb=new JLabel(node.getDisplayName()+":");
			jlb.setFont(f);
			dfb.append(jlb);
			JTable otherTable = new JTable();
			otherTable.setModel(new PackageListTableModel((PackageListNode)node,false,false));
			dfb.append(otherTable);
		}
		JScrollPane jsp=new JScrollPane(dfb.getPanel());
		return(jsp);
	}

	public void init(Node[] selection, TopComponent tc) {
		this.tc=tc;
		for(Node node:selection)
			if(node instanceof PackageManagementNode)
				nodes=node.getChildren().getNodes();
	}



}
