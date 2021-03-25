package org.openthinclient.web.thinclient;

import com.vaadin.ui.*;

import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.web.thinclient.component.ReferencesComponent;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.presenter.ReferencesComponentPresenter;

import java.util.List;
import java.util.function.Function;

/**
 * ReferencesPanel to display and edit all profile-related references and associations
 */
public class ProfileReferencesPanel extends CssLayout {

  CssLayout rows;

  public ProfileReferencesPanel(Class<? extends DirectoryObject> clazz) {

    addStyleName("references-panel");
    addStyleName("references-panel-" + clazz.getSimpleName());

    rows = new CssLayout();
    rows.addStyleName("referenceComponents");
    addComponent(rows);

  }

  public ReferencesComponentPresenter addReferences(String label, List<Item> allItems, List<Item> referencedItems, Function<Item, List<Item>> memberSupplier, boolean isReadOnly) {

    ReferencesComponent rc = new ReferencesComponent(label, isReadOnly);
    ReferencesComponentPresenter rcp = new ReferencesComponentPresenter(rc, allItems, referencedItems, memberSupplier, isReadOnly);
    rows.addComponent(rc);
    return rcp;
  }

}
