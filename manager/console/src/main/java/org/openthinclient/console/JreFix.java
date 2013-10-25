package org.openthinclient.console;

import java.awt.Component;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Utilty Class for fixing problems with the Jre 7u25 and 7u40. Based on the code from 
 * <a href="http://stackoverflow.com/questions/17275259/nullpointerexception-in-invokelater-while-running-through-java-webstart">http://stackoverflow.com/questions/17275259/nullpointerexception-in-invokelater-while-running-through-java-webstart</a>
 */
// suppressing restriction warnings as this class is explicitly designed to work around SUN/Oracle bugs in the JVM
@SuppressWarnings("restriction")
public class JreFix {
    private static String badVersionInfo = null;
	private static sun.awt.AppContext awtEventDispatchContext = null;
    private static sun.awt.AppContext mainThreadContext = null;
    private static Boolean isWebStart = null;

    public static class JvmVersion {
    	public static JvmVersion parse(String version) {
    		
    		if (version == null)
    			return null;
    		
    		Pattern p = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)_(\\d+)");
    		
    		Matcher m = p.matcher(version);
    		
    		if (m.find()) {
    			
    			// the first version fragment is not really significant, as on Sun/Oracle JVMs this is always 1.
    			
    			int generation = Integer.valueOf(m.group(1));
    			int major = Integer.valueOf(m.group(2));
    			int minor = Integer.valueOf(m.group(3));
    			int patch = Integer.valueOf(m.group(4));
    			
    			if (generation != 1)
    				// unknown type of version
    				return null;
    			
    			return new JvmVersion(major, minor, patch);
    		}
    		
    		// unknown version string
    		
    		return null;
    	}    	

		private final int major;
    	private final int minor;
    	private final int patch;
    	
    	public JvmVersion(int major, int minor, int patch) {
			super();
			this.major = major;
			this.minor = minor;
			this.patch = patch;
		}

    	public int getMajor() {
			return major;
		}
    	public int getMinor() {
			return minor;
		}
    	public int getPatch() {
			return patch;
		}
    }
    
    /**
     * Call this early in main().  
     */
    public static void init() {
        if (isWebstart() && isApplicableJvmType()) {
            String javaVersion = System.getProperty("java.version");

            JvmVersion parsedVersion = JvmVersion.parse(javaVersion);

            if (parsedVersion != null) {
            	int major = parsedVersion.getMajor();
            	int minor = parsedVersion.getMinor();
				int patch = parsedVersion.getPatch();
				// we suspect 7u25+ and 6u51 to be broken JVMs as Oracle stated that this bug will be fixed in Java 8, not before.
				if ((major == 7 && minor == 0 && patch >= 25) || (major == 6 && minor == 0 && patch >= 51)) {
            		badVersionInfo = major + "u" + patch;
            	}
            }
            
            if ("javaws-10.25.2.16".equals(System.getProperty("javawebstart.version"))) {
                badVersionInfo = "Web Start 10.25.2.16";
            }
        }

        if (badVersionInfo != null) {
        	System.err.println("You're running a broken JDK/JRE. Trying to apply runtime fix.");
            mainThreadContext = sun.awt.AppContext.getAppContext();
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                    	System.err.println("Checking EDT AppContext...");
                        awtEventDispatchContext = sun.awt.AppContext.getAppContext();
                        
                        if (awtEventDispatchContext == null) {
                        	System.err.println("JRE/JDK has no AppContext assigned to the EDT. Trying to fix this.");
							// if the EDT doesn't have a application context,
							// we're assigning the app context of the thread
							// group "main".
                        	fixCurrentThreadAppContext(null);
                        	
                        }
                        
                    }
                });
            }
            catch (Exception e) {
                displayErrorAndExit(null);
            }

            
            // we're walking up the ThreadGroup hierarchy to ensure that every ThreadGroup has an appropriate AppContext assigned
            ThreadGroup tg = Thread.currentThread().getThreadGroup();
            
            while (tg != null) {
            	
            	if (! hasAppContext(tg)) {
            		System.err.println("Detected ThreadGroup without AppContext: " + tg.getName() + ". Assigning a new AppContext");
            		fixThreadGroupAppContext(null, tg);
            	}
            	
            	tg = tg.getParent();
            	
            }
            
            if (mainThreadContext == null || awtEventDispatchContext == null) {
                 displayErrorAndExit(null);
            }
        }
    }

    private static boolean hasAppContext(ThreadGroup tg) {
    	
        try {
			final Field field = sun.awt.AppContext.class.getDeclaredField("threadGroup2appContext");
			field.setAccessible(true);
			
			@SuppressWarnings("unchecked")
			Map<ThreadGroup, sun.awt.AppContext> threadGroup2appContext = (Map<ThreadGroup, sun.awt.AppContext>)field.get(null);

			return threadGroup2appContext.get(tg) != null;
		} catch (Exception e) {
			// FIXME not sure if that is acutally the right way to return from this method in this case.
			return false;
		}
    	
	}

	public static void invokeNowOrLater(Runnable runnable) {
        if (hasAppContextBug()) {
            invokeLaterOnAwtEventDispatchThreadContext(runnable);
        }
        else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    public static void invokeNowOrWait(Runnable runnable) {
        if (hasAppContextBug()) {
            fixCurrentThreadAppContext(null);
        }

        try {
            SwingUtilities.invokeAndWait(runnable);
        } 
        catch (Exception e) {
            // handle it
        }
    }

    public static boolean hasAppContextBug() {
        return isJreWithAppContextBug() && sun.awt.AppContext.getAppContext() == null;
    }

    public static void invokeLaterOnAwtEventDispatchThreadContext(Runnable runnable) {
        sun.awt.SunToolkit.invokeLaterOnAppContext(awtEventDispatchContext, runnable);
    }

    public static void fixCurrentThreadAppContext(Component parent) {
    	fixThreadGroupAppContext(parent, Thread.currentThread().getThreadGroup());

        if (sun.awt.AppContext.getAppContext() == null) {
             displayErrorAndExit(parent);
        }
    }

	public static void fixThreadGroupAppContext(Component parent,
			final ThreadGroup currentThreadGroup) {
		try {
            final Field field = sun.awt.AppContext.class.getDeclaredField("threadGroup2appContext");
            field.setAccessible(true);
            
            @SuppressWarnings("unchecked")
			Map<ThreadGroup, sun.awt.AppContext> threadGroup2appContext = (Map<ThreadGroup, sun.awt.AppContext>)field.get(null);
            threadGroup2appContext.put(currentThreadGroup, mainThreadContext);
        } 
        catch (Exception e) {
            displayErrorAndExit(parent);
        }
	}

    private static boolean isJreWithAppContextBug() {
        return badVersionInfo != null;
    }

    private static void displayErrorAndExit(Component parent) {
        JLabel msgLabel = new JLabel("<html>" + 
                "Our application cannot run using <b>Web Start</b> with this version of Java.<p><p>" +
                "Java " + badVersionInfo + " contains a bug acknowledged by Oracle (JDK-8019274).");
        JOptionPane.showMessageDialog(parent, msgLabel, "Java Version Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private static boolean isApplicableJvmType() {
        String vendor = System.getProperty("java.vendor");
        String vmName = System.getProperty("java.vm.name");
        if (vendor != null && vmName != null) {
            return vmName.contains("Java HotSpot") &&
                    (vendor.equals("Oracle Corporation") || 
                     vendor.equals("Sun Microsystems Inc."));
        }

        return false;
    }

    private static boolean isWebstart() {
        if (isWebStart == null) {
            try { 
                ServiceManager.lookup("javax.jnlp.BasicService");             
                isWebStart = true;
            } 
            catch (UnavailableServiceException e) { 
                isWebStart = false;
            }           
        }
        return isWebStart;
    }
}