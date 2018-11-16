package org.openthinclient.web.dashboard;

import com.vaadin.server.Resource;
import com.vaadin.ui.*;

public class ContentPanel extends VerticalLayout {

    private static final String PANEL_STYLE_NAME = "dashboard-panel";
    private static final String TITLE_STYLE_NAME = "dashboard-panel-title";
    private static final String IMAGE_STYLE_NAME = "dashboard-panel-image";

    private final AbstractOrderedLayout title;
    private Image titleImage;
    private Label titleLabel;

    public ContentPanel() {
        setSizeUndefined();
        addStyleName(PANEL_STYLE_NAME);
        title = new HorizontalLayout();
        title.setSpacing(false);
        addComponent(title);
    }

    public void setTitle(String text) {
        if(titleLabel == null) {
            titleLabel = new Label();
            titleLabel.addStyleName(TITLE_STYLE_NAME);
            title.addComponent(titleLabel);
        }
        titleLabel.setValue(text);
    }

    public void setImage(Resource resource) {
        if(titleImage == null) {
            titleImage = new Image();
            titleImage.addStyleName(IMAGE_STYLE_NAME);
            title.addComponentAsFirst(titleImage);
        }
        titleImage.setSource(resource);
    }

    public ContentPanel(String title) {
        this();
        setTitle(title);
    }

    public ContentPanel(String title, Resource resource) {
        this(title);
        setImage(resource);
    }
}
