/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Initial Developer of the Original Code is Laszlo Kishalmi. 
 * Portions Copyright 2004-2004 Laszlo Kishalmi. All Rights Reserved.
 */

package org.netbeans.core.startup.layers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import sun.net.www.protocol.jar.URLJarFile;

/**
 * A new JarURLConnection class which intends to replace the original buggy
 * JarURLConnection in the JRE 1.4.1. It can be used only with <code>file:</code> 
 * immediate URLs. 
 * <p>The cache works much better than in the original JarURLConnection.
 * It uses soft references and caches internal URLConnections direclty. NetBeans
 * by default does not use URLConnection cache. This behaviour has been kept, but 
 * can be overridden by setting the <code>netbeans.core.jse141urlpatch.jar.forcecache</code>
 * system property to <code>true</code>. (<b>Note:</b> Caching does not have too much
 * impact on the system performance. NetBeans loads approximately 100 jars URL-s
 * on startup, calling about 640 times on <code>connect()</code> and the garbage
 * collector usually cleans 150 cache entries.)
 * @author Laszlo Kishalmi
 */
public class JarURLConnection extends java.net.JarURLConnection {
    
    /**
     * The URL cache storage. A synchronised HashMap.
     * @see java.util.HashMap
     */
    protected static Map cache = Collections.synchronizedMap(new HashMap());
    /**
     * Cache forcing flag. Set when <code>netbeans.core.jse141urlpatch.jar.forcecache</code>
     * system property is <code>true</code>.
     */
    protected static final boolean force_cache = Boolean.getBoolean("netbeans.core.jse141urlpatch.jar.forcecache");
    
    /** The entry in the jar file. */
    protected  JarEntry jarEntry;

    /** The jar file corresponding to this connection. */
    protected  JarFile jarFile;

    /** The content type for this connection */
    protected  String contentType;

    /**
     * Simple static storage class for the cached objects.
     */
    protected static class CacheEntry {
        /**
         * The cached jar file.
         */
        protected JarFile jarFile;
        /**
         * The cached URL connection.
         */
        protected URLConnection conn;
        
        /**
         * Constructs a new CacheEnrty with JarFile and connection.
         * @param jarFile The cached jar file.
         * @param conn The cached URLConnection.
         */
        protected CacheEntry(JarFile jarFile, URLConnection conn) {
            this.jarFile = jarFile;
            this.conn = conn;
        }
        
        /**
         * Returns the cached jar file.
         * @return the cached jar file.
         */
        public JarFile getJarFile() {
            return jarFile;
        }
        
        /**
         * Returns the cached URL connection.
         * @return the cached URL connection.
         */
        public URLConnection getConnection() {
            return conn;
        }
    }
    
    /** {@inheritDoc} */
    public JarURLConnection(URL url) throws MalformedURLException, IOException {
        super(url);
        
    }

    /** {@inheritDoc} */
    public void connect() throws IOException {
	if (!connected) {
            URL url = getJarFileURL();
            String entryName = getEntryName();
            if (getUseCaches()||force_cache) {
                SoftReference ref = (SoftReference) cache.get(url);
                CacheEntry ce = ref != null ? (CacheEntry) ref.get() : null;
                if (ce == null) {
                    ce = new CacheEntry(createJarFile(url), url.openConnection());
                    synchronized (cache) {
                        cache.put(url, new SoftReference(ce));
                    }
                }
                jarFile = ce.getJarFile();
                jarFileURLConnection = ce.getConnection();
                
            } else {
                jarFile = createJarFile(url);
                jarFileURLConnection = url.openConnection();
            }
            if (entryName != null) {
                jarEntry = (JarEntry)jarFile.getEntry(entryName);
                if (jarEntry == null) {
                    throw new FileNotFoundException("Entry not found: '" + 
                        entryName + "' in " + jarFile.getName());
                }
            }
            
	    connected = true;
	}	            
    }

    /** {@inheritDoc} */
    public java.util.jar.JarFile getJarFile() throws IOException {
        return jarFile;
    }
    
    /** {@inheritDoc} */    
    public InputStream getInputStream() throws IOException {

	if (!connected) connect();
	
	InputStream result = null;
	
	if (getEntryName() != null) {
	    result = jarFile.getInputStream(jarEntry);
	} else {
	    throw new IOException("No entry specified!");
	}
	return result;
    }

    /** {@inheritDoc} */
    public int getContentLength() {
	int result = -1;
	try {
	    connect();
	    if (jarEntry == null) {
		/* if the URL referes to an archive */
		result = jarFileURLConnection.getContentLength();
	    } else {
		/* if the URL referes to an archive entry */
		result = (int)getJarEntry().getSize();
	    }
	} catch (IOException e) {
	}
	return result;
    }

    /** {@inheritDoc} */
    public Object getContent() throws IOException {
	Object result = null;
	
	connect();
	if (getEntryName() == null) {
	    result = jarFile;
	} else {
	    result = super.getContent();
	}
	return result;
    }

    /** {@inheritDoc} */
    public String getContentType() {
	if (contentType == null) {
            String entryName = getEntryName();
	    if (entryName == null) {
		contentType = "x-java/jar";
	    } else {
                if (contentType == null) {
                    contentType = guessContentTypeFromName(entryName);
                }
                if (contentType == null) {
                    contentType = "content/unknown";
                }
	    }
	}
	return contentType;
    }

    /** {@inheritDoc} */
    public String getHeaderField(String name) {
	return jarFileURLConnection != null ? jarFileURLConnection.getHeaderField(name) : null;
    }

    /** {@inheritDoc} */
    public String getHeaderField(int n) {
	return jarFileURLConnection != null ? jarFileURLConnection.getHeaderField(n) : null;
    }

    /** {@inheritDoc} */
    public String getHeaderFieldKey(int n) {
	return jarFileURLConnection != null ? jarFileURLConnection.getHeaderFieldKey(n) : null;
    }

    /** {@inheritDoc} */
    public Map getHeaderFields() {
        return jarFileURLConnection != null ? jarFileURLConnection.getHeaderFields() : Collections.EMPTY_MAP;
    }
        
    /** {@inheritDoc} */
    public void setRequestProperty(String key, String value) {
	// This is a read-only connection.
    }

    /** {@inheritDoc} */
    public String getRequestProperty(String key) {
	return jarFileURLConnection != null ? jarFileURLConnection.getRequestProperty(key) : null;
    }

    /** {@inheritDoc} */
    public void setAllowUserInteraction(boolean allowuserinteraction) {
	if (jarFileURLConnection != null) jarFileURLConnection.setAllowUserInteraction(allowuserinteraction);
    }

    /** {@inheritDoc} */
    public boolean getAllowUserInteraction() {
	return jarFileURLConnection != null ? jarFileURLConnection.getAllowUserInteraction() : false;
    }
    
    /**
     * Creates a JarFile from an URL.
     * @see java.util.jar.JarFile
     * @param url The URL to constrct JarFile from.
     * @throws IOException Is thrown if the JarFile cannot be constructed fro the given URL.
     * @return The JarFile for the given URL.
     */
    protected JarFile createJarFile(URL url) throws IOException {
        String fname = url.getFile();
        File file = new File(URLDecoder.decode(fname, "UTF-8"));
        return new URLJarFile(file);
    }
}
