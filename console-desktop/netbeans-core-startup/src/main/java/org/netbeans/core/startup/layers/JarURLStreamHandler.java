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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Pathched JarURLStreamHandler. It patches 
 * <a href="http://www.netbeans.org/issues/show_bug.cgi?id=44367">issue #44367</a>
 * <p>
 * <b>Original Problem (JRE 1.4.1 only):</b> After calling 
 * {@link java.net.URLConnection#connect() connect}
 * on a <u>non-cached</u> jar URL connection several method calls can lead to
 * <code>NullPointerException</code>.
 * <p>
 * <b>Reason:</b> The JAR URL stream handler initializes the 
 * <code>jarFileURLConnection</code> in its constructor. In the <code>connect()</code>
 * method this field will be set by an URLConnection cache which returns 
 * <code>null</code> when the chaching has been switched off. In consequence calling
 * any of the following method after <code>connect()</code> will cause NPE:
 * <ul>
 *     <li> {@link java.net.URLConnection#getHeaderField(String) getHeaderField}
 *     <li> {@link java.net.URLConnection#setRequestProperty(String, String) setRequestProperty}
 *     <li> {@link java.net.URLConnection#getRequestProperty(String) getRequestProperty}
 *     <li> {@link java.net.URLConnection#setAllowUserInteraction(boolean) setAllowUserInteraction}
 *     <li> {@link java.net.URLConnection#getAllowUserInteraction() getAllowUserInteraction}
 *     <li> {@link java.net.URLConnection#setUseCaches(boolean) setUseCaches}
 *     <li> {@link java.net.URLConnection#getUseCaches()  getUseCache}
 *     <li> {@link java.net.URLConnection#setIfModifiedSince(long) setIfModifiedSince}
 *     <li> {@link java.net.URLConnection#setDefaultUseCaches(boolean) setDefaultUseCaches}
 *     <li> {@link java.net.URLConnection#getDefaultUseCaches() getDefaultUseCaches} 
 * </ul>
 * 
 * <p>
 * <b>Solution:</b> Don't turn to the cache, when it is not required.
 *
 * @author Laszlo Kishalmi
 */
public class JarURLStreamHandler extends sun.net.www.protocol.jar.Handler {
    
    /**
     * This method returns the patched implementation of <code>JarURLConnection</code>
     * if the URL points a local file (uses "file" protocol). It returns
     * with the original implementation of <code>JarURLConnection</code> in any other 
     * case. The reason behind this behavior is to reduce the amount of code which 
     * should be borrowed from the JRE. Unfortunately the <code>URLJarFile</code>
     * has only one public constructor which accepts file parameter.
     * @param u The url to connect.
     * @throws IOException If the URL represented file cannot resource be found.
     * @return The JarURLConnection.
     */
    protected URLConnection openConnection(URL u) throws IOException {
	return isFileURL(u) ? new JarURLConnection(u) : super.openConnection(u);
    }
    
    /**
     * Examines wheter the jar file is beeing accessed be file protocol.
     * @param u The URL to examine.
     * @throws MalformedURLException When the file part of the URL cannot be decoded.
     * @return true if the jar url points to a local file with file protocol.
     */
    protected static boolean isFileURL(URL u) throws MalformedURLException {
        URL inner = new URL(u.getFile());
        if (inner.getProtocol().equalsIgnoreCase("file")) {  // NOI18N
            String host = inner.getHost();
	    if (host == null || host.equals("") || host.equals("~") ||
		host.equals("localhost"))                    // NOI18N
		return true;
        }
        return false;
    }
}
