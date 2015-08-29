/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2003 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.JarFile;
import java.security.*;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Template;
import org.openide.util.lookup.Lookups;

/** Bootstrap main class.
 * @author Jaroslav Tulach, Jesse Glick
 */
public class Main extends Object {
    
    /** Starts the IDE.
     * @param args the command line arguments
     * @throws Exception for lots of reasons
     */
    public static void main (String args[]) throws Exception {
        java.lang.reflect.Method[] m = new java.lang.reflect.Method[1];
        int res = execute (args, System.in, System.out, System.err, m);
        if (res == -1) {
            // Connected to another running NB instance and succeeded in making a call.
            return;
        } else if (res != 0) {
            // Some CLIHandler refused the invocation
            System.exit(res);
        }

        m[0].invoke (null, new Object[] { args });
    }
    
    /** Returns string describing usage of the system. Does that by talking to
     * all registered handlers and asking them to show their usage.
     *
     * @return the usage string for the system
     */
    public static String usage () throws Exception {
        java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream ();
        java.io.ByteArrayOutputStream err = new java.io.ByteArrayOutputStream ();
        
        String[] newArgs = { "--help" };
        
        int res = execute (newArgs, System.in, os, err, null);
        return new String (os.toByteArray ());
    }
        
    /** Constructs the correct ClassLoader, finds main method to execute 
     * and invokes all registered CLIHandlers.
     *
     * @param args the arguments to pass to the handlers
     * @param reader the input stream reader for the handlers
     * @param writer the output stream for the handlers
     * @param methodToCall null or array with one item that will be set to 
     *   a method that shall be executed as the main application
     */
    static int execute (
        String[] args, 
        java.io.InputStream reader, 
        java.io.OutputStream writer,
        java.io.OutputStream error,         
        java.lang.reflect.Method[] methodToCall
    ) throws Exception {     
        // #42431: turn off jar: caches, they are evil
        // Note that setDefaultUseCaches changes a static field
        // yet for some reason it is an instance method!
        new URLConnection(Main.class.getResource("Main.class")) { // NOI18N
            public void connect() throws IOException {}
        }.setDefaultUseCaches(false);
        
        ArrayList list = new ArrayList ();

        HashSet processedDirs = new HashSet ();
        String home = System.getProperty ("netbeans.home"); // NOI18N
        if (home != null) {
            build_cp (new File (home), list, processedDirs);
        }
        // #34069: need to do the same for nbdirs.
        String nbdirs = System.getProperty("netbeans.dirs"); // NOI18N
        if (nbdirs != null) {
            StringTokenizer tok = new StringTokenizer(nbdirs, File.pathSeparator);
            while (tok.hasMoreTokens()) {
                // passing false as last argument as we need to initialize openfile-cli.jar
                build_cp(new File(tok.nextToken()), list, processedDirs);
            }
        }
        
        //
        // prepend classpath
        //
        String prepend = System.getProperty("netbeans.classpath"); // NOI18N
        if (prepend != null) {
            StringTokenizer tok = new StringTokenizer (prepend, File.pathSeparator);
            while (tok.hasMoreElements()) {
                list.add (0, new File (tok.nextToken()));
            }
        }

        // Compute effective dynamic classpath (mostly lib/*.jar) for TopLogging, NbInstaller:
        StringBuffer buf = new StringBuffer(1000);
        Iterator it = list.iterator();
        while (it.hasNext()) {
            if (buf.length() > 0) {
                buf.append(File.pathSeparatorChar);
            }
            buf.append(((File)it.next()).getAbsolutePath());
        }
        System.setProperty("netbeans.dynamic.classpath", buf.toString());
        
        // JarClassLoader treats a File as a dir; for a ZIP/JAR, needs JarFile
        ListIterator it2 = list.listIterator();
        while (it2.hasNext()) {
            File f = (File)it2.next();
            if (f.isFile()) {
                it2.set(new JarFile (f, false));
            }
        }
        
        BootClassLoader loader = new BootClassLoader(list, new ClassLoader[] {
            Main.class.getClassLoader()
        });
        
        // Needed for Lookup.getDefault to find NbTopManager.Lkp.
        // Note that ModuleManager.updateContextClassLoaders will later change
        // the loader on this and other threads to be MM.SystemClassLoader anyway.
        if(!Boolean.getBoolean("netbeans.use-app-classloader")) 
          Thread.currentThread().setContextClassLoader (loader);
        
        
        //
        // Evaluate command line interfaces and lock the user directory
        //
        
        CLIHandler.Status result;
        result = CLIHandler.initialize(args, reader, writer, error, loader, true, false, loader);
        if (result.getExitCode () == CLIHandler.Status.CANNOT_CONNECT) {
            int value = javax.swing.JOptionPane.showConfirmDialog (
                null, 
                java.util.ResourceBundle.getBundle("org/netbeans/Bundle").getString("MSG_AlreadyRunning"), 
                java.util.ResourceBundle.getBundle("org/netbeans/Bundle").getString("MSG_AlreadyRunningTitle"), 
                javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            if (value == javax.swing.JOptionPane.OK_OPTION) {
                result = CLIHandler.initialize(args, reader, writer, error, loader, true, true, loader);
            }
            
        }
        
        
        String className = System.getProperty(
            "netbeans.mainclass", "org.netbeans.core.startup.Main" // NOI18N
        );
        
        Class c = loader.loadClass(className);
        Method m = c.getMethod ("main", new Class[] { String[].class }); // NOI18N
        
        if (methodToCall != null) {
            methodToCall[0] = m;
        }

        return result.getExitCode ();
    }
    
    /**
     * Call when the system is up and running, to complete handling of
     * delayed command-line options like -open FILE.
     */
    public static void finishInitialization() {
        int r = CLIHandler.finishInitialization (false);
        if (r != 0) {
            // Not much to do about it.
            System.err.println ("Post-initialization command-line options could not be run."); // NOI18N
            //System.err.println("r=" + r + " args=" + java.util.Arrays.asList(args.getArguments()));
        }
    }
    
    static final class BootClassLoader extends JarClassLoader 
    implements Runnable {
        private Lookup metaInf;

        private List handlers;
        
        public BootClassLoader(List cp, ClassLoader[] parents) {
            super(cp, parents);
            
            metaInf = Lookups.metaInfServices(this);
    
            String value = null;
            try {
                if (cp.isEmpty ()) {
                    value = searchBuildNumber(this.getResources("META-INF/MANIFEST.MF"));
                } else { 
                    value = searchBuildNumber(this.simpleFindResources("META-INF/MANIFEST.MF"));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            
            if (value == null) {
                System.err.println("Cannot set netbeans.buildnumber property no OpenIDE-Module-Implementation-Version found"); // NOI18N
            } else {
                System.setProperty ("netbeans.buildnumber", value); // NOI18N
            }
        }
        
        /** @param en enumeration of URLs */
        private static String searchBuildNumber(Enumeration en) {
            String value = null;
            try {
                java.util.jar.Manifest mf;
                URL u = null;
                while(en.hasMoreElements()) {
                    u = (URL)en.nextElement();
                    InputStream is = u.openStream();
                    mf = new java.util.jar.Manifest(is);
                    is.close();
                    value = mf.getMainAttributes().getValue("OpenIDE-Module-Implementation-Version"); // NOI18N
                    if (value != null) {
                        break;
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return value;
        }

        private boolean onlyRunRunOnce;
        /** Checks for new JARs in netbeans.user */
        public void run () {
            // do not call this method twice
            if (onlyRunRunOnce) return;
            onlyRunRunOnce = true;
            
            ArrayList toAdd = new ArrayList ();
            String user = System.getProperty ("netbeans.user"); // NOI18N
            try {
                if (user != null) {
                    build_cp (new File (user), toAdd, new HashSet ());
                    // JarClassLoader treats a File as a dir; for a ZIP/JAR, needs JarFile
                    ListIterator it2 = toAdd.listIterator();
                    while (it2.hasNext()) {
                        File f = (File)it2.next();
                        if (f.isFile()) {
                            it2.set(new JarFile (f, false));
                        }
                    }
                }
                
                if (!toAdd.isEmpty ()) {
                    addSources (toAdd);
                    metaInf = Lookups.metaInfServices(this);
                    if (handlers != null) {
                        handlers.clear();
                        handlers.addAll(metaInf.lookup(new Lookup.Template(CLIHandler.class)).allInstances());
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
        
        /** Startup optimalization. See issue 27226. */
        protected PermissionCollection getPermissions(CodeSource cs) {
            return getAllPermission();
        }
        /** Startup optimalization. See issue 27226. */
        private static PermissionCollection modulePermissions;
        /** Startup optimalization. See issue 27226. */
        private static synchronized PermissionCollection getAllPermission() {
            if (modulePermissions == null) {
                modulePermissions = new Permissions();
                modulePermissions.add(new AllPermission());
                modulePermissions.setReadOnly();
            }
            return modulePermissions;
        }

        /** For a given classloader finds all registered CLIHandlers.
         */
        public final Collection allCLIs () {
            if (handlers == null) {
                handlers = new ArrayList(metaInf.lookup(new Lookup.Template(CLIHandler.class)).allInstances());
            }
            return handlers;
        }

        protected boolean isSpecialResource (String pkg) {
            boolean retValue = super.isSpecialResource (pkg);
            if (retValue) return true;

            return false;
        }
    } // end of BootClassLoader
    
    private static void append_jars_to_cp (File dir, Collection toAdd) {
        if (!dir.isDirectory()) return;
        
        File[] arr = dir.listFiles();
        for (int i = 0; i < arr.length; i++) {
            String n = arr[i].getName ();
            /*
            if (n.equals("updater.jar") || // NOI18N
                (dir.getName().equals("locale") && n.startsWith("updater_") && n.endsWith(".jar"))) { // NOI18N
                // Used by launcher, not by us.
                continue;
            }
            */
            if (n.endsWith("jar") || n.endsWith ("zip")) { // NOI18N
                toAdd.add (arr[i]);
            }
        }
    }
        
    
    private static void build_cp(File base, Collection toAdd, Set processedDirs) 
    throws java.io.IOException {
        base = base.getCanonicalFile ();
        if (!processedDirs.add (base)) {
            // already processed
            return;
        }
        
        append_jars_to_cp(new File(base, "core/patches"), toAdd); // NOI18N
        append_jars_to_cp(new File(base, "core"), toAdd); // NOI18N
        // XXX a minor optimization: exclude any unused locale JARs
        // For example, lib/locale/ might contain:
        // core_ja.jar
        // core_f4j.jar
        // core_f4j_ja.jar
        // core_f4j_ce.jar
        // core_f4j_ce_ja.jar
        // core_ru.jar
        // core_fr.jar
        // [etc.]
        // Only some of these will apply to the current session, based on the
        // current values of Locale.default and NbBundle.branding.
        append_jars_to_cp(new File(base, "core/locale"), toAdd); // NOI18N
    }
}
