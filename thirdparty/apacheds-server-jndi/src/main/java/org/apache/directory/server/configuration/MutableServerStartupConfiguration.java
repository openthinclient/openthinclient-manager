/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.configuration;


import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.core.configuration.PartitionConfiguration;

/**
 * A mutable version of {@link ServerStartupConfiguration}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 473656 $, $Date: 2006-11-11 07:39:58 +0100 (Sa, 11. Nov 2006) $
 */
public class MutableServerStartupConfiguration extends ServerStartupConfiguration
{
    private static final long serialVersionUID = 515104910980600099L;


    public MutableServerStartupConfiguration()
    {
        super();
    }
    
    public MutableServerStartupConfiguration( String instanceId )
    {
    	super( instanceId );
    }

    public void setSystemPartitionConfiguration( PartitionConfiguration systemPartitionConfiguration )
    {
        super.setSystemPartitionConfiguration( systemPartitionConfiguration );
    }
    
    
    public void setMaxThreads( int maxThreads )
    {
        super.setMaxThreads( maxThreads );
    }
    
    
    public void setMaxTimeLimit( int maxTimeLimit )
    {
        super.setMaxTimeLimit( maxTimeLimit );
    }
    
    
    public void setMaxSizeLimit( int maxSizeLimit )
    {
        super.setMaxSizeLimit( maxSizeLimit );
    }
    

    public void setSynchPeriodMillis( long synchPeriodMillis )
    {
        super.setSynchPeriodMillis( synchPeriodMillis );
    }
    
    
    public void setAccessControlEnabled( boolean accessControlEnabled )
    {
        super.setAccessControlEnabled( accessControlEnabled );
    }


    public void setAllowAnonymousAccess( boolean arg0 )
    {
        super.setAllowAnonymousAccess( arg0 );
    }

    
    public void setDenormalizeOpAttrsEnabled( boolean denormalizeOpAttrsEnabled )
    {
        super.setDenormalizeOpAttrsEnabled( denormalizeOpAttrsEnabled );
    }
    

    public void setAuthenticatorConfigurations( Set arg0 )
    {
        super.setAuthenticatorConfigurations( arg0 );
    }


    public void setBootstrapSchemas( Set arg0 )
    {
        super.setBootstrapSchemas( arg0 );
    }


    public void setContextPartitionConfigurations( Set arg0 )
    {
        super.setContextPartitionConfigurations( arg0 );
    }


    public void setInterceptorConfigurations( List arg0 )
    {
        super.setInterceptorConfigurations( arg0 );
    }


    public void setTestEntries( List arg0 )
    {
        super.setTestEntries( arg0 );
    }


    public void setWorkingDirectory( File arg0 )
    {
        super.setWorkingDirectory( arg0 );
    }


    public void setEnableKerberos( boolean enableKerberos )
    {
        super.setEnableKerberos( enableKerberos );
    }


    public void setEnableChangePassword( boolean enableChangePassword )
    {
        super.setEnableChangePassword( enableChangePassword );
    }


    public void setEnableNtp( boolean enableNtp )
    {
        super.setEnableNtp( enableNtp );
    }


    public void setLdapPort( int ldapPort )
    {
        super.setLdapPort( ldapPort );
    }


    public void setLdapsPort( int ldapsPort )
    {
        super.setLdapsPort( ldapsPort );
    }


    public void setExtendedOperationHandlers( Collection handlers )
    {
        super.setExtendedOperationHandlers( handlers );
    }


    public void setLdifDirectory( File ldifDirectory )
    {
        super.setLdifDirectory( ldifDirectory );
    }


    public void setLdifFilters( List ldifFilters )
    {
        super.setLdifFilters( ldifFilters );
    }


    public void setEnableLdaps( boolean enableLdaps )
    {
        super.setEnableLdaps( enableLdaps );
    }


    public void setLdapsCertificateFile( File ldapsCertificateFile )
    {
        super.setLdapsCertificateFile( ldapsCertificateFile );
    }


    public void setLdapsCertificatePassword( String ldapsCertificatePassword )
    {
        super.setLdapsCertificatePassword( ldapsCertificatePassword );
    }


    public void setShutdownHookEnabled( boolean shutdownHookEnabled )
    {
        super.setShutdownHookEnabled( shutdownHookEnabled );
    }


    public void setExitVmOnShutdown( boolean exitVmOnShutdown )
    {
        super.setExitVmOnShutdown( exitVmOnShutdown );
    }
}
