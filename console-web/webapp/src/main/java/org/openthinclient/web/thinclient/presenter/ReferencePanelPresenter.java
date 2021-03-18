package org.openthinclient.web.thinclient.presenter;

import org.openthinclient.common.model.*;
import org.openthinclient.web.thinclient.ProfilePropertiesBuilder;
import org.openthinclient.web.thinclient.ProfileReferencesPanel;
import org.openthinclient.web.thinclient.model.Item;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;


public class ReferencePanelPresenter {

  protected ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();
  private ProfileReferencesPanel view;

  public ReferencePanelPresenter(ProfileReferencesPanel view) {
    this.view = view;
  }

  /**
   * default method to show references and handle changes
   * @param members - DirectoryObject which will be shown as Buttons
   * @param title - Title of reference line
   * @param allObjects - all available DirectoryObjects of a type, this item can be selected in single- or multi-selection-box
   * @param clazz - Class of DirectoryObjects
   */
  public void showReference(Set<? extends DirectoryObject> members,
                            String title, Set<? extends DirectoryObject> allObjects, Class clazz,
                            Consumer<List<Item>> profileReferenceChangeConsumer) {

    // values -> saveReference(profile, values, allObjects, clazz)
    showReference(members, title, allObjects, clazz, profileReferenceChangeConsumer,null, false);
  }

  /**
   * show references and handle changes
   * @param members - DirectoryObject which will be shown as Buttons
   * @param title - Title of reference line
   * @param allObjects - all available DirectoryObjects of a type, this item can be selected in single- or multi-selection-box
   * @param clazz - Class of DirectoryObjects
   * @param profileReferenceChangeConsumer - consumer to call after changing a reference, i.e. 'save'-action
   * @param memberSupplier - supplier for members of given Item
   * @param isReadOnly - display items in readonly mode
   */
  public void showReference(Set<? extends DirectoryObject> members,
                            String title,
                            Set<? extends DirectoryObject> allObjects,
                            Class clazz,
                            Consumer<List<Item>> profileReferenceChangeConsumer,
                            Function<Item, List<Item>> memberSupplier,
                            boolean isReadOnly) {

    List<Item> memberItems = builder.createFilteredItemsFromDO(members, clazz);
    ReferencesComponentPresenter presenter = view.addReferences(title, builder.createItems(allObjects), memberItems, memberSupplier, isReadOnly);
    presenter.setProfileReferenceChangedConsumer(profileReferenceChangeConsumer);
  }
}
