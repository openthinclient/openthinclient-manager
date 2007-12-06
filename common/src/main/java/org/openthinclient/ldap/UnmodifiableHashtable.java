/**
 * 
 */
package org.openthinclient.ldap;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * An unmodifiable view of a {@link Hashtable}, by delegating all read-only
 * methods to the table backing the view, but throwing an
 * {@link UnsupportedOperationException} on all mutator methods.
 * 
 * This class sucks inherently, since {@link Hashtable} is a class instead of an
 * interface. However, JNDI requires Hashtables for its environment properties
 * instead of {@link Map}s. <em>sigh</em>.
 * 
 * @author levigo
 * 
 * @param <K> Key type
 * @param <V> Value type
 */
class UnmodifiableHashtable<K, V> extends Hashtable<K, V> {
	private static final long serialVersionUID = 1L;
	private final Hashtable<K, V> delegate;

	public UnmodifiableHashtable(Hashtable<K, V> m) {
		this.delegate = m;
	}

	@Override
	public synchronized void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized V put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void putAll(Map<? extends K, ? extends V> t) {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return Collections.unmodifiableSet(delegate.entrySet());
	}

	@Override
	public Set<K> keySet() {
		return Collections.unmodifiableSet(delegate.keySet());
	}

	@Override
	public Collection<V> values() {
		return Collections.unmodifiableCollection(delegate.values());
	}

	@Override
	public Object clone() {
		return delegate.clone();
	}

	@Override
	public boolean contains(Object value) {
		return delegate.contains(value);
	}

	@Override
	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return delegate.containsValue(value);
	}

	@Override
	public Enumeration<V> elements() {
		return delegate.elements();
	}

	@Override
	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	@Override
	public V get(Object key) {
		return delegate.get(key);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public Enumeration<K> keys() {
		return delegate.keys();
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public String toString() {
		return delegate.toString();
	}
}