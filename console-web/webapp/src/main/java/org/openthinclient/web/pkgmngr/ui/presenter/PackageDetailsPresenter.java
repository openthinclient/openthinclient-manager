package org.openthinclient.web.pkgmngr.ui.presenter;

public class PackageDetailsPresenter {

  public PackageDetailsPresenter(View view) {
    this.view = view;
  }

  public interface View {
    void setName(String name);
    void setVersion(String version);
    void setDescription(String description);

    void hide();
    void show();
  }

  private final View view;

  public void setPackage(org.openthinclient.util.dpkg.Package otcPackage) {

    if (otcPackage != null) {
      view.show();
      view.setName(otcPackage.getName());
      view.setVersion(otcPackage.getVersion().toString());
      view.setDescription(otcPackage.getDescription());
    } else {
      view.hide();
    }

  }
}
