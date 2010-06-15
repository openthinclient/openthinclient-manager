package org.openthinclient.console;

import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openide.windows.TopComponent;
import org.openthinclient.console.nodes.pkgmgr.PackageListNode;

public class StartAtTimeAction extends NodeAction {

	@Override
	protected void performAction(Node[] nodes) {
		boolean b = true;
		for (final Node node : nodes) {
			final Node node2 = node;
			while (b) {
				if (!node2.getClass().equals(PackageListNode.class))
					if (null != node2.getParentNode())
						b = false;

				final StartAtTimeDialog startAtTime = StartAtTimeDialog.getInstance();
				startAtTime.init(node, nodes, new TopComponent());
				startAtTime.doEdit();
			}
		}
		// final WakeAtTime wat = new WakeAtTime();
		// wat.start();
	}

	/*
	 * @see org.openide.util.actions.CallableSystemAction#asynchronous()
	 */
	@Override
	protected boolean asynchronous() {
		return true;
	}

	/*
	 * @see org.openide.util.actions.NodeAction#enable(org.openide.nodes.Node[])
	 */
	@Override
	protected boolean enable(Node[] arg0) {
		return true;
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	@Override
	public String getName() {
		return Messages.getString("action." + this.getClass().getSimpleName()); //$NON-NLS-1$
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getHelpCtx()
	 */
	@Override
	public HelpCtx getHelpCtx() {
		return null;
	}

}
