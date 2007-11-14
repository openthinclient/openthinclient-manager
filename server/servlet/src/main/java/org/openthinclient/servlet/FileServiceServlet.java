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
package org.openthinclient.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class FileServiceServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger
			.getLogger(FileServiceServlet.class);
	private final File basedir;

	public FileServiceServlet() {
		basedir = new File(System.getProperty("jboss.server.data.dir"), "nfs"
				+ File.separator + "root");
	}

	/*
	 * @see org.openthinclient.ejb.FileService#listFiles(java.lang.String)
	 */
	public String[] listFiles(String dirName) throws IOException {
		File dir = makeFile(dirName);

		if (logger.isDebugEnabled())
			logger.debug("Listing files in " + dir);

		return dir.list();
	}

	/*
	 * @see org.openthinclient.ejb.FileService#getFile(java.lang.String)
	 */
	public ByteArrayInputStream getFile(String fileName) throws IOException {
		File file = makeFile(fileName);

		if (logger.isDebugEnabled())
			logger.debug("Getting file " + file);

		FileInputStream is = new FileInputStream(file);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte buffer[] = new byte[1024];

		int read;
		while ((read = is.read(buffer)) > 0)
			baos.write(buffer, 0, read);
		is.close();

		return null;
	}

	/**
	 * @param dir
	 * @return
	 * @throws IOException
	 */
	private File makeFile(String name) throws IOException {
		File f = new File(basedir, name);
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
			if (logger.isDebugEnabled())
				logger.debug("Won't serve this file: " + f);

			response.sendError(404);
		} else {
			if (f.isDirectory()) {
				if (logger.isDebugEnabled())
					logger.debug("Listing Directory: " + f);

				response.setContentType("text/plain");
				response.setCharacterEncoding("ISO-8859-1");

				PrintWriter w = response.getWriter();
				for (File file : f.listFiles()) {
					w.write(file.isDirectory() ? "D " : "F ");
					w.write(file.getName());
					w.write("\n");
				}
			} else {
				if (logger.isDebugEnabled())
					logger.debug("Getting file: " + f);

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
