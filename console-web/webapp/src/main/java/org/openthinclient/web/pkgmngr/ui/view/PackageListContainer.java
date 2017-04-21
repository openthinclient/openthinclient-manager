package org.openthinclient.web.pkgmngr.ui.view;

import com.vaadin.v7.data.Container;
import org.vaadin.viritin.v7.FilterableListContainer;

import java.util.Collection;
import java.util.Collections;

public class PackageListContainer extends FilterableListContainer<AbstractPackageItem> implements Container.Hierarchical {

  /** serialVersionUID */
  private static final long serialVersionUID = -3176386336662441670L;

  public PackageListContainer() {
    super(AbstractPackageItem.class);
  }

  @Override
  public Collection<?> getChildren(Object itemId) {
    // no grouping/child support yet.
    return Collections.emptyList();
  }

  @Override
  public Object getParent(Object itemId) {
    // no grouping/child support yet.
    return null;
  }

  @Override
  public Collection<?> rootItemIds() {
    return getItemIds();
  }

  @Override
  public boolean setParent(Object itemId, Object newParentId) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean areChildrenAllowed(Object itemId) {
    return false;
  }

  @Override
  public boolean setChildrenAllowed(Object itemId, boolean areChildrenAllowed) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isRoot(Object itemId) {
    return getItemIds().contains(itemId);
  }

  @Override
  public boolean hasChildren(Object itemId) {
    return false;
  }

}