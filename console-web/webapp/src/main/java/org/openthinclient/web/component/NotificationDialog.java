package org.openthinclient.web.component;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.ContentMode;

import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.ValoTheme;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MHorizontalLayout;

import java.util.concurrent.TimeUnit;


public class NotificationDialog {

    private final Window window;
    private final HorizontalLayout footer;
    private final Button closeButton;
    private final VerticalLayout content;

    public NotificationDialog(String caption, String description, NotificationDialogType type) {
      
        window = new Window(caption);

        window.setResizable(false);
        window.setClosable(false);
        window.setWidth(60, Sizeable.Unit.PERCENTAGE);
        window.setHeight(null);
        window.center();

        content = new VerticalLayout();
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
          case PLAIN:
            break;
        }

        if (check != null) {
            content.addComponent(check);
        }

        Label infoText = new Label(description, ContentMode.HTML);
        infoText.setStyleName("v-label-notification-dialog-description");
        content.addComponent(infoText);

        // footer
        this.footer = new MHorizontalLayout().withFullWidth().withStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
        closeButton = new MButton("Close").withStyleName(ValoTheme.BUTTON_PRIMARY).withListener((Button.ClickListener) event -> close());
        this.footer.addComponent(closeButton);
        footer.setComponentAlignment(closeButton, Alignment.MIDDLE_RIGHT);
        content.addComponent(footer);

        window.setContent(content);
    }

    public void addContent(Component component) {
        content.addComponent(component, content.getComponentIndex(this.footer));
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
      ERROR,
      PLAIN;
    }


    public void addCloseListener(ClickListener listener) {
      closeButton.addClickListener(listener);
    }

}
