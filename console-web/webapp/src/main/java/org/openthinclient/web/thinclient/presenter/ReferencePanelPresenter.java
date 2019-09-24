package org.openthinclient.web.thinclient.presenter;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.ui.*;
import org.openthinclient.common.model.*;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.ProfilePropertiesBuilder;
import org.openthinclient.web.thinclient.ProfileReferencesPanel;
import org.openthinclient.web.thinclient.model.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;


public class ReferencePanelPresenter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReferencePanelPresenter.class);

  protected ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();
  private ProfileReferencesPanel view;
  private IMessageConveyor mc;

  public ReferencePanelPresenter(ProfileReferencesPanel view) {
    this.view = view;

    mc = new MessageConveyor(UI.getCurrent().getLocale());
//    view.getHead().addClickListener(this::handleItemVisibility);
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
    ReferencesComponentPresenter presenter = view.addReferences(title, mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_ASSOCIATION), builder.createItems(allObjects), memberItems, memberSupplier, isReadOnly);
    presenter.setProfileReferenceChangedConsumer(profileReferenceChangeConsumer);
  }


  // show device associations
  public void showDeviceAssociations(Set<Device> all, Set<? extends DirectoryObject> members, Consumer<List<Item>> profileReferenceChangeConsumer) {
    List<Item> allDevices = builder.createItems(all);
    List<Item> deviceMembers = builder.createFilteredItemsFromDO(members, Device.class);
    ReferencesComponentPresenter presenter = view.addReferences(mc.getMessage(ConsoleWebMessages.UI_ASSOCIATED_DEVICES_HEADER),
        mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_ASSOCIATION),
        allDevices, deviceMembers, null, false);
    presenter.setProfileReferenceChangedConsumer(profileReferenceChangeConsumer);
  }

}