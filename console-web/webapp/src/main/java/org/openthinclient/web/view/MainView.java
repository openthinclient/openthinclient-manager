package org.openthinclient.web.view;

import com.vaadin.navigator.Navigator;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.UI;
import org.openthinclient.web.view.dashboard.DashboardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.components.ValoSideBar;

/*
 * Dashboard MainView is a simple HorizontalLayout that wraps the menu on the
 * left and creates a simple container for the navigator on the right.
 */
@UIScope
@SpringComponent
public class MainView extends HorizontalLayout {

    @Autowired
    public MainView(SpringViewProvider viewProvider, ValoSideBar sideBar) {
        setSizeFull();
        addStyleName("mainview");

        sideBar.setId("dashboard-menu");
        sideBar.setHeader(buildHeader());

        addComponent(sideBar);

        ComponentContainer content = new CssLayout();
        content.addStyleName("view-content");
        content.setSizeFull();
        addComponent(content);
        setExpandRatio(content, 1.0f);

        final Navigator navigator = new Navigator(UI.getCurrent(), content);
        navigator.addProvider(viewProvider);
        if (navigator.getState().isEmpty()) {
            navigator.navigateTo(DashboardView.NAME);
        } else {
            navigator.navigateTo(navigator.getState());
        }

    }

    private Layout buildHeader() {
        Label logo = new Label("openthinclient.org <strong>Manager</strong>", ContentMode.HTML);
        logo.setSizeUndefined();
        HorizontalLayout logoWrapper = new HorizontalLayout(logo);
        logoWrapper.setComponentAlignment(logo, Alignment.MIDDLE_CENTER);
        return logoWrapper;
    }
}
