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
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * File URL stream handler which fixes URL comparison problem in JRE 1.4.1.
 * This handler also fixes 
 * <a href="http://www.netbeans.org/issues/show_bug.cgi?id=48280">issue #48280</a>.
 *
 * <b>Original Problem (JRE 1.4.1 only):</b> Let assume that we have the following URL-s
 * <pre>
 *     URL u1 = new File("/usr/bin").toURI().toURL();
 *     URL u1 = new URL("file:/usr/bin");
 * </pre>
 * Calling {@link java.net.URL#equals(Object) u1.equals(u2)} returns <code>false</code>,
 * however both URLs are referring the very same file.
 * <p>
 * <b>Reason:</b> Calling <code>u1.getHost()</code> returns <code>null</code>
 * while calling <code>u2.getHost()</code> returns an empty string. This situation 
 * is not handled by the original 
 * {@link java.net.URLStreamHandler#equals(java.net.URL, java.net.URL) URLStreamHandler.equals()}
 * method.
 * <p>
 * <b>Solution:</b> Override the method and handle the <code>null</code> vs. empty string
 * situation.
 * @author Laszlo Kishalmi
 */
public class FileURLStreamHandler extends sun.net.www.protocol.file.Handler {
    
    private String getHost(URL u) {
        String host = u.getHost();
        return host == null ? "" : host;
    }
    
    /**
     * Returns true if the hosts of the given URLs are equal.
     * @param u1 The first URL to compare.
     * @param u2 The  URL to compare with.
     * @return true if the hosts of the given URLs are equal.
     */
    protected boolean hostsEqual(URL u1, URL u2) {
	String host1 = getHost(u1);
	String host2 = getHost(u2);
        
        if (!host1.equals(host2)) {
            return super.hostsEqual(u1, u2);
        }
        
        return true;
    }
    
}
