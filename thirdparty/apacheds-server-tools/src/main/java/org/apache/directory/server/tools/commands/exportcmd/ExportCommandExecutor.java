/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */
package org.apache.directory.server.tools.commands.exportcmd;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.directory.server.configuration.ServerStartupConfiguration;
import org.apache.directory.server.tools.ToolCommandListener;
import org.apache.directory.server.tools.execution.BaseToolCommandExecutor;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;
import org.apache.directory.server.tools.util.ToolCommandException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * This is the Executor Class of the Export Command.
 * 
 * The command can be called using the 'execute' method.
 * 
 */
public class ExportCommandExecutor extends BaseToolCommandExecutor {
	// Additional Parameters
	public static final String BASEDN_PARAMETER = "baseDN";
	public static final String EXPORTPOINT_PARAMETER = "exportPoint";
	public static final String SCOPE_PARAMETER = "scope";
	public static final String FILE_PARAMETER = "file";

	// Addutional ListenerParameters
	public static final String ENTRYWRITTENLISTENER_PARAMETER = "entryWrittenListener";

	public static final String SCOPE_OBJECT = "object";
	public static final String SCOPE_ONELEVEL = "onelevel";
	public static final String SCOPE_SUBTREE = "subtree";

	private String baseDN;
	private static final String DEFAULT_BASEDN = "";
	private Set<String> exportPoint;
	private static final String DEFAULT_EXPORTPOINT = "";
	private int scope;
	private static final int DEFAULT_SCOPE = SearchControls.SUBTREE_SCOPE;
	private String ldifFileName;

	// The listeners
	private ToolCommandListener entryWrittenListener;

	public ExportCommandExecutor() {
		super("export");
	}

	/**
	 * Executes the command.
	 * <p>
	 * Use the following Parameters and ListenerParameters to call the command.
	 * <p>
	 * Parameters :
	 * <ul>
	 * <li>"HOST_PARAMETER" with a value of type 'String', representing server
	 * host</li>
	 * <li>"PORT_PARAMETER" with a value of type 'Integer', representing server
	 * port</li>
	 * <li>"USER_PARAMETER" with a value of type 'String', representing user DN</li>
	 * <li>"PASSWORD_PARAMETER" with a value of type 'String', representing user
	 * password</li>
	 * <li>"AUTH_PARAMETER" with a value of type 'String', representing the type
	 * of authentication</li>
	 * <li>"BASEDN_PARAMETER" with a value of type 'String', representing the
	 * base DN for the connection to server</li>
	 * <li>"EXPORTPOINT_PARAMETER" with a value of type 'String', representing
	 * the DN of the export point</li>
	 * <li>"SCOPE_PARAMETER" with a value of type 'String', representing the
	 * scope of the export, choosing from one of 'object', 'onelevel', 'subtree'
	 * values</li>
	 * <li>"FILE_PARAMETER" with a value of type 'String', representing the path
	 * to the file to export entries to</li>
	 * <li>"DEBUG_PARAMETER" with a value of type 'Boolean', true to enable debug</li>
	 * <li>"QUIET_PARAMETER" with a value of type 'Boolean', true to enable quiet</li>
	 * <li>"VERBOSE_PARAMETER" with a value of type 'Boolean', true to enable
	 * verbose</li>
	 * </ul>
	 * <br />
	 * ListenersParameters :
	 * <ul>
	 * <li>"OUTPUTLISTENER_PARAMETER", a listener that will receive all output
	 * messages. It returns messages as a String.</li>
	 * <li>"ERRORLISTENER_PARAMETER", a listener that will receive all error
	 * messages. It returns messages as a String.</li>
	 * <li>"EXCEPTIONLISTENER_PARAMETER", a listener that will receive all
	 * exception(s) raised. It returns Exceptions.</li>
	 * <li>"ENTRYWRITTENLISTENER_PARAMETER", a listener that will be notified
	 * each time an entry is exported. It returns, as a String, the DN of the
	 * entry added</li>
	 * </ul>
	 * <b>Note:</b> All Parameters except "DEBUG_PARAMETER", "QUIET_PARAMETER"
	 * and "VERBOSE_PARAMETER" are required.
	 */
	public void execute(Parameter[] params, ListenerParameter[] listeners) {
		processParameters(params);
		processListeners(listeners);

		try {
			execute();
		} catch (Exception e) {
			notifyExceptionListener(e);
		}
	}

	private void execute() throws Exception {
		// Connecting to server and retrieving entries
		List<NamingEnumeration> entries = connectToServerAndGetEntries();

		// Creating destination file
		File destionationFile = new File(ldifFileName);

		// Deleting the destination file if it already exists
		if (destionationFile.exists()) {
			destionationFile.delete();
		}

		// Creating the writer to generate the LDIF file
		FileWriter fw = new FileWriter(ldifFileName, true);

		BufferedWriter writer = new BufferedWriter(fw);
		OtcLdifComposerImpl composer = new OtcLdifComposerImpl();
		MultiValueMap map = new MultiValueMap();

		int entriesCounter = 1;
		long t0 = System.currentTimeMillis();

		for (NamingEnumeration enumeration : entries) {
			while (enumeration.hasMoreElements()) {
				SearchResult sr = (SearchResult) enumeration.nextElement();
				Attributes attributes = sr.getAttributes();
				NamingEnumeration attributesEnumeration = attributes.getAll();

				map.clear();

				while (attributesEnumeration.hasMoreElements()) {
					Attribute attr = (Attribute) attributesEnumeration.nextElement();
					NamingEnumeration e2 = null;

					e2 = attr.getAll();

					while (e2.hasMoreElements()) {
						Object value = e2.nextElement();
						map.put(attr.getID(), value);
					}
				}

				// Writing entry in the file
				writer.write("dn: " + sr.getNameInNamespace() + "\n");
				writer.write(composer.compose(map) + "\n");

				notifyEntryWrittenListener(sr.getNameInNamespace());
				entriesCounter++;

				if (entriesCounter % 10 == 0) {
					notifyOutputListener(new Character('.'));
				}

				if (entriesCounter % 500 == 0) {
					notifyOutputListener("" + entriesCounter);
				}
			}
		}

		writer.flush();
		writer.close();
		fw.close();

		long t1 = System.currentTimeMillis();

		notifyOutputListener("Done!");
		notifyOutputListener(entriesCounter + " entries exported in "
				+ ((t1 - t0) / 1000) + " seconds");
	}

	/**
	 * Gets and returns the entries from the server.
	 * 
	 * @throws ToolCommandException
	 * @throws NamingException
	 */
	public List<NamingEnumeration> connectToServerAndGetEntries() throws ToolCommandException {
		// Connecting to the LDAP Server
		if (isDebugEnabled()) {
			notifyOutputListener("Connecting to LDAP server");
			notifyOutputListener("Host: " + host);
			notifyOutputListener("Port: " + port);
			notifyOutputListener("User DN: " + user);
			notifyOutputListener("Base DN: " + baseDN);
			notifyOutputListener("Authentication: " + auth);
		}
		Hashtable env = new Hashtable();
		env.put(Context.SECURITY_PRINCIPAL, user);
		env.put(Context.SECURITY_CREDENTIALS, password);
		env.put(Context.SECURITY_AUTHENTICATION, auth);
		env.put(Context.PROVIDER_URL, "ldap://" + host + ":" + port + "/" + baseDN);
		env
				.put(Context.INITIAL_CONTEXT_FACTORY,
						"com.sun.jndi.ldap.LdapCtxFactory");
		DirContext ctx;
		try {
			ctx = new InitialDirContext(env);
		} catch (NamingException e) {
			throw new ToolCommandException(
					"Could not connect to the server.\nError: " + e.getMessage());
		}

		// Setting up search scope
		SearchControls ctls = new SearchControls();
		ctls.setSearchScope(scope);

		// Fetching entries
		List<NamingEnumeration> namingEnumerations = new ArrayList<NamingEnumeration>();
		for (String dn : exportPoint) {
			try {
				namingEnumerations.add(ctx.search(dn, "(objectClass=*)", ctls));
			} catch (NamingException e) {
				throw new ToolCommandException("Could not retreive entriy for dn=" + dn);
			}
		}
		return namingEnumerations;
	}

	private void processParameters(Parameter[] params) {
		Map parameters = new HashMap();
		for (int i = 0; i < params.length; i++) {
			Parameter parameter = params[i];
			parameters.put(parameter.getName(), parameter.getValue());
		}

		// Quiet param
		Boolean quietParam = (Boolean) parameters.get(QUIET_PARAMETER);
		if (quietParam != null) {
			setQuietEnabled(quietParam.booleanValue());
		}

		// Debug param
		Boolean debugParam = (Boolean) parameters.get(DEBUG_PARAMETER);
		if (debugParam != null) {
			setDebugEnabled(debugParam.booleanValue());
		}

		// Verbose param
		Boolean verboseParam = (Boolean) parameters.get(VERBOSE_PARAMETER);
		if (verboseParam != null) {
			setVerboseEnabled(verboseParam.booleanValue());
		}

		// Install-path param
		String installPathParam = (String) parameters.get(INSTALLPATH_PARAMETER);
		if (installPathParam != null) {
			try {
				setLayout(installPathParam);
				if (!isQuietEnabled()) {
					notifyOutputListener("loading settings from: "
							+ getLayout().getConfigurationFile());
				}
				ApplicationContext factory = null;
				URL configUrl;

				configUrl = getLayout().getConfigurationFile().toURL();
				factory = new FileSystemXmlApplicationContext(configUrl.toString());
				setConfiguration((ServerStartupConfiguration) factory
						.getBean("configuration"));
			} catch (MalformedURLException e) {
				notifyErrorListener(e.getMessage());
				notifyExceptionListener(e);
			}
		}

		// Host param
		String hostParam = (String) parameters.get(HOST_PARAMETER);
		if (hostParam != null) {
			host = hostParam;
		} else {
			host = DEFAULT_HOST;

			if (isDebugEnabled()) {
				notifyOutputListener("host set to default: " + host);
			}
		}

		// Port param
		Integer portParam = (Integer) parameters.get(PORT_PARAMETER);
		if (portParam != null) {
			port = portParam.intValue();
		}
		// else if ( getConfiguration() != null )
		// {
		// port = getConfiguration().getLdapConfiguration().getIpPort();
		//
		// if ( isDebugEnabled() )
		// {
		// notifyOutputListener( "port overriden by server.xml configuration: " +
		// port );
		// }
		// }
		else {
			port = DEFAULT_PORT;

			if (isDebugEnabled()) {
				notifyOutputListener("port set to default: " + port);
			}
		}

		// User param
		String userParam = (String) parameters.get(USER_PARAMETER);
		if (userParam != null) {
			user = userParam;
		} else {
			user = DEFAULT_USER;

			if (isDebugEnabled()) {
				notifyOutputListener("user set to default: " + user);
			}
		}

		// Password param
		String passwordParam = (String) parameters.get(PASSWORD_PARAMETER);
		if (passwordParam != null) {
			password = passwordParam;
		} else {
			password = DEFAULT_PASSWORD;

			if (isDebugEnabled()) {
				notifyOutputListener("password set to default: " + password);
			}
		}

		// Auth param
		String authParam = (String) parameters.get(AUTH_PARAMETER);
		if (authParam != null) {
			auth = authParam;
		} else {
			auth = DEFAULT_AUTH;

			if (isDebugEnabled()) {
				notifyOutputListener("authentication type set to default: " + auth);
			}
		}

		// Base DN param
		String baseDNParam = (String) parameters.get(BASEDN_PARAMETER);
		if (baseDNParam != null) {
			baseDN = baseDNParam;
		} else {
			baseDN = DEFAULT_BASEDN;

			if (isDebugEnabled()) {
				notifyOutputListener("base DN set to default: " + baseDN);
			}
		}

		// Export Point param
		Object exportPointParam = parameters.get(EXPORTPOINT_PARAMETER);
		if (exportPointParam != null) {
			if (exportPointParam instanceof Set) {
				exportPoint = (Set<String>) exportPointParam;
			} else {
				exportPoint = Collections.singleton((String) exportPointParam);
			}
		} else {
			exportPoint = Collections.singleton(DEFAULT_EXPORTPOINT);

			if (isDebugEnabled()) {
				notifyOutputListener("export point set to default: " + exportPoint);
			}
		}

		// scope param
		String scopeParam = (String) parameters.get(SCOPE_PARAMETER);
		if (scopeParam != null) {
			if (scopeParam.equals(SCOPE_OBJECT)) {
				scope = SearchControls.OBJECT_SCOPE;
			} else if (scopeParam.equals(SCOPE_ONELEVEL)) {
				scope = SearchControls.ONELEVEL_SCOPE;
			} else if (scopeParam.equals(SCOPE_SUBTREE)) {
				scope = SearchControls.SUBTREE_SCOPE;
			}
		} else {
			scope = DEFAULT_SCOPE;

			if (isDebugEnabled()) {
				notifyOutputListener("scope set to default: " + scope);
			}
		}

		// LdifFile param
		String ldifFileParam = (String) parameters.get(FILE_PARAMETER);
		if (ldifFileParam != null) {
			ldifFileName = ldifFileParam;
		}
	}

	private void processListeners(ListenerParameter[] listeners) {
		Map parameters = new HashMap();
		for (int i = 0; i < listeners.length; i++) {
			ListenerParameter parameter = listeners[i];
			parameters.put(parameter.getName(), parameter.getListener());
		}

		// OutputListener param
		ToolCommandListener outputListener = (ToolCommandListener) parameters
				.get(OUTPUTLISTENER_PARAMETER);
		if (outputListener != null) {
			this.outputListener = outputListener;
		}

		// ErrorListener param
		ToolCommandListener errorListener = (ToolCommandListener) parameters
				.get(ERRORLISTENER_PARAMETER);
		if (errorListener != null) {
			this.errorListener = errorListener;
		}

		// ExceptionListener param
		ToolCommandListener exceptionListener = (ToolCommandListener) parameters
				.get(EXCEPTIONLISTENER_PARAMETER);
		if (exceptionListener != null) {
			this.exceptionListener = exceptionListener;
		}

		// EntryAddedListener param
		ToolCommandListener entryWrittenListener = (ToolCommandListener) parameters
				.get(ENTRYWRITTENLISTENER_PARAMETER);
		if (entryWrittenListener != null) {
			this.entryWrittenListener = entryWrittenListener;
		}
	}

	private void notifyEntryWrittenListener(Serializable o) {
		if (this.entryWrittenListener != null) {
			this.entryWrittenListener.notify(o);
		}
	}
}
