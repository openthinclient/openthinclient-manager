package org.openthinclient.web.ui;

import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.thinclient.RealmSettingsView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@SpringUI(path = "/settings")
public final class SettingsUI extends AbstractUI {

  @Autowired
  @Qualifier("settingsSideBar") OTCSideBar settingsSideBar;

  @Autowired
  PackageManagerExecutionEngine packageManagerExecutionEngine;

  @Override
  protected OTCSideBar getSideBar() {
    return settingsSideBar;
  }

  @Override
  protected String getInitialView() {
    return RealmSettingsView.NAME;
  }

  protected Component buildHeader() {
    CssLayout header = new CssLayout(
      getRealmLabel(),
      buildLogoutButton()
    );
    header.addStyleName("header");
    return header;
  }

}
