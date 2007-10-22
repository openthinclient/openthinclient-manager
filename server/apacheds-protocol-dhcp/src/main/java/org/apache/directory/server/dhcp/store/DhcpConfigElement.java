/*
 * Copyright (c) 1995-2006 levigo holding gmbh. All Rights Reserved.
 * 
 * This software is the proprietary information of levigo holding gmbh  
 * Use is subject to license terms.
 */
package org.apache.directory.server.dhcp.store;


import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.dhcp.options.OptionsField;


/**
 * @author Jörg Henne
 */
public abstract class DhcpConfigElement
{
    public static final String PROPERTY_MAX_LEASE_TIME = "max-lease-time";
        
    /** List of DhcpOptions for ths subnet */
    private OptionsField options = new OptionsField();

    /** Map of properties for this element */
    private Map properties = new HashMap();


    public OptionsField getOptions()
    {
        return options;
    }


    public Map getProperties()
    {
        return properties;
    }
}
