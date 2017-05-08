//package org.openthinclient.web.pkgmngr.ui.design;
//
//import com.vaadin.icons.VaadinIcons;
//import com.vaadin.ui.*;
//import org.openthinclient.pkgmgr.db.Source;
//
//
//public class SourcesListDesignLayout extends VerticalLayout {
//	protected Grid<Source> sourcesTable;
//	protected Button updateButton;
//	protected Button addSourceButton;
//	protected Button deleteSourceButton;
//	protected TextField urlText;
//	protected CheckBox enabledCheckbox;
//	protected TextArea descriptionTextArea;
//	protected Button saveButton;
//	protected Button updateButtonTop;
//	protected Label sourceDetailsLabel;
//	protected Label sourcesLabel;
//	protected HorizontalLayout sourcesLayout;
//	protected VerticalLayout sourceDetailsLayout;
//	protected FormLayout formLayout;
//
//	public SourcesListDesignLayout() {
//		HorizontalSplitPanel hsp = new HorizontalSplitPanel();
//		addComponent(hsp);
//
//		// left pane
//		VerticalLayout vl = new VerticalLayout();
//		vl.setSpacing(true);
//		vl.setSizeFull();
//		hsp.addComponent(vl);
//
//		sourcesLayout = new HorizontalLayout();
//		vl.addComponent(sourcesLayout);
//		sourcesLabel = new Label("Package Sources");
//		sourcesLayout.addComponent(sourcesLabel);
//
//		updateButtonTop = new Button("Update");
////        updateButtonTop.setStyleName("borderless large borderless-colored");
//		updateButtonTop.setIcon(VaadinIcons.ARROW_CIRCLE_DOWN);
//		sourcesLayout.addComponent(updateButtonTop);
//
//		Panel panel = new Panel();
//		vl.addComponent(panel);
//		sourcesTable = new Grid<>();
//		panel.setContent(sourcesTable);
//
//		HorizontalLayout buttons = new HorizontalLayout();
//		vl.addComponent(buttons);
//		CssLayout cssLayoutLeft = new CssLayout();
//		buttons.addComponent(cssLayoutLeft);
//		updateButton = new Button("Update"); // fonticon://FontAwesome/f021
//		updateButton.setIcon(VaadinIcons.ARROW_CIRCLE_DOWN);
//		cssLayoutLeft.addComponent(updateButton);
//
//		CssLayout cssLayoutRight = new CssLayout();
//		cssLayoutRight.setStyleName("v-component-group right");
//		buttons.addComponent(cssLayoutRight);
//		addSourceButton = new Button("Add");
//		addSourceButton.setIcon(VaadinIcons.PLUS);
//		cssLayoutRight.addComponent(addSourceButton);
//
//		deleteSourceButton = new Button("Delete");
//		deleteSourceButton.setIcon(VaadinIcons.DEL);
//		cssLayoutRight.addComponent(deleteSourceButton);
//
//
//		// right pane
//		sourceDetailsLayout = new VerticalLayout();
//		sourceDetailsLayout.setSpacing(true);
//		sourceDetailsLayout.setSizeFull();
//		hsp.addComponent(sourceDetailsLayout);
//
//		sourceDetailsLabel = new Label("Source");
//		sourceDetailsLayout.addComponent(sourceDetailsLabel);
//
//		formLayout = new FormLayout();
//		sourceDetailsLayout.addComponent(formLayout);
//		urlText = new TextField("URL");
//		enabledCheckbox = new CheckBox("Enable this source");
//		descriptionTextArea = new TextArea("Description");
//		formLayout.addComponents(urlText, enabledCheckbox, descriptionTextArea);
//
//		saveButton = new Button("Save");
//		saveButton.setIcon(VaadinIcons.SAFE);
//		sourceDetailsLayout.addComponent(saveButton);
//
//	}
//}
