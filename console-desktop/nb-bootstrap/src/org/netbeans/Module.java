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

package org.netbeans;

import java.io.File;
import java.io.IOException;
import java.security.AllPermission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.openide.ErrorManager;
import org.openide.modules.Dependency;
import org.openide.modules.ModuleInfo;
import org.openide.modules.SpecificationVersion;
import org.openide.util.NbBundle;
import org.openide.util.WeakSet;

/** Object representing one module, possibly installed.
 * Responsible for opening of module JAR file; reading
 * manifest; parsing basic information such as dependencies;
 * and creating a classloader for use by the installer.
 * Methods not defined in ModuleInfo must be called from within
 * the module manager's read mutex as a rule.
 * @author Jesse Glick
 * @since 2.1 the class was made public abstract
 */
public abstract class Module extends ModuleInfo {
    
    public static final String PROP_RELOADABLE = "reloadable"; // NOI18N
    public static final String PROP_CLASS_LOADER = "classLoader"; // NOI18N
    public static final String PROP_MANIFEST = "manifest"; // NOI18N
    public static final String PROP_VALID = "valid"; // NOI18N
    public static final String PROP_PROBLEMS = "problems"; // NOI18N
    
    /** module manifest */
    protected Manifest manifest;
    /** manager which owns this module */
    protected final ModuleManager mgr;
    /** event logging (should not be much here) */
    protected final Events events;
    /** associated history object
     * @see ModuleHistory
     */
    private final Object history;
    /** true if currently enabled; manipulated by ModuleManager */
    private boolean enabled;
    /** whether it is supposed to be automatically loaded when required */
    private final boolean autoload;
    /** */
    protected boolean reloadable;
    /** if true, this module is eagerly turned on whenever it can be */
    private final boolean eager;
    /** code name base (no slash) */
    private String codeNameBase;
    /** code name release, or -1 if undefined */
    private int codeNameRelease;
    /** full code name */
    private String codeName;
    /** provided tokens */
    private String[] provides;
    /** set of dependencies parsed from manifest */
    private Dependency[] dependenciesA;
    /** specification version parsed from manifest, or null */
    private SpecificationVersion specVers;
    /** currently active module classloader */
    protected ClassLoader classloader = null;
    /** localized properties, only non-null if requested from disabled module */
    private Properties localizedProps;
    /** public packages, may be null */
    private PackageExport[] publicPackages;
    /** Set<String> of CNBs of friend modules or null */
    private Set/*<String>*/ friendNames;
    
    /** Use ModuleManager.create as a factory. */
    protected Module(ModuleManager mgr, Events ev, Object history, boolean reloadable, boolean autoload, boolean eager) throws IOException {
        if (autoload && eager) throw new IllegalArgumentException("A module may not be both autoload and eager"); // NOI18N
        this.mgr = mgr;
        this.events = ev;
        this.history = history;
        this.reloadable = reloadable;
        this.autoload = autoload;
        this.eager = eager;
        enabled = false;
    }
    
    /** Create a special-purpose "fixed" JAR. */
    protected Module(ModuleManager mgr, Events ev, Manifest manifest, Object history, ClassLoader classloader) throws InvalidException {
        this.mgr = mgr;
        this.events = ev;
        this.manifest = manifest;
        this.history = history;
        this.classloader = classloader;
        reloadable = false;
        autoload = false;
        eager = false;
        enabled = false;
    }
    
    /** Get the associated module manager. */
    public ModuleManager getManager() {
        return mgr;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    // Access from ModuleManager:
    void setEnabled(boolean enabled) {
        /* #13647: actually can happen if loading of bootstrap modules is rolled back: */
        if (isFixed() && ! enabled) throw new IllegalStateException("Cannot disable a fixed module: " + this); // NOI18N
        this.enabled = enabled;
    }
    
    /** Normally a module once created and managed is valid
     * (that is, either installed or not, but at least managed).
     * If it is deleted any remaining references to it become
     * invalid.
     */
    public boolean isValid() {
        return mgr.get(getCodeNameBase()) == this;
    }
    
    /** Is this module automatically loaded?
     * If so, no information about its state is kept
     * permanently beyond the existence of its JAR file;
     * it is enabled when some real module needs it to be,
     * and disabled when this is no longer the case.
     * @see <a href="http://www.netbeans.org/issues/show_bug.cgi?id=9779">#9779</a>
     */
    public boolean isAutoload() {
        return autoload;
    }
    
    /** Is this module eagerly enabled?
     * If so, no information about its state is kept permanently.
     * It is turned on whenever it can be, i.e. whenever it meets all of
     * its dependencies. This may be used to implement "bridge" modules with
     * simple functionality that just depend on two normal modules.
     * A module may not be simultaneously eager and autoload.
     * @see <a href="http://www.netbeans.org/issues/show_bug.cgi?id=17501">#17501</a>
     * @since org.netbeans.core/1 1.3
     */
    public boolean isEager() {
        return eager;
    }
    
    /** Get an associated arbitrary attribute.
     * Right now, simply provides the main attributes of the manifest.
     * In the future some of these could be suppressed (if only of dangerous
     * interest, e.g. Class-Path) or enhanced with other information available
     * from the core (if needed).
     */
    public Object getAttribute(String attr) {
        return getManifest().getMainAttributes().getValue(attr);
    }
    
    public String getCodeName() {
        return codeName;
    }
    
    public String getCodeNameBase() {
        return codeNameBase;
    }
    
    public int getCodeNameRelease() {
        return codeNameRelease;
    }
    
    public String[] getProvides() {
        return provides;
    }
    /** Test whether the module provides a given token or not. 
     * @since JST-PENDING again used from NbProblemDisplayer
     */
    public final boolean provides(String token) {
        for (int i = 0; i < provides.length; i++) {
            if (provides[i].equals(token)) {
                return true;
            }
        }
        return false;
    }
    
    public Set getDependencies() {
        return new HashSet(Arrays.asList(dependenciesA));
    }
    // Faster to loop over:
    // @since JST-PENDING called from NbInstaller
    public final Dependency[]  getDependenciesArray() {
        return dependenciesA;
    }
    
    public SpecificationVersion getSpecificationVersion() {
        return specVers;
    }
    
    public boolean owns(Class clazz) {
        ClassLoader cl = clazz.getClassLoader();
        if (cl instanceof Util.ModuleProvider) {
            return ((Util.ModuleProvider) cl).getModule() == this;
        }
        return false;
        
    }
    
    /** Get all packages exported by this module to other modules.
     * @return a list (possibly empty) of exported packages, or null to export everything
     * @since org.netbeans.core/1 > 1.4
     * @see "#19621"
     */
    public PackageExport[] getPublicPackages() {
        return publicPackages;
    }
    
    /** Checks whether we use friends attribute and if so, then
     * whether the name of module is listed there.
     */
    boolean isDeclaredAsFriend (Module module) {
        if (friendNames == null) {
            return true;
        }
        return friendNames.contains(module.getCodeNameBase());
    }
    
    /** Parse information from the current manifest.
     * Includes code name, specification version, and dependencies.
     * If anything is in an invalid format, throws an exception with
     * some kind of description of the problem.
     */
    protected void parseManifest() throws InvalidException {
        Attributes attr = manifest.getMainAttributes();
        // Code name
        codeName = attr.getValue("OpenIDE-Module"); // NOI18N
        if (codeName == null) {
            InvalidException e = new InvalidException("Not a module: no OpenIDE-Module tag in manifest of " + /* #17629: important! */this); // NOI18N
            // #29393: plausible user mistake, deal with it politely.
            Util.err.annotate(e, NbBundle.getMessage(Module.class, "EXC_not_a_module", this.toString()));
            throw e;
        }
        try {
            // This has the side effect of checking syntax:
            if (codeName.indexOf(',') != -1) {
                throw new InvalidException("Illegal code name syntax parsing OpenIDE-Module: " + codeName); // NOI18N
            }
            Dependency.create(Dependency.TYPE_MODULE, codeName);
            Object[] cnParse = Util.parseCodeName(codeName);
            codeNameBase = (String)cnParse[0];
            codeNameRelease = (cnParse[1] != null) ? ((Integer)cnParse[1]).intValue() : -1;
            if (cnParse[2] != null) throw new NumberFormatException(codeName);
            // Spec vers
            String specVersS = attr.getValue("OpenIDE-Module-Specification-Version"); // NOI18N
            if (specVersS != null) {
                try {
                    specVers = new SpecificationVersion(specVersS);
                } catch (NumberFormatException nfe) {
                    InvalidException ie = new InvalidException("While parsing OpenIDE-Module-Specification-Version: " + nfe.toString()); // NOI18N
                    Util.err.annotate(ie, nfe);
                    throw ie;
                }
            } else {
                specVers = null;
            }
            // Token provides
            String providesS = attr.getValue("OpenIDE-Module-Provides"); // NOI18N
            if (providesS == null) {
                provides = new String[] {};
            } else {
                StringTokenizer tok = new StringTokenizer(providesS, ", "); // NOI18N
                provides = new String[tok.countTokens()];
                for (int i = 0; i < provides.length; i++) {
                    String provide = tok.nextToken();
                    if (provide.indexOf(',') != -1) {
                        throw new InvalidException("Illegal code name syntax parsing OpenIDE-Module-Provides: " + provide); // NOI18N
                    }
                    Dependency.create(Dependency.TYPE_MODULE, provide);
                    if (provide.lastIndexOf('/') != -1) throw new IllegalArgumentException("Illegal OpenIDE-Module-Provides: " + provide); // NOI18N
                    provides[i] = provide;
                }
                if (new HashSet(Arrays.asList(provides)).size() < provides.length) {
                    throw new IllegalArgumentException("Duplicate entries in OpenIDE-Module-Provides: " + providesS); // NOI18N
                }
            }
            String[] additionalProvides = mgr.refineProvides (this);
            if (additionalProvides != null) {
                if (provides == null) {
                    provides = additionalProvides;
                } else {
                    ArrayList l = new ArrayList ();
                    l.addAll (Arrays.asList (provides));
                    l.addAll (Arrays.asList (additionalProvides));
                    provides = (String[])l.toArray (provides);
                }
            }
            
            // Exports
            String exportsS = attr.getValue("OpenIDE-Module-Public-Packages"); // NOI18N
            if (exportsS != null) {
                if (exportsS.trim().equals("-")) { // NOI18N
                    publicPackages = new PackageExport[0];
                } else {
                    StringTokenizer tok = new StringTokenizer(exportsS, ", "); // NOI18N
                    List exports = new ArrayList(Math.max(tok.countTokens(), 1)); // List<PackageExport>
                    while (tok.hasMoreTokens()) {
                        String piece = tok.nextToken();
                        if (piece.endsWith(".*")) { // NOI18N
                            String pkg = piece.substring(0, piece.length() - 2);
                            Dependency.create(Dependency.TYPE_MODULE, pkg);
                            if (pkg.lastIndexOf('/') != -1) throw new IllegalArgumentException("Illegal OpenIDE-Module-Public-Packages: " + exportsS); // NOI18N
                            exports.add(new PackageExport(pkg.replace('.', '/') + '/', false));
                        } else if (piece.endsWith(".**")) { // NOI18N
                            String pkg = piece.substring(0, piece.length() - 3);
                            Dependency.create(Dependency.TYPE_MODULE, pkg);
                            if (pkg.lastIndexOf('/') != -1) throw new IllegalArgumentException("Illegal OpenIDE-Module-Public-Packages: " + exportsS); // NOI18N
                            exports.add(new PackageExport(pkg.replace('.', '/') + '/', true));
                        } else {
                            throw new IllegalArgumentException("Illegal OpenIDE-Module-Public-Packages: " + exportsS); // NOI18N
                        }
                    }
                    if (exports.isEmpty()) throw new IllegalArgumentException("Illegal OpenIDE-Module-Public-Packages: " + exportsS); // NOI18N
                    publicPackages = (PackageExport[])exports.toArray(new PackageExport[exports.size()]);
                }
            } else {
                // XXX new link?
                Util.err.log(ErrorManager.WARNING, "Warning: module " + codeNameBase + " does not declare OpenIDE-Module-Public-Packages in its manifest, so all packages are considered public by default: http://www.netbeans.org/download/dev/javadoc/OpenAPIs/org/openide/doc-files/upgrade.html#3.4-public-packages");
                publicPackages = null;
            }
            
            {
                // friends 
                String friends = attr.getValue("OpenIDE-Module-Friends"); // NOI18N
                if (friends != null) {
                    StringTokenizer tok = new StringTokenizer(friends, ", "); // NOI18N
                    HashSet set = new HashSet ();
                    while (tok.hasMoreTokens()) {
                        String piece = tok.nextToken();
                        if (piece.indexOf('/') != -1) {
                            throw new IllegalArgumentException("May specify only module code name bases in OpenIDE-Module-Friends, not major release versions: " + piece); // NOI18N
                        }
                        // Indirect way of checking syntax:
                        Dependency.create(Dependency.TYPE_MODULE, piece);
                        // OK, add it.
                        set.add(piece);
                    }
                    if (set.isEmpty()) {
                        throw new IllegalArgumentException("Empty OpenIDE-Module-Friends: " + friends); // NOI18N
                    }
                    if (publicPackages == null || publicPackages.length == 0) {
                        throw new IllegalArgumentException("No use specifying OpenIDE-Module-Friends without any public packages: " + friends); // NOI18N
                    }
                    this.friendNames = set;
                }
            }
            
            
            // Dependencies
            Set dependencies = new HashSet(20); // Set<Dependency>
            // First convert IDE/1 -> org.openide/1, so we never have to deal with
            // "IDE deps" internally:
            Set openideDeps = Dependency.create(Dependency.TYPE_IDE, attr.getValue("OpenIDE-Module-IDE-Dependencies")); // NOI18N
            if (!openideDeps.isEmpty()) {
                // If empty, leave it that way; NbInstaller will add it anyway.
                Dependency d = (Dependency)openideDeps.iterator().next();
                String name = d.getName();
                if (!name.startsWith("IDE/")) throw new IllegalStateException("Weird IDE dep: " + name); // NOI18N
                dependencies.addAll(Dependency.create(Dependency.TYPE_MODULE, "org.openide/" + name.substring(4) + " > " + d.getVersion())); // NOI18N
                if (dependencies.size() != 1) throw new IllegalStateException("Should be singleton: " + dependencies); // NOI18N
                
                Util.err.log(ErrorManager.WARNING, "Warning: the module " + codeNameBase + " uses OpenIDE-Module-IDE-Dependencies which is deprecated. See http://openide.netbeans.org/proposals/arch/modularize.html"); // NOI18N
            }
            dependencies.addAll(Dependency.create(Dependency.TYPE_JAVA, attr.getValue("OpenIDE-Module-Java-Dependencies"))); // NOI18N
            dependencies.addAll(Dependency.create(Dependency.TYPE_MODULE, attr.getValue("OpenIDE-Module-Module-Dependencies"))); // NOI18N
            String pkgdeps = attr.getValue("OpenIDE-Module-Package-Dependencies"); // NOI18N
            if (pkgdeps != null) {
                // XXX: Util.err.log(ErrorManager.WARNING, "Warning: module " + codeNameBase + " uses the OpenIDE-Module-Package-Dependencies manifest attribute, which is now deprecated: XXX URL TBD");
                dependencies.addAll(Dependency.create(Dependency.TYPE_PACKAGE, pkgdeps)); // NOI18N
            }
            dependencies.addAll(Dependency.create(Dependency.TYPE_REQUIRES, attr.getValue("OpenIDE-Module-Requires"))); // NOI18N
            // Permit the concrete installer to make some changes:
            mgr.refineDependencies(this, dependencies);
            dependenciesA = (Dependency[])dependencies.toArray(new Dependency[dependencies.size()]);
        } catch (IllegalArgumentException iae) {
            InvalidException ie = new InvalidException("While parsing a dependency attribute: " + iae.toString()); // NOI18N
            Util.err.annotate(ie, iae);
            throw ie;
        }
    }

    /** Get all JARs loaded by this module.
     * Includes the module itself, any locale variants of the module,
     * any extensions specified with Class-Path, any locale variants
     * of those extensions.
     * The list will be in classpath order (patches first).
     * Currently the temp JAR is provided in the case of test modules, to prevent
     * sporadic ZIP file exceptions when background threads (like Java parsing) tries
     * to open libraries found in the library path.
     * JARs already present in the classpath are <em>not</em> listed.
     * @return a <code>List&lt;File&gt;</code> of JARs
     */
    public abstract List getAllJars();

    /** Is this module supposed to be easily reloadable?
     * If so, it is suitable for testing inside the IDE.
     * Controls whether a copy of the JAR file is made before
     * passing it to the classloader, which can affect locking
     * and refreshing of the JAR.
     */
    public boolean isReloadable() {
        return reloadable;
    }
    
    /** Set whether this module is supposed to be reloadable.
     * Has no immediate effect, only impacts what happens the
     * next time it is enabled (after having been disabled if
     * necessary).
     * Must be called from within a write mutex.
     * @param r whether the module should be considered reloadable
     */
    public abstract void setReloadable(boolean r);
    
    /** Used as a flag to tell if this module was really successfully released.
     * Currently does not work, so if it cannot be made to work, delete it.
     * (Someone seems to be holding a strong reference to the classloader--who?!)
     */
    private transient boolean released;
    /** Count which release() call is really being checked. */
    private transient int releaseCount = 0;

    /** Reload this module. Access from ModuleManager.
     * If an exception is thrown, the module is considered
     * to be in an invalid state.
     * @since JST-PENDING: needed from ModuleSystem
     */
    public abstract void reload() throws IOException;
    
    // impl of ModuleInfo method
    public ClassLoader getClassLoader() throws IllegalArgumentException {
        if (!enabled) {
            throw new IllegalArgumentException("Not enabled: " + codeNameBase); // NOI18N
        }
        assert classloader != null : "Should have had a non-null loader for " + this;
        return classloader;
    }

    // Access from ModuleManager:
    /** Turn on the classloader. Passed a list of parent modules to use.
     * The parents should already have had their classloaders initialized.
     */
    protected abstract void classLoaderUp(Set parents) throws IOException;

    /** Turn off the classloader and release all resources. */
    protected abstract void classLoaderDown();
    /** Should be called after turning off the classloader of one or more modules & GC'ing. */
    protected abstract void cleanup();
    
    /** Notify the module that it is being deleted. */
    protected abstract void destroy();
    
    /**
     * Fixed modules are treated differently.
     * @see FixedModule
     */
    public abstract boolean isFixed();
    
    /** Get the JAR this module is packaged in.
     * May be null for modules installed specially, e.g.
     * automatically from the classpath.
     * @see #isFixed
     */
    public File getJarFile() {
        return null;
    }
    
    /** Get the JAR manifest.
     * Should never be null, even if disabled.
     * Might change if a module is reloaded.
     * It is not guaranteed that change events will be fired
     * for changes in this property.
     */
    public Manifest getManifest() {
        return manifest;
    }
    
    /** Get a set of {@link org.openide.modules.Dependency} objects representing missed dependencies.
     * This module is examined to see
     * why it would not be installable.
     * If it is enabled, there are no problems.
     * If it is in fact installable (possibly only
     * by also enabling some other managed modules which are currently disabled), and
     * all of its non-module dependencies are met, the returned set will be empty.
     * Otherwise it will contain a list of reasons why this module cannot be installed:
     * non-module dependencies which are not met; and module dependencies on modules
     * which either do not exist in the managed set, or are the wrong version,
     * or themselves cannot be installed
     * for some reason or another (which may be separately examined).
     * Note that in the (illegal) situation of two or more modules forming a cyclic
     * dependency cycle, none of them will be installable, and the missing dependencies
     * for each will be stated as the dependencies on the others. Again other modules
     * dependent on modules in the cycle will list failed dependencies on the cyclic modules.
     * Missing package dependencies are not guaranteed to be reported unless an install
     * of the module has already been attempted, and failed due to them.
     * The set may also contain {@link InvalidException}s representing known failures
     * of the module to be installed, e.g. due to classloader problems, missing runtime
     * resources, or failed ad-hoc dependencies. Again these are not guaranteed to be
     * reported unless an install has already been attempted and failed due to them.
     */
    public Set getProblems() {
        if (! isValid()) throw new IllegalStateException("Not valid: " + this); // NOI18N
        if (isEnabled()) return Collections.EMPTY_SET;
        return Collections.unmodifiableSet(mgr.missingDependencies(this));
    }
    
    // Access from ChangeFirer:
    final void firePropertyChange0(String prop, Object old, Object nue) {
        if (Util.err.isLoggable(ErrorManager.INFORMATIONAL)) {
            Util.err.log("Module.propertyChange: " + this + " " + prop + ": " + old + " -> " + nue);
        }
        firePropertyChange(prop, old, nue);
    }
    
    /** Get the history object representing what has happened to this module before.
     * @see ModuleHistory
     */
    public final Object getHistory() {
        return history;
    }
    
    /** String representation for debugging. */
    public String toString() {
        String s = "Module:" + getCodeNameBase(); // NOI18N
        if (!isValid()) s += "[invalid]"; // NOI18N
        return s;
    }
    
    /** PermissionCollection with an instance of AllPermission. */
    private static PermissionCollection modulePermissions;
    /** @return initialized @see #modulePermission */
    private static synchronized PermissionCollection getAllPermission() {
        if (modulePermissions == null) {
            modulePermissions = new Permissions();
            modulePermissions.add(new AllPermission());
            modulePermissions.setReadOnly();
        }
        return modulePermissions;
    }

    /** Struct representing a package exported from a module.
     * @since org.netbeans.core/1 > 1.4
     * @see Module#getPublicPackages
     */
    public static final class PackageExport {
        /** Package to export, in the form <samp>org/netbeans/modules/foo/</samp>. */
        public final String pkg;
        /** If true, export subpackages also. */
        public final boolean recursive;
        /** Create a package export struct with the named parameters. */
        public PackageExport(String pkg, boolean recursive) {
            this.pkg = pkg;
            this.recursive = recursive;
        }
        public String toString() {
            return "PackageExport[" + pkg + (recursive ? "**/" : "") + "]"; // NOI18N
        }
    }

}
