/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2003 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.openide.util.actions;

import org.openide.ErrorManager;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.util.WeakSet;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;

import java.beans.*;

import java.lang.ref.*;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Action;
import javax.swing.ActionMap;


/* enabled is old; action perf prop is not public --jglick
*
* <P>
* <TABLE BORDER COLS=3 WIDTH=100%>
* <TR><TH WIDTH=15%>Property<TH WIDTH=15%>Property Type<TH>Description
* <TR><TD> Enabled  <TD> boolean   <TD> The explicite enabled/disabled
*                                       state of the action.
* <TR><TD> ActionPerformer  <TD> ActionPerformer  <TD> The class that performs the action
* </TABLE>
*/

/** Action that can have a performer of the action attached to it at any time,
* or changed.
* The action will be automatically disabled
* when it has no performer.
* <p>Also may be made sensitive to changes in window focus.
* @author   Ian Formanek, Jaroslav Tulach, Petr Hamernik
*/
public abstract class CallbackSystemAction extends CallableSystemAction implements ContextAwareAction {
    /** action performer */
    private static final String PROP_ACTION_PERFORMER = "actionPerformer"; // NOI18N

    /** a list of all actions that has survive focus change set to false */
    private static final WeakSet notSurviving = new WeakSet(37);

    /** a list of CallableSystemAction actions surviving focus change */
    private static final WeakSet surviving = new WeakSet(37);

    /** key to access listener */
    private static final Object LISTENER = new Object();
    static final long serialVersionUID = -6305817805474624653L;

    /** logging */
    private static final ErrorManager err = ErrorManager.getDefault().getInstance(
            "org.openide.util.actions.CallbackSystemAction"
        ); // NOI18N
    private static final boolean errLog = err.isLoggable(err.INFORMATIONAL);

    /** Initialize the action to have no performer.
    */
    protected void initialize() {
        super.initialize();
        updateEnabled();
        setSurviveFocusChange(false);
    }

    /** Get the current action performer.
    * @return the current action performer, or <code>null</code> if there is currently no performer
    */
    public ActionPerformer getActionPerformer() {
        return (ActionPerformer) getProperty(PROP_ACTION_PERFORMER);
    }

    /** Set the action performer.
    * The specified value can be <code>null</code>, which means that the action will have no performer
    * and is disabled. ({@link #isEnabled} will return <code>false</code> regardless its previous state.)
    * <P>
    * This method is <em>too dynamic</em> it depends on the actuall order of callers and
    * is for example very fragile with respect to focus switching and correct delivering of
    * focus change events. That is why an alternative based on
    * <a href="http://openide.netbeans.org/proposals/actions/design.html#callback">ActionMap proposal</a>
    * has been developed.
    * <P>
    * So if you are providing a <a href="@org-openide-windows@/org/openide/windows/TopComponent.html">TopComponent</a>
    * and want to provide
    * your own handling of <a href="@org-openide-actions@/org/openide/actions/CopyAction.html">CopyAction</a> use following code:
    * <PRE>
    * TopComponent tc = ...;
    * javax.swing.Action yourCopyAction = ...; // the action to invoke instead of Copy
    *
    * CopyAction globalCopyAction = SystemAction.get (CopyAction.class);
    * Object key = globalCopyAction.getActionMapKey(); // key is a special value defined by all CallbackSystemActions
    *
    * // and finally:
    * tc.getActionMap ().put (key, yourCopyAction);
    * </PRE>
    * This code registers <code>yourCopyAction</code> with <code>tc</code>
    * top component, so whenever a <code>globalCopyAction</code> is invoked,
    * your action is being delegated to.
    *
    * @param performer the new action performer or <code>null</code> to disable
    *
    * @deprecated use TopComponent.getActionMap() as described in the javadoc
    */
    public void setActionPerformer(ActionPerformer performer) {
        putProperty(PROP_ACTION_PERFORMER, performer);
        updateEnabled();
    }

    /** Updates the enabled state by checking performer and ActionMap
     */
    private void updateEnabled() {
        javax.swing.Action action = GlobalManager.getDefault().findGlobalAction(
                getActionMapKey(), getSurviveFocusChange()
            );

        if (action != null) {
            setEnabled(action.isEnabled());

            synchronized (LISTENER) {
                ActionDelegateListener l = (ActionDelegateListener) getProperty(LISTENER);

                if ((l == null) || (l.get() != this)) {
                    l = new ActionDelegateListener(this, action);
                    putProperty(LISTENER, l);
                } else {
                    l.attach(action);
                }
            }
        } else {
            if (getActionPerformer() != null) {
                // we have performer
                setEnabled(true);
            } else {
                setEnabled(false);
            }

            clearListener();
        }
    }

    /** Clears the listener.
     */
    private void clearListener() {
        synchronized (LISTENER) {
            // remove listener on any action
            ActionDelegateListener l = (ActionDelegateListener) getProperty(LISTENER);

            if (l != null) {
                l.clear();
                putProperty(LISTENER, null);
            }
        }
    }

    /** Perform the action. Tries the performer and then scans the ActionMap
     * of selected topcomponent.
     */
    public void actionPerformed(final ActionEvent ev) {
        // First try global context action.
        final Action action = GlobalManager.getDefault().findGlobalAction(getActionMapKey(), getSurviveFocusChange());

        if (action != null) {
            if (action.isEnabled()) {
                action.actionPerformed(ev);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }

            return;
        }

        final ActionPerformer ap = getActionPerformer();

        if (ap != null) {
            org.netbeans.modules.openide.util.ActionsBridge.doPerformAction(
                this,
                new org.netbeans.modules.openide.util.ActionsBridge.ActionRunnable(ev, this, asynchronous ()) {
                    public void run() {
                        ap.performAction(CallbackSystemAction.this);
                    }
                }
            );

            return;
        }

        Toolkit.getDefaultToolkit().beep();
    }

    /** Perform the action.
    * This default implementation calls the assigned action performer if it
    * exists, otherwise does nothing.
    */
    public void performAction() {
        ActionPerformer ap = getActionPerformer();

        if (ap != null) {
            ap.performAction(this);
        }
    }

    /** Getter for action map key, which is used to find action from provided
     * context (i.e. <code>ActionMap</code> provided by the context),
     * which acts as a callback.
     * Override this method in subclasses to provide 'nice' key.
     * @return key which is used to find the action which performs callback,
     *      default returned key is a class name.
     * @since 3.29 */
    public Object getActionMapKey() {
        return getClass().getName();
    }

    /** Test whether the action will survive a change in focus.
    * By default, it will not.
    * @return <code>true</code> if the enabled state of the action survives focus changes
    */
    public boolean getSurviveFocusChange() {
        getProperty(null); // force initialization

        return !notSurviving.contains(getClass());
    }

    /** Implements <code>ContextAwareAction</code> interface method. */
    public Action createContextAwareInstance(Lookup actionContext) {
        return new DelegateAction(this, actionContext);
    }

    /** Set whether the action will survive a change in focus.
    * If <code>false</code>, then the action will be automatically
    * disabled (using {@link #setActionPerformer}) when the window
    * focus changes.
    *
    * @param b <code>true</code> to survive focus changes, <code>false</code> to be sensitive to them
    */
    public void setSurviveFocusChange(boolean b) {
        synchronized (notSurviving) {
            if (b) {
                notSurviving.remove(getClass());
                surviving.add(getClass());
            } else {
                notSurviving.add(getClass());
                surviving.remove(getClass());
            }
        }
    }

    /** Array of actions from a set of classes.
     */
    private static ArrayList toInstances(java.util.Set s) {
        ArrayList actions;

        synchronized (notSurviving) {
            actions = new ArrayList(s.size());

            Iterator it = s.iterator();

            while (it.hasNext()) {
                Class c = (Class) it.next();

                Object a = SystemAction.findObject(c, false);

                if (a != null) {
                    actions.add(a);
                }
            }
        }

        return actions;
    }

    /** Clears all action performers for those that has setSurviveFocusChange
     * on true.
     */
    private static void clearActionPerformers() {
        ArrayList actions = toInstances(notSurviving);

        // clear the performers out of any loop
        Iterator it = actions.iterator();

        while (it.hasNext()) {
            CallbackSystemAction a = (CallbackSystemAction) it.next();
            a.setActionPerformer(null);
        }

        actions = toInstances(surviving);

        // clear the performers out of any loop
        it = actions.iterator();

        while (it.hasNext()) {
            CallbackSystemAction a = (CallbackSystemAction) it.next();

            if (errLog) {
                err.log("updateEnabled: " + a); // NOI18N
            }

            a.updateEnabled();
        }
    }

    /** Listener on a global context.
     */
    private static final class GlobalManager implements LookupListener {
        private static GlobalManager instance;
        private Lookup.Result result;
        private Reference actionMap = new WeakReference(null);
        private final ActionMap survive = new ActionMap();

        private GlobalManager() {
            result = Utilities.actionsGlobalContext().lookup(new Lookup.Template(ActionMap.class));
            result.addLookupListener(this);
            resultChanged(null);
        }

        public synchronized static GlobalManager getDefault() {
            if (instance != null) {
                return instance;
            }

            instance = new GlobalManager();

            return instance;
        }

        public Action findGlobalAction(Object key, boolean surviveFocusChange) {
            ActionMap map = (ActionMap) actionMap.get();
            Action a = (map == null) ? null : map.get(key);

            if (surviveFocusChange) {
                if (a == null) {
                    a = survive.get(key);

                    if (a != null) {
                        a = ((WeakAction) a).getDelegate();
                    }

                    if (errLog) {
                        err.log("No action for key: " + key + " using delegate: " + a); // NOI18N
                    }
                } else {
                    if (errLog) {
                        err.log("New action for key: " + key + " put: " + a);
                    }

                    survive.put(key, new WeakAction(a));
                }
            }

            if (errLog) {
                err.log("Action for key: " + key + " is: " + a); // NOI18N
            }

            return a;
        }

        /** Change all that do not survive ActionMap change */
        public void resultChanged(org.openide.util.LookupEvent ev) {
            ActionMap a = (ActionMap) Utilities.actionsGlobalContext().lookup(ActionMap.class);

            if (errLog) {
                err.log("changed map : " + a); // NOI18N
                err.log("previous map: " + actionMap.get()); // NOI18N
            }

            if (a == actionMap.get()) {
                return;
            }

            actionMap = new WeakReference(a);

            if (errLog) {
                err.log("clearActionPerformers"); // NOI18N
            }

            clearActionPerformers();
        }
    }
     // end of LookupListener

    /** An action that holds a weak reference to other action.
     */
    private static final class WeakAction extends WeakReference implements Action {
        public WeakAction(Action delegate) {
            super(delegate);
        }

        public Action getDelegate() {
            return (Action) super.get();
        }

        public Object getValue(String key) {
            throw new UnsupportedOperationException();
        }

        public void putValue(String key, Object value) {
            throw new UnsupportedOperationException();
        }

        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException();
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
            throw new UnsupportedOperationException();
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            throw new UnsupportedOperationException();
        }

        public void setEnabled(boolean b) {
            throw new UnsupportedOperationException();
        }

        public boolean isEnabled() {
            throw new UnsupportedOperationException();
        }
    }

    /** A class that listens on changes in enabled state of an action
     * and updates the state of the action according to it.
     */
    private static final class ActionDelegateListener extends WeakReference implements PropertyChangeListener {
        private WeakReference delegate;

        public ActionDelegateListener(CallbackSystemAction c, javax.swing.Action delegate) {
            super(c);
            this.delegate = new WeakReference(delegate);
            delegate.addPropertyChangeListener(this);
        }

        public void clear() {
            javax.swing.Action a;

            WeakReference d = delegate;
            a = (d == null) ? null : (javax.swing.Action) d.get();

            if (a == null) {
                return;
            }

            delegate = null;

            a.removePropertyChangeListener(this);
        }

        public void attach(javax.swing.Action action) {
            WeakReference d = delegate;

            if ((d != null) && (d.get() == action)) {
                return;
            }

            Action prev = (Action) d.get();

            // reattaches to different action
            if (prev != null) {
                prev.removePropertyChangeListener(this);
            }

            this.delegate = new WeakReference(action);
            action.addPropertyChangeListener(this);
        }

        public void propertyChange(java.beans.PropertyChangeEvent evt) {
            synchronized (LISTENER) {
                WeakReference d = delegate;

                if ((d == null) || (d.get() == null)) {
                    return;
                }
            }

            CallbackSystemAction c = (CallbackSystemAction) get();

            if (c != null) {
                c.updateEnabled();
            }
        }
    }

    /** A delegate action that is usually associated with a specific lookup and
     * extract the nodes it operates on from it. Otherwise it delegates to the
     * regular NodeAction.
     */
    private static final class DelegateAction extends Object implements javax.swing.Action,
        org.openide.util.LookupListener, Presenter.Menu, Presenter.Popup, Presenter.Toolbar, PropertyChangeListener {
        /** action to delegate too */
        private CallbackSystemAction delegate;

        /** lookup we are associated with (or null) */
        private org.openide.util.Lookup.Result result;

        /** previous state of enabled */
        private boolean enabled;

        /** support for listeners */
        private PropertyChangeSupport support = new PropertyChangeSupport(this);

        /** listener to check listen on state of action(s) we delegate to */
        private PropertyChangeListener weakL;

        /** last action we were listening to */
        private WeakReference lastRef;

        public DelegateAction(CallbackSystemAction a, Lookup actionContext) {
            this.delegate = a;
            this.weakL = org.openide.util.WeakListeners.propertyChange(this, null);
            this.enabled = a.getActionPerformer() != null;

            this.result = actionContext.lookup(new org.openide.util.Lookup.Template(javax.swing.ActionMap.class));
            this.result.addLookupListener(
                (LookupListener) org.openide.util.WeakListeners.create(LookupListener.class, this, this.result)
            );
            resultChanged(null);
        }

        /** Overrides superclass method, adds delegate description. */
        public String toString() {
            return super.toString() + "[delegate=" + delegate + "]"; // NOI18N
        }

        /** Invoked when an action occurs.
         */
        public void actionPerformed(final java.awt.event.ActionEvent e) {
            final javax.swing.Action a = findAction();

            if (a != null) {
                org.netbeans.modules.openide.util.ActionsBridge.ActionRunnable run;
                run = new org.netbeans.modules.openide.util.ActionsBridge.ActionRunnable(e, delegate, delegate.asynchronous()) {
                            public void run() {
                                a.actionPerformed(e);
                            }
                        };

                org.netbeans.modules.openide.util.ActionsBridge.doPerformAction(delegate, run);
            } else {
                // XXX #30303 if the action falls back to the old behaviour
                // it may not be performed in case it is in dialog and
                // is not transmodal. 
                // This is just a hack, see TopComponent.processKeyBinding.
                Object source = e.getSource();

                if (
                    source instanceof Component &&
                        javax.swing.SwingUtilities.getWindowAncestor((Component) source) instanceof java.awt.Dialog
                ) {
                    Object value = delegate.getValue("OpenIDE-Transmodal-Action"); // NOI18N

                    if (!Boolean.TRUE.equals(value)) {
                        return;
                    }
                }

                delegate.actionPerformed(e);
            }
        }

        public boolean isEnabled() {
            javax.swing.Action a = findAction();

            if (a == null) {
                a = delegate;
            }

            // 40915 - hold last action weakly
            javax.swing.Action last = (lastRef == null) ? null : (javax.swing.Action) lastRef.get();

            if (a != last) {
                if (last != null) {
                    last.removePropertyChangeListener(weakL);
                }

                lastRef = new WeakReference(a);
                a.addPropertyChangeListener(weakL);
            }

            return a.isEnabled();
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            support.addPropertyChangeListener(listener);
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
            support.removePropertyChangeListener(listener);
        }

        public void putValue(String key, Object o) {
        }

        public Object getValue(String key) {
            return delegate.getValue(key);
        }

        public void setEnabled(boolean b) {
        }

        public void resultChanged(org.openide.util.LookupEvent ev) {
            boolean newEnabled = isEnabled();

            if (newEnabled != enabled) {
                support.firePropertyChange(PROP_ENABLED, enabled, newEnabled);
                enabled = newEnabled;
            }
        }

        public void propertyChange(PropertyChangeEvent evt) {
            resultChanged(null);
        }

        /*** Finds an action that we should delegate to
         * @return the action or null
         */
        private javax.swing.Action findAction() {
            java.util.Collection c = (result != null) ? result.allInstances() : java.util.Collections.EMPTY_LIST;

            if (!c.isEmpty()) {
                Object key = delegate.getActionMapKey();

                for (Iterator it = c.iterator(); it.hasNext();) {
                    javax.swing.ActionMap map = (javax.swing.ActionMap) it.next();
                    javax.swing.Action action = map.get(key);

                    if (action != null) {
                        return action;
                    }
                }
            }

            return null;
        }

        public javax.swing.JMenuItem getMenuPresenter() {
            if (isMethodOverriden(delegate, "getMenuPresenter")) { // NOI18N

                return delegate.getMenuPresenter();
            } else {
                return org.netbeans.modules.openide.util.AWTBridge.getDefault().createMenuPresenter(this);
            }
        }

        public javax.swing.JMenuItem getPopupPresenter() {
            if (isMethodOverriden(delegate, "getPopupPresenter")) { // NOI18N

                return delegate.getPopupPresenter();
            } else {
                return org.netbeans.modules.openide.util.AWTBridge.getDefault().createPopupPresenter(this);
            }
        }

        public java.awt.Component getToolbarPresenter() {
            if (isMethodOverriden(delegate, "getToolbarPresenter")) { // NOI18N

                return delegate.getToolbarPresenter();
            } else {
                return org.netbeans.modules.openide.util.AWTBridge.getDefault().createToolbarPresenter(this);
            }
        }

        private boolean isMethodOverriden(CallableSystemAction d, String name) {
            try {
                java.lang.reflect.Method m = d.getClass().getMethod(name, new Class[0]);

                return m.getDeclaringClass() != CallableSystemAction.class;
            } catch (java.lang.NoSuchMethodException ex) {
                ex.printStackTrace();
                throw new IllegalStateException("Error searching for method " + name + " in " + d); // NOI18N
            }
        }

        protected void finalize() {
            javax.swing.Action last = (lastRef == null) ? null : (javax.swing.Action) lastRef.get();

            if (last != null) {
                last.removePropertyChangeListener(weakL);
            }
        }
    }
     // end of DelegateAction
}
