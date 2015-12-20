package org.openthinclient.web.pkgmngr.ui;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.UI;
import org.openthinclient.web.pkgmngr.ui.design.PackageManagerMainDesign;

@Theme("valo")
@SpringUI(path = "/package-manager-test")
public class PackageManagerTestUI extends UI {
  @Override
  protected void init(VaadinRequest request) {

    setContent(new PackageManagerMainDesign());

  }
}
