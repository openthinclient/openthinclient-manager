package org.openthinclient.web.pkgmngr.ui;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.UI;
import org.openthinclient.pkgmgr.PackageManager;
import org.springframework.beans.factory.annotation.Autowired;

@Theme("valo")
@SpringUI(path = "/package-manager-test")
public class PackageManagerTestUI extends UI {

  @Autowired
  PackageManager packageManager;

  @Override
  protected void init(VaadinRequest request) {


  }


}
