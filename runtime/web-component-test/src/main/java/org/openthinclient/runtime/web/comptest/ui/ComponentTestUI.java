package org.openthinclient.runtime.web.comptest.ui;

import com.vaadin.annotations.Theme;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import org.openthinclient.runtime.web.comptest.ui.tests.ComponentTest;
import org.openthinclient.runtime.web.comptest.ui.tests.InstallationPlanSummaryDialogTest;
import org.openthinclient.runtime.web.comptest.ui.tests.ProgressDialogTest;

import java.util.function.Supplier;

@SpringUI
@Theme("openthinclient")
public class ComponentTestUI extends UI {

    private MainView mainView;
    private Panel contentArea;
    private Label titleLabel;
    private Button backButton;

    @Override
    protected void init(VaadinRequest request) {

        mainView = new MainView(this, //
                new ProgressDialogTest(), //
                new InstallationPlanSummaryDialogTest() //
        );
        titleLabel = new Label();
        titleLabel.addStyleName(ValoTheme.LABEL_H1);

        backButton = new Button("Back");
        backButton.setIcon(FontAwesome.CHEVRON_LEFT);
        backButton.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
        backButton.addStyleName(ValoTheme.BUTTON_HUGE);
        backButton.addClickListener(e -> showMainView());

        final VerticalLayout content = new VerticalLayout();

        final HorizontalLayout top = new HorizontalLayout();
        top.setSpacing(true);
        top.addComponent(titleLabel);
        top.addComponent(backButton);
        contentArea = new Panel();
        contentArea.addStyleName(ValoTheme.PANEL_BORDERLESS);
        content.setMargin(true);

        content.addComponent(top);
        content.addComponent(contentArea);

        setContent(content);

        showMainView();
    }

    public void show(String title, Supplier<Component> supplier) {
        if (supplier == null) {
            showMainView();
        } else {
            titleLabel.setValue(title);
            contentArea.setContent(supplier.get());
            backButton.setVisible(true);
        }
    }

    public void showMainView() {
        titleLabel.setValue("Component Tests");
        contentArea.setContent(mainView);
        backButton.setVisible(false);
    }

    public static class MainView extends HorizontalLayout {

        public MainView(ComponentTestUI ui, ComponentTest... componentTests) {
            setSpacing(true);
            addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);

            for (ComponentTest test : componentTests) {
                addComponent(new ComponentTestTile(ui, test.getTitle(), test.getDetails(), test));
            }
        }
    }
}
