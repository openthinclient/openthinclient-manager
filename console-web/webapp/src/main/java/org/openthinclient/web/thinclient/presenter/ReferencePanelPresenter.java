package org.openthinclient.web.thinclient.presenter;

import org.openthinclient.common.model.*;
import org.openthinclient.web.thinclient.ProfilePropertiesBuilder;
import org.openthinclient.web.thinclient.ProfileReferencesPanel;
import org.openthinclient.web.thinclient.model.Item;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;


public class ReferencePanelPresenter {

  private ProfileReferencesPanel view;

  public ReferencePanelPresenter(ProfileReferencesPanel view) {
    this.view = view;
  }

  public void showReference(Collection<? extends DirectoryObject> members,
                            String title) {
    showReference(members, title, Collections.emptySet(), null, null);
  }

  public void showReferenceReadOnly(Collection<? extends DirectoryObject> members,
                                    String title,
                                    Function<Item, List<Item>> memberSupplier) {
    showReference(members, title, Collections.emptySet(), null, memberSupplier);
  }

  public void showReference(Collection<? extends DirectoryObject> members,
                            String title, Set<? extends DirectoryObject> allObjects,
                            Consumer<List<Item>> profileReferenceChangeConsumer) {
    showReference(members, title, allObjects, profileReferenceChangeConsumer, null);
  }

  public void showReference(Collection<? extends DirectoryObject> members,
                            Class<? extends DirectoryObject> clazz,
                            String title, Set<? extends DirectoryObject> allObjects,
                            Consumer<List<Item>> profileReferenceChangeConsumer) {
    showReference(filterByClass(members, clazz),
                  title, allObjects, profileReferenceChangeConsumer, null);
  }

  public void showReference(Collection<? extends DirectoryObject> members,
                            Class<? extends DirectoryObject> clazz,
                            String title,
                            Set<? extends DirectoryObject> allObjects,
                            Consumer<List<Item>> profileReferenceChangeConsumer,
                            Function<Item, List<Item>> memberSupplier) {
    showReference(filterByClass(members, clazz),
                  title, allObjects, profileReferenceChangeConsumer, null);
  }

  /**
   * show references and handle changes
   * @param members - DirectoryObject which will be shown as Buttons
   * @param title - Title of reference line
   * @param allObjects - all available DirectoryObjects of a type, this item can be selected in multi-selection-box
   * @param profileReferenceChangeConsumer - consumer to call after changing a reference, i.e. 'save'-action
   * @param memberSupplier - supplier for members of given Item
   */
  public void showReference(Collection<? extends DirectoryObject> members,
                            String title,
                            Set<? extends DirectoryObject> allObjects,
                            Consumer<List<Item>> profileReferenceChangeConsumer,
                            Function<Item, List<Item>> memberSupplier) {

    boolean isReadOnly = (profileReferenceChangeConsumer == null);
    List<Item> memberItems = ProfilePropertiesBuilder.createItems(members);
    ReferencesComponentPresenter presenter = view.addReferences(title,
                                                                ProfilePropertiesBuilder.createItems(allObjects),
                                                                memberItems,
                                                                memberSupplier,
                                                                isReadOnly);
    presenter.setProfileReferenceChangedConsumer(profileReferenceChangeConsumer);
  }

  private Collection<? extends DirectoryObject> filterByClass(Collection<? extends DirectoryObject> members, Class<? extends DirectoryObject> clazz) {
    return members.stream().filter(clazz::isInstance).collect(Collectors.toSet());
  }
}
