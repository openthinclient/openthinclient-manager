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
package org.openthinclient.ldap;

/**
 * FIXME: the DirectoryException should probably just subclass from
 * NameingException instead of wrapping one, most of the time.
 * 
 * @author levigo
 */
public class DirectoryException extends Exception {

  private static final long serialVersionUID = 1L;

  public DirectoryException() {
    super();
  }

  /**
   * @param message
   * @param cause
   */
  public DirectoryException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * @param message
   */
  public DirectoryException(String message) {
    super(message);
  }

  /**
   * @param cause
   */
  public DirectoryException(Throwable cause) {
    super(cause);
  }

}
