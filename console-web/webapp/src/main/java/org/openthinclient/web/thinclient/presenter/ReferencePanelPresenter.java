package org.openthinclient.web.thinclient.presenter;

import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.BindingValidationStatus;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.ui.Button;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.component.ItemGroupPanel;
import org.openthinclient.web.thinclient.component.ReferencePanel;
import org.openthinclient.web.thinclient.model.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ReferencePanelPresenter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReferencePanelPresenter.class);

  ProfilePanel profilePanel;
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