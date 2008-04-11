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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

/**
 * @author levigo
 */
public class CacheCleaner {
	private static final Logger logger = Logger.getLogger(CacheCleaner.class);

	private static Set<NFSFile> dirtyFilesSet = new HashSet<NFSFile>();
	private static Set<NFSFile> openFilesSet = new HashSet<NFSFile>();

	private static class ExpiryTask extends TimerTask {
		@Override
		public void run() {
			logger.debug("Running expiry");
			try {
				synchronized (this) {
					for (final Iterator<NFSFile> i = dirtyFilesSet.iterator(); i
							.hasNext();) {
						final NFSFile file = i.next();
						synchronized (file) {
							if (file.getLastAccessTimestamp()
									+ file.getExport().getCacheTimeout() < System
									.currentTimeMillis())
								try {
									if (logger.isDebugEnabled())
										logger.debug("Flushing cache for " + file.getFile());

									file.flushCache();

									openFilesSet.remove(file);

									i.remove();
								} catch (final IOException e) {
									logger.warn("Got exception flushing cache for "
											+ file.getFile());
								}
						}
					}
				}

				// we don't synchronize everything. therefore the estimate is just that:
				// an estimate. Therefore we bring down the count of open files to
				// zero, when there is nothing cached.
				if (dirtyFilesSet.size() == 0 && openFilesSet.size() != 0)
					openFilesSet.clear();

			} catch (final ConcurrentModificationException e) {
				// ignore!
			}

			if (logger.isDebugEnabled())
				logger.debug("Expiry done. Dirty files remaining: "
						+ dirtyFilesSet.size() + " open files remaining: "
						+ openFilesSet.size());
		}
	}

	private static Timer expiryTimer;

	private static long EXPIRY_INTERVAL = 5000;

	private static final int MAX_OPEN_FILES = 100;

	static {
		expiryTimer = new Timer();
		expiryTimer.scheduleAtFixedRate(new ExpiryTask(), EXPIRY_INTERVAL,
				EXPIRY_INTERVAL);
	}

	public synchronized static void registerDirtyFile(NFSFile file) {
		dirtyFilesSet.add(file);
		if (file.isChannelOpen()) {
			openFilesSet.add(file);
			if (openFilesSet.size() >= MAX_OPEN_FILES)
				forceCacheFlush();
		}
	}

	private synchronized static void forceCacheFlush() {
		logger.info("Number of open files: " + openFilesSet.size()
				+ ". Forcing flush of cache.");
		final List<NFSFile> sortedList = new ArrayList<NFSFile>(dirtyFilesSet);
		Collections.sort(sortedList, new Comparator<NFSFile>() {
			public int compare(NFSFile f1, NFSFile f2) {
				// implement reverse order!
				// we assume that the difference can't ever exceed Integer.MAX_VALUE;
				return (int) (f1.getLastAccessTimestamp() - f2.getLastAccessTimestamp());
			}
		});

		final int lowWaterMark = MAX_OPEN_FILES * 2 / 3;
		for (final Iterator<NFSFile> i = sortedList.iterator(); i.hasNext();) {
			final NFSFile file = i.next();

			synchronized (file) {
				try {
					if (logger.isDebugEnabled())
						logger.debug("Flushing cache for file of age "
								+ (System.currentTimeMillis() - file.getLastAccessTimestamp()));

					file.flushCache();
					openFilesSet.remove(file);
					dirtyFilesSet.remove(file);
				} catch (final IOException e) {
					logger.warn("Got exception flushing cache for " + file.getFile());
				}
			}

			// cleanup up to low water mark.
			if (openFilesSet.size() < lowWaterMark)
				break;
		}
	}

	public synchronized static void flushAll() {
		if (logger.isDebugEnabled())
			logger.debug("Flushing cache for all " + openFilesSet.size()
					+ " open files.");

		for (final Iterator i = openFilesSet.iterator(); i.hasNext();) {
			final NFSFile file = (NFSFile) i.next();
			synchronized (file) {
				try {
					file.flushCache();
					i.remove();
					dirtyFilesSet.remove(file);
				} catch (final IOException e) {
					logger.warn("Got exception flushing cache for " + file.getFile());
				}
			}
		}
	}
}
