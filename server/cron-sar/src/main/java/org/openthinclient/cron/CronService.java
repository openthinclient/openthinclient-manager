/*******************************************************************************
 * openthinclient.org ThinClient suite
 * 
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 ******************************************************************************/
package org.openthinclient.cron;

import it.sauronsoftware.cron4j.Scheduler;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.log4j.Logger;
import org.jboss.system.ServiceMBeanSupport;
import org.openthinclient.common.model.Realm;

/**
 * @author levigo
 */
public class CronService extends ServiceMBeanSupport
		implements
			CronServiceMBean,
			PropertyChangeListener {

	private static final Logger logger = Logger.getLogger(CronService.class);
	protected final Scheduler scheduler = new Scheduler();
	private final WakeUp task = new WakeUp();

	@Override
	public void startService() throws Exception {
		logger.info("Starting...");

		addToScheduler("* * * * *");
		getLdapData();
		scheduler.start();

	}

	public void addToScheduler(String cron) {
		String min, hour, day, month, wday;

		min = "*";
		hour = "*";
		day = "*";
		month = "*";
		wday = "*";

		scheduler.schedule(cron, task);
	}

	@Override
	public void stopService() throws Exception {
		logger.info("Stopping...");
		scheduler.stop();
	}

	public void propertyChange(PropertyChangeEvent evt) {
		// to override

	}

	protected String getLdapData() {
		return null;

	}

	public boolean isEqual(Map zz, Map xx) {
		// besser!?
		if (zz != null && !zz.isEmpty() && xx != null && !xx.isEmpty()) {
			if (!zz.toString().equals(xx.toString()))
				return false;
		} else if (zz != null && !zz.isEmpty() || xx != null && !xx.isEmpty())
			return false;

		return true;
	}

	protected DirContext getContext(Realm realm) throws NamingException {
		final Hashtable env = new Hashtable();
		env
				.put(Context.INITIAL_CONTEXT_FACTORY,
						"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, realm.getConnectionDescriptor().getLDAPUrl());
		return new InitialDirContext(env);
	}

}