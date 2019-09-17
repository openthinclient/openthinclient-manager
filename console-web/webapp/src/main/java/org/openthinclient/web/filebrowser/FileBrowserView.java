package org.openthinclient.web.filebrowser;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.google.common.base.Strings;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.data.provider.QuerySortOrder;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FileResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.grid.GridClientRpc;
import com.vaadin.shared.ui.grid.ScrollDestination;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
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
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.openthinclient.web.ui.ManagerUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = "filebrowser", ui= ManagerUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT, captionCode="UI_FILEBROWSER_HEADER", order = 90)
@ThemeIcon("icon/filebrowser.svg")
public final class FileBrowserView extends Panel implements View, FileUploadView {

   private static final Logger LOGGER = LoggerFactory.getLogger(FileBrowserView.class);
   public static final String ICON_PREFIX_VAADIN = "vaadin:";
   @Autowired
   private ManagerHome managerHome;
   @Autowired
   private PackageMetadataManager metadataManager;
   @Autowired
   private EventBus.SessionEventBus eventBus;

   private final IMessageConveyor mc;
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
   private ComboBox<Bookmark> bookmarkComboBox;

   private List<File> visibleItems = new ArrayList<>();

   public FileBrowserView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     mc = new MessageConveyor(UI.getCurrent().getLocale());
     setSizeFull();
     eventBus.publish(this, new DashboardEvent.UpdateHeaderLabelEvent(mc.getMessage(UI_FILEBROWSER_HEADER)));
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

   @PostConstruct
   private void init() {
     setContent(buildContent());
   }

   private Component buildContent() {

      LOGGER.debug("Managing files from ", managerHome.getLocation());
      this.selectedFileItem = managerHome.getLocation().toPath();

      content = new VerticalLayout();
      content.setSpacing(true);
      content.setMargin(new MarginInfo(true, true, true,true));
      content.setSizeFull();

      HorizontalLayout controlBar = new HorizontalLayout();
      controlBar.setSpacing(true);

      this.contentButton = new Button(mc.getMessage(UI_FILEBROWSER_BUTTON_VIEWCONTENT), event -> {
         showSubwindow(new ContentViewSubWindow(selectedFileItem));
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

      bookmarkComboBox = new ComboBox<>();
      bookmarkComboBox.setTextInputAllowed(false);
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
      if (e.isUserOriginated()) { // if user selects a value form ComboBox explicitly
         final Bookmark bookmark = e.getValue();
         final Path targetPath;
         if (bookmark != null) {
            final Path path = Paths.get(bookmark.getPath());
            targetPath = managerHome.getLocation().toPath().resolve(path);
         } else {
            targetPath = managerHome.getLocation().toPath();
         }
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

      docList = new TreeGrid() {
        @Override
        public void scrollTo(int row, ScrollDestination destination) {
          Objects.requireNonNull(destination, "ScrollDestination can not be null");
          getRpcProxy(GridClientRpc.class).scrollToRow(row, destination);
        }
      };
      dataProvider = new FileSystemDataProvider(managerHome.getLocation());
      visibleItems = dataProvider.fetchChildrenFromBackEnd(new HierarchicalQuery<>(null, managerHome.getLocation()))
                                 .collect(Collectors.toList());
      docList.setDataProvider(dataProvider);
      docList.setSizeFull();

      docList.addItemClickListener(event -> onSelectedFileItemChanged(event.getItem()));
      docList.addCollapseListener(event -> visibleItems.removeAll(getCollapsedChilds(event.getCollapsedItem())));
      docList.addExpandListener(event -> {
        File item = event.getExpandedItem();
        List<File> childrenExpanded = getExpandedChilds(item);
        visibleItems.addAll(visibleItems.indexOf(item) + 1, childrenExpanded);
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

  /**
   * Find all collapsable files and folders
   * @param item the root-file to start search for childs
   * @return a list of collapsed 'hidden' files
   */
  private List<File> getCollapsedChilds(File item) {
    List<File> collapsedChilds = new ArrayList<>();
    dataProvider.fetchChildrenFromBackEnd(new HierarchicalQuery<>(null, item)).forEach(child -> {
      collapsedChilds.add(child);
      if (docList.isExpanded(child)) {
        collapsedChilds.addAll(getCollapsedChilds(child));
      }
    });
    return collapsedChilds;
  }

  /**
   * Find all expandible files and folders
   * @param item the root-file to start search for childs
   * @return a list of expanded 'visible' files
   */
  private List<File> getExpandedChilds(File item) {
    List<File> expandedChilds = new ArrayList<>();
    dataProvider.fetchChildrenFromBackEnd(new HierarchicalQuery<>(null, item)).forEach(child -> {
        expandedChilds.add(child);
        if (docList.isExpanded(child)) {
          expandedChilds.addAll(expandedChilds.indexOf(child) + 1, getExpandedChilds(child));
        }
    });
    return expandedChilds;
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
          File file = directory.toFile();
          if (!docList.isExpanded(file)) {
            pathsToExpand.add(file);
          };
          directory = directory.getParent();
        }
        Collections.reverse(pathsToExpand);
        docList.expand(pathsToExpand);
        int indexOf = visibleItems.indexOf(expand.toFile());
        docList.scrollTo(indexOf, ScrollDestination.START);

        if (expand.equals(managerHome.toPath())) {
            selectedFileItem = null;
            docList.deselectAll();
            // Collapse anything if managerHome is selected
           try {
              Files.newDirectoryStream(managerHome.toPath(), path -> path.toFile().isDirectory())
                   .forEach(path -> docList.collapse(path.toFile()));
           } catch (IOException e) {
              LOGGER.error("Error occurred while resolving directories in managerHome: " + e.getMessage());
           }
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
      new ArrayList<>(downloadButton.getExtensions()).forEach(ex -> downloadButton.removeExtension(ex));
      if (selectedFileItem != null && Files.isRegularFile(selectedFileItem)) {
         FileDownloader fileDownloader = new FileDownloader(new FileResource(selectedFileItem.toFile()));
         fileDownloader.setOverrideContentType(false);
         fileDownloader.extend(downloadButton);
      }

      // Reset Bookmark-ComboBox: select a ComboBox-Itmem if expanded path matches, or set ComboBox to null
      Optional<Bookmark> bookmarkExists = metadataManager.getBookmarks()
          .filter(bookmark -> selectedFileItem.toAbsolutePath().equals(managerHome.getLocation().toPath().resolve(Paths.get(bookmark.getPath()))))
          .findFirst();
      if (bookmarkExists.isPresent()) {
         bookmarkComboBox.setValue(bookmarkExists.get());
      } else {
         bookmarkComboBox.setValue(null);
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

  @Override
  public void uploadSucceed(Path file) {
    refresh(file);
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
      public int getChildCount(HierarchicalQuery<File, FilenameFilter> query) {
         return (int) fetchChildren(query).count();
      }

      @Override
      protected Stream<File> fetchChildrenFromBackEnd(HierarchicalQuery<File, FilenameFilter> query) {
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

  @Override
  public void attach() {
    super.attach();
    eventBus.subscribe(this);
  }

  @Override
  public void detach() {
    eventBus.unsubscribe(this);
    super.detach();
  }
}
