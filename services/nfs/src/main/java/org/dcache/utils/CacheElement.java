/*
 * Copyright (c) 2009 - 2017 Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program (see the file COPYING.LIB for more
 * details); if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.dcache.utils;

import java.time.Clock;
import java.util.Date;

/**
 * CacheElement wrapper.
 *
 * Keeps track elements creation and last access time.
 * @param <V>
 */
public class CacheElement<V> {

    /**
     * Maximum allowed time, in milliseconds, that an object is allowed to be cached.
     * After expiration of this time cache entry invalidated.
     */
    private final long _maxLifeTime;
    /**
     * Time in milliseconds since last use of the object. After expiration of this
     * time cache entry is invalidated.
     */
    private final long _idleTime;
    /**
     * Element creation time.
     */
    private final long _creationTime;
    /**
     * Elements last access time.
     */
    private long _lastAccessTime;
    /**
     * internal object.
     */
    private final V _inner;

    private final Clock _clock;

    CacheElement(V inner, Clock clock, long maxLifeTime, long idleTime) {
        _clock = clock;
        _creationTime = _clock.millis();
        _lastAccessTime = _creationTime;
        _inner = inner;
        _maxLifeTime = maxLifeTime;
        _idleTime = idleTime;
    }

    /**
     * Get internal object stored in this element.
     * This operation will update this element's last access time with the current time.
     *
     * @return internal object.
     */
    public V getObject() {
        _lastAccessTime = _clock.millis();
        return _inner;
    }

    /**
     * Get internal object stored in this element. In opposite to {@link #getObject}
     * the last access time of the element will not be updated.
     *
     * @return internal object.
     */
    public V peekObject() {
        return _inner;
    }

    /**
     * Check the entry's validity at the specified time.
     *
     * @param time in milliseconds since 1 of January 1970.
     *
     * @return true if entry still valid and false otherwise.
     */
    public boolean validAt(long time) {
        return time - _lastAccessTime < _idleTime && time - _creationTime < _maxLifeTime;
    }

    @Override
    public String toString() {
        long now = _clock.millis();
        return String.format("Element: [%s], created: %s, last access: %s, life time %d, idle: %d, max idle: %d",
            _inner.toString(), new Date( _creationTime), new Date(_lastAccessTime),
            _maxLifeTime, now - _lastAccessTime, _idleTime);
    }
}
