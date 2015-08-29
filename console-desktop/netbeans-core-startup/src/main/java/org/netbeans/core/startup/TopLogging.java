/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.core.startup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import org.openide.ErrorManager;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * A class that provides logging facility for the IDE - once instantiated, it
 * redirects the System.err into a log file.
 */
public class TopLogging
{
    /** The name of the log file */
    public static final String LOG_FILE_NAME = "messages.log"; // NOI18N

    private static final boolean disabledConsole = ! Boolean.getBoolean("netbeans.logger.console"); // NOI18N

    private PrintStream logPrintStream;

    private static TopLogging topLogging;
    
    /** Maximal size of log file.*/
    private static final long LOG_MAX_SIZE = 
        Long.getLong("org.netbeans.core.TopLogging.LOG_MAX_SIZE", 0x40000).longValue(); // NOI18N

    /** Number of old log files that are maintained.*/    
    private static final int LOG_COUNT = 
        Integer.getInteger("org.netbeans.core.TopLogging.LOG_COUNT", 3).intValue(); // NOI18N
    
    
    /** Creates a new TopLogging - redirects the System.err to a log file.
     * @param logDir A directory for the log file
     */
    TopLogging (String logDir) throws IOException  {
        topLogging = this;

        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.US);
        Date date = new Date();
        if (logDir == null) {
            // no demultiplexing -- everything goes just to stderr.
            logPrintStream = System.err;
            logPrintStream.println("-------------------------------------------------------------------------------"); // NOI18N
            logPrintStream.println(">Log Session: "+df.format (date)); // NOI18N
            logPrintStream.println(">System Info: "); // NOI18N
            try {
                printSystemInfo(logPrintStream);
            } catch (Throwable t) {
                // Serious problems.
                t.printStackTrace();
                logPrintStream.flush();
            }
            logPrintStream.println("-------------------------------------------------------------------------------"); // NOI18N
            logPrintStream = new PrintStream(new OutputStream() {
                public void write(int b) { /* intentionally empty */ }
            });
            return;
        }
        OutputStream log = null;
        
        File logFileDir = new File (logDir);
        if (! logFileDir.exists () && ! logFileDir.mkdirs ()) {
            throw new IOException ("Cannot make directory to contain log file"); // NOI18N
        }
        File logFile = createLogFile (logFileDir, LOG_FILE_NAME);
        if ((logFile.exists() && !logFile.canWrite()) || logFile.isDirectory()) {
            throw new IOException ("Cannot write to file"); // NOI18N
        }

        log = new BufferedOutputStream(new FileOutputStream(logFile.getAbsolutePath(), true));

        final PrintStream stderr = System.err;
        logPrintStream = new PrintStream(new StreamDemultiplexor(stderr, log), false, "UTF-8"); // NOI18N
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                logPrintStream.flush(); // #31519
                logPrintStream.close();
            }
        });
        logPrintStream.println("-------------------------------------------------------------------------------"); // NOI18N
        logPrintStream.println(">Log Session: "+ df.format (date)); // NOI18N
        logPrintStream.println(">System Info: "); // NOI18N
        printSystemInfo(logPrintStream);
        logPrintStream.println("-------------------------------------------------------------------------------"); // NOI18N

        System.setErr(logPrintStream);
    }

    private static TopLogging getDefault() {
        if (topLogging == null) {
            try {
                new TopLogging(CLIOptions.getLogDir());
            } catch (IOException x) {
                ErrorManager.getDefault().notify(x);
            }
        }
        return topLogging;
    }

    /** This method limits size of log files. There is kept: actual log file   
     *  and old log files. This method prevents from growing log file infinitely.*/
    private static File createLogFile (File parent, String chld) {
        long firstModified = 0;
        File renameTo = null;
        File retFile = new File (parent, chld);
        
        if (!retFile.exists() || retFile.length() < LOG_MAX_SIZE)
            return retFile;
        
        for (int i = 1; i < LOG_COUNT;i++) {
            String logName = chld + "."+i; // NOI18N
            File logFile = new File (parent, logName);
            
            if (!logFile.exists()) {
                renameTo = logFile;               
                break;
            }
            
            long logModif = logFile.lastModified();
            if ((firstModified == 0 || logModif < firstModified) &&  logModif > 0) {
                firstModified = logModif;
                renameTo = logFile;
            }            
        }

        if (renameTo != null) {
            if (renameTo.exists()) renameTo.delete();
            retFile.renameTo(renameTo);
        }
        
        return retFile;
    }
    
    public static void printSystemInfo(PrintStream ps) {
        String buildNumber = System.getProperty ("netbeans.buildnumber"); // NOI18N
        String currentVersion = NbBundle.getMessage(TopLogging.class, "currentVersion", buildNumber );
        ps.println("  Product Version         = " + currentVersion); // NOI18N
        ps.println("  Operating System        = " + System.getProperty("os.name", "unknown")
                   + " version " + System.getProperty("os.version", "unknown")
                   + " running on " +  System.getProperty("os.arch", "unknown"));
        ps.println("  Java; VM; Vendor; Home  = " + System.getProperty("java.version", "unknown") + "; " +
                   System.getProperty("java.vm.name", "unknown") + " " + System.getProperty("java.vm.version", "") + "; " +
                   System.getProperty("java.vendor", "unknown") + "; " +
                   System.getProperty("java.home", "unknown"));
        ps.print(  "  System Locale; Encoding = " + Locale.getDefault()); // NOI18N
        String branding = NbBundle.getBranding ();
        if (branding != null) {
            ps.print(" (" + branding + ")"); // NOI18N
        }
        ps.println("; " + System.getProperty("file.encoding", "unknown")); // NOI18N
        ps.println("  Home Dir.; Current Dir. = " + System.getProperty("user.home", "unknown") + "; " +
                   System.getProperty("user.dir", "unknown"));
        ps.print(  "  Installation; User Dir. = "); // NOI18N
        String nbdirs = System.getProperty("netbeans.dirs");
        if (nbdirs != null) { // noted in #67862: should show all clusters here.
            StringTokenizer tok = new StringTokenizer(nbdirs, File.pathSeparator);
            while (tok.hasMoreTokens()) {
                ps.print(FileUtil.normalizeFile(new File(tok.nextToken())));
                ps.print(File.pathSeparatorChar);
            }
        }
        ps.println(CLIOptions.getHomeDir() + "; " + CLIOptions.getUserDir()); // NOI18N
        ps.println("  Boot & Ext. Classpath   = " + createBootClassPath()); // NOI18N
        ps.println("  Application Classpath   = " + System.getProperty("java.class.path", "unknown")); // NOI18N
        ps.println("  Startup Classpath       = " + System.getProperty("netbeans.dynamic.classpath", "unknown")); // NOI18N
    }

    // Copied from NbClassPath:
    private static String createBootClassPath() {
        // boot
        String boot = System.getProperty("sun.boot.class.path"); // NOI18N
        StringBuffer sb = (boot != null ? new StringBuffer(boot) : new StringBuffer());
        
        // std extensions
        findBootJars(System.getProperty("java.ext.dirs"), sb);
        findBootJars(System.getProperty("java.endorsed.dirs"), sb);
        return sb.toString();
    }

    /** Scans path list for something that can be added to classpath.
     * @param extensions null or path list
     * @param sb buffer to put results to
     */
    private static void findBootJars(final String extensions, final StringBuffer sb) {
        if (extensions != null) {
            for (StringTokenizer st = new StringTokenizer(extensions, File.pathSeparator); st.hasMoreTokens();) {
                File dir = new File(st.nextToken());
                File[] entries = dir.listFiles();
                if (entries != null) {
                    for (int i = 0; i < entries.length; i++) {
                        String name = entries[i].getName().toLowerCase(Locale.US);
                        if (name.endsWith(".zip") || name.endsWith(".jar")) { // NOI18N
                            if (sb.length() > 0) {
                                sb.append(File.pathSeparatorChar);
                            }
                            sb.append(entries[i].getPath());
                        }
                    }
                }
            }
        }
    }
    
    protected void finalize() throws Throwable {
        logPrintStream.flush();
        logPrintStream.close();
    }

    /** @since JST-PENDING needed by NbErrorManager */
    public static PrintStream getLogOutputStream() {
        if (System.getProperty("netbeans.user") == null) { // NOI18N
            // No user directory. E.g. from <makeparserdb>. Skip ide.log.
            return System.err;
        }
        return TopLogging.getDefault().logPrintStream;
    }

    private static final class StreamDemultiplexor extends OutputStream implements Runnable {
        
        /** task to flush the log file, or null */
        private RequestProcessor.Task logFlushTask;

        /** processor in which to flush them */
        private static final RequestProcessor RP = new RequestProcessor("Flush ide.log"); // NOI18N

        /** a lock for flushing */
        private static final Object FLUSH_LOCK = new String("org.netbeans.core.TopLogging.StreamDemultiplexor.FLUSH_LOCK"); // NOI18N

        /** delay for flushing */
        private static final int FLUSH_DELAY = Integer.getInteger("netbeans.logger.flush.delay", 15000).intValue(); // NOI18N

        private final OutputStream stderr;
        private OutputStream log;

        StreamDemultiplexor(PrintStream stderr, OutputStream log) {
            this.stderr = stderr;
            this.log = log;
        }
        
        public void write(int b) throws IOException {
            log.write(b);
            if (! disabledConsole)
                stderr.write(b);
            flushLog();
        }

        public void write(byte b[]) throws IOException {
            log.write(b);
            if (! disabledConsole) stderr.write(b);
            flushLog();
        }

        public void write(byte b[],
                          int off,
                          int len)
        throws IOException {
            log.write(b, off, len);
            if (! disabledConsole) stderr.write(b, off, len);
            flushLog();
        }

        public void flush() throws IOException {
            log.flush();
            stderr.flush();
        }

        public void close() throws IOException {
            log.close();
            stderr.close();
        }

        /**
         * Flush the log file asynch.
         * Waits for e.g. 15 seconds after the first write.
         * Note that this is only a delay to force a flush; if there is a lot
         * of content, the buffer will fill up and it may have been written out
         * long before. This just catches any trailing content.
         * @see "#31519"
         */
        private void flushLog() {
            synchronized (FLUSH_LOCK) {
                if (logFlushTask == null) {
                    logFlushTask = RP.create(this);
                    logFlushTask.schedule(FLUSH_DELAY);
                }
            }
        }

        /**
         * Flush log messages periodically.
         */
        public void run() {
            synchronized (FLUSH_LOCK) {
                try {
                    flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                logFlushTask = null;
            }
        }

    }
}
