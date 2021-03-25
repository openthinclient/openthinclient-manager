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
    showReference(members, title, Collections.emptySet(), null, null, true);
  }

  public void showReferenceAddendum(Collection<? extends DirectoryObject> members,
                                    String title) {
    showReference(members, title, Collections.emptySet(), null, null, false);
  }

  public void showReferenceAddendum(Collection<? extends DirectoryObject> members,
                                    String title,
                                    Function<Item, List<Item>> memberSupplier) {
    showReference(members, title, Collections.emptySet(), null, memberSupplier, false);
  }


  public void showReference(Collection<? extends DirectoryObject> members,
                            String title, Set<? extends DirectoryObject> allObjects,
                            Consumer<List<Item>> profileReferenceChangeConsumer) {
    showReference(members, title, allObjects, profileReferenceChangeConsumer, null, true);
  }

  public void showReference(Collection<? extends DirectoryObject> members,
                            Class<? extends DirectoryObject> clazz,
                            String title, Set<? extends DirectoryObject> allObjects,
                            Consumer<List<Item>> profileReferenceChangeConsumer) {
    showReference(filterByClass(members, clazz),
                  title, allObjects, profileReferenceChangeConsumer, null, true);
  }

  public void showReference(Collection<? extends DirectoryObject> members,
                            Class<? extends DirectoryObject> clazz,
                            String title,
                            Set<? extends DirectoryObject> allObjects,
                            Consumer<List<Item>> profileReferenceChangeConsumer,
                            Function<Item, List<Item>> memberSupplier) {
    showReference(filterByClass(members, clazz),
                  title, allObjects, profileReferenceChangeConsumer, null, true);
  }

  public void showReference(Collection<? extends DirectoryObject> members,
                            String title,
                            Set<? extends DirectoryObject> allObjects,
                            Consumer<List<Item>> profileReferenceChangeConsumer,
                            Function<Item, List<Item>> memberSupplier) {
      showReference(members, title, allObjects, profileReferenceChangeConsumer, memberSupplier, true);
  }

  /**
   * show references and handle changes
   * @param members - DirectoryObject which will be shown as Buttons
   * @param title - Title of reference line
   * @param allObjects - all available DirectoryObjects of a type, this item can be selected in multi-selection-box
   * @param profileReferenceChangeConsumer - consumer to call after changing a reference, i.e. 'save'-action
   * @param memberSupplier - supplier for members of given Item
   * @param isReferenceStart - whether this starts a new reference section (and needs to be visually separated)
   */
  public void showReference(Collection<? extends DirectoryObject> members,
                            String title,
                            Set<? extends DirectoryObject> allObjects,
                            Consumer<List<Item>> profileReferenceChangeConsumer,
                            Function<Item, List<Item>> memberSupplier,
                            boolean isReferenceStart) {

    boolean isReadOnly = (profileReferenceChangeConsumer == null);
    List<Item> memberItems = ProfilePropertiesBuilder.createItems(members);
    ReferencesComponentPresenter presenter = view.addReferences(title,
                                                                ProfilePropertiesBuilder.createItems(allObjects),
                                                                memberItems,
                                                                memberSupplier,
                                                                isReadOnly,
                                                                isReferenceStart);
    presenter.setProfileReferenceChangedConsumer(profileReferenceChangeConsumer);
  }

  private Collection<? extends DirectoryObject> filterByClass(Collection<? extends DirectoryObject> members, Class<? extends DirectoryObject> clazz) {
    return members.stream().filter(clazz::isInstance).collect(Collectors.toSet());
  }
}
