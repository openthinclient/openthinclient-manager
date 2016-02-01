package org.openthinclient.web.pkgmngr.ui.presenter;

public class PackageDetailsPresenter {

  private final View view;

  public PackageDetailsPresenter(View view) {
    this.view = view;
  }

  public void setPackage(org.openthinclient.pkgmgr.db.Package otcPackage) {

    if (otcPackage != null) {
      view.show();
      view.setName(otcPackage.getName());
      view.setVersion(otcPackage.getVersion().toString());
      view.setDescription(otcPackage.getDescription());
    } else {
      view.hide();
    }

  }

  public interface View {

    void setName(String name);

    void setVersion(String version);

    void setDescription(String description);

    void hide();

    void show();
  }
}
