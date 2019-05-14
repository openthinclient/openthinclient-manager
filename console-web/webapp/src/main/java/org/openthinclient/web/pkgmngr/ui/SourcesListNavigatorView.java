package org.openthinclient.web.pkgmngr.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.pkgmngr.ui.presenter.SourcesListPresenter;
import org.openthinclient.web.pkgmngr.ui.view.SourcesListView;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SOURCESLISTNAVIGATORVIEW_CAPTION;

@SpringView(name = "sources")
@SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode = "UI_SOURCESLISTNAVIGATORVIEW_CAPTION", order = 2)
public class SourcesListNavigatorView extends Panel implements View {

    /** serialVersionUID */
    private static final long serialVersionUID = 7614943414937772542L;
    private final IMessageConveyor mc;

    @Autowired
    PackageManager packageManager;

    private SourcesListPresenter presenter;

    @Autowired
    public SourcesListNavigatorView(final EventBus.SessionEventBus eventBus,
                                    final DashboardNotificationService notificationService) {

        final SourcesListView sourcesListView = new SourcesListView();
        presenter = new SourcesListPresenter(sourcesListView);
        mc = new MessageConveyor(UI.getCurrent().getLocale());

        setSizeFull();
        addStyleName("sources");
        eventBus.publish(this, new DashboardEvent.UpdateHeaderLabelEvent(mc.getMessage(UI_SOURCESLISTNAVIGATORVIEW_CAPTION)));
        setContent(sourcesListView);
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        presenter.setPackageManager(packageManager);
    }
}
