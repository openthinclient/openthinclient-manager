package org.openthinclient.web.component;

import java.util.concurrent.TimeUnit;

import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MHorizontalLayout;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;


public class NotificationDialog {

    private final Window window;
    private final HorizontalLayout footer;
    private final Button closeButton;

    public NotificationDialog(String caption, String description, NotificationDialogType type) {
      
        window = new Window(caption);

        window.setResizable(false);
        window.setClosable(false);
        window.setWidth(60, Sizeable.Unit.PERCENTAGE);
        window.setHeight(null);
        window.center();


        final VerticalLayout content = new VerticalLayout();
        content.setMargin(true);
        content.setSpacing(true);
        content.setWidth("100%");

        Label check = null;
        switch (type) {
          case SUCCESS:
            check = new Label(FontAwesome.CHECK_CIRCLE.getHtml() + " Success", ContentMode.HTML);
            check.setStyleName("state-label-success-xl");
            break;
          case ERROR:
            check = new Label(FontAwesome.TIMES_CIRCLE.getHtml() + " Failed", ContentMode.HTML);
            check.setStyleName("state-label-error-xl");
            break;
        }
        content.addComponent(check);

        content.addComponent(new Label(description));

        // footer
        this.footer = new MHorizontalLayout().withFullWidth().withStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
        closeButton = new MButton("Close").withStyleName(ValoTheme.BUTTON_PRIMARY).withListener(e -> close());
        this.footer.addComponent(closeButton);
        footer.setComponentAlignment(closeButton, Alignment.MIDDLE_RIGHT);
        content.addComponent(footer);

        window.setContent(content);
    }

    public void open(boolean modal) {
        window.setModal(modal);
        final UI ui = UI.getCurrent();
        if (!ui.getWindows().contains(window)) {
            ui.setPollInterval((int) TimeUnit.SECONDS.toMillis(1));
            ui.addWindow(window);
        }
    }

    public void close() {
        // disable polling
        UI.getCurrent().setPollInterval(-1);
        UI.getCurrent().removeWindow(window);
    }

   
    public enum NotificationDialogType {
      SUCCESS,
      ERROR;
    }


    public void addCloseListener(ClickListener listener) {
      closeButton.addClickListener(listener);
    }

}
