package org.openthinclient.web.pkgmngr.ui;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SOURCESLISTNAVIGATORVIEW_CAPTION;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.pkgmngr.ui.presenter.SourcesListPresenter;
import org.openthinclient.web.pkgmngr.ui.view.SourcesListView;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.openthinclient.web.ui.OtcView;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Responsive;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

@SpringView(name = "sources")
@SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode = "UI_SOURCESLISTNAVIGATORVIEW_CAPTION", order = 2)
public class SourcesListNavigatorView extends OtcView  {

    /** serialVersionUID */
    private static final long serialVersionUID = 7614943414937772542L;

    @Autowired
    PackageManager packageManager;

    private SourcesListPresenter presenter;

    @Autowired
    public SourcesListNavigatorView(final EventBus.SessionEventBus eventBus,
                                    final DashboardNotificationService notificationService) {
        super(UI_SOURCESLISTNAVIGATORVIEW_CAPTION, eventBus, notificationService);

        final SourcesListView sourcesListView = new SourcesListView();
        presenter = new SourcesListPresenter(sourcesListView);
        addStyleName("sources");
        setPanelContent(sourcesListView);
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        presenter.setPackageManager(packageManager);
    }
}
