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
 *******************************************************************************/
/**
 * 
 */
package org.openthinclient.ldap.auth;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class UsernamePasswordHandler implements CallbackHandler {

	private transient final String username;
	private transient char[] password;
	private transient final Object credential;

	public UsernamePasswordHandler(String username, char[] password) {
		super();
		this.username = username;
		this.password = password;
		this.credential = password;
	}

	public UsernamePasswordHandler(String username, Object credential) {
		super();
		this.username = username;
		this.credential = credential;
	}

	public void handle(Callback[] callbacks) throws UnsupportedCallbackException {

		for (int i = 0; i < callbacks.length; i++) {
			Callback c = callbacks[i];
			if (c instanceof NameCallback) {
				NameCallback nc = (NameCallback) c;
				nc.setName(username);
			} else if (c instanceof PasswordCallback) {
				PasswordCallback pc = (PasswordCallback) c;
				if (password == null) {
					if (credential != null) {
						String tmp = credential.toString();
						password = tmp.toCharArray();
					}
				}

				pc.setPassword(password);
			} else {
				throw new UnsupportedCallbackException(callbacks[i],
						"Unrecognized Callback");
			}
		}
	}
}
