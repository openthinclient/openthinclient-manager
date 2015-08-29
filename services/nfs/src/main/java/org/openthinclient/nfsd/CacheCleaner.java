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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author levigo
 */
public class CacheCleaner {
	private static final Logger LOG = LoggerFactory.getLogger(CacheCleaner.class);

	private static class Janitor implements Runnable {
		private final BlockingQueue<NFSFile> taintQueue;
		private final Set<NFSFile> dirtyFilesSet = new HashSet<NFSFile>();
		private final Set<NFSFile> openFilesSet = new HashSet<NFSFile>();
		private boolean shutdownRequested;

		public Janitor(BlockingQueue<NFSFile> taintQueue) {
			this.taintQueue = taintQueue;

			final Thread t = new Thread(this, "CacheCleaner");
			t.setDaemon(true);
			t.setPriority(Thread.NORM_PRIORITY - 1);
			t.start();
		}

		private static long EXPIRY_INTERVAL = 5000;
		private static final int MAX_OPEN_FILES = 256;

		public void run() {
			while (!shutdownRequested)
				try {
					final NFSFile file = taintQueue.poll(EXPIRY_INTERVAL,
							TimeUnit.MILLISECONDS);
					if (null != file) {
						dirtyFilesSet.add(file);
						if (file.isChannelOpen()) {
							openFilesSet.add(file);
							if (openFilesSet.size() >= MAX_OPEN_FILES)
								watermarkCacheFlush();
						}
					} else
						// no file - it must be time for an expiry
						expire();
				} catch (final InterruptedException e) {
					// ignore
				}
		}

		private synchronized void expire() {
			LOG.debug("Running expiry");
			for (final Iterator<NFSFile> i = dirtyFilesSet.iterator(); i.hasNext();) {
				final NFSFile file = i.next();
				synchronized (file) {
					if (file.getLastAccessTimestamp()
							+ file.getExport().getCacheTimeout() < System.currentTimeMillis())
						try {
							if (LOG.isDebugEnabled())
								LOG.debug("Flushing cache for " + file.getFile());

							file.flushCache();
							openFilesSet.remove(file);
							i.remove();
						} catch (final IOException e) {
							LOG.warn("Got exception flushing cache for " + file.getFile());
						}
				}
			}

			// we don't synchronize everything. therefore the estimate is just
			// that:
			// an estimate. Therefore we bring down the count of open files to
			// zero, when there is nothing cached.
			if (dirtyFilesSet.size() == 0 && openFilesSet.size() != 0)
				openFilesSet.clear();

			if (LOG.isDebugEnabled())
				LOG.debug("Expiry done. Dirty files remaining: "
						+ dirtyFilesSet.size() + " open files remaining: "
						+ openFilesSet.size());
		}

		private synchronized void watermarkCacheFlush() {
			LOG.info("Number of open files: " + openFilesSet.size()
              + ". Forcing flush of cache.");
			final List<NFSFile> sortedList = new ArrayList<NFSFile>(dirtyFilesSet);
			Collections.sort(sortedList, new Comparator<NFSFile>() {
				public int compare(NFSFile f1, NFSFile f2) {
					// implement reverse order!
					// we assume that the difference can't ever exceed Integer.MAX_VALUE;
					return (int) (f1.getLastAccessTimestamp() - f2
							.getLastAccessTimestamp());
				}
			});

			final int lowWaterMark = MAX_OPEN_FILES * 2 / 3;
			for (final Iterator<NFSFile> i = sortedList.iterator(); i.hasNext();) {
				final NFSFile file = i.next();

				synchronized (file) {
					try {
						if (LOG.isDebugEnabled())
							LOG
									.debug("Flushing cache for file of age "
											+ (System.currentTimeMillis() - file
													.getLastAccessTimestamp()));

						file.flushCache();
						openFilesSet.remove(file);
						dirtyFilesSet.remove(file);
					} catch (final IOException e) {
						LOG.warn("Got exception flushing cache for " + file.getFile());
					}
				}

				// cleanup up to low water mark.
				if (openFilesSet.size() < lowWaterMark)
					break;
			}
		}
	}

	private static final BlockingQueue<NFSFile> taintQueue = new LinkedBlockingQueue<NFSFile>();
	private static final Janitor janitor = new Janitor(taintQueue);

	public static void registerDirtyFile(NFSFile file) {
		if (null != file)
			if (file.isChannelOpen())
				taintQueue.offer(file);
	}
}
