package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.ui.*;
import org.openthinclient.web.thinclient.component.ReferenceSection;
import org.openthinclient.web.thinclient.component.ReferencesComponent;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.presenter.ReferencePanelPresenter;
import org.openthinclient.web.thinclient.presenter.ReferencesComponentPresenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ReferencesPanel to display and edit all profile-related references and associations
 */
public class ProfileReferencesPanel extends CssLayout {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfileReferencesPanel.class);


  IMessageConveyor mc;
  VerticalLayout rows;

  public ProfileReferencesPanel(String name, Class clazz) {

    mc = new MessageConveyor(UI.getCurrent().getLocale());
    addStyleName("references-panel-" + clazz.getSimpleName());

    addComponent(new Label(name));

    rows = new VerticalLayout();
    addComponent(rows);

  }

  public ReferencesComponentPresenter addReferences(String label, String buttonCaption, List<Item> allItems, List<Item> referencedItems, boolean isReadOnly) {

    // TODO beachten wg. Presenter und so
    ReferenceSection referenceSection= new ReferenceSection(buttonCaption);
//    rows.addComponent(referenceSection);
//    ReferencePanelPresenter rpp = new ReferencePanelPresenter(this, referencesPanel);

    ReferencesComponent rc = new ReferencesComponent(label);
    ReferencesComponentPresenter rcp = new ReferencesComponentPresenter(rc, allItems, referencedItems, isReadOnly);
    rc.setSpacing(false);

    rows.addComponent(rc);

    return rcp;
  }

}
