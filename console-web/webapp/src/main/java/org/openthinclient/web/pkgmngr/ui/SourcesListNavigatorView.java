package org.openthinclient.web.pkgmngr.ui;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SOURCESLISTNAVIGATORVIEW_CAPTION;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.web.pkgmngr.ui.presenter.SourcesListPresenter;
import org.openthinclient.web.pkgmngr.ui.view.SourcesListView;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
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
@SideBarItem(sectionId = DashboardSections.PACKAGE_MANAGEMENT, captionCode = "UI_SOURCESLISTNAVIGATORVIEW_CAPTION")
public class SourcesListNavigatorView extends Panel implements View {

    /** serialVersionUID */
    private static final long serialVersionUID = 7614943414937772542L;

    private  SourcesListPresenter presenter;

    @Autowired
    PackageManager packageManager;
    @Autowired
    EventBus.SessionEventBus eventBus;

    public SourcesListNavigatorView() {
       
       final IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
       
       addStyleName(ValoTheme.PANEL_BORDERLESS);
       setSizeFull();

       VerticalLayout root = new VerticalLayout();
       root.setSizeFull();
       root.setMargin(true);
       root.addStyleName("dashboard-view");
       setContent(root);
       Responsive.makeResponsive(root);

       root.addComponent(new ViewHeader(mc.getMessage(UI_SOURCESLISTNAVIGATORVIEW_CAPTION)));

       // Content
       final SourcesListView sourcesListView = new SourcesListView();
       presenter = new SourcesListPresenter(sourcesListView);
       root.addComponent(sourcesListView);
       root.setExpandRatio(sourcesListView, 1);
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        presenter.setPackageManager(packageManager);
    }

    @Override
    public void attach() {
        super.attach();
//        eventBus.subscribe(presenter);
    }

    @Override
    public void detach() {
//        eventBus.unsubscribe(presenter);
        super.detach();
    }
}
