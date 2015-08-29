/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2002 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.openide.util.lookup;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;

import java.util.*;


/**
 * Simple proxy lookup. Keeps reference to a lookup it delegates to and
 * forwards all requests.
 *
 * @author Jaroslav Tulach
 */
final class SimpleProxyLookup extends org.openide.util.Lookup {
    /** the provider to check for the status */
    private Provider provider;

    /** the lookup we currently delegate to */
    private Lookup delegate;

    /** map of all templates to Reference (results) associated to this lookup */
    private WeakHashMap results;

    /**
     * @param provider provider to delegate to
     */
    SimpleProxyLookup(Provider provider) {
        this.provider = provider;
    }

    /** Checks whether we still delegate to the same lookup */
    private Lookup checkLookup() {
        Lookup l = provider.getLookup();

        // iterator over Reference (ProxyResult)
        Iterator toCheck = null;

        synchronized (this) {
            if (l != delegate) {
                this.delegate = l;

                if (results != null) {
                    toCheck = Arrays.asList(results.values().toArray()).iterator();
                }
            }
        }

        if (toCheck != null) {
            // update
            ArrayList evAndListeners = new ArrayList();
            for (Iterator it = toCheck; it.hasNext(); ) {
                java.lang.ref.Reference ref = (java.lang.ref.Reference) it.next();
                ProxyResult p = (ProxyResult) ref.get();

                if (p != null && p.updateLookup(l)) {
                    p.collectFires(evAndListeners);
                }
            }
            
            for (Iterator it = evAndListeners.iterator(); it.hasNext(); ) {
                LookupEvent ev = (LookupEvent)it.next();
                LookupListener ll = (LookupListener)it.next();
                ll.resultChanged(ev);
            }
        }

        return delegate;
    }

    public Result lookup(Template template) {
        synchronized (this) {
            if (results == null) {
                results = new WeakHashMap();
            } else {
                java.lang.ref.Reference ref = (java.lang.ref.Reference) results.get(template);

                if (ref != null) {
                    ProxyResult p = (ProxyResult) ref.get();

                    if (p != null) {
                        return p;
                    }
                }
            }

            ProxyResult p = new ProxyResult(template);
            results.put(template, new java.lang.ref.WeakReference(p));

            return p;
        }
    }

    public Object lookup(Class clazz) {
        return checkLookup().lookup(clazz);
    }

    public Item lookupItem(Template template) {
        return checkLookup().lookupItem(template);
    }

    /**
     * Result used in SimpleLookup. It holds a reference to the collection
     * passed in constructor. As the contents of this lookup result never
     * changes the addLookupListener and removeLookupListener are empty.
     */
    private final class ProxyResult extends WaitableResult implements LookupListener {
        /** Template used for this result. It is never null.*/
        private Template template;

        /** result to delegate to */
        private Lookup.Result delegate;

        /** listeners set */
        private javax.swing.event.EventListenerList listeners;
        private LookupListener lastListener;

        /** Just remembers the supplied argument in variable template.*/
        ProxyResult(Template template) {
            this.template = template;
        }

        /** Checks state of the result
         */
        private Result checkResult() {
            updateLookup(checkLookup());

            return this.delegate;
        }

        /** Updates the state of the lookup.
         * @return true if the lookup really changed
         */
        public boolean updateLookup(Lookup l) {
            Collection oldPairs = (delegate != null) ? delegate.allItems() : null;

            LookupListener removedListener;

            synchronized (this) {
                if ((delegate != null) && (lastListener != null)) {
                    removedListener = lastListener;
                    delegate.removeLookupListener(lastListener);
                } else {
                    removedListener = null;
                }
            }

            // cannot call to foreign code 
            Lookup.Result res = l.lookup(template);

            synchronized (this) {
                if (removedListener == lastListener) {
                    delegate = res;
                    lastListener = new WeakResult(this, delegate);
                    delegate.addLookupListener(lastListener);
                }
            }

            if (oldPairs == null) {
                // nobody knows about a change
                return false;
            }

            Collection newPairs = delegate.allItems();

            // See #34961 for explanation.
            if (!(oldPairs instanceof List)) {
                if (oldPairs == Collections.EMPTY_SET) {
                    // avoid allocation
                    oldPairs = Collections.EMPTY_LIST;
                } else {
                    oldPairs = new ArrayList(oldPairs);
                }
            }

            if (!(newPairs instanceof List)) {
                newPairs = new ArrayList(newPairs);
            }

            return !oldPairs.equals(newPairs);
        }

        public synchronized void addLookupListener(LookupListener l) {
            if (listeners == null) {
                listeners = new javax.swing.event.EventListenerList();
            }

            listeners.add(LookupListener.class, l);
        }

        public synchronized void removeLookupListener(LookupListener l) {
            if (listeners != null) {
                listeners.remove(LookupListener.class, l);
            }
        }

        public java.util.Collection allInstances() {
            return checkResult().allInstances();
        }

        public Set allClasses() {
            return checkResult().allClasses();
        }

        public Collection allItems() {
            return checkResult().allItems();
        }

        protected void beforeLookup(Lookup.Template t) {
            Lookup.Result r = checkResult();

            if (r instanceof WaitableResult) {
                ((WaitableResult) r).beforeLookup(t);
            }
        }

        /** A change in lookup occured.
         * @param ev event describing the change
         *
         */
        public void resultChanged(LookupEvent anEvent) {
            collectFires(null);
        } 
        
        protected void collectFires(Collection evAndListeners) {
            javax.swing.event.EventListenerList l = this.listeners;

            if (l == null) {
                return;
            }

            Object[] listeners = l.getListenerList();

            if (listeners.length == 0) {
                return;
            }

            LookupEvent ev = new LookupEvent(this);
            AbstractLookup.notifyListeners(listeners, ev, evAndListeners);
        }
    }
     // end of ProxyResult
    private final class WeakResult extends WaitableResult implements LookupListener {
        private Lookup.Result source;
        private Reference result;
        
        public WeakResult(ProxyResult r, Lookup.Result s) {
            this.result = new WeakReference(r);
            this.source = s;
        }
        
        protected void beforeLookup(Lookup.Template t) {
            ProxyResult r = (ProxyResult)result.get();
            if (r != null) {
                r.beforeLookup(t);
            } else {
                source.removeLookupListener(this);
            }
        }

        protected void collectFires(Collection evAndListeners) {
            ProxyResult r = (ProxyResult)result.get();
            if (r != null) {
                r.collectFires(evAndListeners);
            } else {
                source.removeLookupListener(this);
            }
        }

        public void addLookupListener(LookupListener l) {
            assert false;
        }

        public void removeLookupListener(LookupListener l) {
            assert false;
        }

        public Collection allInstances() {
            assert false;
            return null;
        }

        public void resultChanged(LookupEvent ev) {
            ProxyResult r = (ProxyResult)result.get();
            if (r != null) {
                r.resultChanged(ev);
            } else {
                source.removeLookupListener(this);
            }
        }

        public Collection allItems() {
            assert false;
            return null;
        }

        public Set allClasses() {
            assert false;
            return null;
        }
    } // end of WeakResult
}
