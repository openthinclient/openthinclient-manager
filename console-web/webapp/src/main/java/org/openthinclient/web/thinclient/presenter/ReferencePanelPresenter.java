package org.openthinclient.web.thinclient.presenter;

import com.vaadin.ui.Button;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.component.ReferencePanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ReferencePanelPresenter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReferencePanelPresenter.class);

  private ProfilePanel profilePanel;
  private ReferencePanel view;

  public ReferencePanelPresenter(ProfilePanel profilePanel, ReferencePanel view) {
    this.profilePanel = profilePanel;
    this.view = view;

    view.getHead().addClickListener(this::handleItemVisibility);
  }


  public void handleItemVisibility(Button.ClickEvent clickEvent) {
    if (view.isItemsVisible()) {
      view.collapseItems();
    } else {
      view.expandItems();
      profilePanel.handleItemGroupVisibility(view);
    }
  }

}