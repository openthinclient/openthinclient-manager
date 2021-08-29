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
package org.openthinclient.nfsd;

/*
 * This code is based on:
 * JNFSD - Free NFSD. Mark Mitchell 2001 markmitche11@aol.com
 * http://hometown.aol.com/markmitche11
 */


/*
 * Misc constants for the NFS server.
 */

public class NFSConstants {
  // Unix file modes
  public static final int MISC_STFILE = 0100000;
  public static final int MISC_STDIR = 0040000;
  public static final int MISC_STREAD = 0000555;
  public static final int MISC_STWRITE = 0000300;
  public static final int NFSMODE_LNK  = 0120000;

  // Various errors copied from nfs.h
  public static final int PROC_UNAVAIL = 3;
}
