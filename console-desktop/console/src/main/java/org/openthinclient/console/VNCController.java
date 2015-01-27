package org.openthinclient.console;

import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.glavsoft.rfb.protocol.ProtocolSettings;
import com.glavsoft.viewer.ConnectionPresenter;
import com.glavsoft.viewer.UiSettings;
import com.glavsoft.viewer.Viewer;
import com.glavsoft.viewer.cli.Parser;
import com.glavsoft.viewer.mvp.View;
import com.glavsoft.viewer.swing.ConnectionParams;
import com.glavsoft.viewer.swing.ParametersHandler;
import com.glavsoft.viewer.swing.SwingConnectionWorkerFactory;
import com.glavsoft.viewer.swing.SwingViewerWindowFactory;
import com.glavsoft.viewer.swing.gui.ConnectionView;

@SuppressWarnings("serial")
public class VNCController extends Viewer implements WindowListener {

	private Logger logger;
	private int paramsMask;
	private boolean allowAppletInteractiveConnections;

	private final ConnectionParams connectionParams;
	private String passwordFromParams;
	boolean isSeparateFrame = true;
	boolean isApplet = true;
	private final ProtocolSettings settings;
	private final UiSettings uiSettings;
	private volatile boolean isAppletStopped = false;
	private ConnectionPresenter connectionPresenter;

	public static void openConnection(String[] args) {
		Parser parser = new Parser();
		ParametersHandler.completeParserOptions(parser);

		parser.parse(args);
		
		VNCController viewer = new VNCController(parser);
		SwingUtilities.invokeLater(viewer);
	}

	public VNCController() {
		logger = Logger.getLogger(getClass().getName());
		connectionParams = new ConnectionParams();
		settings = ProtocolSettings.getDefaultSettings();
		uiSettings = new UiSettings();
	}

	private VNCController(Parser parser) {
		this();
		setLoggingLevel(parser.isSet(ParametersHandler.ARG_VERBOSE) ? Level.FINE
				: parser.isSet(ParametersHandler.ARG_VERBOSE_MORE) ? Level.FINER
						: Level.INFO);

		paramsMask = ParametersHandler.completeSettingsFromCLI(parser,
				connectionParams, settings, uiSettings);
		passwordFromParams = parser.getValueFor(ParametersHandler.ARG_PASSWORD);
		logger.info("TightVNC Viewer version " + ver());
		isApplet = false;
	}

	private void setLoggingLevel(Level levelToSet) {
		final Logger appLogger = Logger.getLogger("com.glavsoft");
		appLogger.setLevel(levelToSet);
		ConsoleHandler ch = null;
		for (Handler h : appLogger.getHandlers()) {
			if (h instanceof ConsoleHandler) {
				ch = (ConsoleHandler) h;
				break;
			}
		}
		if (null == ch) {
			ch = new ConsoleHandler();
			appLogger.addHandler(ch);
		}
		// ch.setFormatter(new SimpleFormatter());
		ch.setLevel(levelToSet);
	}

	@Override
	public void windowClosing(WindowEvent e) {
		if (e != null && e.getComponent() != null) {
			final Window w = e.getWindow();
			if (w != null) {
				w.setVisible(false);
				w.dispose();
			}
		}
		closeApp();
	}

	/**
	 * Closes App(lication) or stops App(let).
	 */
	public void closeApp() {
		if (connectionPresenter != null) {
			connectionPresenter.cancelConnection();
			logger.info("Connections cancelled.");
		}
		if (isApplet) {
			if (!isAppletStopped) {
				logger.severe("Applet is stopped.");
				isAppletStopped = true;
				repaint();
				stop();
			}
		}
	}

	@Override
	public void paint(Graphics g) {
		if (!isAppletStopped) {
			super.paint(g);
		} else {
			getContentPane().removeAll();
			g.clearRect(0, 0, getWidth(), getHeight());
			g.drawString("Disconnected", 10, 20);
		}
	}

	@Override
	public void destroy() {
		this.closeApp();

	}

	@Override
	public void init() {
		paramsMask = ParametersHandler.completeSettingsFromApplet(this,
				connectionParams, settings, uiSettings);
		isSeparateFrame = ParametersHandler.isSeparateFrame;
		passwordFromParams = getParameter(ParametersHandler.ARG_PASSWORD);
		isApplet = true;
		allowAppletInteractiveConnections = ParametersHandler.allowAppletInteractiveConnections;
		repaint();

		try {
			SwingUtilities.invokeAndWait(this);
		} catch (Exception e) {
			logger.severe(e.getMessage());
		}
	}

	private boolean checkJsch() {
		try {
			Class.forName("com.jcraft.jsch.JSch");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	@Override
	public void run() {

		final boolean hasJsch = checkJsch();
		final boolean allowInteractive = allowAppletInteractiveConnections
				|| !isApplet;
		connectionPresenter = new ConnectionPresenter(hasJsch, allowInteractive);
		connectionPresenter.addModel("ConnectionParamsModel", connectionParams);
		final ConnectionView connectionView = new ConnectionView(
				VNCController.this, // appWindowListener
				connectionPresenter, hasJsch);
		connectionPresenter.addView(ConnectionPresenter.CONNECTION_VIEW,
				connectionView);
		if (isApplet) {
			connectionPresenter.addView("AppletStatusStringView", new View() {
				public void showView() { /* nop */
				}

				public void closeView() { /* nop */
				}

				@SuppressWarnings("UnusedDeclaration")
				public void setMessage(String message) {
					VNCController.this.getAppletContext().showStatus(message);
				}
			});
		}

		// we're telling the SwingWindowFactory to behave as if it would be an
		// applet. This will ensure that the application is not going to be
		// closed after closing the window.
		SwingViewerWindowFactory viewerWindowFactory = new SwingViewerWindowFactory(
				isSeparateFrame, true, this);

		connectionPresenter
				.setConnectionWorkerFactory(new SwingConnectionWorkerFactory(
						connectionView.getFrame(), passwordFromParams,
						connectionPresenter, viewerWindowFactory));

		connectionPresenter.startConnection(settings, uiSettings, paramsMask);
	}

}
