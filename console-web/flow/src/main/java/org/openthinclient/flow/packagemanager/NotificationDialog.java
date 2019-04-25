package org.openthinclient.flow.packagemanager;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;


public class NotificationDialog {

    private final Dialog window;
    private final HorizontalLayout footer;
    private final Button closeButton;
    private final VerticalLayout content;

    public NotificationDialog(String caption, String description, NotificationDialogType type) {
      
        window = new Dialog();

//        window.setResizable(false);
//        window.setClosable(false);
//        window.setWidth(60, Sizeable.Unit.PERCENTAGE);
//        window.setHeight(null);
//        window.center();

        content = new VerticalLayout();
        content.setMargin(true);
        content.setSpacing(true);
        content.setWidth("100%");

        Label check = null;
        switch (type) {
          case SUCCESS:
//            check = new Label(FontAwesome.CHECK_CIRCLE.getHtml() + " Success", ContentMode.HTML);
            check = new Label("Success");
            check.setClassName("state-label-success-xl");
            break;
          case ERROR:
//            check = new Label(FontAwesome.TIMES_CIRCLE.getHtml() + " Failed", ContentMode.HTML);
            check = new Label("Failed");
            check.setClassName("state-label-error-xl");
            break;
          case PLAIN:
            break;
        }

        if (check != null) {
            content.add(check);
        }

        Label infoText = new Label(description);
        infoText.setClassName("v-label-notification-dialog-description");
        content.add(infoText);

        // footer
      // TODO add footer
        this.footer = new HorizontalLayout();
//        this.footer = new MHorizontalLayout().withFullWidth().withStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
//        closeButton = new MButton("Close").withStyleName(ValoTheme.BUTTON_PRIMARY).withListener((Button.ClickListener) event -> close());
        closeButton = new Button("Close");
        closeButton.addClickListener(event -> close());
//        this.footer.addComponent(closeButton);
//        footer.setComponentAlignment(closeButton, Alignment.MIDDLE_RIGHT);
//        content.addComponent(footer);

        window.add(content);
    }

    public void addContent(Component component) {
//        content.add(component, content.getComponentIndex(this.footer));
      content.add(component);
    }

    public void open(boolean modal) {
//        window.setModal(modal);
//        final UI ui = UI.getCurrent();
//        if (!ui.getWindows().contains(window)) {
//            ui.setPollInterval((int) TimeUnit.SECONDS.toMillis(1));
//            ui.addWindow(window);
//        }
    }

    public void close() {
        // disable polling
        UI.getCurrent().setPollInterval(-1);
        UI.getCurrent().remove(window);
    }

   
    public enum NotificationDialogType {
      SUCCESS,
      ERROR,
      PLAIN;
    }


    public void addCloseListener(ComponentEventListener listener) {
      closeButton.addClickListener(listener);
    }

}
