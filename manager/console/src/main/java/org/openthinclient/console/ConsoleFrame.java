package org.openthinclient.console;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.naming.NamingException;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXStatusBar;
import org.netbeans.core.startup.MainLookup;
import org.netbeans.core.startup.layers.ModuleLayeredFileSystem;
import org.openide.ErrorManager;
import org.openthinclient.console.ui.TitleComponent;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.Sizes;
import com.jgoodies.forms.util.LayoutStyle;
import com.levigo.util.messaging.DefaultMessageFactory;
import com.levigo.util.messaging.Message;
import com.levigo.util.messaging.MessageManager;
import com.levigo.util.messaging.dialog.DefaultDialogMessageListener;
import com.levigo.util.swing.SlickBevelBorder;
import com.levigo.util.swing.TitledPanel;
import com.levigo.util.swing.action.Context;
import com.levigo.util.swing.action.DefaultMenuComponentFactory;

/**
 * The main openthinclient.org application frame.
 */
public class ConsoleFrame extends JFrame {
	private static final Logger logger = Logger.getLogger(ConsoleFrame.class);

	private static final long serialVersionUID = 1L;

	public static final Preferences PREFERENCES_ROOT = Preferences.userRoot()
			.node("org.openthinclient/console");

	private final static Dimension DEFAULT_SIZE = new Dimension(1024, 768);

	private DetailViewTopComponent detailHolder;
	private DetailViewTopObject detailHolderObject;

	private JSplitPane splitPaneDetail;

	private JMenuBar menuBar;

	private JXStatusBar statusBar;

	private int statusBarCursor;

	protected Context context;

	protected static ConsoleFrame INSTANCE;

	/**
	 * Create new default MPA Frame.
	 * 
	 * @param args
	 * 
	 * @throws Exception
	 */
	public ConsoleFrame(String[] args) {
		init();

		setVisible(false);
		final ApplicationSplash lSplash = new ApplicationSplash(this, Toolkit
				.getDefaultToolkit()
				.getImage(this.getClass().getResource("splash.gif")));

		MainLookup.moduleClassLoadersUp();

		try {
			String nbHome = System.getProperty("netbeans.user"); // NOI18N
			if (nbHome == null) {
				nbHome = System.getProperty("user.home"); // NOI18N
				System.setProperty("netbeans.user", nbHome + File.separator
						+ ".openthinclient"); // NOI18N
			}
			final List<URL> urls = new ArrayList<URL>(1);
			urls.add(getClass().getResource("layer.xml"));
			ModuleLayeredFileSystem.getUserModuleLayer().addURLs(urls);

			setTitle(Messages.getString("ConsoleFrame.title")); //$NON-NLS-1$
			setDefaultCloseOperation(EXIT_ON_CLOSE);

			initGUI(args.length > 0 ? args[0] : null);

			lSplash.dispose();
		} catch (final Throwable e) {
			ErrorManager.getDefault().notify(e);
			System.exit(1);
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setVisible(true);
				ConsoleFrame.INSTANCE = ConsoleFrame.this;
			}
		});
	}

	/**
	 * Initialize the GUI. Create components yada, yada.
	 * 
	 * @param initialPage
	 */
	protected void initGUI(String initialPage) {
		initLAF();

		final MainTreeTopComponent mttc = MainTreeTopComponent.getDefault();
		mttc.requestActive();

		// must be after MainTreeTopComponent instantiation
		// else you will hit https://issues.openthinclient.org/otc/browse/SUITE-100
		UIDefaults.install(this);

		initMenuBar(context);
		setJMenuBar(menuBar);

		detailHolder = DetailViewTopComponent.getDefault();
		detailHolderObject = DetailViewTopObject.getDefault();

		detailHolder.componentOpened();

		splitPaneDetail = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
				new TitledPanel(detailHolder), new TitledPanel(detailHolderObject));
		splitPaneDetail.getComponent(1).setVisible(false);

		splitPaneDetail.setBorder(new EmptyBorder(2, 0, 2, 0));

		getContentPane().add(splitPaneDetail, BorderLayout.CENTER);

		final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				true, new TitledPanel(mttc), splitPaneDetail);
		splitPane.setBorder(new EmptyBorder(2, 0, 2, 0));

		splitPane.setDividerLocation(265);

		getContentPane().add(splitPane, BorderLayout.CENTER);

		// toolbar
		final JToolBar toolbar = DefaultMenuComponentFactory.getInstance(
				"/org/openthinclient/console/menucomponents.properties").getToolbar(
				"toolbar", context);
		toolbar.setBorder(new EmptyBorder(2, 2, 2, 2));
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new TitleComponent("Manager"));
		panel.add(toolbar, BorderLayout.SOUTH);
		getContentPane().add(panel, BorderLayout.NORTH);

		// status bar
		// statusBar = new JXStatusBar(); //$NON-NLS-1$ //$NON-NLS-2$
		// getContentPane().add(statusBar, BorderLayout.SOUTH);

		// go for it.
		// UIUtilities.centerOnScreen(this);
	}

	private void initLAF() {
		// UI-Settings for Panel title
		final int gap = LayoutStyle.getCurrent().getNarrowLinePad().getPixelSize(
				this);
		UIManager.put("TitledPanel.border", new LineBorder(UIManager
				.getColor("controlShadow")));
		// UIManager.put("TitledPanel.border", new CompoundBorder(
		// new ShadowPopupBorder(), new LineBorder(UIManager
		// .getColor("controlShadow"))));
		UIManager.put("TitledPanel.titleBarBorder", new CompoundBorder(
				new SlickBevelBorder(SlickBevelBorder.RAISED), new EmptyBorder(gap,
						gap, gap, gap)));

		UIManager.put("SplitPane.border", new EmptyBorder(2, 2, 2, 2));
		UIManager.put("SplitPaneDivider.border", new EmptyBorder(1, 1, 1, 1));
	}

	private void addStatusComponent(JComponent c) {
		final Box b = Box.createHorizontalBox();

		if (statusBarCursor > 0) {
			statusBar.add(Box.createHorizontalStrut(Sizes.DLUX2.getPixelSize(this)),
					statusBarCursor);
			statusBarCursor++;
		}

		b.setBorder(new CompoundBorder(SlickBevelBorder.getLoweredBevelBorder(),
				Borders.DLU2_BORDER));
		b.add(c);

		statusBar.add(b, statusBarCursor);
		statusBarCursor++;
	}

	/**
	 * Basic login - for testing purposes only.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		JreFix.init();
		
		try {
			Logger.getRootLogger().setLevel(Level.ALL);

			basicInitialization();
			
			new ConsoleFrame(args);
		} catch (final Throwable e) {
			ErrorManager.getDefault().notify(e);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	static void basicInitialization() throws NamingException {
		MessageManager.addMessageFactory(new DefaultMessageFactory(
				"org.openthinclient.console.Bundle")); //$NON-NLS-1$

		// listen to anything else but Message.DISPLAY by showing a
		// MessageDialog
		MessageManager.addMessageListener(DefaultDialogMessageListener
				.getInstance());
		DefaultDialogMessageListener.getInstance().setResizable(true);
		DefaultDialogMessageListener.getInstance().ignore(Message.DISPLAY);
		DefaultDialogMessageListener.getInstance().setCenterOnParent(true);
	}

	/**
	 * Create the application's menu bar.
	 * 
	 * @param context
	 */
	protected void initMenuBar(Context context) {
		menuBar = new JMenuBar();
		// "File" menu
		final DefaultMenuComponentFactory mcf = DefaultMenuComponentFactory
				.getInstance("/org/openthinclient/console/menucomponents.properties");
		menuBar.add(mcf.getMenu("file", context));
		menuBar.add(mcf.getMenu("realm", context));
	}

	protected void init() {
		setIconImage(Toolkit.getDefaultToolkit().getImage(
				this.getClass().getResource("icon.png")));
		initContext();
		setStartUpSize();
	}

	protected void initContext() {
		context = new Context(getRootPane(), Context.ACTIVE_CHILD);
		context.clear();
		context.add(this);
	}

	protected void setStartUpSize() {
		// ensure minimum size
		final Dimension mySize = getSize();
		if (mySize.width < DEFAULT_SIZE.width)
			mySize.width = DEFAULT_SIZE.width;
		if (mySize.height < DEFAULT_SIZE.height)
			mySize.height = DEFAULT_SIZE.height;
		setSize(mySize);

		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screenSize.width - mySize.width) / 2,
				(screenSize.height - mySize.height) / 2);
	}

	public static ConsoleFrame getINSTANCE() {
		return INSTANCE;
	}

	public void showObjectDetails(int length) {
		if (length < 10)
			splitPaneDetail.setDividerLocation(100 + 15 * length);
		else
			splitPaneDetail.setDividerLocation(0.33);
		splitPaneDetail.getComponent(1).setVisible(true);
	}

	public void hideObjectDetails() {
		splitPaneDetail.getComponent(1).setVisible(false);

	}
}
