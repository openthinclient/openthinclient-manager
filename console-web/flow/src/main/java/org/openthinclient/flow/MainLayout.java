package org.openthinclient.flow;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.openthinclient.flow.staticmenu.MainMenu;
import org.openthinclient.flow.staticmenu.MainMenuBar;

/**
 * Application main layout containing everything.
 *
 * @author Vaadin
 */
@StyleSheet("frontend://src/site.css")
@Theme(Lumo.class)
public class MainLayout extends Div implements RouterLayout {

    private MainMenuBar menu = new MainMenu();

    /**
     * Setup main layout.
     */
    public MainLayout() {

        setClassName("row");

        add(menu);

    }
}
