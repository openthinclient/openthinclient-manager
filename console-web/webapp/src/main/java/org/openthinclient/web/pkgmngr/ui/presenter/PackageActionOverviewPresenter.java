package org.openthinclient.web.pkgmngr.ui.presenter;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.UI;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.web.i18n.ConsoleWebMessages;

import java.util.Collection;

import ch.qos.cal10n.MessageConveyor;

public class PackageActionOverviewPresenter {

  private final View view;
  private final MessageConveyor mc;

  private PackageDetailsListPresenter.Mode mode;
  private Runnable callback;

  public PackageActionOverviewPresenter(View view) {
    this.view = view;
    mc = new MessageConveyor(UI.getCurrent().getLocale());

    view.setPackageNameColumnCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME));
    view.setPackageVersionColumnCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION));
    view.onPerfomAction(() -> {
      if (callback != null)
        callback.run();
    });
  }

  public void setPackages(Collection<Package> otcPackages) {
    view.setPackages(otcPackages);
  }

  public PackageDetailsListPresenter.Mode getMode() {
    return mode;
  }

  public void setMode(PackageDetailsListPresenter.Mode mode) {

    switch (mode) {

      case UPDATE:
        // fall through
      case INSTALL:
        view.setViewCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_BUTTON_INSTALL_LABEL_MULTI));
        view.setViewDescription(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_ACTION_INSTALL_DESCRIPTION));
        view.setActionCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_BUTTON_INSTALL_CAPTION));
        view.setActionIcon(VaadinIcons.DOWNLOAD);
        break;
      case UNINSTALL:
        view.setViewCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_BUTTON_UNINSTALL_LABEL_MULTI));
        view.setViewDescription(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_ACTION_UNINSTALL_DESCRIPTION));
        view.setActionCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_BUTTON_UNINSTALL_CAPTION));
        view.setActionIcon(VaadinIcons.TRASH);
        break;
    }

    this.mode = mode;
  }

  public void onPerformAction(Runnable callback) {
    this.callback = callback;
  }

  public void show() {
    view.show();
  }

  public void hide() {
    view.hide();
  }

  public interface View {

    void hide();

    void show();

    void setPackages(Collection<Package> packages);

    void setActionCaption(String caption);

    void setActionIcon(VaadinIcons icon);

    void setPackageNameColumnCaption(String caption);

    void setPackageVersionColumnCaption(String caption);

    void setViewCaption(String caption);

    void setViewDescription(String description);

    void onPerfomAction(Runnable callback);
  }

}
