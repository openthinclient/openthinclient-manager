package org.openthinclient.flow.packagemanager;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RoutePrefix;
import org.openthinclient.flow.MainLayout;
import org.openthinclient.flow.packagemanager.presenter.SourcesListPresenter;
import org.openthinclient.pkgmgr.PackageManager;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "sources", layout = MainLayout.class)
@RoutePrefix(value = "ui")
@HtmlImport("frontend://src/SourcesListDesign.html")
@Tag("sources-list-design")
public class SourcesListNavigatorView extends PolymerTemplate<SourcesListPresenter.View> {

    /** serialVersionUID */
    private static final long serialVersionUID = 7614943414937772542L;
    private final IMessageConveyor mc;

    @Autowired
    PackageManager packageManager;

    private SourcesListPresenter presenter;

    public SourcesListNavigatorView() {

//        final SourcesListView sourcesListView = new SourcesListView();
        presenter = new SourcesListPresenter(getModel());
        mc = new MessageConveyor(UI.getCurrent().getLocale());
//
        presenter.setPackageManager(packageManager);
//
////        setSizeFull();
//        addClassName("sources");
//        add(sourcesListView);
    }

//    @Override
//    public void enter(ViewChangeListener.ViewChangeEvent event) {
//        presenter.setPackageManager(packageManager);
//    }

    @Override
    protected SourcesListPresenter.View getModel() {
        return super.getModel();
    }


}
