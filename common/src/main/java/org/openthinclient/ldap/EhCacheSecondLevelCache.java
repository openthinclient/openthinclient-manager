package org.openthinclient.ldap;

import java.io.IOException;

import javax.naming.Name;
import javax.naming.directory.Attributes;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.hibernate.EhCache;

import org.apache.log4j.Logger;

/**
 * A SecondLevelCache implementation using {@link EhCache} as its backing store.
 * 
 * @author levigo
 */
public class EhCacheSecondLevelCache implements SecondLevelCache {
	private static final Logger logger = Logger
			.getLogger(EhCacheSecondLevelCache.class);

	private Cache cache;

	public EhCacheSecondLevelCache() {
		try {
			if (CacheManager.getInstance().cacheExists("mapping"))
				cache = CacheManager.getInstance().getCache("mapping");
			else {
				cache = new Cache("mapping", 5000, false, false, 120, 120);
				CacheManager.getInstance().addCache(cache);
			}
		} catch (final CacheException e) {
			logger.error("Can't create cache. Caching is disabled", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openthinclient.ldap.SecondLevelCache#getEntry(javax.naming.Name)
	 */
	public Attributes getEntry(Name name) {
		if (null == cache || Mapping.disableCache)
			return null;
		try {
			final Element element = cache.get(name);
			if (null != element && logger.isDebugEnabled())
				logger.debug("Global cache hit for " + name);
			return (Attributes) (null != element ? element.getValue() : null);
		} catch (final Throwable e) {
			logger.warn("Can't get from cache", e);
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openthinclient.ldap.SecondLevelCache#purgeEntry(javax.naming.Name)
	 */
	public boolean purgeEntry(Name name) throws IllegalStateException {
		if (null != cache)
			return cache.remove(name);
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openthinclient.ldap.SecondLevelCache#putEntry(javax.naming.Name,
	 *      javax.naming.directory.Attributes)
	 */
	public void putEntry(Name name, Attributes a) {
		if (null != cache) {
			cache.put(new Element(name, a));
			if (logger.isDebugEnabled())
				logger.debug("Caching entry for " + name);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openthinclient.ldap.SecondLevelCache#clear()
	 */
	public void clear() throws IllegalStateException, IOException {
		if (null != cache)
			cache.removeAll();
	}
}
