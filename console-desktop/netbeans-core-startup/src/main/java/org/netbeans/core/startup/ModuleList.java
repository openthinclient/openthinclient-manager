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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.netbeans.DuplicateException;
import org.netbeans.Events;
import org.netbeans.InvalidException;
import org.netbeans.Module;
import org.netbeans.ModuleManager;
import org.netbeans.Util;
import org.openide.ErrorManager;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.modules.Dependency;
import org.openide.modules.InstalledFileLocator;
import org.openide.modules.ModuleInstall;
import org.openide.modules.SpecificationVersion;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;
import org.openide.util.WeakSet;
import org.openide.util.io.NbObjectInputStream;
import org.openide.util.io.NbObjectOutputStream;
import org.openide.xml.EntityCatalog;
import org.openide.xml.XMLUtil;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/** Class responsible for maintaining the list of modules in the IDE persistently.
 * This class understands the "module status" XML format, and the list of modules
 * present in the Modules/ folder. And it can keep track of module histories.
 * Methods must be called from within appropriate mutex access.
 * @author Jesse Glick
 */
final class ModuleList {
    
    /** The DTD for a module status. */
    public static final String PUBLIC_ID = "-//NetBeans//DTD Module Status 1.0//EN"; // NOI18N
    public static final String SYSTEM_ID = "http://www.netbeans.org/dtds/module-status-1_0.dtd"; // NOI18N
    
    /** Whether to validate module XML files.
     * Safer; only slows down startup in case quickie parse of XML statuses fails for some reason.
     */
    private static final boolean VALIDATE_XML = true;
    
    /** associated module manager */
    private final ModuleManager mgr;
    /** Modules/ folder containing XML data */
    private final FileObject folder;
    /** to fire events with */
    private final Events ev;
    /** map from code name (base)s to statuses of modules on disk */
    private final Map/*<String,DiskStatus>*/ statuses = new HashMap(100);
    /** whether the initial round has been triggered or not */
    private boolean triggered = false;
    /** listener for changes in modules, etc.; see comment on class Listener */
    private final Listener listener = new Listener();
    /** any module install sers from externalizedModules.ser, from class name to data */
    private final Map/*<String,byte[]>*/ compatibilitySers = new HashMap(100);
    /** atomic actions I have used to change Modules/*.xml */
    private final Set/*<FileSystem.AtomicAction>*/ myAtomicActions = Collections.synchronizedSet(new WeakSet(100));
    
    /** Create the list manager.
     * @param mgr the module manager which will actually control the modules at runtime
     * @param folder the Modules/ folder on the system file system to scan/write
     * @param ev the event logger
     */
    public ModuleList(ModuleManager mgr, FileObject folder, Events ev) {
        this.mgr = mgr;
        this.folder = folder;
        this.ev = ev;
        Util.err.log("ModuleList created, storage in " + folder);
    }
    
    /** Read an initial list of modules from disk according to their stored settings.
     * Just reads the XML files in the Modules/ directory, and adds those to
     * the manager's list of modules. Errors are handled internally.
     * Note that the modules encountered are not turned on at this point even if
     * the XML says they should be; but they are added to the list of modules to
     * enable as needed. All discovered modules are returned.
     * Write mutex only.
     */
    public Set readInitial() {
        ev.log(Events.START_READ);
        final Set/*<Module>*/ read = new HashSet();
        try {
            folder.getFileSystem().runAtomicAction(new FileSystem.AtomicAction() {
                public void run() throws IOException {
        FileObject[] children = folder.getChildren();
        ev.log(Events.PERF_TICK, "list of files found");

	XMLReader reader = null;
	
        for (int i = 0; i < children.length; i++) {
            if (children[i].hasExt("ser")) { // NOI18N
                // Fine, skip over.
            } else if (children[i].hasExt("xml")) { // NOI18N
                // Assume this is one of ours. Note fixed naming scheme.
                try {
                    String nameDashes = children[i].getName(); // NOI18N
                    char[] badChars = {'.', '/', '>', '='};
                    for (int j = 0; j < 4; j++) {
                        if (nameDashes.indexOf(badChars[j]) != -1) {
                            throw new IllegalArgumentException("Bad name: " + nameDashes); // NOI18N
                        }
                    }
                    String name = nameDashes.replace('-', '.').intern(); // NOI18N
                    // Now name is the code name base of the module we expect to find.
                    // Check its format (throws IllegalArgumentException if bad):
                    Dependency.create(Dependency.TYPE_MODULE, name);
		    
                    // OK, read it from disk.
                    Map props;
                    InputStream is = children[i].getInputStream();
                    try {
                        props = readStatus(new BufferedInputStream(is));
                        if (props == null) {
                            Util.err.log(ErrorManager.WARNING, "Note - failed to parse " + children[i] + " the quick way, falling back on XMLReader");
                            is.close();
                            is = children[i].getInputStream();
                            InputSource src = new InputSource(is);
                            // Make sure any includes etc. are handled properly:
                            src.setSystemId(children[i].getURL().toExternalForm());
                            if (reader == null) {
                                try {
                                    reader = XMLUtil.createXMLReader();
                                } catch(SAXException e) {
                                    IllegalStateException ise = new IllegalStateException(e.toString());
                                    Util.err.annotate(ise, e);
                                    throw ise;
                                }
                                reader.setEntityResolver(listener);
                                reader.setErrorHandler(listener);
                            }
                            props = readStatus(src,reader);
                        }
                    } finally {
                        is.close();
                    }
                    if (! name.equals(props.get("name"))) throw new IOException("Code name mismatch: " /* #25011 */ + name + " vs. " + props.get("name")); // NOI18N
                    String jar = (String)props.get("jar"); // NOI18N
                    File jarFile;
                    try {
                        jarFile = findJarByName(jar, name);
                    } catch (FileNotFoundException fnfe) {
                        //Util.err.log("Cannot find: " + fnfe.getMessage());
                        ev.log(Events.MISSING_JAR_FILE, new File(fnfe.getMessage()));
                        try {
                            children[i].delete();
                        } catch (IOException ioe) {
                            Util.err.notify(ioe);
                        }
                        continue;
                    }

                    ModuleHistory history = new ModuleHistory(jar); // NOI18N
                    Integer prevReleaseI = (Integer)props.get("release"); // NOI18N
                    int prevRelease = (prevReleaseI == null ? -1 : prevReleaseI.intValue());
                    SpecificationVersion prevSpec = (SpecificationVersion)props.get("specversion"); // NOI18N
                    history.upgrade(prevRelease, prevSpec);
                    Boolean reloadableB = (Boolean)props.get("reloadable"); // NOI18N
                    boolean reloadable = (reloadableB != null ? reloadableB.booleanValue() : false);
                    Boolean enabledB = (Boolean)props.get("enabled"); // NOI18N
                    boolean enabled = (enabledB != null ? enabledB.booleanValue() : false);
                    Boolean autoloadB = (Boolean)props.get("autoload"); // NOI18N
                    boolean autoload = (autoloadB != null ? autoloadB.booleanValue() : false);
                    Boolean eagerB = (Boolean)props.get("eager"); // NOI18N
                    boolean eager = (eagerB != null ? eagerB.booleanValue() : false);
                    String installer = (String)props.get("installer"); // NOI18N
                    if (installer != null) {
                        if (! installer.equals(nameDashes + ".ser")) throw new IOException("Incorrect installer ser name: " + installer); // NOI18N
                        // Load from disk in mentioned file.
                        FileObject installerSer = folder.getFileObject(nameDashes, "ser"); // NOI18N
                        if (installerSer == null) throw new IOException("No such install ser: " + installer + "; I see only: " + Arrays.asList(children)); // NOI18N
                        // Hope the stored state is not >Integer.MAX_INT! :-)
                        byte[] buf = new byte[(int)installerSer.getSize()];
                        InputStream is2 = installerSer.getInputStream();
                        try {
                            is2.read(buf);
                        } finally {
                            is2.close();
                        }
                        history.setInstallerState(buf);
                        // Quasi-prop which is stored separately.
                        props.put("installerState", buf); // NOI18N
                    }
                    Module m = mgr.create(jarFile, history, reloadable, autoload, eager);
                    read.add(m);
                    DiskStatus status = new DiskStatus();
                    status.module = m;
                    status.file = children[i];
                    //status.lastApprovedChange = children[i].lastModified().getTime();
                    status.pendingInstall = enabled;
                    // Will only really be flushed if mgr props != disk props, i.e
                    // if version changed or could not be enabled.
                    //status.pendingFlush = true;
                    status.diskProps = props;
                    statuses.put(name, status);
                // FIXME: quick hack to suppress annoying duplicate window
                } catch (DuplicateException e) {
                		e.printStackTrace();
                } catch (Exception e) {
                    Util.err.annotate(e, ErrorManager.EXCEPTION, "Error encountered while reading " + children[i], null, null, null); // NOI18N
                    Util.err.notify(e);
                }
            } else {
                Util.err.log("Strange file encountered in modules folder: " + children[i]);
            }
            ev.log( Events.MODULES_FILE_PROCESSED, children[i] );
        }
        if (Util.err.isLoggable(ErrorManager.INFORMATIONAL)) {
            Util.err.log("read initial XML files: statuses=" + statuses);
        }
        ev.log(Events.FINISH_READ, read);
        // Handle changes in the Modules/ folder on disk by parsing & applying them.
        folder.addFileChangeListener(FileUtil.weakFileChangeListener (listener, folder));
                }});
        } catch (IOException ioe) {
            Util.err.notify(ioe);
        }
        return read;
    }
    
    /**
     * Try to find a module JAR by an XML-supplied name.
     * @param jar the JAR name (relative to an install dir, or a full path)
     * @param name code name base of the module JAR
     * @return an actual JAR file
     * @throws FileNotFoundException if no such JAR file could be found on disk
     * @throws IOException if something else was wrong
     */
    private File findJarByName(String jar, String name) throws IOException {
        File f = new File(jar);
        if (f.isAbsolute()) {
            if (!f.isFile()) throw new FileNotFoundException(f.getAbsolutePath());
            return f;
        } else {
            f = InstalledFileLocator.getDefault().locate(jar, name, false);
            if (f != null) {
                return f;
            } else {
                throw new FileNotFoundException(jar);
            }
        }
    }
    
    /** Actually go ahead and enable modules which were queued up by
     * reading methods. Should be done after as many modules
     * are collected as possible, in case they have odd mutual
     * dependencies. Also begins listening to further changes.
     * Pass in a list of boot modules which you would
     * like to also try to enable now.
     */
    public void trigger(Set boot) {
        ev.log(Events.PERF_START, "ModuleList.trigger"); // NOI18N
        if (triggered) throw new IllegalStateException("Duplicate call to trigger()"); // NOI18N
        Set/*<Module>*/ maybeEnable = new HashSet(boot);
        Iterator it = statuses.values().iterator();
        while (it.hasNext()) {
            DiskStatus status = (DiskStatus)it.next();
            if (status.pendingInstall) {
                // We are going to try to turn it on...
                status.pendingInstall = false;
                Module m = status.module;
                if (m.isEnabled() || m.isAutoload() || m.isEager()) throw new IllegalStateException();
                maybeEnable.add(m);
            }
        }
        ev.log(Events.PERF_TICK, "modules to enable prepared"); // NOI18N
	
        if (! maybeEnable.isEmpty()) {
            ev.log(Events.START_AUTO_RESTORE, maybeEnable);
            installNew(maybeEnable);
            ev.log(Events.FINISH_AUTO_RESTORE, maybeEnable);
        }
        Util.err.log("ModuleList.trigger: enabled new modules, flushing changes...");
        triggered = true;
        flushInitial();
        ev.log(Events.PERF_END, "ModuleList.trigger"); // NOI18N
    }
    // XXX is this method still needed? rethink...
    private void installNew(Set/*<Module>*/ modules) {
        if (modules.isEmpty()) {
            return;
        }
        ev.log(Events.PERF_START, "ModuleList.installNew"); // NOI18N
        // First suppress all autoloads.
        Iterator it = modules.iterator();
        while (it.hasNext()) {
            Module m = (Module)it.next();
            if (m.isAutoload() || m.isEager()) {
                it.remove();
            } else if (m.isEnabled()) {
                // Can happen in obscure circumstances: old module A
                // now exists again but with dependency on new module B,
                // and a complete build was not done for A+B, so they have
                // no existing Modules/ *.xml. In such a case B will already
                // have been turned on when restoring A; harmless to remove
                // it from the list here.
                Util.err.log("#17295 fix active for " + m.getCodeNameBase());
                it.remove();
            } else if (!m.isValid()) {
                // Again can also happen if the user upgrades from one version
                // of a module to another. In this case ModuleList correctly removed
                // the old dead module from the manager's list, however it is still
                // in the set of modules to restore.
                Util.err.log("#17471 fix active for " + m.getCodeNameBase());
                it.remove();
            }
        }
        List/*<Module>*/ toEnable = mgr.simulateEnable(modules);
        it = toEnable.iterator();
        while (it.hasNext()) {
            Module m = (Module)it.next();
            if (m.isAutoload() || m.isEager()) {
                continue;
            }
            // Quietly turn on others as well:
            if (! modules.contains(m)) {
                modules.add(m);
            }
        }
        Set/*<Module>*/ missing = new HashSet(modules);
        missing.removeAll(toEnable);
        if (! missing.isEmpty()) {
            // Include also problematic autoloads and so on needed by these modules.
            Util.transitiveClosureModuleDependencies(mgr, missing);
            it = missing.iterator();
            while (it.hasNext()) {
                Module m = (Module)it.next();
                if (m.getProblems().isEmpty()) {
                    it.remove();
                }
            }
            ev.log(Events.FAILED_INSTALL_NEW, missing);
            modules.removeAll(missing);
        }
        try {
            mgr.enable(modules);
        } catch (InvalidException ie) {
            Util.err.notify(ErrorManager.INFORMATIONAL, ie);
            Module bad = ie.getModule();
            if (bad == null) throw new IllegalStateException();
            ev.log(Events.FAILED_INSTALL_NEW_UNEXPECTED, bad, ie);
            modules.remove(bad);
            // Try again without it. Note that some other dependent modules might
            // then be in the missing list for the second round.
            installNew(modules);
        }
        ev.log(Events.PERF_END, "ModuleList.installNew"); // NOI18N
    }
    
    /** Record initial condition of a module installer.
     * First, if any stored state of the installer was found on disk,
     * that is if it is kept in the ModuleHistory, then deserialize
     * it (to the found installer). If there are deserialization errors,
     * this step is simply skipped. Or, if there was no prerecorded state,
     * and the ModuleInstall class overrides writeExternal or a similar
     * serialization-related method, thus indicating that it wishes to
     * serialize state, then this initial object is serialized to the
     * history's state for comparison with the later value. If there are
     * problems with this serialization, the history receives a dummy state
     * (empty bytestream) to indicate that something should be saved later.
     * If there is no prerecorded state and no writeExternal method, it is
     * assumed that serialization is irrelevant to this installer and so the
     * state is left null and nothing will be done here or in the postpare method.
     * If this installer was mentioned in externalizedModules.ser, also try to
     * handle the situation (determine if the state was useful or not etc.).
     * Access from NbInstaller.
     */
    void installPrepare(Module m, ModuleInstall inst) {
        if (! (m.getHistory() instanceof ModuleHistory)) {
            Util.err.log(m + " had strange history " + m.getHistory() + ", ignoring...");
            return;
        }
        ModuleHistory hist = (ModuleHistory)m.getHistory();
        // We might have loaded something from externalizedModules.ser before.
        byte[] compatSer = (byte[])compatibilitySers.get(inst.getClass().getName());
        if (compatSer != null) {
            Util.err.log("Had some old-style state for " + m);
            if (isReallyExternalizable(inst.getClass())) {
                // OK, maybe it was not useless, let's see...
                // Compare virgin state to what we had; if different, load
                // the old state and record that we want to track state.
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
                    new NbObjectOutputStream(baos).writeObject(inst);
                    baos.close();
                    if (Utilities.compareObjects(compatSer, baos.toByteArray())) {
                        Util.err.log("Old-style state for " + m + " was gratuitous");
                        // leave hist.installerState null
                    } else {
                        Util.err.log("Old-style state for " + m + " was useful, loading it...");
                        // Make sure it is recorded as "changed" in history by writing something
                        // fake now. In installPostpare, we will load the new installer state
                        // and call setInstallerState again, so the result will be written to disk.
                        hist.setInstallerState(new byte[0]);
                        // And also load it into the actual installer.
                        InputStream is = new ByteArrayInputStream(compatSer);
                        Object o = new NbObjectInputStream(is).readObject();
                        if (o != inst) throw new ClassCastException("Stored " + o + " but expecting " + inst); // NOI18N
                    }
                } catch (Exception e) {
                    Util.err.notify(e);
                    // Try later to continue.
                    hist.setInstallerState(new byte[0]);
                } catch (LinkageError le) {
                    Util.err.notify(le);
                    // Try later to continue.
                    hist.setInstallerState(new byte[0]);
                }
            } else {
                Util.err.log(m + " did not want to store install state");
                // leave hist.installerState null
            }
        } else if (hist.getInstallerState() != null) {
            // We already have some state, load it now.
            Util.err.log("Loading install state for " + m);
            try {
                InputStream is = new ByteArrayInputStream(hist.getInstallerState());
                // Note: NBOOS requires the system class loader to be in order.
                // Technically we have not yet fired any changes in it. However,
                // assuming that we are not in the first block of modules to be
                // loaded (that is, core = bootstraps) this will work because the
                // available systemClassLoader is just appended to. Better would
                // probably be to use a special ObjectInputStream resolving
                // specifically to the module's classloader, as this is far more
                // direct and possibly more reliable.
                Object o = new NbObjectInputStream(is).readObject();
                // The joys of SharedClassObject. The deserialization itself actually
                // is assumed to overwrite the state of the install singleton. We
                // can only confirm that we actually deserialized the same thing we
                // were expecting too (it is too late to revert anything of course).
                if (o != inst) throw new ClassCastException("Stored " + o + " but expecting " + inst); // NOI18N
            } catch (Exception e) {
                // IOException, ClassNotFoundException, and maybe unchecked stuff
                Util.err.notify(e);
                // Nothing else to do, hope that the install object was not corrupted
                // by the failed deserialization! If it was, it cannot be saved now.
            } catch (LinkageError le) {
                Util.err.notify(le);
            }
        } else {
            // Virgin installer. First we check if it really cares about serialization
            // at all, because it not we do not want to waste time forcing it.
            if (isReallyExternalizable(inst.getClass())) {
                Util.err.log("Checking pre-install state of " + m);
                Util.err.log(ErrorManager.WARNING, "Warning: use of writeExternal (or writeReplace) in " + inst.getClass().getName() + " is deprecated; use normal settings instead");
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
                    new NbObjectOutputStream(baos).writeObject(inst);
                    baos.close();
                    // Keep track of the installer's state before we installed it.
                    // This will be compared to its state afterwards so we can
                    // avoid writing anything if nothing changed, thus avoid
                    // polluting the disk.
                    hist.setInstallerState(baos.toByteArray());
                } catch (Exception e) {
                    Util.err.notify(e);
                    // Remember that it is *supposed* to be serializable to something.
                    hist.setInstallerState(new byte[0]);
                } catch (LinkageError le) {
                    Util.err.notify(le);
                    hist.setInstallerState(new byte[0]);
                }
            } else {
                // It does not want to store anything. Leave the installer state null
                // and continue.
                Util.err.log(m + " did not want to store install state");
            }
        }
    }
    /** Check if a class (extends ModuleInstall) is truly externalizable,
     * e.g. overrides writeExternal.
     */
    private static boolean isReallyExternalizable(Class clazz) {
        Class c;
        for (c = clazz; c != ModuleInstall.class && c != Object.class; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod("writeExternal", new Class[] {ObjectOutput.class}); // NOI18N
                // [PENDING] check that m is public, nonstatic, returns Void.TYPE and includes at most
                // IOException and unchecked exceptions in its clauses, else die
                // OK, it does something nontrivial.
                return true;
            } catch (NoSuchMethodException nsme) {
                // Didn't find it at this level, continue.
            }
            try {
                Method m = c.getDeclaredMethod("writeReplace", new Class[] {}); // NOI18N
                // [PENDING] check that m is nonstatic, returns Object, throws ObjectStreamException
                // Designates a serializable replacer, this is special.
                return true;
            } catch (NoSuchMethodException nsme) {
                // Again keep on looking.
            }
        }
        // Hit a superclass.
        if (c == Object.class) throw new IllegalArgumentException("Class " + clazz + " was not a ModuleInstall"); // NOI18N
        // Hit ModuleInstall. Did not find anything. Assumed to not do anything during externalization.
        return false;
    }
    
    /** Acknowledge later conditions of a module installer.
     * If the module history indicates a nonnull installer state, then we
     * try to serialize the current state to a temporary buffer and compare
     * to the previous state. If the serialization succeeds, and they differ,
     * then the new state is recorded in the history and will later be written to disk.
     * Access from NbInstaller.
     */
    void installPostpare(Module m, ModuleInstall inst) {
        if (! (m.getHistory() instanceof ModuleHistory)) {
            Util.err.log(m + " had strange history " + m.getHistory() + ", ignoring...");
            return;
        }
        ModuleHistory hist = (ModuleHistory)m.getHistory();
        if (hist.getInstallerState() != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
                new NbObjectOutputStream(baos).writeObject(inst);
                baos.close();
                byte[] old = hist.getInstallerState();
                byte[] nue = baos.toByteArray();
                if (Utilities.compareObjects(old, nue)) {
                    // State has not changed.
                    Util.err.log(m + " did not change installer state (" + old.length + " bytes), not writing anything");
                } else {
                    // It did change. Store new version.
                    Util.err.log(m + " changed installer state after loading");
                    hist.setInstallerState(nue);
                }
            } catch (Exception e) {
                Util.err.notify(e);
                // We could not compare, so don't bother writing out any old state.
                //hist.setInstallerState(null);
            } catch (LinkageError le) {
                Util.err.notify(le);
            }
        } else {
            // Nothing stored (does not writeExternal), do nothing.
            Util.err.log(m + " has no saved state");
        }
    }
    
    /** Read an XML file using an XMLReader and parse into a map of properties.
     * One distinguished property 'name' is the code name base
     * and is taken from the root element. Others are taken
     * from the param elements.
     * Properties are of type String, Boolean, Integer, or SpecificationVersion
     * according to the property name.
     * @param is the input stream
     * @param reader the XML reader to use to parse; may be null
     * @return a map of named properties to values of various types
     */
    private Map readStatus(InputSource is, XMLReader reader) throws IOException, SAXException {
        if (reader == null) {
            reader = XMLUtil.createXMLReader(VALIDATE_XML);
            reader.setEntityResolver(listener);
            reader.setErrorHandler(listener);
        }
        final Map/*<String,Object>*/ m = new HashMap();

        DefaultHandler handler = new DefaultHandler() {
            private String modName;
            private String paramName;
            private StringBuffer data = new StringBuffer();
	    
            @Override
						public void startElement(String uri,
                                     String localname,
                                     String qname,
                                     Attributes attrs) throws SAXException {
                if ("module".equals(qname) ) { // NOI18N
                    modName = attrs.getValue("name"); // NOI18N
                    if( modName == null )
                        throw new SAXException("No module name"); // NOI18N
                    m.put("name", modName.intern()); // NOI18N
                }
                else if (modName != null && "param".equals(qname)) { // NOI18N
                    paramName = attrs.getValue("name");
                    if( paramName == null ) {
                        throw new SAXException("No param name"); // NOI18N
                    }
                    paramName = paramName.intern();
                    data.setLength(0);
                }
            }
	    
            @Override
						public void characters(char[] ch, int start, int len) {
                if(modName != null  && paramName != null)
                    data.append( ch, start, len );
            }
            
            @Override
						public void endElement (String uri, String localname, String qname)
                throws SAXException
            {
                if ("param".equals(qname)) { // NOI18N
                    if (modName != null && paramName != null) {
                        if (data.length() == 0)
                            throw new SAXException("No text contents in " + paramName + " of " + modName); // NOI18N
                        
                        try {
                            m.put(paramName, processStatusParam(paramName, data.toString()));
                        } catch (NumberFormatException nfe) {
                            // From either Integer or SpecificationVersion constructors.
                            SAXException saxe = new SAXException(nfe.toString());
                            Util.err.annotate(saxe, nfe);
                            throw saxe;
                        }

                        data.setLength(0);
                        paramName = null;
                    }
                }
                else if ("module".equals(qname)) { // NOI18N
                    modName = null;
                }
            }
        };
        
        reader.setContentHandler(handler);
        reader.parse(is);

        sanityCheckStatus(m);

        return m;
    }
    
    /** Parse a param value according to a natural type.
     * @param k the param name (must be interned!)
     * @param v the raw string value from XML
     * @return some parsed value suitable for the status map
     */
    private Object processStatusParam(String k, String v) throws NumberFormatException {
        if (k == "release") { // NOI18N
            return new Integer(v);
        } else if (k == "enabled" // NOI18N
                   || k == "autoload" // NOI18N
                   || k == "eager" // NOI18N
                   || k == "reloadable" // NOI18N
                   ) {
            return Boolean.valueOf(v);
        } else if (k == "specversion") { // NOI18N
            return new SpecificationVersion(v);
        } else {
            // Other properties are of type String.
            // Intern the smaller ones which are likely to be repeated somewhere.
            if (v.length() < 100) v = v.intern();
            return v;
        }
    }
    
    /** Just checks that all the right stuff is there.
     */
    private void sanityCheckStatus(Map m) throws IOException {
        if (m.get("jar") == null) // NOI18N
            throw new IOException("Must define jar param"); // NOI18N
        if (m.get("autoload") != null // NOI18N
            && ((Boolean)m.get("autoload")).booleanValue() // NOI18N
            && m.get("enabled") != null) // NOI18N
            throw new IOException("Autoloads cannot specify enablement"); // NOI18N
        if (m.get("eager") != null // NOI18N
            && ((Boolean)m.get("eager")).booleanValue() // NOI18N
            && m.get("enabled") != null) // NOI18N
            throw new IOException("Eager modules cannot specify enablement"); // NOI18N
        // Compatibility:
        String origin = (String)m.remove("origin"); // NOI18N
        if (origin != null) {
            String jar = (String)m.get("jar"); // NOI18N
            String newjar;
            if (origin.equals("user") || origin.equals("installation")) { // NOI18N
                newjar = "modules/" + jar; // NOI18N
            } else if (origin.equals("user/autoload") || origin.equals("installation/autoload")) { // NOI18N
                newjar = "modules/autoload/" + jar; // NOI18N
            } else if (origin.equals("user/eager") || origin.equals("installation/eager")) { // NOI18N
                newjar = "modules/eager/" + jar; // NOI18N
            } else if (origin.equals("adhoc")) { // NOI18N
                newjar = jar;
            } else {
                throw new IOException("Unrecognized origin " + origin + " for " + jar); // NOI18N
            }
            Util.err.log(ErrorManager.WARNING, "Upgrading 'jar' param from " + jar + " to " + newjar + " and removing 'origin' " + origin);
            m.put("jar", newjar); // NOI18N
        }
    }

    // Encoding irrelevant for these getBytes() calls: all are ASCII...
    // (unless someone has their system encoding set to UCS-16!)
    private static final byte[] MODULE_XML_INTRO = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE module PUBLIC \"-//NetBeans//DTD Module Status 1.0//EN\"\n                        \"http://www.netbeans.org/dtds/module-status-1_0.dtd\">\n<module name=\"".getBytes(); // NOI18N
//    private static final byte[] MODULE_XML_DIV1 = ">\n    <param name=\"".getBytes(); // NOI18N
    private static final byte[] MODULE_XML_INTRO_END = ">\n".getBytes(); // NOI18N
    private static final byte[] MODULE_XML_DIV2 = "   <param name=\"".getBytes(); // NOI18N
    private static final byte[] MODULE_XML_DIV3 = "/param>\n".getBytes(); // NOI18N
    private static final byte[] MODULE_XML_END = "/module>\n".getBytes(); // NOI18N
    /** Just like {@link #readStatus(InputSource,XMLReader} but avoids using an XML parser.
     * If it does not manage to parse it this way, it returns null, in which case
     * you have to use a real parser.
     * @see "#26786"
     */
    private Map readStatus(InputStream is) throws IOException {
        Map/*<String,Object>*/ m = new HashMap(15);
        if (!expect(is, MODULE_XML_INTRO)) {
            Util.err.log("Could not read intro");
            return null;
        }
        String name = readTo(is, '"');
        if (name == null) {
            Util.err.log("Could not read code name base");
            return null;
        }
        m.put("name", name.intern()); // NOI18N
        if (!expect(is, MODULE_XML_INTRO_END)) {
            Util.err.log("Could not read stuff after cnb");
            return null;
        }
        // Now we have <param>s some number of times, finally </module>.
    PARSE:
        while (true) {
            int c = is.read();
            switch (c) {
            case ' ':
                // <param>
                if (!expect(is, MODULE_XML_DIV2)) {
                    Util.err.log("Could not read up to param");
                    return null;
                }
                String k = readTo(is, '"');
                if (k == null) {
                    Util.err.log("Could not read param");
                    return null;
                }
                k = k.intern();
                if (is.read() != '>') {
                    Util.err.log("No > at end of <param> " + k);
                    return null;
                }
                String v = readTo(is, '<');
                if (v == null) {
                    Util.err.log("Could not read value of " + k);
                    return null;
                }
                if (!expect(is, MODULE_XML_DIV3)) {
                    Util.err.log("Could not read end of param " + k);
                    return null;
                }
                try {
                    m.put(k, processStatusParam(k, v));
                } catch (NumberFormatException nfe) {
                    Util.err.log("Number misparse: " + nfe);
                    return null;
                }
                break;
            case '<':
                // </module>
                if (!expect(is, MODULE_XML_END)) {
                    Util.err.log("Strange ending");
                    return null;
                }
                if (is.read() != -1) {
                    Util.err.log("Trailing garbage");
                    return null;
                }
                // Success!
                break PARSE;
            default:
                Util.err.log("Strange stuff after <param>s: " + c);
                return null;
            }
        }
        sanityCheckStatus(m);
        return m;
    }
    
    /** Read some stuff from a stream and skip over it.
     * Newline conventions are normalized to Unix \n.
     * @return true upon success, false if stream contained something else
     */
    private boolean expect(InputStream is, byte[] stuff) throws IOException {
        int len = stuff.length;
        boolean inNewline = false;
        for (int i = 0; i < len; ) {
            int c = is.read();
            if (c == 10 || c == 13) {
                // Normalize: s/[\r\n]+/\n/g
                if (inNewline) {
                    continue;
                } else {
                    inNewline = true;
                    c = 10;
                }
            } else {
                inNewline = false;
            }
            if (c != stuff[i++]) {
                return false;
            }
        }
        if (stuff[len - 1] == 10) {
            // Expecting something ending in a \n - so we have to
            // read any further \r or \n and discard.
            if (!is.markSupported()) throw new IOException("Mark not supported"); // NOI18N
            is.mark(1);
            int c = is.read();
            if (c != -1 && c != 10 && c != 13) {
                // Got some non-newline character, push it back!
                is.reset();
            }
        }
        return true;
    }
    /** Read a maximal string until delim is encountered (which will be removed from stream).
     * This impl reads only ASCII, for speed.
     * Newline conventions are normalized to Unix \n.
     * @return the read string, or null if the delim is not encountered before EOF.
     */
    private String readTo(InputStream is, char delim) throws IOException {
        if (delim == 10) {
            // Not implemented - stream might have "foo\r\n" and we would
            // return "foo" and leave "\n" in the stream.
            throw new IOException("Not implemented"); // NOI18N
        }
        CharArrayWriter caw = new CharArrayWriter(100);
        boolean inNewline = false;
        while (true) {
            int c = is.read();
            if (c == -1) return null;
            if (c > 126) return null;
            if (c == 10 || c == 13) {
                // Normalize: s/[\r\n]+/\n/g
                if (inNewline) {
                    continue;
                } else {
                    inNewline = true;
                    c = 10;
                }
            } else if (c < 32 && c != 9) {
                // Random control character!
                return null;
            } else {
                inNewline = false;
            }
            if (c == delim) {
                return caw.toString();
            } else {
                caw.write(c);
            }
        }
    }
    
    /** Write a module's status to disk in the form of an XML file.
     * The map of parameters must contain one named 'name' with the code
     * name base of the module.
     */
    private void writeStatus(Map m, OutputStream os) throws IOException {
        String codeName = (String)m.get("name"); // NOI18N
        if (codeName == null)
            throw new IllegalArgumentException("no code name present"); // NOI18N

        Writer w = new OutputStreamWriter(os, "UTF-8"); // NOI18N
        w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); // NOI18N
        w.write("<!DOCTYPE module PUBLIC \""); // NOI18N
        w.write(PUBLIC_ID);
        w.write("\"\n                        \""); // NOI18N
        w.write(SYSTEM_ID);
        w.write("\">\n"); // NOI18N
        w.write("<module name=\""); // NOI18N
        w.write(XMLUtil.toAttributeValue(codeName)); // NOI18N
        w.write("\">\n");       // NOI18N

        // Use TreeMap to sort the keys by name; since the module status files might
        // be version-controlled we want to avoid gratuitous format changes.
        Iterator it = new TreeMap(m).entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            String name = (String)entry.getKey();
            if (name.equals("installerState") || name.equals("name")) { // NOI18N
                // Skip this one, it is a pseudo-param.
                continue;
            }
            
            Object val = entry.getValue();

            w.write("    <param name=\""); // NOI18N
            w.write(XMLUtil.toAttributeValue(name)); // NOI18N
            w.write("\">");     // NOI18N
            w.write(XMLUtil.toElementContent(val.toString()));
            w.write("</param>\n"); // NOI18N
        }

        w.write("</module>\n"); // NOI18N
        w.flush();
    }
    
    /** Write information about a module out to disk.
     * If the old status is given as null, this is a newly
     * added module; create an appropriate status and return it.
     * Else update the existing status and return it (it is
     * assumed properties are already updated).
     * Should write the XML and create/rewrite/delete the serialized
     * installer file as needed.
     */
    private DiskStatus writeOut(Module m, DiskStatus old) throws IOException {
        final DiskStatus nue;
        if (old == null) {
            nue = new DiskStatus();
            nue.module = m;
            nue.diskProps = computeProperties(m);
        } else {
            nue = old;
        }
        FileSystem.AtomicAction aa = new FileSystem.AtomicAction() {
            public void run() throws IOException {
                boolean created;
                if (nue.file == null) {
                    created = true;
                    nue.file = folder.createData(((String)nue.diskProps.get("name")).replace('.', '-'), "xml"); // NOI18N
                } else {
                    created = false;
                    // Just verify that no one else touched it since we last did.
                    if (/*nue.lastApprovedChange != nue.file.lastModified().getTime()*/nue.dirty) {
                        // Oops, something is wrong.
                        // XXX should this only warn instead?
                        throw new IOException("Will not clobber external changes in " + nue.file); // NOI18N
                    }
                }
                Util.err.log("ModuleList: (re)writing " + nue.file);
                FileLock lock = nue.file.lock();
                try {
                    OutputStream os = nue.file.getOutputStream(lock);
                    try {
                        writeStatus(nue.diskProps, os);
                    } finally {
                        os.close();
                    }
                } finally {
                    lock.releaseLock();
                }
                //nue.lastApprovedChange = nue.file.lastModified().getTime();
                // Now check up on the installer ser.
                byte[] data = (byte[])nue.diskProps.get("installerState"); // NOI18N
                if (data != null) {
                    String installerName = (String)nue.diskProps.get("installer"); // NOI18N
                    FileObject ser = folder.getFileObject(installerName);
                    if (ser == null) {
                        // Need to make it.
                        int idx = installerName.lastIndexOf('.'); // NOI18N
                        ser = folder.createData(installerName.substring(0, idx), installerName.substring(idx + 1));
                    }
                    // Now write it.
                    lock = ser.lock();
                    try {
                        OutputStream os = ser.getOutputStream(lock);
                        try {
                            os.write(data);
                        } finally {
                            os.close();
                        }
                    } finally {
                        lock.releaseLock();
                    }
                } else {
                    /* Probably not right:
                    // Delete any existing one.
                    if (ser != null) {
                        ser.delete();
                    }
                     */
                }
            }
        };
        myAtomicActions.add(aa);
        folder.getFileSystem().runAtomicAction(aa);
        return nue;
    }
    
    /** Delete a module from disk.
     */
    private void deleteFromDisk(final Module m, final DiskStatus status) throws IOException {
        final String nameDashes = m.getCodeNameBase().replace('.', '-'); // NOI18N
        //final long expectedTime = status.lastApprovedChange;
        FileSystem.AtomicAction aa = new FileSystem.AtomicAction() {
            public void run() throws IOException {
                FileObject xml = folder.getFileObject(nameDashes, "xml"); // NOI18N
                if (xml == null) {
                    // Could be that the XML was already deleted externally, etc.
                    Util.err.log("ModuleList: " + m + "'s XML already gone from disk");
                    return;
                }
                //if (xml == null) throw new IOException("No such XML file: " + nameDashes + ".xml"); // NOI18N
                if (status.dirty) {
                    // Someone wrote to the file since we did. Don't delete it blindly!
                    // XXX should this throw an exception, or just warn??
                    throw new IOException("Unapproved external change to " + xml); // NOI18N
                }
                Util.err.log("ModuleList: deleting " + xml);
                /*
                if (xml.lastModified().getTime() != expectedTime) {
                    // Someone wrote to the file since we did. Don't delete it blindly!
                    throw new IOException("Unapproved external change to " + xml); // NOI18N
                }
                 */
                xml.delete();
                FileObject ser = folder.getFileObject(nameDashes, "ser"); // NOI18N
                if (ser != null) {
                    Util.err.log("(and also " + ser + ")");
                    ser.delete();
                }
            }
        };
        myAtomicActions.add(aa);
        folder.getFileSystem().runAtomicAction(aa);
    }
    
    /** Flush the initial state of the module installer after startup to disk.
     * This means:
     * 1. Find all modules in the manager.
     * 2. Anything for which we have no status, write out its XML now
     *    and create a status object for it.
     * 3. Anything for which we have a status, compare the status we
     *    have to its current state (don't forget the installer
     *    serialization state--if this is nonnull, that counts as an
     *    automatic change because it means the module was loaded and
     *    needed to store something).
     * 4. For any changes found in 3., write out new XML (and if
     *    there is any installer state, a new installer ser).
     * 5. Attach listeners to the manager and all modules to catch further
     *    changes in the system so they may be flushed.
     * We could in principle start listening right after readInitial()
     * but it should be more efficient to wait and see what has really
     * changed. Also, some XML may say that a module is enabled, and in
     * fact trigger() was not able to turn it on. In that case, this will
     * show up as a change in step 3. and we will rewrite it as disabled.
     * Called within write mutex by trigger().
     */
    private void flushInitial() {
        Util.err.log("Flushing initial module list...");
        // Find all modules for which we have status already. Treat
        // them as possibly changed, and attach listeners.
        Iterator it = mgr.getModules().iterator();
        while (it.hasNext()) {
            Module m = (Module)it.next();
            DiskStatus status = (DiskStatus)statuses.get(m.getCodeNameBase());
            if (status != null) {
                moduleChanged(m, status);
                m.addPropertyChangeListener(listener);
            }
        }
        // Now find all new and deleted modules.
        moduleListChanged();
        // And listener for new or deleted modules.
        mgr.addPropertyChangeListener(listener);
    }
    
    /** Does the real work when the list of modules changes.
     * Finds newly added modules, creates XML and status for
     * them and begins listening for changes; finds deleted
     * modules, removes their listener, XML, and status.
     * May be called within read or write mutex; since it
     * could be in the read mutex, synchronize (on statuses).
     */
    private void moduleListChanged() {
        synchronized (statuses) {
            if (Util.err.isLoggable(ErrorManager.INFORMATIONAL)) {
                Util.err.log("ModuleList: moduleListChanged; statuses=" + statuses);
            }
            // Newly added modules first.
            Iterator it = mgr.getModules().iterator();
            while (it.hasNext()) {
                Module m = (Module)it.next();
                if (m.isFixed() || m.getAllJars().isEmpty()) {
                    // No way, we don't manage these.
                    continue;
                }
                final String name = m.getCodeNameBase();
                if (statuses.get(name) == null) {
                    // Yup, it's new. Write it out.
                    Util.err.log("moduleListChanged: added: " + m);
                    try {
                        statuses.put(name, writeOut(m, null));
                        m.addPropertyChangeListener(listener);
                    } catch (IOException ioe) {
                        Util.err.notify(ioe);
                        // XXX Now what? Keep it in our list or what??
                    }
                }
            }
            // Now deleted & recreated modules.
            it = statuses.values().iterator();
            while (it.hasNext()) {
                DiskStatus status = (DiskStatus)it.next();
                if (! status.module.isValid()) {
                    status.module.removePropertyChangeListener(listener);
                    Module nue = mgr.get(status.module.getCodeNameBase());
                    if (nue != null) {
                        // Deleted, but a new module with the same code name base
                        // was created (#5922 e.g.). So change the module reference
                        // in the status and write out any changes to disk.
                        Util.err.log("moduleListChanged: recreated: " + nue);
                        nue.addPropertyChangeListener(listener);
                        status.module = nue;
                        moduleChanged(nue, status);
                    } else {
                        // Newly deleted.
                        Util.err.log("moduleListChanged: deleted: " + status.module);
                        it.remove();
                        try {
                            deleteFromDisk(status.module, status);
                        } catch (IOException ioe) {
                            Util.err.notify(ioe);
                        }
                    }
                }
            }
        }
    }
    
    /** Does the real work when one module changes.
     * Compares old and new state and writes XML
     * (and perhaps serialized installer state) as needed.
     * May be called within read or write mutex; since it
     * could be in the read mutex, synchronize (on status).
     */
    private void moduleChanged(Module m, DiskStatus status) {
        synchronized (status) {
            if (Util.err.isLoggable(ErrorManager.INFORMATIONAL)) {
                Util.err.log("ModuleList: moduleChanged: " + m);
            }
            Map newProps = computeProperties(m);
            if (! Utilities.compareObjects(status.diskProps, newProps)) {
                if (Util.err.isLoggable(ErrorManager.INFORMATIONAL)) {
                    Set changes = new HashSet(newProps.entrySet());
                    changes.removeAll(status.diskProps.entrySet());
                    Util.err.log("ModuleList: changes are " + changes);
                }
                // We need to write changes.
                status.diskProps = newProps;
                try {
                    writeOut(m, status);
                } catch (IOException ioe) {
                    Util.err.notify(ioe);
                    // XXX now what? continue to manage it anyway?
                }
            }
        }
    }
    
    /** Compute what properties we would want to store in XML
     * for this module. I.e. 'name', 'reloadable', etc.
     * The special property 'installerState' may be set (only
     * if the normal property 'installer' is also set) and
     * will be a byte[] rather than a string, which means that
     * the indicated installer state should be written out.
     */
    private Map computeProperties(Module m) {
        if (m.isFixed() || ! m.isValid()) throw new IllegalArgumentException("fixed or invalid: " + m); // NOI18N
        if (! (m.getHistory() instanceof ModuleHistory)) throw new IllegalArgumentException("weird history: " + m); // NOI18N
        Map p = new HashMap();
        p.put("name", m.getCodeNameBase()); // NOI18N
        int rel = m.getCodeNameRelease();
        if (rel >= 0) {
            p.put("release", new Integer(rel)); // NOI18N
        }
        SpecificationVersion spec = m.getSpecificationVersion();
        if (spec != null) {
            p.put("specversion", spec); // NOI18N
        }
        if (!m.isAutoload() && !m.isEager()) {
            p.put("enabled", m.isEnabled() ? Boolean.TRUE : Boolean.FALSE); // NOI18N
        }
        p.put("autoload", m.isAutoload() ? Boolean.TRUE : Boolean.FALSE); // NOI18N
        p.put("eager", m.isEager() ? Boolean.TRUE : Boolean.FALSE); // NOI18N
        p.put("reloadable", m.isReloadable() ? Boolean.TRUE : Boolean.FALSE); // NOI18N
        ModuleHistory hist = (ModuleHistory)m.getHistory();
        p.put("jar", hist.getJar()); // NOI18N
        if (hist.getInstallerStateChanged()) {
            p.put("installer", m.getCodeNameBase().replace('.', '-') + ".ser"); // NOI18N
            p.put("installerState", hist.getInstallerState()); // NOI18N
        }
        return p;
    }
    
    private static RequestProcessor rpListener = null;
    /** Listener for changes in set of modules and various properties of individual modules.
     * Also serves as a strict error handler for XML parsing.
     * Also listens to changes in the Modules/ folder and processes them in req proc.
     */
    private final class Listener implements PropertyChangeListener, ErrorHandler, EntityResolver, FileChangeListener, Runnable {
        
        Listener() {}
        
        // Property change coming from ModuleManager or some known Module.
        
        private boolean listening = true;
        public void propertyChange(PropertyChangeEvent evt) {
            if (! triggered) throw new IllegalStateException("Property change before trigger()"); // NOI18N
            // REMEMBER this is inside *read* mutex, it is forbidden to even attempt
            // to get write access synchronously here!
            String prop = evt.getPropertyName();
            Object src = evt.getSource();
            if (!listening) {
                // #27106: do not react to our own changes while we are making them
                if (Util.err.isLoggable(ErrorManager.INFORMATIONAL)) {
                    Util.err.log("ModuleList: ignoring own change " + prop + " from " + src);
                }
                return;
            }
            if (ModuleManager.PROP_CLASS_LOADER.equals(prop) ||
                    ModuleManager.PROP_ENABLED_MODULES.equals(prop) ||
                    Module.PROP_CLASS_LOADER.equals(prop) ||
                    Module.PROP_PROBLEMS.equals(prop) ||
                    Module.PROP_VALID.equals(prop)) {
                // Properties we are not directly interested in, ignore.
                // Note that rather than paying attention to PROP_VALID
                // we simply deal with deletions when PROP_MODULES is fired.
                return;
            } else if (ModuleManager.PROP_MODULES.equals(prop)) {
                moduleListChanged();
            } else if (src instanceof Module) {
                // enabled, manifest, reloadable, possibly other stuff in the future
                Module m = (Module)src;
                if (! m.isValid()) {
                    // Skip it. We will get PROP_MODULES sometime anyway.
                    return;
                }
                DiskStatus status = (DiskStatus)statuses.get(m.getCodeNameBase());
                if (status == null) {
                    throw new IllegalStateException("Unknown module " + m + "; statuses=" + statuses); // NOI18N
                }
                if (status.pendingInstall && Module.PROP_ENABLED.equals(prop)) {
                    throw new IllegalStateException("Got PROP_ENABLED on " + m + " before trigger()"); // NOI18N
                }
                moduleChanged(m, status);
            } else {
                Util.err.log("Unexpected property change: " + evt + " prop=" + prop + " src=" + src);
            }
        }
        
        // SAX stuff.
        
        public void warning(SAXParseException e) throws SAXException {
            Util.err.notify(ErrorManager.WARNING, e);
        }
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }
        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }
        public InputSource resolveEntity(String pubid, String sysid) throws SAXException, IOException {
            if (pubid.equals(PUBLIC_ID)) {
                if (VALIDATE_XML) {
                    // We certainly know where to get this from.
                    return new InputSource(ModuleList.class.getResource("module-status-1_0.dtd").toExternalForm()); // NOI18N
                } else {
                    // Not validating, don't load any DTD! Significantly faster.
                    return new InputSource(new ByteArrayInputStream(new byte[0]));
                }
            } else {
                // Otherwise try the standard places.
                return EntityCatalog.getDefault().resolveEntity(pubid, sysid);
            }
        }
        
        // Changes in Modules/ folder.
        
        public void fileDeleted(FileEvent ev) {
            if (isOurs(ev)) {
                if (Util.err.isLoggable(ErrorManager.INFORMATIONAL)) {
                    Util.err.log("ModuleList: got expected deletion " + ev);
                }
                return;
            }
            FileObject fo = ev.getFile();
            fileDeleted0(fo.getName(), fo.getExt()/*, ev.getTime()*/);
        }
        
        public void fileDataCreated(FileEvent ev) {
            if (isOurs(ev)) {
                if (Util.err.isLoggable(ErrorManager.INFORMATIONAL)) {
                    Util.err.log("ModuleList: got expected creation " + ev);
                }
                return;
            }
            FileObject fo = ev.getFile();
            fileCreated0(fo, fo.getName(), fo.getExt()/*, ev.getTime()*/);
        }
        
        public void fileRenamed(FileRenameEvent ev) {
            if (isOurs(ev)) {
                throw new IllegalStateException("I don't rename anything! " + ev); // NOI18N
            }
            FileObject fo = ev.getFile();
            fileDeleted0(ev.getName(), ev.getExt()/*, ev.getTime()*/);
            fileCreated0(fo, fo.getName(), fo.getExt()/*, ev.getTime()*/);
        }
        
        private void fileCreated0(FileObject fo, String name, String ext/*, long time*/) {
            if ("xml".equals(ext)) { // NOI18N
                String codenamebase = name.replace('-', '.');
                DiskStatus status = (DiskStatus)statuses.get(codenamebase);
                Util.err.log("ModuleList: outside file creation event for " + codenamebase);
                if (status != null) {
                    // XXX should this really happen??
                    status.dirty = true;
                }
                runme();
            } else if ("ser".equals(ext)) { // NOI18N
                // XXX handle newly added installers?? or not
            } // else ignore
        }
        
        private void fileDeleted0(String name, String ext/*, long time*/) {
            if ("xml".equals(ext)) { // NOI18N
                // Removed module.
                String codenamebase = name.replace('-', '.');
                DiskStatus status = (DiskStatus)statuses.get(codenamebase);
                Util.err.log("ModuleList: outside file deletion event for " + codenamebase);
                if (status != null) {
                    // XXX should this ever happen?
                    status.dirty = true;
                }
                runme();
            } else if ("ser".equals(ext)) { // NOI18N
                // XXX handle newly deleted installers?? or not
            } // else ignore
        }
        
        public void fileChanged(FileEvent ev) {
            if (isOurs(ev)) {
                if (Util.err.isLoggable(ErrorManager.INFORMATIONAL)) {
                    Util.err.log("ModuleList: got expected modification " + ev);
                }
                return;
            }
            FileObject fo = ev.getFile();
            String name = fo.getName();
            String ext = fo.getExt();
            if ("xml".equals(ext)) { // NOI18N
                // Changed module.
                String codenamebase = name.replace('-', '.');
                DiskStatus status = (DiskStatus)statuses.get(codenamebase);
                Util.err.log("ModuleList: outside file modification event for " + codenamebase + ": " + ev);
                if (status != null) {
                    status.dirty = true;
                } else {
                    // XXX should this ever happen?
                }
                runme();
            } else if ("ser".equals(ext)) { // NOI18N
                // XXX handle changes of installers?? or not
            } // else ignore
        }
        
        public void fileFolderCreated(FileEvent ev) {
            // ignore
        }
        public void fileAttributeChanged(FileAttributeEvent ev) {
            // ignore
        }
        
        /** Check if a given file event in the Modules/ folder was a result
         * of our own manipulations, as opposed to some other code (or polled
         * refresh) manipulating one of these XML files. See #15573.
         */
        private boolean isOurs(FileEvent ev) {
            Iterator it = myAtomicActions.iterator();
            while (it.hasNext()) {
                if (ev.firedFrom((FileSystem.AtomicAction)it.next())) {
                    return true;
                }
            }
            return false;
        }
        
        // Dealing with changes in Modules/ folder and processing them.
        
        private boolean pendingRun = false;
        private synchronized void runme() {
            if (! pendingRun) {
                pendingRun = true;
                if (rpListener == null) {
                    rpListener = new RequestProcessor("org.netbeans.core.modules.ModuleList.Listener"); // NOI18N
                }
                rpListener.post(this);
            }
        }
        public void run() {
            synchronized (this) {
                pendingRun = false;
            }
            Util.err.log("ModuleList: will process outstanding external XML changes");
            mgr.mutexPrivileged().enterWriteAccess();
            try {
                folder.getFileSystem().runAtomicAction(new FileSystem.AtomicAction() {
                    public void run() throws IOException {
                        // 1. For any dirty XML for which status exists but reloadable differs from XML: change.
                        // 2. For any XML for which we have no status: create & create status, as disabled.
                        // 3. For all dirty XML which says enabled but status says disabled: batch-enable as possible.
                        //    (Where not possible, mark disabled in XML??)
                        // 4. For all dirty XML which says disabled but status says enabled: batch-disable plus others.
                        // 5. For all status for which no XML exists: batch-disable plus others, then delete.
                        // 6. For any dirty XML for which jar/autoload/eager/release/specversion differs from
                        //    actual state of module: warn but do nothing.
                        // 7. For now, ignore any changes in *.ser.
                        // 8. For any dirty XML for which status now exists: replace diskProps with contents of XML.
                        // 9. Mark all statuses clean.
                        // Code name to module XMLs found on disk:
                        Map/*<String,FileObject>*/ xmlfiles = prepareXMLFiles();
                        // Code name to properties for dirty XML or XML sans status only.
                        Map/*<String,Map<String,Object>>*/ dirtyprops = prepareDirtyProps(xmlfiles);
                        // #27106: do not listen to changes we ourselves produce.
                        // It only matters if statuses has not been updated before
                        // the changes are fired.
                        listening = false;
                        try {
                            stepCheckReloadable(xmlfiles, dirtyprops);
                            stepCreate(xmlfiles, dirtyprops);
                            stepEnable(xmlfiles, dirtyprops);
                            stepDisable(xmlfiles, dirtyprops);
                            stepDelete(xmlfiles, dirtyprops);
                            stepCheckMisc(xmlfiles, dirtyprops);
                            stepCheckSer(xmlfiles, dirtyprops);
                        } finally {
                            listening = true;
                            stepUpdateProps(xmlfiles, dirtyprops);
                            stepMarkClean(xmlfiles, dirtyprops);
                        }
                    }
                });
                Util.err.log("ModuleList: finished processing outstanding external XML changes");
            } catch (IOException ioe) {
                Util.err.notify(ioe);
            } finally {
                mgr.mutexPrivileged().exitWriteAccess();
            }
        }
        // All the steps called from the run() method to handle disk changes:
        private Map/*<String,FileObject>*/ prepareXMLFiles() {
            Util.err.log("ModuleList: prepareXMLFiles");
            Map/*<String,FileObject>*/ xmlfiles = new HashMap(100);
            FileObject[] kids = folder.getChildren();
            for (int i = 0; i < kids.length; i++) {
                if (kids[i].hasExt("xml")) { // NOI18N
                    xmlfiles.put(kids[i].getName().replace('-', '.'), kids[i]);
                }
            }
            return xmlfiles;
        }
        private Map/*<String,Map<String,Object>>*/ prepareDirtyProps(Map/*<String,FileObject>*/ xmlfiles) throws IOException {
            Util.err.log("ModuleList: prepareDirtyProps");
            Map/*<String,Map<String,Object>>*/ dirtyprops = new HashMap(100);
            Iterator it = xmlfiles.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                String cnb = (String)entry.getKey();
                DiskStatus status = (DiskStatus)statuses.get(cnb);
                if (status == null || status.dirty) {
                    FileObject xmlfile = (FileObject)entry.getValue();
                    if (xmlfile == null || ! xmlfile.canRead ()) {
                        continue;
                    }
                    InputStream is = xmlfile.getInputStream();
                    try {
                        InputSource src = new InputSource(is);
                        src.setSystemId(xmlfile.getURL().toString());
                        try {
                            dirtyprops.put(cnb, readStatus(src, null));
                        } catch (SAXException saxe) {
                            IOException ioe = new IOException(saxe.toString());
                            Util.err.annotate(ioe, saxe);
                            throw ioe;
                        }
                    } finally {
                        is.close();
                    }
                }
            }
            return dirtyprops;
        }
        private void stepCheckReloadable(Map/*<String,FileObject>*/ xmlfiles, Map/*<String,Map<String,Object>>*/ dirtyprops) {
            Util.err.log("ModuleList: stepCheckReloadable");
            Iterator it = dirtyprops.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                String cnb = (String)entry.getKey();
                DiskStatus status = (DiskStatus)statuses.get(cnb);
                if (status != null) {
                    Map props = (Map)entry.getValue();
                    Boolean diskReloadableB = (Boolean)props.get("reloadable"); // NOI18N
                    boolean diskReloadable = (diskReloadableB != null ? diskReloadableB.booleanValue() : false);
                    boolean memReloadable = status.module.isReloadable();
                    if (memReloadable != diskReloadable) {
                        Util.err.log("Disk change in reloadable for " + cnb + " from " + memReloadable + " to " + diskReloadable);
                        status.module.setReloadable(diskReloadable);
                    }
                }
            }
        }
        private void stepCreate(Map/*<String,FileObject>*/ xmlfiles, Map/*<String,Map<String,Object>>*/ dirtyprops) throws IOException {
            Util.err.log("ModuleList: stepCreate");
            Iterator it = xmlfiles.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                String cnb = (String)entry.getKey();
                if (! statuses.containsKey(cnb)) {
                    FileObject xmlfile = (FileObject)entry.getValue();
                    Map props = (Map)dirtyprops.get(cnb);
                    if (! cnb.equals(props.get("name"))) throw new IOException("Code name mismatch"); // NOI18N
                    String jar = (String)props.get("jar"); // NOI18N
                    File jarFile = findJarByName(jar, cnb);
                    Boolean reloadableB = (Boolean)props.get("reloadable"); // NOI18N
                    boolean reloadable = (reloadableB != null ? reloadableB.booleanValue() : false);
                    Boolean autoloadB = (Boolean)props.get("autoload"); // NOI18N
                    boolean autoload = (autoloadB != null ? autoloadB.booleanValue() : false);
                    Boolean eagerB = (Boolean)props.get("eager"); // NOI18N
                    boolean eager = (eagerB != null ? eagerB.booleanValue() : false);
                    Module m;
                    try {
                        m = mgr.create(jarFile, new ModuleHistory(jar), reloadable, autoload, eager);
                    } catch (DuplicateException dupe) {
                        // XXX should this be tolerated somehow? In case the original is
                        // in fact scheduled for deletion anyway?
                        IOException ioe = new IOException(dupe.toString());
                        Util.err.annotate(ioe, dupe);
                        throw ioe;
                    }
                    m.addPropertyChangeListener(this);
                    // Mark the status as disabled for the moment, so in step 3 it will be turned on
                    // if in dirtyprops it was marked enabled.
                    Map statusProps;
                    if (props.get("enabled") != null && ((Boolean)props.get("enabled")).booleanValue()) { // NOI18N
                        statusProps = new HashMap(props);
                        statusProps.put("enabled", Boolean.FALSE); // NOI18N
                    } else {
                        statusProps = props;
                    }
                    DiskStatus status = new DiskStatus();
                    status.module = m;
                    status.file = xmlfile;
                    status.diskProps = statusProps;
                    statuses.put(cnb, status);
                }
            }
        }
        private void stepEnable(Map/*<String,FileObject>*/ xmlfiles, Map/*<String,Map<String,Object>>*/ dirtyprops) throws IOException {
            Util.err.log("ModuleList: stepEnable");
            Set/*<Module>*/ toenable = new HashSet();
            Iterator it = dirtyprops.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                String cnb = (String)entry.getKey();
                Map props = (Map)entry.getValue();
                if (props.get("enabled") != null && ((Boolean)props.get("enabled")).booleanValue()) { // NOI18N
                    DiskStatus status = (DiskStatus)statuses.get(cnb);
                    if (status.diskProps.get("enabled") == null || ! ((Boolean)status.diskProps.get("enabled")).booleanValue()) { // NOI18N
                        if (status.module.isEnabled()) throw new IllegalStateException("Already enabled: " + status.module); // NOI18N
                        toenable.add(status.module);
                    }
                }
            }
            installNew(toenable);
        }
        private void stepDisable(Map/*<String,FileObject>*/ xmlfiles, Map/*<String,Map<String,Object>>*/ dirtyprops) throws IOException {
            Util.err.log("ModuleList: stepDisable");
            Set/*<Module>*/ todisable = new HashSet();
            Iterator it = dirtyprops.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                String cnb = (String)entry.getKey();
                Map props = (Map)entry.getValue();
                if (props.get("enabled") == null || ! ((Boolean)props.get("enabled")).booleanValue()) { // NOI18N
                    DiskStatus status = (DiskStatus)statuses.get(cnb);
                    if (status.diskProps.get("enabled") != null && ((Boolean)status.diskProps.get("enabled")).booleanValue()) { // NOI18N
                        if (! status.module.isEnabled()) throw new IllegalStateException("Already disabled: " + status.module); // NOI18N
                        todisable.add(status.module);
                    }
                }
            }
            if (todisable.isEmpty()) {
                return;
            }
            List reallydisable = mgr.simulateDisable(todisable);
            it = reallydisable.iterator();
            while (it.hasNext()) {
                Module m = (Module)it.next();
                if (!m.isAutoload() && !m.isEager() && !todisable.contains(m)) {
                    todisable.add(m);
                }
            }
            mgr.disable(todisable);
        }
        private void stepDelete(Map/*<String,FileObject>*/ xmlfiles, Map/*<String,Map<String,Object>>*/ dirtyprops) throws IOException {
            Util.err.log("ModuleList: stepDelete");
            Set/*<Module>*/ todelete = new HashSet();
            Iterator it = statuses.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                String cnb = (String)entry.getKey();
                DiskStatus status = (DiskStatus)entry.getValue();
                if (! xmlfiles.containsKey(cnb)) {
                    Module m = status.module;
                    todelete.add(m);
                    it.remove();
                }
            }
            if (todelete.isEmpty()) {
                return;
            }
            Set/*<Module>*/ todisable = new HashSet();
            it = todelete.iterator();
            while (it.hasNext()) {
                Module m = (Module)it.next();
                if (m.isEnabled() && !m.isAutoload() && !m.isEager()) {
                    todisable.add(m);
                }
            }
            List reallydisable = mgr.simulateDisable(todisable);
            it = reallydisable.iterator();
            while (it.hasNext()) {
                Module m = (Module)it.next();
                if (!m.isAutoload() && !m.isEager() && !todisable.contains(m)) {
                    todisable.add(m);
                }
            }
            mgr.disable(todisable);
            // In case someone tried to delete an enabled autoload/eager module:
            it = todelete.iterator();
            while (it.hasNext()) {
                Module m = (Module)it.next();
                if (m.isEnabled()) {
                    if (!m.isAutoload() && !m.isEager()) throw new IllegalStateException("Module " + m + " scheduled for deletion could not be disabled yet was not an autoload nor eager"); // NOI18N
                    // XXX is it better to find all regular module using it and turn all of those off?
                    ev.log(Events.CANT_DELETE_ENABLED_AUTOLOAD, m);
                    it.remove();
                } else {
                    mgr.delete(m);
                }
            }
        }
        private void stepCheckMisc(Map/*<String,FileObject>*/ xmlfiles, Map/*<String,Map<String,Object>>*/ dirtyprops) {
            Util.err.log("ModuleList: stepCheckMisc");
            String[] toCheck = {"jar", "autoload", "eager", "release", "specversion"}; // NOI18N
            Iterator it = dirtyprops.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                String cnb = (String)entry.getKey();
                Map props = (Map)entry.getValue();
                DiskStatus status = (DiskStatus)statuses.get(cnb);
                Map diskProps = status.diskProps;
                for (int i = 0; i < toCheck.length; i++) {
                    String prop = toCheck[i];
                    Object onDisk = props.get(prop);
                    Object inMem = diskProps.get(prop);
                    if (! Utilities.compareObjects(onDisk, inMem)) {
                        ev.log(Events.MISC_PROP_MISMATCH, status.module, prop, onDisk, inMem);
                    }
                }
            }
        }
        private void stepCheckSer(Map/*<String,FileObject>*/ xmlfiles, Map/*<String,Map<String,Object>>*/ dirtyprops) {
            // There is NO step 7!
        }
        private void stepUpdateProps(Map/*<String,FileObject>*/ xmlfiles, Map/*<String,Map<String,Object>>*/ dirtyprops) {
            Util.err.log("ModuleList: stepUpdateProps");
            Iterator it = dirtyprops.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                String cnb = (String)entry.getKey();
                DiskStatus status = (DiskStatus)statuses.get(cnb);
                if (status != null) {
                    Map props = (Map)entry.getValue();
                    status.diskProps = props;
                }
            }
        }
        private void stepMarkClean(Map/*<String,FileObject>*/ xmlfiles, Map/*<String,Map<String,Object>>*/ dirtyprops) {
            Util.err.log("ModuleList: stepMarkClean");
            Iterator it = statuses.values().iterator();
            while (it.hasNext()) {
                DiskStatus status = (DiskStatus)it.next();
                status.dirty = false;
            }
        }
        
    }
    
    /** Representation of the status of a module on disk and so on. */
    private static final class DiskStatus {
        /** Initialize as a struct, i.e. member by member: */
        public DiskStatus() {}
        /** actual module object */
        public Module module;
        /** XML file holding its status */
        public FileObject file;
        /** timestamp of last modification to XML file that this class did */
        //public long lastApprovedChange;
        /** if true, this module was scanned and should be enabled but we are waiting for trigger */
        public boolean pendingInstall = false;
        /** properties of the module on disk */
        public Map/*<String,String|Integer|Boolean|SpecificationVersion>*/ diskProps;
        /** if true, the XML was changed on disk by someone else */
        public boolean dirty = false;
        /** for debugging: */
        @Override
				public String toString() {
            return "DiskStatus[module=" + module + // NOI18N
                ",valid=" + module.isValid() + // NOI18N
                ",file=" + file + /*",lastApprovedChange=" + new Date(lastApprovedChange) +*/ // NOI18N
                ",dirty=" + dirty + // NOI18N
                ",pendingInstall=" + pendingInstall + // NOI18N
                ",diskProps=" + diskProps + "]"; // NOI18N
        }
    }
    
}
