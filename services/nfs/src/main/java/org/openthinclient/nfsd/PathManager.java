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
/*
 * This code is based on: JNFSD - Free NFSD. Mark Mitchell 2001
 * markmitche11@aol.com http://hometown.aol.com/markmitche11
 */
package org.openthinclient.nfsd;

import com.levigo.util.collections.IntHashtable;
import org.openthinclient.mountd.Exporter;
import org.openthinclient.nfsd.tea.nfs_fh;
import org.openthinclient.nfsd.tea.nfs_prot;
import org.openthinclient.service.nfs.NFSExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Joerg Henne
 */
public class PathManager {
	static final Logger LOG = LoggerFactory.getLogger(PathManager.class);

	private final File handleDatabase;

	private final IntHashtable handlesToFiles;
	private final Map<File, nfs_fh> filesToHandles;
	private boolean isChanged;

	private final byte handleGeneration[];

	private int currentHandleCounter = 0;

	private final Exporter exporter;

	/**
	 * Construct a new PathManager. The path data is persisted into the specified
	 * path database.
	 * 
	 * @throws IOException If the given handle database is not writable.
	 */
	public PathManager(File handleDatabase, Exporter exporter) throws IOException {
		this.handleDatabase = handleDatabase;
		this.exporter = exporter;
		handlesToFiles = new IntHashtable();
		filesToHandles = new HashMap<File, nfs_fh>();
		isChanged = true;

		// initialize the handle generation
		handleGeneration = new byte[8];
		final long currentTimeMillis = System.currentTimeMillis();
		handleGeneration[0] = (byte) (currentTimeMillis >> 56 & 0xff | 1);
		handleGeneration[1] = (byte) (currentTimeMillis >> 48 & 0xff);
		handleGeneration[2] = (byte) (currentTimeMillis >> 40 & 0xff);
		handleGeneration[3] = (byte) (currentTimeMillis >> 32 & 0xff);
		handleGeneration[4] = (byte) (currentTimeMillis >> 24 & 0xff);
		handleGeneration[5] = (byte) (currentTimeMillis >> 16 & 0xff);
		handleGeneration[6] = (byte) (currentTimeMillis >> 8 & 0xff);
		handleGeneration[7] = (byte) (currentTimeMillis & 0xff);

		loadPathDatabase();
	}

	private void loadPathDatabase() throws IOException {
		if (null != handleDatabase) {
			if (handleDatabase.exists() && !handleDatabase.canWrite())
				throw new IOException("The handle database must be writable.");

			// make sure that we can create a tmp path database.
			final File tmp = File.createTempFile("paths", ".db", handleDatabase
					.getAbsoluteFile().getParentFile());
			if (null == tmp)
				throw new IOException("Can't create tmp handle database at " + tmp);
			tmp.delete();

			// does it exist yet?
			if (handleDatabase.exists()) {
				LOG.info("Loading path database at " + handleDatabase);

				// Get the exports currently served by the exporter
				final List<NFSExport> exports = exporter.getExports();

				// actually load the path database.
				final BufferedReader br = new BufferedReader(new FileReader(
						handleDatabase));

				// the path database consists of a simple text format:
				// ggggggggggggggiiiiiiii <filename>
				// with g denoting the handle generation (a long in hex format),
				// i the file id (an int in hex format)
				// and the filename.
				while (true) {
					final String line = br.readLine();
					if (null == line)
						break;
					try {
						final nfs_fh fh = new nfs_fh(new byte[nfs_prot.NFS_FHSIZE]);
						parseHex(line, 0, fh.data, 12);

						final File path = new File(line.substring(25));
						if (!(path.exists() || path.isHidden())) {
							if (LOG.isInfoEnabled())
								LOG.info("Not loading nonexistent path " + path);
							continue;
						}

						// find corresponding export.
						// if several exports match the path, the one with the best
						// (longest)
						// match is chosen.
						NFSExport bestMatch = null;
						int bestLength = 0;
						for (final NFSExport export : exports) {
							final String exportRoot = export.getRoot().getAbsolutePath();
							if (path.getAbsolutePath().startsWith(exportRoot)
									&& exportRoot.length() > bestLength) {
								bestMatch = export;
								bestLength = exportRoot.length();
							}
						}

						// did we find an export?
						if (null == bestMatch) {
							if (LOG.isInfoEnabled())
								LOG.info("Path seems to be no longer exported: " + path);
							continue;
						}

						final int id = handleToInt(fh);
						currentHandleCounter = Math.max(currentHandleCounter, id + 1);
						handlesToFiles.put(id, new NFSFile(fh, path, null, bestMatch));
						filesToHandles.put(path, fh);
					} catch (final Exception e) {
						// ignore the line when parsing failed.
						LOG.warn("Can't parse this line: " + line);
					}
				}

				// resolve parent-child relationships
				final List<NFSFile> filesToRemove = new ArrayList<NFSFile>();
				for (final Enumeration<NFSFile> i = handlesToFiles.elements(); i
						.hasMoreElements();) {
					final NFSFile file = i.nextElement();
					final File parent = file.getFile().getParentFile();

					final nfs_fh parentHandle = filesToHandles.get(parent);
					// is there no parent handle? This would be ok, if the file
					// corresponded to the export root.
					if (null == parentHandle
							&& !file.getFile().equals(file.getExport().getRoot())) {
						LOG.warn("Parent for file " + file.getFile()
                    + " not found in handle database.");
						filesToRemove.add(file);
					} else if (null == parentHandle) {
						// this is a fs root. just leave the <null> parent
					} else {
						final NFSFile parentFile = (NFSFile) handlesToFiles
								.get(handleToInt(parentHandle));
						if (null == parentFile) {
							LOG
									.warn("Parent file for handle not found. Should not happen!");
							filesToRemove.add(file);
						} else
							file.setParentDirectory(parentFile);
					}
				}

				// get rid of files that have to be dumped
				for (final Iterator<NFSFile> i = filesToRemove.iterator(); i.hasNext();) {
					final NFSFile file = i.next();
					filesToHandles.remove(file.getFile());
					handlesToFiles.remove(handleToInt(file.getHandle()));
				}

				isChanged = false;
			}
		}
	}

	/**
	 * With very high loads and a large number of files it might by problematic to
	 * flush the path database synchronously. In this case we would need to do
	 * something a lot smarter. However, for now this seems to be ok.
	 * 
	 * @throws IOException
	 */
	public synchronized void flushPathDatabase() throws IOException {
		if (null != handleDatabase && isChanged) {
			if (handleDatabase.exists() && !handleDatabase.canWrite())
				throw new IOException("The handle database must be writable.");

			// make sure that we can create a tmp path database.
			final File tmp = File.createTempFile("paths", ".db", handleDatabase
					.getParentFile());
			if (null == tmp)
				throw new IOException("Can't create tmp handle database at " + tmp);

			LOG.info("Saving path database at " + handleDatabase);

			// actually save the path database.
			final BufferedWriter bw = new BufferedWriter(new FileWriter(tmp));

			for (final Enumeration<NFSFile> i = handlesToFiles.elements(); i
					.hasMoreElements();) {
				final NFSFile file = i.nextElement();
				file.flushCache();
				toHex(file.getHandle().data, 0, 12, bw);
				bw.write(' ');
				bw.write(file.getFile().getAbsolutePath());
				bw.write('\n');
			}

			bw.close();

			final File handleDBBackup = new File(handleDatabase.getAbsolutePath()
					+ "~");
			handleDBBackup.delete();
			handleDatabase.renameTo(handleDBBackup);
			tmp.renameTo(handleDatabase);

			isChanged = false;
		}
	}

	private void toHex(byte[] bs, int offset, int length, BufferedWriter bw)
			throws IOException {
		final int end = offset + length;
		for (int i = offset; i < end; i++) {
			bw.write(intToChar(bs[i] >> 4 & 0xf));
			bw.write(intToChar(bs[i] & 0xf));
		}
	}

	private char intToChar(int i) {
		if (i >= 0 && i <= 9)
			return (char) (i + '0');
		else if (i >= 0xa && i <= 0xf)
			return (char) (i + 'a' - 0xa);
		else
			throw new IllegalArgumentException();
	}

	private static void parseHex(String line, int start, byte bytes[], int length) {
		int charIdx = start;
		for (int i = 0; i < length; i += 1) {
			bytes[i] = (byte) (charToInt(line.charAt(charIdx++)) << 4);
			bytes[i] |= (byte) charToInt(line.charAt(charIdx++));
		}
	}

	private static int charToInt(char c) {
		if (c >= '0' && c <= '9')
			return c - '0';
		else if (c >= 'a' && c <= 'f')
			return c - 'a' + 0xa;
		else if (c >= 'A' && c <= 'F')
			return c - 'A' + 0xa;
		else
			throw new NumberFormatException("Illegal hex character " + c);
	}

	static int handleToInt(nfs_fh handle) {
		final byte h[] = handle.data;
		return (0xff & h[8]) << 24 | (0xff & h[9]) << 16 | (0xff & h[10]) << 8
				| 0xff & h[11];
	}

	/**
	 * Get an NFSFile by its nfs_fh handle. The file has to exist.
	 * 
	 * @param fh
	 * @return
	 * @throws StaleHandleException if the handle is not defined or the handle
	 *           generation doesn't match.
	 */
	public NFSFile getNFSFileByHandle(nfs_fh fh) throws StaleHandleException {
		final NFSFile nfsFile = (NFSFile) handlesToFiles.get(handleToInt(fh));
		if (null == nfsFile)
			throw new StaleHandleException();

		if (!nfsFile.validateHandle(fh))
			throw new StaleHandleException("Handle was defined, but wrong generation");

		nfsFile.updateTimestamp();
		return nfsFile;
	}

	public synchronized boolean handleForFileExists(File f) {
		return filesToHandles.get(f) != null;
	}

	public synchronized nfs_fh getHandleByFile(File f)
			throws StaleHandleException {
		// we like em better
		if (!f.isAbsolute())
			f = f.getAbsoluteFile();

		nfs_fh handle = filesToHandles.get(f);
		if (null == handle) {
			handle = createHandleForFile(f, null);
			filesToHandles.put(f, handle);
			isChanged = true;
		}
		return handle;
	}

	public synchronized int getIDByFile(File f) throws StaleHandleException {
		nfs_fh handle = filesToHandles.get(f);
		if (null == handle) {
			handle = createHandleForFile(f, null);
			filesToHandles.put(f, handle);
			isChanged = true;
		}
		return getIDFromHandle(handle);
	}

	/**
	 * @param handle
	 * @return
	 */
	private static int getIDFromHandle(nfs_fh handle) {
		return NFSServer.byteToInt(handle.data, 8);

	}

	private nfs_fh createHandleForFile(File f, NFSExport export)
			throws StaleHandleException {
		final nfs_fh fh = new nfs_fh(new byte[nfs_prot.NFS_FHSIZE]);

		System.arraycopy(handleGeneration, 0, fh.data, 0, 8);

		final int h = currentHandleCounter++;
		fh.data[8] = (byte) (h >> 24 & 0xff);
		fh.data[9] = (byte) (h >> 16 & 0xff);
		fh.data[10] = (byte) (h >> 8 & 0xff);
		fh.data[11] = (byte) (h & 0xff);

		if (null == export) {
			// find NFSFile for parent directory
			final File parentFile = f.getParentFile();
			final nfs_fh parentHandle = filesToHandles.get(parentFile);
			if (null == parentHandle)
				throw new StaleHandleException(f + " doesn't have a parent handle");
			final int id = getIDFromHandle(parentHandle);
			final NFSFile parent = (NFSFile) handlesToFiles.get(id);
			if (null == parent)
				throw new StaleHandleException("Not NFS file for parent handle for "
						+ f);

			handlesToFiles.put(h, new NFSFile(fh, f, parent, parent.getExport()));
		} else
			handlesToFiles.put(h, new NFSFile(fh, f, null, export));

		isChanged = true;

		return fh;
	}

	public nfs_fh getHandleForExport(NFSExport e) throws StaleHandleException {
		File root = e.getRoot();

		// we like em better
		if (!root.isAbsolute())
			root = root.getAbsoluteFile();

		nfs_fh handle = filesToHandles.get(root);
		if (null == handle) {
			handle = createHandleForFile(root, e);
			filesToHandles.put(root, handle);
			isChanged = true;
		}

		return handle;
	}

	public synchronized void purgeFileAndHandle(File f)
			throws StaleHandleException {
		// we like em better
		if (!f.isAbsolute())
			f = f.getAbsoluteFile();

		handlesToFiles.remove(getIDByFile(f));
		filesToHandles.remove(f);
	}

	public synchronized void movePath(File from, File to) {
		LinkedHashMap<File, File> moveMap;
		moveMap = new LinkedHashMap<File, File>();

		final LinkedHashMap<File, File> recursiveMoveMap = getMoveMap(from, to,
				moveMap);

		for (final Iterator i = recursiveMoveMap.entrySet().iterator(); i.hasNext();) {
			final Map.Entry pairs = (Map.Entry) i.next();
			moveMapEntries((File) pairs.getKey(), (File) pairs.getValue());
		}
		isChanged = true;
	}

	private synchronized LinkedHashMap<File, File> getMoveMap(File from, File to,
			LinkedHashMap<File, File> sortedMoveMap) {
		if (!from.isAbsolute())
			from = from.getAbsoluteFile();
		if (!to.isAbsolute())
			to = to.getAbsoluteFile();

		// did the to-File exist before? Get rid of the handles!
		final nfs_fh fh = filesToHandles.get(to);
		if (null != fh) {
			filesToHandles.remove(to);
			handlesToFiles.remove(handleToInt(fh));
		}
		// put entry itself
		sortedMoveMap.put(from, to);

		// if the entry is a directory, move the contained files
		if (to.isDirectory())
			// and recursively collect map entries to move
			for (final Iterator<File> i = filesToHandles.keySet().iterator(); i
					.hasNext();) {
				final File f = i.next();
				if (f.getParentFile().equals(from)) {
					final File t = new File(to, f.getName());
					getMoveMap(f, t, sortedMoveMap);
					sortedMoveMap.put(f, t);
				}
			}

		return sortedMoveMap;
	}

	/**
	 * @param from
	 * @param to
	 */
	private synchronized void moveMapEntries(File from, File to) {

		nfs_fh fhFrom = filesToHandles.get(from);
		if (null == fhFrom)
			try {
				fhFrom = getHandleByFile(from);
			} catch (final StaleHandleException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		if (null != fhFrom) {
			filesToHandles.remove(from);
			final NFSFile nfsFileFrom = (NFSFile) handlesToFiles
					.get(handleToInt(fhFrom));

			final File parentFileTo = to.getParentFile();
			nfs_fh parentHandleTo = filesToHandles.get(parentFileTo);

			if (null == parentHandleTo)
				try {
					parentHandleTo = getHandleByFile(to.getParentFile());
				} catch (final StaleHandleException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			if (null != parentHandleTo) {
				final int id = getIDFromHandle(parentHandleTo);
				final NFSFile parentTo = (NFSFile) handlesToFiles.get(id);

				if (null != parentTo) {
					final NFSFile nfsFileTo = new NFSFile(nfsFileFrom.getHandle(), to,
							parentTo, parentTo.getExport());
					filesToHandles.put(to, fhFrom);
					handlesToFiles.put(handleToInt(fhFrom), nfsFileTo);
				} else
					LOG.warn("missing handle->file map for parentHandleTo: "
							+ parentHandleTo);
			} else
				LOG.warn("missing file->handle map for parentFileTo: "
						+ parentFileTo);

			isChanged = true;
		} else
			LOG.warn("missing file->handle map for from: " + from);
	}

	/**
	 * @throws Exception if something goes wrong. Yes, I don't want to be more
	 *           specific here.
	 */
	public synchronized void shutdown() throws Exception {
		// force flush database
		isChanged = true;
		flushPathDatabase();
	}

	public void flushFile(File f) {
		try {
			if (!f.isAbsolute())
				f = f.getAbsoluteFile();

			final nfs_fh fh = filesToHandles.get(f);
			if (null != fh) {
				final NFSFile nfsFile = getNFSFileByHandle(fh);
				nfsFile.flushCache();
			}
		} catch (final Exception e) {
			LOG.error("Unable to flush cache for: " + f.getAbsolutePath());
		}
	}

	public boolean createMissigHandles(File f) {
		if (!f.isAbsolute())
			f = f.getAbsoluteFile();

		if (f.isFile())
			f = f.getParentFile();

		// return if handle already exists
		if (handleForFileExists(f))
			return true;

		// get first parent dir saved in db
		File firstParent = f;
		final LinkedList<File> filesToAdd = new LinkedList<File>();
		while (!handleForFileExists(firstParent) && null != firstParent) {
			filesToAdd.addFirst(firstParent);
			firstParent = firstParent.getParentFile();
		}

		if (null == firstParent) {
			LOG.error("Unable to get any parent of: " + f);
			return false;
		}

		try {
			final NFSExport export = getNFSFileByHandle(getHandleByFile(firstParent))
					.getExport();

			for (final Iterator i = filesToAdd.iterator(); i.hasNext();) {
				final File fileToAdd = (File) i.next();

				final nfs_fh fileToAddParentNfsHandle = getHandleByFile(fileToAdd
						.getParentFile());
				new NFSFile(getHandleByFile(fileToAdd), fileToAdd,
						getNFSFileByHandle(fileToAddParentNfsHandle), export);
			}
			return true;

		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 
	 * @param filesList list of files which should be removed from the NFS DB
	 * @return true only if all files could be deleted from the NFS Database
	 */
	public boolean removeFileFromNFS(Collection<File> filesList) {
		for (File file : filesList) {
			flushFile(file);
			if (file.delete()) {
				if (handleForFileExists(file))
					try {
						if (!file.isAbsolute())
							file = file.getAbsoluteFile();
						handlesToFiles.remove(getIDByFile(file));
						filesToHandles.remove(file);
					} catch (final StaleHandleException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			} else if (file.isFile()) {
				LOG.error("Unable to remove File: " + file.getPath());
				return false;
			}
		}
		isChanged = true;
		return true;
	}
}
