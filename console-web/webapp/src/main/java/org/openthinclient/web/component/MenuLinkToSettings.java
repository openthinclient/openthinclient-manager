package org.openthinclient.web.component;

import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.UI;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.openthinclient.web.ui.SettingsUI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import java.io.Serializable;

/**
 * Menu-link to Settings view
 */
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT, caption = "Settings")
@ThemeIcon("icon/info.svg")
@Component
@UIScope
public class MenuLinkToSettings implements Runnable, Serializable {

    @Autowired
    SettingsUI settingsUI;

    @Override
    public void run() {
        UI.getCurrent().getPage().setLocation("settings");
    }
}