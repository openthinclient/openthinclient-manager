package org.openthinclient.web.pkgmngr.ui.view;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Grid;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.web.pkgmngr.ui.design.PackageActionOverviewDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageActionOverviewPresenter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class PackageActionOverviewView extends PackageActionOverviewDesign implements PackageActionOverviewPresenter.View {

  private final Grid.Column<ResolvedPackageItem, String> packageNameColumn;
  private final Grid.Column<ResolvedPackageItem, String> packageVersionColumn;
  private final ListDataProvider<ResolvedPackageItem> dataProvider;
  private Collection<Package> packages;
  private Runnable callback;

  public PackageActionOverviewView() {
    packageNameColumn = packageSelectionGrid.addColumn(AbstractPackageItem::getName);
    packageVersionColumn = packageSelectionGrid.addColumn(AbstractPackageItem::getDisplayVersion);
    dataProvider = new ListDataProvider<>(new ArrayList<>());
    packageSelectionGrid.setDataProvider(dataProvider);
    performActionButton.addClickListener((e) -> callback.run());
  }

  @Override
  public void hide() {
    setVisible(false);
  }

  @Override
  public void show() {
    setVisible(true);
  }

  @Override
  public void setPackages(Collection<Package> packages) {
    this.packages = packages;
    dataProvider.getItems().clear();
    if (packages != null) {
      dataProvider.getItems().addAll(packages.stream().map(ResolvedPackageItem::new).collect(Collectors.toList()));
    }
    dataProvider.refreshAll();
  }

  @Override
  public void setActionCaption(String caption) {
    performActionButton.setCaption(caption);
  }

  @Override
  public void setActionIcon(VaadinIcons icon) {
    performActionButton.setIcon(icon);
  }

  @Override
  public void setPackageNameColumnCaption(String caption) {
    packageNameColumn.setCaption(caption);
  }

  @Override
  public void setPackageVersionColumnCaption(String caption) {
    packageVersionColumn.setCaption(caption);
  }

  @Override
  public void setViewCaption(String caption) {
    labelCaption.setValue(caption);
  }

  @Override
  public void setViewDescription(String description) {
    descriptionLabel.setValue(description);
  }

  @Override
  public void onPerfomAction(Runnable callback) {
    this.callback = callback;
  }

}
