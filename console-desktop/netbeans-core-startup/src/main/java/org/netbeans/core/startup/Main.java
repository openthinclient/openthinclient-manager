/*
 * Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License Version 1.0
 * (the "License"). You may not use this file except in compliance with the
 * License. A copy of the License is available at http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original Code is
 * Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun Microsystems, Inc.
 * All Rights Reserved.
 */

package org.netbeans.core.startup;

import java.beans.Introspector;
import java.beans.PropertyEditorManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.Repository;
import org.openide.modules.Dependency;
import org.openide.modules.InstalledFileLocator;
import org.openide.modules.SpecificationVersion;
import org.openide.util.NbBundle;
import org.openide.util.SharedClassObject;
import org.openide.util.Utilities;

/**
 * Main class for NetBeans when run in GUI mode.
 */
public final class Main extends Object {
	/** module subsystem */
	private static ModuleSystem moduleSystem;
	/** module subsystem is fully ready */
	private static boolean moduleSystemInitialized;

	/** is there a splash screen or not */
	private static Splash.SplashOutput splash;

	/** is there progress bar in splash or not */
	private static final boolean noBar = Boolean
			.getBoolean("netbeans.splash.nobar");

	/**
	 * Defines a max value for splash progress bar.
	 */
	public static void setSplashMaxSteps(int maxSteps) {
		if (noBar || CLIOptions.noSplash || splash == null)
			return;
		splash.setMaxSteps(maxSteps);
	}

	/**
	 * Adds temporary steps to create a max value for splash progress bar later.
	 */
	public static void addToSplashMaxSteps(int steps) {
		if (noBar || CLIOptions.noSplash || splash == null)
			return;
		splash.addToMaxSteps(steps);
	}

	/**
	 * Adds temporary steps and creates a max value for splash progress bar.
	 */
	public static void addAndSetSplashMaxSteps(int steps) {
		if (noBar || CLIOptions.noSplash || splash == null)
			return;
		splash.addAndSetMaxSteps(steps);
	}

	/**
	 * Increments a current value of splash progress bar by one step.
	 */
	public static void incrementSplashProgressBar() {
		incrementSplashProgressBar(1);
	}

	/**
	 * Increments a current value of splash progress bar by given steps.
	 */
	public static void incrementSplashProgressBar(int steps) {
		if (noBar || CLIOptions.noSplash || splash == null)
			return;
		splash.increment(steps);
	}

	/**
	 * Prints the text to splash screen or to status line, if available.
	 */
	public static void setStatusText(String msg) {
		if (splash != null)
			splash.print(msg);
		if (moduleSystemInitialized)
			org.netbeans.core.startup.CoreBridge.conditionallyPrintStatus(msg);
	}

	/**
	 * Starts TopThreadGroup which properly overrides uncaughtException Further -
	 * new thread in the group execs main
	 */
	public static void main(String[] argv) throws Exception {
		final TopThreadGroup tg = new TopThreadGroup("IDE Main", argv); // NOI18N
		// -
		// programatic
		// name
		StartLog.logStart("Forwarding to topThreadGroup"); // NOI18N
		tg.start();
		StartLog.logProgress("Main.main finished"); // NOI18N
	}

	private static boolean nbFactoryInitialized;

	/** Initializes default stream factory */
	public static void initializeURLFactory() {
		if (!nbFactoryInitialized) {
			final java.net.URLStreamHandlerFactory fact = new NbURLStreamHandlerFactory();
			try {
				java.net.URL.setURLStreamHandlerFactory(fact);
			} catch (final Error e) {
				// Can happen if we try to start NB twice in the same VM.
				// Print the error but try to continue.
				e.printStackTrace();
			}
			nbFactoryInitialized = true;
		}
	}

	/**
	 * Sets up the custom font size and theme url for the plaf library to process.
	 */
	static void initUICustomizations() {
		if (!CLIOptions.isGui())
			return;

		URL themeURL = null;
		final boolean wantTheme = Boolean.getBoolean("netbeans.useTheme")
				|| CLIOptions.uiClass != null
				&& CLIOptions.uiClass.getName().indexOf("MetalLookAndFeel") >= 0;

		try {
			if (wantTheme) {
				// Put a couple things into UIDefaults for the plaf library to
				// process if it wants
				final FileObject fo = Repository.getDefault().getDefaultFileSystem()
						.findResource("themes.xml"); // NOI18N
				if (fo == null)
					// file in <home>/lib packed as
					// /org/netbeans/core/resources/themes.xml
					themeURL = Main.class.getResource("resources/themes.xml"); // NOI18N
				else
					try {
						themeURL = fo.getURL();
					} catch (final FileStateInvalidException fsie) {
						// do nothing
					}
			}
			// Bugfix #33546: If fontsize was not set from cammand line try to set
			// it from bundle key
			if (CLIOptions.uiFontSize == 0) {
				String key = "";
				try {
					key = NbBundle.getMessage(Main.class, "CTL_globalFontSize"); // NOI18N
				} catch (final MissingResourceException mre) {
					// Key not found, nothing to do
				}
				if (key.length() > 0)
					try {
						CLIOptions.uiFontSize = Integer.parseInt(key);
					} catch (final NumberFormatException exc) {
						// Incorrect value, nothing to do
					}
			}
		} finally {
			CoreBridge.getDefault().initializePlaf(CLIOptions.uiClass,
					CLIOptions.uiFontSize, themeURL);
		}
		if (CLIOptions.uiFontSize > 0
				&& "GTK".equals(UIManager.getLookAndFeel().getID()))
			ErrorManager.getDefault().log(ErrorManager.WARNING,
					NbBundle.getMessage(Main.class, "GTK_FONTSIZE_UNSUPPORTED")); // NOI18N
		StartLog.logProgress("Fonts updated"); // NOI18N
	}

	/** Get and initialize module subsystem. */
	public static ModuleSystem getModuleSystem() {
		synchronized (Main.class) {
			if (moduleSystem != null)
				return moduleSystem;

			StartLog.logStart("Modules initialization"); // NOI18N
			try {
				moduleSystem = new ModuleSystem(Repository.getDefault()
						.getDefaultFileSystem());
			} catch (final IOException ioe) {
				// System will be screwed up.
				final IllegalStateException ise = new IllegalStateException(
						"Module system cannot be created"); // NOI18N
				ErrorManager.getDefault().annotate(ise, ioe);
				throw ise;
			}
			StartLog.logProgress("ModuleSystem created"); // NOI18N
		}

		moduleSystem.loadBootModules();
		moduleSystem.readList();
		Main.addAndSetSplashMaxSteps(30); // additional steps after loading all
		// modules
		moduleSystem.restore();
		StartLog.logEnd("Modules initialization"); // NOI18N

		moduleSystemInitialized = true;

		return moduleSystem;
	}

	/**
	 * Is used to find out whether the system has already been initialized for
	 * the first time or not yet.
	 * 
	 * @return true if changes in the lookup shall mean real changes, false if
	 *         it just the first initalization
	 */
	public static boolean isInitialized() {
		return moduleSystemInitialized;
	}

	/**
	 * @exception SecurityException if it is called multiple times
	 */
	static void start(String[] args) throws SecurityException {
		StartLog.logEnd("Forwarding to topThreadGroup"); // NOI18N
		StartLog.logStart("Preparation"); // NOI18N

		// just setup some reasonable values for this deprecated property
		// 6.2 seems to be like the right version as that is the last one
		// that ever saw openide
		System.setProperty("org.openide.specification.version", "6.2"); // NOI18N
		System.setProperty("org.openide.version", "deprecated"); // NOI18N
		System.setProperty("org.openide.major.version", "IDE/1"); // NOI18N

		// Enforce JDK 1.4+ since we would not work without it.
		if (Dependency.JAVA_SPEC.compareTo(new SpecificationVersion("1.4")) < 0) { // NOI18N
			System.err.println("The IDE requires JDK 1.4 or higher to run."); // XXX
			// I18N?
			org.netbeans.TopSecurityManager.exit(1);
		}

		// In the past we derived ${jdk.home} from ${java.home} by appending
		// "/.." to the end of ${java.home} assuming that JRE is under JDK
		// directory. It does not always work. On MacOS X JDK and JRE files
		// are mixed together, thus ${jdk.home} == ${java.home}. In several
		// Linux distros JRE and JDK are installed at the same directory level
		// with ${jdk.home}/jre a symlink to ${java.home}, which means
		// ${java.home}/.. != ${jdk.home}.
		//
		// Now the launcher can set ${jdk.home} explicitly because it knows
		// best where the JDK is.

		String jdkHome = System.getProperty("jdk.home"); // NOI18N

		if (jdkHome == null) {
			jdkHome = System.getProperty("java.home"); // NOI18N

			if (Utilities.getOperatingSystem() != Utilities.OS_MAC)
				jdkHome += File.separator + ".."; // NOI18N

			System.setProperty("jdk.home", jdkHome); // NOI18N
		}

		// read environment properties from external file, if any
		try {
			readEnvMap();
		} catch (final Exception e) {
			e.printStackTrace();
		}

		// initialize the URL factory
		initializeURLFactory();

		if (System.getProperties().get("org.openide.TopManager") == null) { // NOI18N
			// this tells the system that we run in guy mode
			System.setProperty("org.openide.TopManager.GUI", "true"); // NOI18N
			// update the top manager to our main if it has not been provided yet
			System.getProperties().put(
			// Note that it is no longer actually a TopManager; historical relic:
					"org.openide.TopManager", // NOI18N
					"org.netbeans.core.NonGui" // NOI18N
			);
		}

		CLIOptions.initialize();
		StartLog.logProgress("Command line parsed"); // NOI18N

		// 5. initialize GUI
		StartLog.logStart("XML Factories"); // NOI18N

		// org.netbeans.core.startup.SAXFactoryImpl.install();
		// org.netbeans.core.startup.DOMFactoryImpl.install();
		// Bugfix #35919: Log message to console when initialization of local
		// graphics environment fails eg. due to incorrect value of $DISPLAY
		// on X Windows (Linux, Solaris). In such case IDE will not start
		// so we must inform user about error.

		if (CLIOptions.isGui())
			try {
				java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
			} catch (final java.lang.InternalError exc) {
				String s = NbBundle.getMessage(Main.class, "EXC_GraphicsStartFails1",
						exc.getMessage());
				System.out.println(s);
				s = NbBundle.getMessage(Main.class, "EXC_GraphicsStartFails2",
						CLIOptions.getUserDir() + "/var/log/messages.log");
				System.out.println(s);
				throw exc;
			}
		StartLog.logEnd("XML Factories"); // NOI18N

		org.netbeans.core.startup.InstalledFileLocatorImpl.prepareCache();

		// Initialize beans - [PENDING - better place for this ?]
		// [PENDING - can PropertyEditorManager garbage collect ?]
		final String[] sysbisp = Introspector.getBeanInfoSearchPath();
		final String[] nbbisp = new String[]{"org.netbeans.beaninfo", // NOI18N
		};
		final String[] allbisp = new String[sysbisp.length + nbbisp.length];
		System.arraycopy(nbbisp, 0, allbisp, 0, nbbisp.length);
		System.arraycopy(sysbisp, 0, allbisp, nbbisp.length, sysbisp.length);
		Introspector.setBeanInfoSearchPath(allbisp);

		// -----------------------------------------------------------------------------------------------------
		// 7. Initialize FileSystems
		assert Repository.getDefault() instanceof NbRepository : "Has to be NbRepository: "
				+ Repository.getDefault(); // NOI18N
		StartLog.logProgress("Repository initialized"); // NOI18N

		// -----------------------------------------------------------------------------------------------------
		// License check
		try {
			if (System.getProperty("netbeans.full.hack") == null
					&& System.getProperty("netbeans.close") == null)
				if (!handleLicenseCheck())
					org.netbeans.TopSecurityManager.exit(0);
		} catch (final Exception e) {
			ErrorManager.getDefault().notify(e);
		}
		StartLog.logProgress("License check performed"); // NOI18N

		// -----------------------------------------------------------------------------------------------------
		// Upgrade
		try {
			if (System.getProperty("netbeans.full.hack") == null
					&& System.getProperty("netbeans.close") == null)
				if (!handleImportOfUserDir())
					org.netbeans.TopSecurityManager.exit(0);
		} catch (final Exception e) {
			ErrorManager.getDefault().notify(e);
		}
		StartLog.logProgress("Upgrade wizard consulted"); // NOI18N

		//
		// 8.5 - we can show the splash only after the upgrade wizard finished
		//

		showSplash();

		// -----------------------------------------------------------------------------------------------------

		setStatusText(NbBundle.getMessage(Main.class, "MSG_IDEInit"));

		// -----------------------------------------------------------------------------------------------------
		// 9. Modules

		getModuleSystem();

		// property editors are registered in modules, so wait a while before
		// loading them
		registerPropertyEditors();

		// -----------------------------------------------------------------------------------------------------
		// this indirectly sets system properties for proxy servers with values
		// taken from IDESettings
		SharedClassObject.findObject(getKlass("org.netbeans.core.IDESettings"),
				true);
		StartLog.logProgress("IDE settings loaded"); // NOI18N

		{
			final java.util.Iterator it = org.openide.util.Lookup.getDefault()
					.lookup(
							new org.openide.util.Lookup.Template(
									org.netbeans.core.startup.RunLevel.class)).allInstances()
					.iterator();

			while (it.hasNext()) {
				final org.netbeans.core.startup.RunLevel level = (org.netbeans.core.startup.RunLevel) it
						.next();
				level.run();
			}
		}

		// finish starting
		if (splash != null) {
			Splash.hideSplash(splash);
			splash = null;
		}
		StartLog.logProgress("Splash hidden"); // NOI18N
		StartLog.logEnd("Preparation"); // NOI18N
	}

	/**
	 * Return splash screen.
	 */
	final static Splash.SplashOutput getSplash() {
		return splash;
	}

	/**
	 * This is a notification about hiding wizards during startup (Import,
	 * Setup). It makes splash screen visible again.
	 */
	protected static void showSplash() {
		if (!CLIOptions.noSplash) {
			if (splash != null) {
				if (Splash.isVisible(splash))
					return;
				splash = null;
			}
			splash = Splash.showSplash();
		}
	}

	/**
	 * Flag to avoid multiple adds of the same path to the of
	 * PropertyEditorManager if multiple tests call registerPropertyEditors()
	 */
	private static boolean editorsRegistered = false;

	/**
	 * Register NB specific property editors. Allows property editor unit tests
	 * to work correctly without initializing full NetBeans environment.
	 * 
	 * @since 1.98
	 */
	public static final void registerPropertyEditors() {
		// issue 31879
		if (editorsRegistered)
			return;
		final String[] syspesp = PropertyEditorManager.getEditorSearchPath();
		final String[] nbpesp = new String[]{"org.netbeans.beaninfo.editors", // NOI18N
				"org.openide.explorer.propertysheet.editors", // NOI18N
		};
		final String[] allpesp = new String[syspesp.length + nbpesp.length];
		System.arraycopy(nbpesp, 0, allpesp, 0, nbpesp.length);
		System.arraycopy(syspesp, 0, allpesp, nbpesp.length, syspesp.length);
		PropertyEditorManager.setEditorSearchPath(allpesp);
		PropertyEditorManager.registerEditor(java.lang.Character.TYPE,
				getKlass("org.netbeans.beaninfo.editors.CharEditor")); // NOI18N
		PropertyEditorManager.registerEditor(getKlass("[Ljava.lang.String;"),
				getKlass("org.netbeans.beaninfo.editors.StringArrayEditor")); // NOI18N
		// bugfix #28676, register editor for a property which type is array of
		// data objects
		PropertyEditorManager.registerEditor(
				getKlass("[Lorg.openide.loaders.DataObject;"),
				getKlass("org.netbeans.beaninfo.editors.DataObjectArrayEditor")); // NOI18N
		// use replacement hintable/internationalizable primitive editors -
		// issues 20376, 5278
		PropertyEditorManager.registerEditor(Integer.TYPE,
				getKlass("org.netbeans.beaninfo.editors.IntEditor"));
		PropertyEditorManager.registerEditor(Boolean.TYPE,
				getKlass("org.netbeans.beaninfo.editors.BoolEditor"));
		StartLog.logProgress("PropertyEditors registered"); // NOI18N
		editorsRegistered = true;
	}

	/** Lazily loads classes */
	// #9951
	static final Class getKlass(String cls) {
		try {
			ClassLoader loader;
			final ModuleSystem ms = moduleSystem;
			if (ms != null)
				loader = ms.getManager().getClassLoader();
			else
				loader = Main.class.getClassLoader();

			return Class.forName(cls, false, loader);
		} catch (final ClassNotFoundException e) {
			throw new NoClassDefFoundError(e.getLocalizedMessage());
		}
	}

	/**
	 * Puts a property into the system ones, but only if the value is not null.
	 * 
	 * @param propName name of property
	 * @param value value to assign or null
	 * @param failbackValue value to assign if the previous value is null
	 */
	private static void putSystemProperty(String propName, String value,
			String failbackValue) {
		if (System.getProperty(propName) == null)
			// only set it if not null
			if (value != null)
				System.setProperty(propName, value);
			else {
				if (!Boolean.getBoolean("netbeans.suppress.sysprop.warning")) {
					System.err.println("Warning: Versioning property \"" + propName + // NOI18N
							"\" is not set. Defaulting to \"" + failbackValue + '"' // NOI18N
					);
					System.err
							.println("(to suppress this message run with -Dnetbeans.suppress.sysprop.warning=true)"); // NOI18N
				}
				System.setProperty(propName, failbackValue);
			}
	}

	/**
	 * Does import of userdir. Made non-private just for testing purposes.
	 * 
	 * @return true if the execution should continue or false if it should stop
	 */
	static boolean handleImportOfUserDir() {
		class ImportHandler implements Runnable {
			private final File installed = new File(new File(CLIOptions.getUserDir(),
					"var"), "imported"); // NOI18N
			private String classname;
			private boolean executedOk;

			public boolean shouldDoAnImport() {
				classname = System.getProperty("netbeans.importclass"); // NOI18N

				return classname != null && !installed.exists();
			}

			public void run() {
				// This module is included in our distro somewhere... may or may
				// not be turned on.
				// Whatever - try running some classes from it anyway.
				try {
					final Class clazz = getKlass(classname);

					// Method showMethod = wizardClass.getMethod(
					// "handleUpgrade", new Class[] { Splash.SplashOutput.class
					// } ); // NOI18N
					final Method showMethod = clazz.getMethod("main",
							new Class[]{String[].class}); // NOI18N
					showMethod.invoke(null, new Object[]{new String[0]});
					executedOk = true;
				} catch (final java.lang.reflect.InvocationTargetException ex) {
					// canceled by user, all is fine
					if (ex.getTargetException() instanceof org.openide.util.UserCancelException)
						executedOk = true;
				} catch (final Exception e) {
					// If exceptions are thrown, notify them - something is
					// broken.
					e.printStackTrace();
				} catch (final LinkageError e) {
					// These too...
					e.printStackTrace();
				}
			}

			public boolean canContinue() {
				if (shouldDoAnImport())
					try {
						SwingUtilities.invokeAndWait(this);
						if (executedOk) {
							// if the import went fine, then we are fine
							// just create the file
							installed.getParentFile().mkdirs();
							installed.createNewFile();
							return true;
						} else
							return false;
					} catch (final IOException ex) {
						// file was not created a bit of problem but go on
						ex.printStackTrace();
						return true;
					} catch (final java.lang.reflect.InvocationTargetException ex) {
						return false;
					} catch (final InterruptedException ex) {
						ex.printStackTrace();
						return false;
					}
				else
					// if there is no need to upgrade that every thing is good
					return true;
			}
		}

		final ImportHandler handler = new ImportHandler();

		return handler.canContinue();
	}

	/**
	 * Displays license to user to accept if necessary. Made non-private just
	 * for testing purposes.
	 * 
	 * @return true if the execution should continue or false if it should stop
	 */
	static boolean handleLicenseCheck() {
		class LicenseHandler implements Runnable {
			private String classname;
			private boolean executedOk;

			/** Checks if licence was accepted already or not. */
			public boolean shouldDisplayLicense() {
				final File f = InstalledFileLocator.getDefault().locate(
						"var/license_accepted", null, false); // NOI18N
				if (f != null)
					return false;
				classname = System.getProperty("netbeans.accept_license_class"); // NOI18N
				return classname != null;
			}

			public void run() {
				final Class clazz = getKlass(classname);

				// This module is included in our distro somewhere... may or may
				// not be turned on.
				// Whatever - try running some classes from it anyway.
				try {
					final Method showMethod = clazz.getMethod("showLicensePanel", null); // NOI18N
					showMethod.invoke(null, null);
					executedOk = true;
					// User accepted license => create file marker in userdir
					final File f = new File(new File(CLIOptions.getUserDir(), "var"),
							"license_accepted"); // NOI18N
					if (!f.exists()) {
						f.getParentFile().mkdirs();
						try {
							f.createNewFile();
						} catch (final IOException exc) {
							exc.printStackTrace();
						}
					}
				} catch (final java.lang.reflect.InvocationTargetException ex) {
					// canceled by user, all is fine
					if (ex.getTargetException() instanceof org.openide.util.UserCancelException)
						executedOk = false;
					else
						ex.printStackTrace();
				} catch (final Exception ex) {
					// If exceptions are thrown, notify them - something is
					// broken.
					ex.printStackTrace();
				} catch (final LinkageError ex) {
					// These too...
					ex.printStackTrace();
				}
			}

			public boolean canContinue() {
				if (shouldDisplayLicense())
					try {
						SwingUtilities.invokeAndWait(this);
						if (executedOk)
							return true;
						else
							return false;
					} catch (final java.lang.reflect.InvocationTargetException ex) {
						return false;
					} catch (final InterruptedException ex) {
						ex.printStackTrace();
						return false;
					}
				else
					// if there is no need to upgrade that every thing is good
					return true;
			}
		}

		final LicenseHandler handler = new LicenseHandler();

		return handler.canContinue();
	}

	/**
	 * Reads system properties from a file on a disk and stores them in
	 * System.getPropeties ().
	 */
	private static void readEnvMap() throws IOException {
		final java.util.Properties env = System.getProperties();

		if (Dependency.JAVA_SPEC.compareTo(new SpecificationVersion("1.5")) >= 0)
			try {
				final java.lang.reflect.Method getenv = System.class.getMethod(
						"getenv", null);
				final Map m = (Map) getenv.invoke(null, null);
				for (final Iterator it = m.entrySet().iterator(); it.hasNext();) {
					final Map.Entry entry = (Map.Entry) it.next();
					final String key = (String) entry.getKey();
					final String value = (String) entry.getValue();

					env.put("Env-".concat(key), value); // NOI18N
					// E.g. on Turkish Unix, want env-display not
					// env-d\u0131splay:
					env.put("env-".concat(key.toLowerCase(Locale.US)), value); // NOI18N
				}
				return;
			} catch (final Exception e) {
				final IOException ioe = new IOException();
				ioe.initCause(e);
				throw ioe;
			}

		final String envfile = System.getProperty("netbeans.osenv"); // NOI18N
		if (envfile != null) {
			// XXX is any non-ASCII encoding even defined? unclear...
			final BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(envfile)));
			// #30621: use \0 when possible, \n as a fallback
			final char sep = Boolean.getBoolean("netbeans.osenv.nullsep")
					? '\0'
					: '\n';
			final StringBuffer key = new StringBuffer(100);
			final StringBuffer value = new StringBuffer(1000);
			boolean inkey = true;
			while (true) {
				final int c = in.read();
				if (c == -1)
					break;
				final char cc = (char) c;
				if (inkey) {
					if (cc == sep)
						throw new IOException("Environment variable name starting with '"
								+ key + "' contained the separator (char)" + (int) sep); // NOI18N
					else if (cc == '=')
						inkey = false;
					else
						key.append(cc);
				} else if (cc == sep) {
					// [pnejedly] These new String() calls are intentional
					// because of memory consumption. Don't touch them
					// unless you know what you're doing
					inkey = true;
					final String k = key.toString();
					final String v = new String(value.toString());
					env.put(new String("Env-" + k), v); // NOI18N
					// E.g. on Turkish Unix, want env-display not
					// env-d\u0131splay:
					env.put(new String("env-" + k.toLowerCase(Locale.US)), v); // NOI18N
					key.setLength(0);
					value.setLength(0);
				} else
					value.append(cc);
			}
		}
	}

}
