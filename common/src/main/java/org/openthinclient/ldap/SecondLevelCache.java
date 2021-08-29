package org.openthinclient.ldap;

import java.io.IOException;

import javax.naming.Name;
import javax.naming.directory.Attributes;

public interface SecondLevelCache {

	/**
	 * Get the cache entry associated with the given Name.
	 * 
	 * @param name
	 * @return
	 */
	public abstract Attributes getEntry(Name name);

	/**
	 * Purge the cache entry associated with the given name.
	 * 
	 * @param name
	 * @return
	 * @throws IllegalStateException
	 */
	public abstract boolean purgeEntry(Name name) throws IllegalStateException;

	/**
	 * Store a cache entry for the given name.
	 * 
	 * @param name
	 * @param object
	 */
	public abstract void putEntry(Name name, Attributes a);

	public abstract void clear() throws IllegalStateException, IOException;

}
