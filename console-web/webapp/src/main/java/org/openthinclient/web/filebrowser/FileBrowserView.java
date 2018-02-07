package org.openthinclient.web.filebrowser;

import com.google.common.base.Strings;

import com.vaadin.data.HasValue;
import com.vaadin.data.provider.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.data.provider.QuerySortOrder;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Extension;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FileResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.server.Responsive;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.DateRenderer;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.util.FileTypeResolver;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.openthinclient.meta.Bookmark;
import org.openthinclient.meta.PackageMetadataManager;
import org.openthinclient.meta.PackageMetadataUtil;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_BUTTON_DOWNLOAD;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_BUTTON_MKDIR;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_BUTTON_RMDIR;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_BUTTON_UPLOAD;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_BUTTON_VIEWCONTENT;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_COLUMN_MODIFIED;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_COLUMN_NAME;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_COLUMN_SIZE;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_HEADER;

@SuppressWarnings("serial")
@SpringView(name = "filebrowser")
@SideBarItem(sectionId = DashboardSections.COMMON, captionCode="UI_FILEBROWSER_HEADER", order = 99)
public final class FileBrowserView extends Panel implements View {

   private static final Logger LOGGER = LoggerFactory.getLogger(FileBrowserView.class);
   public static final String ICON_PREFIX_VAADIN = "vaadin:";
   @Autowired
   private ManagerHome managerHome;
   @Autowired
   private PackageMetadataManager metadataManager;

   private final IMessageConveyor mc;
   private final VerticalLayout root;
   private VerticalLayout content;
   private Button removeDirButton;
   private Path selectedFileItem;
   private Button contentButton;
   private Button createDirButton;
   private Button downloadButton;
   private Button uploadButton;
   private TreeGrid<File> docList;
   private Window subWindow;
   private FileSystemDataProvider dataProvider;

   public FileBrowserView() {

      mc = new MessageConveyor(UI.getCurrent().getLocale());
      
      addStyleName(ValoTheme.PANEL_BORDERLESS);
      setSizeFull();
      DashboardEventBus.register(this);

      root = new VerticalLayout();
      root.setSizeFull();
      root.setMargin(false);
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
      content.setMargin(new MarginInfo(false, true, false,false));
      content.setSizeFull();

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

      final ComboBox<Bookmark> bookmarkComboBox = new ComboBox<>();
      bookmarkComboBox.setPlaceholder(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_BOOKMARKS));
      bookmarkComboBox.setEmptySelectionAllowed(true);
      bookmarkComboBox.setItemIconGenerator(FileBrowserView::resolveIcon);
      bookmarkComboBox.setItemCaptionGenerator(this::resolveBookmarkLabel);
      bookmarkComboBox.setItems(metadataManager.getBookmarks());
      bookmarkComboBox.setWidth(100, Unit.PERCENTAGE);

      bookmarkComboBox.addValueChangeListener(this::navigatoToBookmark);
      controlBar.addComponent(bookmarkComboBox);
      controlBar.setExpandRatio(bookmarkComboBox, 1);

      controlBar.setWidth(100, Unit.PERCENTAGE);
      content.addComponent(controlBar);
      createTreeGrid();
      content.addComponent(docList);
      content.setExpandRatio(docList, 1);

      return content;
   }

   private void navigatoToBookmark(HasValue.ValueChangeEvent<Bookmark> e) {

      final Bookmark bookmark = e.getValue();
      if (bookmark != null) {

         final Path path = Paths.get(bookmark.getPath());

         final Path targetPath = managerHome.getLocation().toPath().resolve(path);

         refresh(targetPath);

      }
   }

   private String resolveBookmarkLabel(Bookmark bookmark) {

      Locale locale = getLocale();
      if (locale == null) {
         final UI ui = UI.getCurrent();
         if (ui != null)
            locale = ui.getLocale();
      }

      return PackageMetadataUtil.resolveLabel(locale, bookmark);
   }

   static Resource resolveIcon(Bookmark bookmark) {

      if (!Strings.isNullOrEmpty(bookmark.getIcon())) {
         if (bookmark.getIcon().startsWith(ICON_PREFIX_VAADIN)) {
            final String icon = bookmark.getIcon().substring(ICON_PREFIX_VAADIN.length());

            // convert to typical enum constant name
            try {
               return VaadinIcons.valueOf(icon.toUpperCase().replace('-', '_'));
            } catch (IllegalArgumentException e) {
               LOGGER.info("Non existing icon requested: " + icon, e);
            }

         }

      }

      return null;
   }

   private void showSubwindow(Window windowToShow) {
      UI.getCurrent().removeWindow(subWindow);
      UI.getCurrent().addWindow(subWindow = windowToShow);
   }

   private void createTreeGrid() {

      docList = new TreeGrid<>();
      dataProvider = new FileSystemDataProvider(managerHome.getLocation());
      docList.setDataProvider(dataProvider);
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
      selectedFileItem = expand;
      dataProvider.refreshAll();
      if (expand != null) {
        File managerHome = this.managerHome.getLocation();

        // expand all directory nodes in path
        Path directory = Files.isDirectory(expand) ? expand : expand.getParent();
        List<File> pathsToExpand = new ArrayList<>();
        while (!directory.equals(managerHome.toPath())) {
          pathsToExpand.add(directory.toFile());
          directory = directory.getParent();
        }
        docList.collapse();
        docList.expand(pathsToExpand);

        if (expand.equals(managerHome.toPath())) {
            selectedFileItem = null;
            docList.deselectAll();
        } else {
            docList.select(expand.toFile());
        }
        enableOrDisableButtons();
      }
   }

   private void onSelectedFileItemChanged(File value) {
       if (value != null) {
           selectedFileItem = value.toPath();
       } else {
           selectedFileItem = null;
       }

     enableOrDisableButtons();

      // Remove FileDownload-extensions on button-object
      new ArrayList<Extension>(downloadButton.getExtensions()).forEach(ex -> downloadButton.removeExtension(ex));
      if (selectedFileItem != null && Files.isRegularFile(selectedFileItem)) {
         FileDownloader fileDownloader = new FileDownloader(new FileResource(selectedFileItem.toFile()));
         fileDownloader.setOverrideContentType(false);
         fileDownloader.extend(downloadButton);
      }

   }

  private void enableOrDisableButtons() {
    contentButton.setEnabled(selectedFileItem != null && isMimeTypeSupported(FileTypeResolver.getMIMEType(selectedFileItem.toFile())));
    removeDirButton.setEnabled(selectedFileItem != null);
    downloadButton.setEnabled(selectedFileItem != null);
  }

  @Override
   public void enter(ViewChangeEvent event) {

   }

   class FileSystemDataProvider extends AbstractBackEndHierarchicalDataProvider<File, FilenameFilter> {

      private  final Comparator<File> nameComparator = (fileA, fileB) -> {
         return String.CASE_INSENSITIVE_ORDER.compare(fileA.getName(), fileB.getName());
      };

      private  final Comparator<File> sizeComparator = Comparator.comparingLong(File::length);

      private  final Comparator<File> lastModifiedComparator = Comparator.comparingLong(File::lastModified);

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
