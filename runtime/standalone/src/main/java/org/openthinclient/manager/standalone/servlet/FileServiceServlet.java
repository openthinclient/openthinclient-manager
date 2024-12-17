/**
 * ****************************************************************************
 * openthinclient.org ThinClient suite
 * <p/>
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * <p/>
 * <p/>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * ****************************************************************************
 */
package org.openthinclient.manager.standalone.servlet;

import com.google.common.base.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FileServiceServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(FileServiceServlet.class);
  private final File basedir;

  public FileServiceServlet(File basedir) {
    this.basedir = basedir;
  }

  public String[] listFiles(String dirName) throws IOException {
    File dir = makeFile(dirName);

    if (logger.isDebugEnabled())
      logger.debug("Listing files in " + dir);

    return dir.list();
  }

  private File makeFile(String name) throws IOException {
      File f;
      if (Strings.isNullOrEmpty(name))
          f = basedir;
      else
          f = new File(basedir, name);
    String canonicalPath = f.getCanonicalPath();
    if (!canonicalPath.startsWith(basedir.getCanonicalPath()))
      throw new IOException("The file named " + name
              + " can or may not be resolved.");

    return f;
  }

  /*
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {

    File f = makeFile(request.getPathInfo());
    if (!f.exists() || !f.canRead()) {
      logger.info("HTTP 404 - File not found for "
                + request.getRemoteAddr() + ": "
                + request.getServletPath() + request.getPathInfo());

      response.sendError(404);
    } else {
      if (f.isDirectory()) {
        logger.info("HTTP 200 - Sending directory listing "
                    + request.getServletPath() + request.getPathInfo()
                    + " to " + request.getRemoteAddr());

        response.setContentType("text/plain");
        response.setCharacterEncoding("ISO-8859-1");

        PrintWriter w = response.getWriter();
        for (File file : f.listFiles()) {
          w.write(file.isDirectory() ? "D " : "F ");
          w.write(file.getName());
          w.write("\n");
        }
      } else {
        logger.info("HTTP 200 - Sending file "
                    + request.getServletPath() + request.getPathInfo()
                    + " to " + request.getRemoteAddr());

        response.setContentType("application/octet-stream");

        ServletOutputStream os = response.getOutputStream();
        FileInputStream is = new FileInputStream(f);
        byte buffer[] = new byte[1024];

        int read;
        while ((read = is.read(buffer)) > 0)
          os.write(buffer, 0, read);
        is.close();
        os.flush();
      }
    }
  }
}
