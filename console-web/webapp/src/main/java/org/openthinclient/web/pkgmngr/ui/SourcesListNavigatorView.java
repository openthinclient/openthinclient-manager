package org.openthinclient.web.pkgmngr.ui;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.openthinclient.web.pkgmngr.ui.presenter.SourcesListPresenter;
import org.openthinclient.web.pkgmngr.ui.view.SourcesListView;
import org.openthinclient.web.view.DashboardSections;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

@SpringView(name="sources")
@SideBarItem(sectionId = DashboardSections.PACKAGE_MANAGEMENT, caption = "Package Sources")
public class SourcesListNavigatorView extends Panel implements View {

  private final SourcesListPresenter presenter;

  @Autowired
  DPKGPackageManager packageManager;

  public SourcesListNavigatorView() {
    final VerticalLayout content = new VerticalLayout();
    final SourcesListView sourcesListView = new SourcesListView();

    presenter = new SourcesListPresenter(sourcesListView);

    content.addComponent(sourcesListView);
    content.setSizeFull();
    setContent(content);
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {

    presenter.setPackageManager(packageManager);

  }
}
