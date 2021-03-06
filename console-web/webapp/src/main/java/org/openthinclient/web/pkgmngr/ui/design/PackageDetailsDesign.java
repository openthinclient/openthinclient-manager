package org.openthinclient.web.pkgmngr.ui.design;

import com.vaadin.annotations.AutoGenerated;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.declarative.Design;

/** 
 * !! DO NOT EDIT THIS FILE !!
 * 
 * This class is generated by Vaadin Designer and will be overwritten.
 * 
 * Please make a subclass with logic and additional interfaces as needed,
 * e.g class LoginView extends LoginDesign implements View { }
 */
@DesignRoot
@AutoGenerated
@SuppressWarnings("serial")
public class PackageDetailsDesign extends VerticalLayout {
  protected Label name;
  protected Label version;
  protected HorizontalLayout inlineActionBar;
  protected TabSheet mainTabSheet;
  protected VerticalLayout tabComponentCommon;
  protected Label shortDescription;
  protected Label description;
  protected Label sourceUrl;
  protected VerticalLayout tabComponentRelations;
  protected TabSheet relationsTabSheet;
  protected Grid<org.openthinclient.web.pkgmngr.ui.view.AbstractPackageItem> dependencies;
  protected Grid<org.openthinclient.web.pkgmngr.ui.view.AbstractPackageItem> conflicts;
  protected Grid<org.openthinclient.web.pkgmngr.ui.view.AbstractPackageItem> provides;
  protected Panel tabComponentChangelog;
  protected Label changeLog;
//  protected Panel tabComponentLicense;
  protected TextArea license;
  protected CheckBox acceptLicenseCheckbox;
  protected VerticalLayout tabComponentLicense;

  public PackageDetailsDesign() {
    Design.read(this);
  }
}
