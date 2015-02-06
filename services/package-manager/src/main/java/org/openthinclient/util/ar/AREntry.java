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
package org.openthinclient.util.ar;
 
import java.io.IOException;
import java.util.Date;

/**
 * @author levigo
 */
public class AREntry {
  /** 
   * Construct a new AREntry from an ar entry header string.
   * 
   * @param entryHeader
   * @throws IOException
   */
  AREntry(String entryHeader) throws IOException {
    parseEntryHeader(entryHeader);
  }

  /**
   * @param entryHeader
   * @throws IOException
   */
  private void parseEntryHeader(String entryHeader) throws IOException {
    name = entryHeader.substring(0, 16).trim();
    try {
      date = new Date(Long.parseLong(entryHeader.substring(16, 28).trim()) * 1000);
      user = Integer.parseInt(entryHeader.substring(28, 32).trim());
      group = Integer.parseInt(entryHeader.substring(34, 38).trim());
      mode = Integer.parseInt(entryHeader.substring(40, 47).trim(), 8);
      length = Long.parseLong(entryHeader.substring(48, 57).trim());
    } catch (NumberFormatException e) {
      throw new IOException("Invalid format of archive: entry header cannot be parsed");
    }
  }

  private String name;
  private long length;
  private Date date;
  private int mode;
  private int user;
  private int group;

  public Date getDate() {
    return date;
  }

  public int getGroup() {
    return group;
  }

  public long getLength() {
    return length;
  }

  public int getMode() {
    return mode;
  }

  public String getName() {
    return name;
  }

  public int getUser() {
    return user;
  }
  
  void setName(String name) {
    this.name = name;
  }
  
  /* 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return name + " " + length + " " + date + " " + mode + " " + user + "/" + group;
  }
}
