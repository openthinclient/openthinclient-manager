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

package org.netbeans.core.startup.layers;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.openide.filesystems.FileSystem;
import org.openide.filesystems.XMLFileSystem;

/**
 * Fake cache manager that does not in fact cache anything.
 * Just uses an in-memory XMLFileSystem.
 * @author Jesse Glick
 */
final class NonCacheManager extends LayerCacheManager {
    
    public NonCacheManager() {
        super(null);
    }
    
    public boolean cacheExists() {
        return false;
    }
    
    public void cleanupCache() throws IOException {
        // do nothing
    }
    
    public boolean supportsLoad() {
        return true;
    }
    
    public FileSystem createEmptyFileSystem() throws IOException {
        return new XMLFileSystem();
    }
    
    public void load(FileSystem fs) throws IOException {
        throw new IllegalStateException();
    }
    
    public void store(FileSystem fs, List urls) throws IOException {
        try {
            ((XMLFileSystem)fs).setXmlUrls((URL[])urls.toArray(new URL[urls.size()]));
        } catch (PropertyVetoException pve) {
            IOException ioe = new IOException(pve.toString());
            LayerCacheManager.err.annotate(ioe, pve);
            throw ioe;
        }
    }
    
}
