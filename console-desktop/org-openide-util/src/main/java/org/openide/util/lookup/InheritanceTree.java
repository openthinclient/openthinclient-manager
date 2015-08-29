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
package org.openide.util.lookup;

import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup.ReferenceIterator;
import org.openide.util.lookup.AbstractLookup.ReferenceToResult;

import java.io.*;

import java.lang.ref.WeakReference;

import java.util.*;


/** A tree to represent classes with inheritance. Description of the
 * data structure by Petr Nejedly:
 * <P>
 * So pretend I'm Lookup implementation. I've got a bunch of Items (e.g.
 * setPairs() method),
 * didn't do anything on them yet (no startup penalty) so I know nothing
 * about them.
 * Then I'll be asked for all instances implementing given interface or a
 * class. I surely need
 * to check all the Items now, as I don't know anything abou them. I surely
 * don't want to call
 * Item.getClass() as it will dismiss the whole effort. So all I have is
 * Item.instanceOf()
 * and I'll call it on every Item. I'll cache results, so the next time
 * you'll ask me for
 * the same interface/class, I'll answer immediatelly. But what if you ask
 * me for another
 * interface/class? I'll have to scan all Items for it again, unless I can
 * be sure some
 * of them can't implement it. The only source of this knowledge are the
 * previous questions
 * and my rulings on them. Here the algorithm have to be split into two
 * paths. If you
 * previously asked me for interfaces only, I'll have no hint for
 * subsequent queries,
 * but if you asked me for a class in history, and then for another class
 * and these classes
 * are not in inheritance relation (I can check hierarchy of lookup
 * arguments, because
 * they are already resolved/loaded) I can tell that those returned in
 * previous query can't
 * implement the newly asked class (they are in different hierarchy branch)
 * and I need to
 * ask less Items.
 * <P>
 * So if we use mostly classes for asking for services (and it is a trend
 * to use
 * abstract classes for this purpose in IDE anyway), this could be usable.
 * <P>
 * The data structure for separating the Items based on previous queries is
 * simple
 * tree, with every node tagged with one class. The tree's root is,
 * naturally,
 * java.lang.Object, is marked invited and initially contains all the
 * Items.
 * For every class query, the missing part of class hierarchy tree is
 * created,
 * the node of the class looked up is marked as invited and all Items from
 * nearest
 * invited parent (sperclass) are dragged to this node. The result are then
 * all
 * Items from this node and all the nodes deeper in hierarchy. Because it
 * may
 * be too complicated to walk through the children nodes, the results could
 * be
 * cached in the map.
 * For interface lookup, there is a little hint in reality (interfaces
 * and superinterfaces), but it would be harder to exploit it, so we could
 * fall-back
 * to walking through all the Items and cache results.
 *
 *
 * @author  Jaroslav Tulach
 */
final class InheritanceTree extends Object implements Serializable, AbstractLookup.Storage {
    private static final long serialVersionUID = 1L;

    /** the root item (represents Object) */
    private transient Node object;

    /** Map of queried interfaces.
     * <p>Type: <code>Map&lt;Class, (Collection&lt;AbstractLookup.Pair&gt; | AbstractLookup.Pair)&gt;</code>
     */
    private transient Map interfaces;

    /** Map (Class, ReferenceToResult) of all listeners that are waiting in
     * changes in class Class
     */
    private transient Map reg;

    /** Constructor
     */
    public InheritanceTree() {
        object = new Node(java.lang.Object.class);
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.writeObject(object);

        if (interfaces != null) {
            Iterator it = interfaces.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                Class c = (Class) e.getKey();
                oos.writeObject(c.getName());

                Object o = e.getValue();

                if (!(o instanceof Collection) && !(o instanceof AbstractLookup.Pair)) {
                    throw new ClassCastException(String.valueOf(o));
                }

                oos.writeObject(o);
            }
        }

        oos.writeObject(null);
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        object = (Node) ois.readObject();
        interfaces = new WeakHashMap();

        String clazz;
        ClassLoader l = (ClassLoader) Lookup.getDefault().lookup(ClassLoader.class);

        while ((clazz = (String) ois.readObject()) != null) {
            Object o = ois.readObject();

            if (!(o instanceof Collection) && !(o instanceof AbstractLookup.Pair)) {
                throw new ClassCastException(String.valueOf(o));
            }

            Class c = Class.forName(clazz, false, l);
            interfaces.put(c, o);
        }
    }

    /** Adds an item into the tree.
    * @param item to add
    * @return true if the Item has been added for the first time or false if some other
    *    item equal to this one already existed in the lookup
    */
    public boolean add(AbstractLookup.Pair item, Object transaction) {
        ArrayList affected = (ArrayList) transaction;

        Node node = registerClass(object, item);

        affected.add(node.getType());

        if (node.assignItem(this, item)) {
            // this is the first item added to n.items
            // ok, we have to test interfaces too
        } else {
            // equal item is already there => stop processing
            return false;
        }

        boolean registeredAsInterface = registerInterface(item, affected);

        return registeredAsInterface;
    }

    /** Removes an item.
    */
    public void remove(AbstractLookup.Pair item, Object transaction) {
        ArrayList affected = (ArrayList) transaction;

        Node n = removeClass(object, item);

        if (n != null) {
            affected.add(n.getType());
        }

        removeInterface(item, affected);
    }

    /** Removes all items that are not present in the provided collection.
    * @param retain collection of Pairs to keep them in
    * @param notify set of Classes that has possibly changed
    */
    public void retainAll(Map retain, Object transaction) {
        ArrayList notify = (ArrayList) transaction;

        retainAllInterface(retain, notify);
        retainAllClasses(object, retain, notify);
    }

    /** Queries for instances of given class.
    * @param clazz the class to check
    * @return enumeration of Item
    * @see #unsorted
    */
    public Enumeration lookup(Class clazz) {
        if ((clazz != null) && clazz.isInterface()) {
            return searchInterface(clazz);
        } else {
            return searchClass(object, clazz);
        }
    }

    /** A method to check whether the enumeration returned from
     * lookup method is sorted or is not
     * @param en enumeration to check
     * @return true if it is unsorted and needs to be sorted to find
     *   pair with smallest index
     */
    public static boolean unsorted(Enumeration en) {
        return en instanceof NeedsSortEnum;
    }

    /** Prints debug messages.
     * @param out stream to output to
     * @param instances print also instances of the
     */
    public void print(java.io.PrintStream out, boolean instances) {
        printNode(object, "", out, instances); // NOI18N
    }

    //
    // methods to work on classes which are not interfaces
    //

    /** Searches the subtree and register the item where necessary.
    * @return the node that should contain the item
    */
    private static Node registerClass(Node n, AbstractLookup.Pair item) {
        if (!n.accepts(item)) {
            return null;
        }

        if (n.children != null) {
            Iterator it = n.children.iterator();

            for (;;) {
                Node ch = extractNode(it);

                if (ch == null) {
                    break;
                }

                Node result = registerClass(ch, item);

                if (result != null) {
                    // it is in subclass, in case of classes, it cannot
                    // be any other class
                    return result;
                }
            }
        }

        // ok, nobody of our subclasses wants the class, I'll take it
        return n;
    }

    /** Removes the item from the tree of objects.
    * @return most narrow class that this item was removed from
    */
    private static Node removeClass(Node n, AbstractLookup.Pair item) {
        if (!n.accepts(item)) {
            return null;
        }

        if ((n.items != null) && n.items.remove(item)) {
            // this node really contains the item
            return n;
        }

        if (n.children != null) {
            Iterator it = n.children.iterator();

            for (;;) {
                Node ch = extractNode(it);

                if (ch == null) {
                    break;
                }

                Node result = removeClass(ch, item);

                // If the children node was emptied, remove it if possible.
                if (((ch.items == null) || ch.items.isEmpty()) && ((ch.children == null) || ch.children.isEmpty())) {
                    it.remove();
                }

                if (result != null) {
                    // it is in subclass, in case of classes, it cannot
                    // be any other class
                    return result;
                }
            }
        }

        // nobody found
        return null;
    }

    /** Finds a node that represents a class.
    * @param n node to search from
    * @param clazz the clazz to find
    * @return node that represents clazz in the tree or null if the clazz is not
    *    represented under the node n
    */
    private Node classToNode(final Node n, final Class clazz) {
        if (!n.accepts(clazz)) {
            // nothing from us
            return null;
        }

        if (n.getType() == clazz) {
            // we have found what we need
            return n;
        }

        if (n.children != null) {
            // have to proceed to children
            Iterator it = n.children.iterator();

            for (;;) {
                final Node ch = extractNode(it);

                if (ch == null) {
                    break;
                }

                Node found = classToNode(ch, clazz);

                if ((found != null) && ch.deserialized()) {
                    class VerifyJob implements AbstractLookup.ISE.Job {
                        private AbstractLookup.Pair[] pairs;
                        private boolean[] answers;

                        public VerifyJob(Collection items) {
                            if (items != null) {
                                pairs = (AbstractLookup.Pair[]) items.toArray(new AbstractLookup.Pair[0]);
                            }
                        }

                        public void before() {
                            // make sure the node is converted into deserialized state
                            ch.deserialized();

                            if (pairs != null) {
                                answers = new boolean[pairs.length];

                                for (int i = 0; i < pairs.length; i++) {
                                    answers[i] = pairs[i].instanceOf(clazz);
                                }
                            }
                        }

                        public void inside() {
                            if (pairs != null) {
                                for (int i = 0; i < pairs.length; i++) {
                                    if (answers[i]) {
                                        ch.assignItem(InheritanceTree.this, pairs[i]);
                                        n.items.remove(pairs[i]);
                                    }
                                }
                            }

                            if (n.children != null) {
                                // consolidate all nodes that represent the same class
                                HashMap nodes = new HashMap(n.children.size() * 3);

                                Iterator child = n.children.iterator();

                                while (child.hasNext()) {
                                    Node node = extractNode(child);
                                    Node prev = (Node) nodes.put(node.getType(), node);

                                    if (prev != null) {
                                        child.remove();
                                        nodes.put(node.getType(), prev);

                                        // mark as being deserialized
                                        prev.markDeserialized();

                                        if (prev.children == null) {
                                            prev.children = node.children;
                                        } else {
                                            if (node.children != null) {
                                                prev.children.addAll(node.children);
                                            }
                                        }

                                        if (node.items != null) {
                                            Iterator items = node.items.iterator();

                                            while (items.hasNext()) {
                                                AbstractLookup.Pair item = (AbstractLookup.Pair) items.next();
                                                prev.assignItem(InheritanceTree.this, item);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    VerifyJob verify = new VerifyJob(n.items);

                    try {
                        verify.before();
                    } catch (AbstractLookup.ISE ex) {
                        // mark deserialized again
                        ch.markDeserialized();
                        ex.registerJob(verify);
                        throw ex;
                    }

                    verify.inside();

                    found = classToNode(ch, clazz);
                }

                if (found != null) {
                    // class found in one of subnodes
                    return found;
                }
            }
        }

        class TwoJobs implements AbstractLookup.ISE.Job {
            private AbstractLookup.Pair[] pairs;
            private boolean[] answers;
            private Node newNode;

            public void before() {
                // have to create new subnode and possibly reparent one of my own
                // but all changes can be done only if we will not be interrupted from
                // outside - e.g. instanceOf methods will not throw exception
                // first of all let's compute the answers to method instanceOf
                AbstractLookup.Pair[] arr = null;
                boolean[] boolArr = null;

                if (n.items != null) {
                    arr = new AbstractLookup.Pair[n.items.size()];
                    boolArr = new boolean[n.items.size()];

                    int i = 0;
                    Iterator it = n.items.iterator();

                    while (it.hasNext()) {
                        AbstractLookup.Pair item = (AbstractLookup.Pair) it.next();
                        arr[i] = item;
                        boolArr[i] = item.instanceOf(clazz);
                        i++;
                    }
                }

                pairs = arr;
                answers = boolArr;
            }

            public void inside() {
                // test if the query has not chagned since
                if (pairs != null) {
                    if (!Arrays.equals(n.items.toArray(), pairs)) {
                        // ok, let try once more
                        return;
                    }
                }

                internal();
            }

            public void internal() {
                ArrayList reparent = null;

                if (n.children == null) {
                    n.children = new ArrayList();
                } else {
                    // scan thru all my nodes if some of them are not a subclass
                    // of clazz => then they would need to become child of newNode
                    Iterator it = n.children.iterator();

                    for (;;) {
                        Node r = extractNode(it);

                        if (r == null) {
                            break;
                        }

                        if (clazz.isAssignableFrom(r.getType())) {
                            if (reparent == null) {
                                reparent = new ArrayList();
                            }

                            reparent.add(r);
                            it.remove();
                        }
                    }
                }

                newNode = new Node(clazz);
                n.children.add(newNode);

                if (reparent != null) {
                    // reassing reparent node as a child of newNode
                    newNode.children = reparent;
                }

                // now take all my items that are instances of that class and
                // reasign them
                if (n.items != null) {
                    Iterator it = n.items.iterator();
                    int i = 0;

                    while (it.hasNext()) {
                        AbstractLookup.Pair item = (AbstractLookup.Pair) it.next();

                        if (answers[i]) { // answers[i] is precomputed value of item.instanceOf (clazz))
                            it.remove();
                            newNode.assignItem(InheritanceTree.this, pairs[i]);
                        }

                        i++;
                    }
                }
            }
        }

        TwoJobs j = new TwoJobs();

        try {
            j.before();
        } catch (AbstractLookup.ISE ex) {
            // ok, it is not possible to call instanceOf now, let's 
            // schedule it for later
            // so register recovery job 
            ex.registerJob(j);
            throw ex;
        }

        j.internal();

        // newNode represents my clazz
        return j.newNode;
    }

    /** Search for a requested class.
    * @return enumeration of Pair
    */
    private Enumeration searchClass(Node n, Class clazz) {
        if (clazz != null) {
            n = classToNode(n, clazz);
        }

        if (n == null) {
            // not for us
            return org.openide.util.Enumerations.empty();
        } else {
            return nodeToEnum(n);
        }
    }

    /** Retains all classes. Removes nodes which items and children are emptied, works
     * recursivelly from specified root node.
     * @param node root node from which to start to process the tree
     * @param retain a map from (Item, AbstractLookup.Info) that describes which items to retain
     *    and witch integer to assign them
     * @param notify collection of classes will be changed
     * @return <code>true<code> if some items were changed and node items and children are emptied,
     * those nodes, excluding root, will be removed from tree */
    private boolean retainAllClasses(Node node, Map retain, Collection notify) {
        boolean retained = false;

        if ((node.items != null) && (retain != null)) {
            Iterator it = node.items.iterator();

            while (it.hasNext()) {
                AbstractLookup.Pair item = (AbstractLookup.Pair) it.next();
                AbstractLookup.Info n = (AbstractLookup.Info) retain.remove(item);

                if (n == null) {
                    // remove this item, it should not be there
                    it.remove();
                    retained = true;
                } else {
                    // change the index
                    if (item.getIndex() != n.index) {
                        item.setIndex(null, n.index);

                        //                        notify.addAll ((ArrayList)n.transaction);
                    }
                }
            }

            if (retained && (notify != null)) {
                // type of this node has been changed
                notify.add(node.getType());
            }
        }

        if (node.children != null) {
            for (Iterator it = node.children.iterator();;) {
                Node ch = extractNode(it);

                if (ch == null) {
                    break;
                }

                boolean result = retainAllClasses(ch, retain, notify);

                if (result) {
                    // The children node was emptied and has no children -> remove it.
                    it.remove();
                }
            }
        }

        return retained && node.items.isEmpty() && ((node.children == null) || node.children.isEmpty());
    }

    /** A method that creates enumeration of all items under given node.
     *
     * @param n node to create enumeration for
     * @return enumeration of Pairs
     */
    private static Enumeration nodeToEnum(Node n) {
        if (n.children == null) {
            // create a simple enumeration because we do not have children
            return (n.items == null) ? org.openide.util.Enumerations.empty() : Collections.enumeration(n.items);
        }

        // we have found what we need
        // now we have to just build the enumeration
        class DeepAndItems implements org.openide.util.Enumerations.Processor {
            public Object process(Object obj, Collection toAdd) {
                Node n2 = (Node) obj;

                if (n2.children != null) {
                    toAdd.addAll(n2.children);
                }

                if ((n2.items == null) || n2.items.isEmpty()) {
                    return org.openide.util.Enumerations.empty();
                } else {
                    return Collections.enumeration(n2.items);
                }
            }
        }

        Enumeration en = org.openide.util.Enumerations.queue(
                
            // initial node is our current one
            org.openide.util.Enumerations.singleton(n), new DeepAndItems()
            );

        // create enumeration of Items
        return new NeedsSortEnum(org.openide.util.Enumerations.concat(en));
    }

    //
    // Methods to work on interfaces
    // 

    /** Registers an item with interfaces.
    * @param item item to register
    * @param affected list of classes that were affected
    * @return false if similar item has already been registered
    */
    private boolean registerInterface(AbstractLookup.Pair item, Collection affected) {
        if (interfaces == null) {
            return true;
        }

        Iterator it = interfaces.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Class iface = (Class) entry.getKey();

            if (item.instanceOf(iface)) {
                Object value = entry.getValue();

                if (value instanceof Collection) {
                    Collection set = (Collection) value;

                    if (!set.add(item)) {
                        // item is already there, probably (if everything is correct) is registered in 
                        // all other ifaces too, so stop additional testing
                        return false;
                    }
                } else {
                    // there is just one pair right now
                    if (value.equals(item)) {
                        // item is there => stop processing (same as above)
                        return false;
                    }

                    // otherwise replace the single item with ArrayList
                    ArrayList ll = new ArrayList(3);
                    ll.add(value);
                    ll.add(item);
                    entry.setValue(ll);
                }

                affected.add(iface);
            }
        }

        return true;
    }

    /** Removes interface.
    * @param item item to register
    * @param affected list of classes that were affected
    */
    private void removeInterface(AbstractLookup.Pair item, Collection affected) {
        if (interfaces == null) {
            return;
        }

        Iterator it = interfaces.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Object value = entry.getValue();

            if (value instanceof Collection) {
                Collection set = (Collection) value;

                if (set.remove(item)) {
                    if (set.size() == 1) {
                        // if there is just one item remaining change to single item mode
                        entry.setValue(set.iterator().next());
                    }

                    // adds the Class the item was register to into affected
                    affected.add(entry.getKey());
                }
            } else {
                // single item value
                if (value.equals(item)) {
                    // Emptied -> remove.
                    it.remove();

                    affected.add(entry.getKey());
                }
            }
        }
    }

    /** Retains some items.
    * @param retainItems items to retain and their mapping to index numbers
    *    (AbstractLookup.Pair -> AbstractLookup.Info)
    * @param affected list of classes that were affected
    */
    private void retainAllInterface(Map retainItems, Collection affected) {
        if (interfaces == null) {
            return;
        }

        Iterator it = interfaces.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Object value = entry.getValue();

            HashMap retain = new HashMap(retainItems);

            Iterator elems;
            boolean multi = value instanceof Collection;

            if (multi) {
                // collection mode
                elems = ((Collection) value).iterator();
            } else {
                // single item mode
                elems = Collections.singleton(value).iterator();
            }

            boolean changed = false;
            boolean reordered = false;

            while (elems.hasNext()) {
                AbstractLookup.Pair p = (AbstractLookup.Pair) elems.next();

                AbstractLookup.Info n = (AbstractLookup.Info) retain.remove(p);

                if (n == null) {
                    if (multi) {
                        // remove it
                        elems.remove();
                    }

                    changed = true;
                } else {
                    if (p.getIndex() != n.index) {
                        // improve the index
                        p.setIndex(null, n.index);

                        //                    affected.addAll ((ArrayList)n.transaction);
                        reordered = true;
                    }
                }
            }

            if (reordered && value instanceof List) {
                // if reordered, than update the order in the collection
                List l = (List) value;
                Collections.sort(l, ALPairComparator.DEFAULT);
            }

            if (changed) {
                if (multi) {
                    Collection c = (Collection) value;

                    if (c.size() == 1) {
                        // back to single item mode
                        entry.setValue(c.iterator().next());
                    }
                } else {
                    // remove in single mode => remove completely
                    it.remove();
                }

                // adds the Class the item was register to into affected
                affected.add(entry.getKey());
            }
        }
    }

    /** Searches for a clazz between interfaces.
    * @param clazz class to search for
    * @return enumeration of Items
    */
    private Enumeration searchInterface(final Class clazz) {
        if (interfaces == null) {
            // first call for interface, only initialize
            interfaces = new WeakHashMap();
        }

        Object obj = interfaces.get(clazz);

        if (obj == null) {
            // set of items
            AbstractLookup.Pair one = null;
            ArrayList items = null;

            Enumeration en = lookup(Object.class);

            while (en.hasMoreElements()) {
                AbstractLookup.Pair it = (AbstractLookup.Pair) en.nextElement();

                if (it.instanceOf(clazz)) {
                    // ok, this item implements given clazz
                    if (one == null) {
                        one = it;
                    } else {
                        if (items == null) {
                            items = new ArrayList(3);
                            items.add(one);
                        }

                        items.add(it);
                    }
                }
            }

            if ((items == null) && (one != null)) {
                // single item mode
                interfaces.put(clazz, one);

                return org.openide.util.Enumerations.singleton(one);
            } else {
                if (items == null) {
                    items = new ArrayList(2);
                }

                interfaces.put(clazz, items);

                return Collections.enumeration(items);
            }
        } else {
            if (obj instanceof Collection) {
                return Collections.enumeration((Collection) obj);
            } else {
                // single item mode
                return org.openide.util.Enumerations.singleton(obj);
            }
        }
    }

    /** Extracts a node from an iterator, returning null if no next element found
     */
    private static Node extractNode(Iterator it) {
        while (it.hasNext()) {
            Node n = (Node) it.next();

            if (n.get() == null) {
                it.remove();
            } else {
                return n;
            }
        }

        return null;
    }

    /** Prints debug info about the node.
     * @param n node to print
     * @param sp spaces to add
     * @param out where
     * @param instances print also instances
     */
    private static void printNode(Node n, String sp, java.io.PrintStream out, boolean instances) {
        int i;
        Iterator it;

        Class type = n.getType();

        out.print(sp);
        out.println("Node for: " + type + "\t" + ((type == null) ? null : type.getClassLoader())); // NOI18N

        if (n.items != null) {
            i = 0;
            it = new ArrayList(n.items).iterator();

            while (it.hasNext()) {
                AbstractLookup.Pair p = (AbstractLookup.Pair) it.next();
                out.print(sp);
                out.print("  item (" + i++ + "): ");
                out.print(p); // NOI18N
                out.print(" id: " + Integer.toHexString(System.identityHashCode(p))); // NOI18N
                out.print(" index: "); // NOI18N
                out.print(p.getIndex());

                if (instances) {
                    out.print(" I: " + p.getInstance());
                }

                out.println();
            }
        }

        if (n.children != null) {
            i = 0;
            it = n.children.iterator();

            while (it.hasNext()) {
                Node ch = (Node) it.next();
                printNode(ch, sp + "  ", out, instances); // NOI18N
            }
        }
    }

    public ReferenceToResult registerReferenceToResult(ReferenceToResult newRef) {
        if (reg == null) {
            reg = new HashMap();
        }

        Class clazz = newRef.template.getType();

        // initialize the data structures if not yet
        lookup(clazz);

        // newRef will be the new head of the list
        return (ReferenceToResult) reg.put(clazz, newRef);
    }

    public ReferenceToResult cleanUpResult(Lookup.Template templ) {
        collectListeners(null, templ.getType());

        return (reg == null) ? null : (ReferenceToResult) reg.get(templ.getType());
    }

    public Object beginTransaction(int ensure) {
        return new ArrayList();
    }

    public void endTransaction(Object transaction, Set allAffectedResults) {
        ArrayList list = (ArrayList) transaction;

        if (list.size() == 1) {
            // probably the most common case
            collectListeners(allAffectedResults, (Class) list.get(0));
        } else {
            Iterator it = list.iterator();

            while (it.hasNext()) {
                collectListeners(allAffectedResults, (Class) it.next());
            }
        }
    }

    /** Notifies all listeners that are interested in changes in this class.
     * Should be called from synchronized places.
     * @param allAffectedResults adds Results into this set
     * @param c the class that has changed
     */
    private void collectListeners(Set allAffectedResults, Class c) {
        if (reg == null) {
            return;
        }

        while (c != null) {
            ReferenceToResult first = (ReferenceToResult) reg.get(c);
            ReferenceIterator it = new ReferenceIterator(first);

            while (it.next()) {
                Object result = it.current().getResult();

                if (allAffectedResults != null) {
                    // add result
                    allAffectedResults.add(result);
                }
            }

            if (first != it.first()) {
                if (it.first() == null) {
                    // we do not need have more results on this object
                    reg.remove(c);
                } else {
                    // move the head of the list
                    reg.put(c, it.first());
                }
            }

            c = c.getSuperclass();
        }

        if (reg.isEmpty()) {
            // clean up the list of all results if we do not need them anymore
            reg = null;
        }
    }

    /** Node in the tree.
    */
    static final class Node extends WeakReference implements Serializable {
        static final long serialVersionUID = 3L;

        /** children nodes */
        public ArrayList children; // List<Node>

        /** list of items assigned to this node (suspect to be subclasses) */
        public List items; // List<AbstractLookup.Pair>

        /** Constructor.
        */
        public Node(Class clazz) {
            super(clazz);
        }

        /** Returns true if the object was deserialized also clears the serialized flag.
         * @return true if so.
         */
        public boolean deserialized() {
            if ((items == null) || items instanceof ArrayList) {
                return false;
            }

            if (items.isEmpty()) {
                items = null;
            } else {
                items = new ArrayList(items);
            }

            return true;
        }

        /** Marks this item as being deserialized.
         */
        public void markDeserialized() {
            items = (items == null) ? Collections.EMPTY_LIST : Collections.synchronizedList(items);
        }

        /** Getter for the type associated with this node.
         */
        public Class getType() {
            Class c = (Class) get();

            // if  garbage collected, then return a garbage
            return (c == null) ? Void.TYPE : c;
        }

        /** Checks whether a node can represent an class.
        */
        public boolean accepts(Class clazz) {
            if (getType() == Object.class) {
                return true;
            }

            return getType().isAssignableFrom(clazz);
        }

        /** Checks whether item is instance of this node.
        */
        public boolean accepts(AbstractLookup.Pair item) {
            if (getType() == Object.class) {
                // Object.class
                return true;
            }

            return item.instanceOf(getType());
        }

        /** Assings an item to this node.
        * @param item the item
        * @return true if item has been added as new
        */
        public boolean assignItem(InheritanceTree tree, AbstractLookup.Pair item) {
            if ((items == null) || (items == Collections.EMPTY_LIST)) {
                items = new ArrayList();
                items.add(item);

                return true;
            }

            if (items.contains(item)) {
                int i = items.indexOf(item);
                AbstractLookup.Pair old = (AbstractLookup.Pair) items.get(i);

                if (old != item) {
                    // replace the items there
                    item.setIndex(tree, old.getIndex());
                }

                items.remove(old);
                items.add(item);

                return false;
            }

            items.add(item);

            return true;
        }

        private Object writeReplace() {
            return new R(this);
        }

        public String toString() {
            return "Node for " + get();
        }
    }
     // End of class Node.

    private static final class R implements Serializable {
        static final long serialVersionUID = 1L;
        private static ClassLoader l;
        private String clazzName;
        private transient Class clazz;
        private ArrayList children;
        private ArrayList items;

        public R(Node n) {
            this.clazzName = n.getType().getName();
            this.children = n.children;

            if (n.items instanceof ArrayList || (n.items == null)) {
                this.items = (ArrayList) n.items;
            } else {
                this.items = new ArrayList(n.items);
            }
        }

        private void readObject(ObjectInputStream ois)
        throws IOException, ClassNotFoundException {
            ois.defaultReadObject();

            if (l == null) {
                l = (ClassLoader) Lookup.getDefault().lookup(ClassLoader.class);
            }

            clazz = Class.forName(clazzName, false, l);
        }

        private Object readResolve() throws ObjectStreamException {
            Node n = new Node(clazz);
            n.children = children;
            n.items = items;
            n.markDeserialized();

            return n;
        }
    }
     // end of R

    /** Just a marker class to be able to do instanceof and find out
     * that this enumeration is not sorted
     */
    private static final class NeedsSortEnum implements Enumeration {
        private Enumeration en;

        public NeedsSortEnum(Enumeration en) {
            this.en = en;
        }

        public boolean hasMoreElements() {
            return en.hasMoreElements();
        }

        public Object nextElement() {
            return en.nextElement();
        }
    }
     // end of NeedsSortEnum
}
