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
 
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The ARInputStream is used to read UNIX 'ar' style archive files.
 * 
 * @author levigo
 */
public class ARInputStream extends FilterInputStream {
  private static final int ENTRY_HEADER_LENGTH = 61;

  public static void main(String[] arguments) {
    for (int i = 0; i < arguments.length; i++) {
      String arg = arguments[i];
      //Für jedes Argument (File) das ihm Übergeben wurde startet er sich neu
      File f = new File(arg);
      try {
        ARInputStream ais = new ARInputStream(new FileInputStream(f));
        System.out.println(f + ":");
        AREntry e;
        //solange e =  
        while ((e = ais.getNextEntry()) != null)
          System.out.println("   " + e);
      } catch (FileNotFoundException e) {
        System.err.println(f + ": " + e.toString());
        e.printStackTrace();
      } catch (IOException e) {
        System.err.println("Exception processing " + f);
        e.printStackTrace();
      }
    }
  }

  private AREntry currentEntry;
  private long bytesReadFromEntry;

  private AREntry queuedEntry;
  private byte extendedDirectory[];
  private boolean eofReached;

  /**
   * @param in
   * @throws IOException
   */
  public ARInputStream(InputStream in) throws IOException {
    super(new BufferedInputStream(in));

    verifyARHeader();
    checkForDirectoryEntry();
  }

  /*
   * @see java.io.FilterInputStream#read()
   */
  public int read() throws IOException {
    // simulate EOF at every entry boundary
    if (null != currentEntry && bytesReadFromEntry >= currentEntry.getLength())
      return -1;

    int r = in.read();

    // handle real EOF
    if (r < 0)
      eofReached = true;
    else
      bytesReadFromEntry++;

    return r;
  }

  /*
   * @see java.io.FilterInputStream#read(byte[], int, int)
   */
  public int read(byte[] b, int off, int len) throws IOException {
    // simulate EOF at every entry boundary
    if (null != currentEntry && bytesReadFromEntry >= currentEntry.getLength())
      return -1;

    int r = in.read(b, off, Math.min(len, (int)(currentEntry.getLength()
        - bytesReadFromEntry)));

    // handle real EOF
    if (r < 0)
      eofReached = true;
    else
      bytesReadFromEntry += r;

    return r;
  }

  /**
   * @throws IOException
   *  
   */
  private void checkForDirectoryEntry() throws IOException {
    AREntry next = getNextEntry();
    if (next.getName().startsWith("//")) {
      // read extended directory
      extendedDirectory = new byte[(int) next.getLength()];
      if (readFully(extendedDirectory) != next.getLength())
        throw new IOException(
            "Invalid format of archive: incomplete extended directory");
    } else
      queuedEntry = next;
  }

  /**
   * @param extendedDirectory2
   * @throws IOException
   */
  private int readFully(byte[] buffer) throws IOException {
    int read = 0;
    while (read < buffer.length) {
      int r = in.read(buffer, read, buffer.length - read);
      if (r < 0)
        break;

      read += r;
    }

    return read;
  }

  public AREntry getNextEntry() throws IOException {
    // unqueue queued entry
    if (null != queuedEntry) {
      AREntry tmp = queuedEntry;
      queuedEntry = null;
      return tmp;
    }

    if (eofReached)
      return null;

    // do we need to skip some bytes?
    if (null != currentEntry)
      while (bytesReadFromEntry < currentEntry.getLength())
        bytesReadFromEntry += skip(currentEntry.getLength()
            - bytesReadFromEntry);

    currentEntry = null;
    String entryHeader = readLine(ENTRY_HEADER_LENGTH);
    if (entryHeader.equals("\n") && !eofReached)
      entryHeader = readLine(ENTRY_HEADER_LENGTH);

    if (eofReached)
      return null;

    bytesReadFromEntry = 0;
    if (!entryHeader.endsWith("`\n"))
      throw new IOException("Invalid format of archive: Entry header not found");

    AREntry entry = new AREntry(entryHeader);
    resolveLongNames(entry);
    currentEntry = entry;

    return entry;
  }

  /**
   * @param entry
   * @throws IOException
   */
  private void resolveLongNames(AREntry entry) throws IOException {
    if (null != extendedDirectory && entry.getName().startsWith("/")) {
      try {
        // parse directory offset
        int directoryOffset = Integer.parseInt(entry.getName().substring(1));
        if (directoryOffset >= extendedDirectory.length - 1)
          throw new IOException(
              "Invalid format of archive: extended name entry points outside directory");

        // find end of entry
        int i = directoryOffset;
        while (i < extendedDirectory.length) {
          if (extendedDirectory[i] == '\n')
            break;
        }

        if (i == directoryOffset)
          throw new IOException(
              "Invalid format of archive: extended name entry of length null");

        // set extended name
        entry.setName(new String(extendedDirectory, directoryOffset, i,
            "ISO8859-1"));
      } catch (NumberFormatException e) {
        // the stuff following the / wasn't a number. Leave the name as is.
      }
    }
  }

  /**
   * Verify the presence of a proper ar header at the beginning of the archive.
   * 
   * @throws IOException
   */
  private void verifyARHeader() throws IOException {
    String header = readLine(20);
    if (!header.startsWith("!<arch>\n"))
      throw new IOException("Invalid format of archive: ar header not found.");
  }

  /**
   * Read a line from the archive. A line is terminated by a newline character
   * or end of file. Read no more that maxLength bytes in order to complete the
   * line. If the line has not been completed after maxLength characters, the
   * method return the strinc constructed of the characters read so far. It is
   * the caller's resposibility to check the presence of the terminating newline
   * character.
   * 
   * @throws IOException
   */
  private String readLine(int maxLength) throws IOException {
    byte headerBuffer[] = new byte[maxLength];
    int i = 0;
    while (i < headerBuffer.length) {
      int r = read();

      if (r < 0)
        break;

      headerBuffer[i++] = (byte) r;
      if (r == '\n')
        break;
    }

    return new String(headerBuffer, 0, i, "ISO8859-1");
  }
}
