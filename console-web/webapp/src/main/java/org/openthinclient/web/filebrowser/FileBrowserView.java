package org.openthinclient.web.filebrowser;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.data.provider.QuerySortOrder;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.*;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.renderers.DateRenderer;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.util.FileTypeResolver;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = "filebrowser")
@SideBarItem(sectionId = DashboardSections.COMMON, captionCode="UI_FILEBROWSER_HEADER", order = 99)
public final class FileBrowserView extends Panel implements View {

   private static final Logger LOGGER = LoggerFactory.getLogger(FileBrowserView.class);
   final IMessageConveyor mc;
   private final VerticalLayout root;
   @Autowired
   private EventBus.SessionEventBus eventBus;
   @Autowired
   private ManagerHome managerHome;
   private VerticalLayout content;
   private Button removeDirButton;
   private Path selectedFileItem;
   private Button contentButton;
   private Button createDirButton;
   private Button downloadButton;
   private Button uploadButton;
   private TreeGrid<File> docList;
   private Window subWindow;

   public FileBrowserView() {

      mc = new MessageConveyor(UI.getCurrent().getLocale());
      
      addStyleName(ValoTheme.PANEL_BORDERLESS);
      setSizeFull();
      DashboardEventBus.register(this);

      root = new VerticalLayout();
      root.setSizeFull();
      root.setMargin(true);
      root.addStyleName("dashboard-view");
      setContent(root);
      Responsive.makeResponsive(root);

      root.addComponent(new ViewHeader(mc.getMessage(UI_FILEBROWSER_HEADER)));

   }

   public static boolean isMimeTypeSupported(String mimeType) {
      switch (mimeType) {
         case "text/plain":
         case "text/xml":
         case "text/html":
         case "image/png":
         case "image/gif":
         case "image/jpg":
         case "image/jpeg":
            return true;
      }
      return false;
   }

   @Override
   public String getCaption() {
      return mc.getMessage(UI_FILEBROWSER_HEADER);
   }

   @PostConstruct
   private void init() {
      Component content = buildContent();
      root.addComponent(content);
      root.setExpandRatio(content, 1);
   }

   private Component buildContent() {

      LOGGER.debug("Managing files from ", managerHome.getLocation());
      this.selectedFileItem = managerHome.getLocation().toPath();

      content = new VerticalLayout();
      content.setSpacing(true);

      HorizontalLayout controlBar = new HorizontalLayout();
      controlBar.setSpacing(true);

      this.contentButton = new Button(mc.getMessage(UI_FILEBROWSER_BUTTON_VIEWCONTENT), event -> {
         showSubwindow(new ContentViewSubWindow(this, selectedFileItem));
      });
      this.contentButton.setEnabled(false);
      this.contentButton.setIcon(FontAwesome.EYE);
      this.contentButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      controlBar.addComponent(this.contentButton);

      // Create directory
      this.createDirButton = new Button(mc.getMessage(UI_FILEBROWSER_BUTTON_MKDIR), event -> {
         showSubwindow(new CreateDirectorySubWindow(this, selectedFileItem, managerHome.getLocation().toPath()));
      });
      this.createDirButton.setIcon(FontAwesome.FOLDER_O);
      this.createDirButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      controlBar.addComponent(this.createDirButton);

      this.removeDirButton = new Button(mc.getMessage(UI_FILEBROWSER_BUTTON_RMDIR), event -> {
         showSubwindow(new RemoveItemSubWindow(this, selectedFileItem));
      });
      this.removeDirButton.setIcon(FontAwesome.TIMES);
      this.removeDirButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      this.removeDirButton.setEnabled(false);
      controlBar.addComponent(removeDirButton);

      // Upload/Download group
      CssLayout groupUploadDownload = new CssLayout();
      groupUploadDownload.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
      this.downloadButton = new Button(mc.getMessage(UI_FILEBROWSER_BUTTON_DOWNLOAD));
      this.downloadButton.setIcon(FontAwesome.DOWNLOAD);
      this.downloadButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      this.downloadButton.setEnabled(false);
      groupUploadDownload.addComponent(this.downloadButton);

      uploadButton = new Button(mc.getMessage(UI_FILEBROWSER_BUTTON_UPLOAD), event -> {
         showSubwindow(new FileUploadSubWindow(this, selectedFileItem, managerHome.getLocation().toPath()));
      });
      uploadButton.setIcon(FontAwesome.UPLOAD);
      uploadButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      groupUploadDownload.addComponent(uploadButton);
      controlBar.addComponent(groupUploadDownload);

      content.addComponent(controlBar);
      createTreeGrid();
      content.addComponent(docList);

      return content;
   }

   private void showSubwindow(Window windowToShow) {
      UI.getCurrent().removeWindow(subWindow);
      UI.getCurrent().addWindow(subWindow = windowToShow);
   }

   private void createTreeGrid() {

      docList = new TreeGrid<>();
      docList.setDataProvider(new FileSystemDataProvider(managerHome.getLocation()));
      docList.setSizeFull();
      docList.addItemClickListener(event -> {
            onSelectedFileItemChanged(event.getItem());
      });

      docList.addColumn(file -> {
         String iconHtml;
         if (file.isDirectory()) {
            iconHtml = VaadinIcons.FOLDER_O.getHtml();
         } else {
            iconHtml = VaadinIcons.FILE_O.getHtml();
         }
         return iconHtml + " " + Jsoup.clean(file.getName(), Whitelist.simpleText());
      }, new HtmlRenderer()).setCaption(mc.getMessage(UI_FILEBROWSER_COLUMN_NAME)).setId("file-name");

      docList.addColumn(
              file -> file.isDirectory() ? "--" : file.length() + " bytes")
              .setCaption(mc.getMessage(UI_FILEBROWSER_COLUMN_SIZE)).setId("file-size");

      docList.addColumn(file -> new Date(file.lastModified()),
              new DateRenderer()).setCaption(mc.getMessage(UI_FILEBROWSER_COLUMN_MODIFIED))
              .setId("file-last-modified");

      docList.setHierarchyColumn("file-name");

   }

   public void refresh(Path expand) {
      selectedFileItem = null;
      content.removeComponent(docList);
      createTreeGrid();
      content.addComponent(docList);
      if (expand != null) {
         docList.select(expand.toFile());
         docList.expand(expand.getParent().toFile());
      }
   }

   private void onSelectedFileItemChanged(File value) {
       if (value != null) {
           selectedFileItem = value.toPath();
       } else {
           selectedFileItem = null;
       }

      contentButton.setEnabled(selectedFileItem != null && isMimeTypeSupported(FileTypeResolver.getMIMEType(selectedFileItem.toFile())));
      removeDirButton.setEnabled(selectedFileItem != null);
      downloadButton.setEnabled(selectedFileItem != null);

      // Remove FileDownload-extensions on button-object
      new ArrayList<Extension>(downloadButton.getExtensions()).forEach(ex -> downloadButton.removeExtension(ex));
      if (selectedFileItem != null && Files.isRegularFile(selectedFileItem)) {
         FileDownloader fileDownloader = new FileDownloader(new FileResource(selectedFileItem.toFile()));
         fileDownloader.setOverrideContentType(false);
         fileDownloader.extend(downloadButton);
      }

   }
      
   @Override
   public void enter(ViewChangeEvent event) {

   }

   class FileSystemDataProvider extends AbstractBackEndHierarchicalDataProvider<File, FilenameFilter> {

      private  final Comparator<File> nameComparator = (fileA, fileB) -> {
         return String.CASE_INSENSITIVE_ORDER.compare(fileA.getName(), fileB.getName());
      };

      private  final Comparator<File> sizeComparator = (fileA, fileB) -> {
         return Long.compare(fileA.length(), fileB.length());
      };

      private  final Comparator<File> lastModifiedComparator = (fileA, fileB) -> {
         return Long.compare(fileA.lastModified(), fileB.lastModified());
      };

      private final File root;

      public FileSystemDataProvider(File root) {
         this.root = root;
      }

      @Override
      public int getChildCount(
              HierarchicalQuery<File, FilenameFilter> query) {
         return (int) fetchChildren(query).count();
      }

      @Override
      protected Stream<File> fetchChildrenFromBackEnd(
              HierarchicalQuery<File, FilenameFilter> query) {
         final File parent = query.getParentOptional().orElse(root);
         Stream<File> filteredFiles = query.getFilter()
                 .map(filter -> Stream.of(parent.listFiles(filter)))
                 .orElse(Stream.of(parent.listFiles()))
                 .skip(query.getOffset()).limit(query.getLimit());
         return sortFileStream(filteredFiles, query.getSortOrders());
      }

      @Override
      public boolean hasChildren(File item) {
         return item.list() != null && item.list().length > 0;
      }

      private Stream<File> sortFileStream(Stream<File> fileStream,
                                          List<QuerySortOrder> sortOrders) {

         if (sortOrders.isEmpty()) {
            return fileStream;
         }

         List<Comparator<File>> comparators = sortOrders.stream()
                 .map(sortOrder -> {
                    Comparator<File> comparator = null;
                    if (sortOrder.getSorted().equals("file-name")) {
                       comparator = nameComparator;
                    } else if (sortOrder.getSorted().equals("file-size")) {
                       comparator = sizeComparator;
                    } else if (sortOrder.getSorted().equals("file-last-modified")) {
                       comparator = lastModifiedComparator;
                    }
                    if (comparator != null && sortOrder
                            .getDirection() == SortDirection.DESCENDING) {
                       comparator = comparator.reversed();
                    }
                    return comparator;
                 }).filter(Objects::nonNull).collect(Collectors.toList());

         if (comparators.isEmpty()) {
            return fileStream;
         }

         Comparator<File> first = comparators.remove(0);
         Comparator<File> combinedComparators = comparators.stream()
                 .reduce(first, Comparator::thenComparing);
         return fileStream.sorted(combinedComparators);
      }
   }
}
