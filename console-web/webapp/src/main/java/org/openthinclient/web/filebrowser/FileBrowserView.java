package org.openthinclient.web.filebrowser;

import java.io.File;

import javax.annotation.PostConstruct;

import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import com.google.common.io.PatternFilenameFilter;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.FilesystemContainer;
import com.vaadin.data.util.TextFileProperty;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Responsive;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
@SpringView(name = "filebrowser")
@SideBarItem(sectionId = DashboardSections.COMMON, caption = "Filebrowser")
public final class FileBrowserView extends Panel implements View {

   private static final Logger LOGGER = LoggerFactory.getLogger(FileBrowserView.class);
   
   @Autowired
   private EventBus.SessionEventBus eventBus;
   @Autowired
   private ManagerHome managerHome;

   private final VerticalLayout root;

   public FileBrowserView() {
      
      addStyleName(ValoTheme.PANEL_BORDERLESS);
      setSizeFull();
      DashboardEventBus.register(this);

      root = new VerticalLayout();
      root.setSizeFull();
      root.setMargin(true);
      root.addStyleName("dashboard-view");
      setContent(root);
      Responsive.makeResponsive(root);

      root.addComponent(new ViewHeader("Filebrowser"));
      root.addComponent(buildSparklines());

   }
   
   @PostConstruct
   private void init() {
      Component content = buildContent();
      root.addComponent(content);
      root.setExpandRatio(content, 1);      
   }

   private Component buildSparklines() {
      CssLayout sparks = new CssLayout();
      sparks.addStyleName("sparks");
      sparks.setWidth("100%");
      Responsive.makeResponsive(sparks);

      return sparks;
  } 
   
   private Component buildContent() {

      LOGGER.debug("Listing files from ", managerHome.getLocation());
      
      // CollapsibleFileSystemContainer  docs = new CollapsibleFileSystemContainer(managerHome.getLocation() /*, new PatternFilenameFilter(".*\\.txt|.*\\.xml"), true */);
      FilesystemContainer docs = new FilesystemContainer(managerHome.getLocation() /*, new PatternFilenameFilter(".*\\.txt|.*\\.xml"), true */);
      TreeTable  docList = new TreeTable("Documents");
      docList.setContainerDataSource(docs);
      docList.setItemIconPropertyId("Icon");
      docList.setVisibleColumns(new Object[]{"Name", "Size", "Last Modified"});
      
      DocEditor docView = new DocEditor();

      HorizontalSplitPanel split = new HorizontalSplitPanel();
      split.addComponent(docList);
      split.addComponent(docView);
      docList.setSizeFull();
      docList.addValueChangeListener(new ValueChangeListener() {
         public void valueChange(ValueChangeEvent event) {
            File value = (File) event.getProperty().getValue();
            if (!value.isDirectory()) {
               docView.setPropertyDataSource(new TextFileProperty(value));
            } else {
               docView.setPropertyDataSource(null);
            }
         }
      });
      docList.setImmediate(true);
      docList.setSelectable(true);      
      
      return split;
  }

   @Override
   public void enter(ViewChangeEvent event) { }
 
}
