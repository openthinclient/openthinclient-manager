package org.openthinclient.flow.filebrowser;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RoutePrefix;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.openthinclient.flow.MainLayout;
import org.openthinclient.flow.i18n.ConsoleWebMessages;
import org.openthinclient.flow.staticmenu.BaseViewLayout;
import org.openthinclient.flow.v8.FileTypeResolver;
import org.openthinclient.flow.v8.VaadinIcons;
import org.openthinclient.meta.Bookmark;
import org.openthinclient.meta.PackageMetadataManager;
import org.openthinclient.meta.PackageMetadataUtil;
import org.openthinclient.service.common.home.ManagerHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openthinclient.flow.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
//@SpringView(name = "filebrowser")
//@SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode="UI_FILEBROWSER_HEADER", order = 4)
//@ThemeIcon("icon/files-white.svg")
@Route(value = FileBrowserView.VIEW_NAME, layout = MainLayout.class)
@RoutePrefix("ui")
public final class FileBrowserView extends BaseViewLayout {

   private static final Logger LOGGER = LoggerFactory.getLogger(FileBrowserView.class);
   public static final String ICON_PREFIX_VAADIN = "vaadin:";
   public static final String VIEW_NAME = "filebrowser";

    ManagerHome managerHome;
    PackageMetadataManager metadataManager;

   private final IMessageConveyor mc;
   private VerticalLayout content;
   private Button removeDirButton;
   private Path selectedFileItem;
   private Button contentButton;
   private Button createDirButton;
   private Button downloadButton;
   private Button uploadButton;
   private TreeGrid<File> docList;
   private Dialog subWindow;
   private FileSystemDataProvider dataProvider;
   private ComboBox<Bookmark> bookmarkComboBox;

   private List<File> visibleItems = new ArrayList<>();

   public FileBrowserView(@Autowired ManagerHome managerHome, @Autowired PackageMetadataManager metadataManager) {

     mc = new MessageConveyor(UI.getCurrent().getLocale());
     setSizeFull();
     this.managerHome = managerHome;
     this.metadataManager = metadataManager;

     add(buildContent());
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

   private Component buildContent() {

      LOGGER.debug("Managing files from ", managerHome.getLocation());
      this.selectedFileItem = managerHome.getLocation().toPath();

      content = new VerticalLayout();
      content.setSpacing(true);
//      content.setMargin(new MarginInfo(true, true, true,true));
//      content.setSizeFull();

      HorizontalLayout controlBar = new HorizontalLayout();
      controlBar.setSpacing(true);

      this.contentButton = new Button(mc.getMessage(UI_FILEBROWSER_BUTTON_VIEWCONTENT), event -> {
         showSubwindow(new ContentViewSubWindow(selectedFileItem));
      });
      this.contentButton.setEnabled(false);
//      this.contentButton.setIcon(FontAwesome.EYE);
//      this.contentButton.addClassName(ButtonVariant.LUMO_ICON);
      controlBar.add(this.contentButton);

      // Create directory
      this.createDirButton = new Button(mc.getMessage(UI_FILEBROWSER_BUTTON_MKDIR), event -> {
         showSubwindow(new CreateDirectorySubWindow(this, selectedFileItem, managerHome.getLocation().toPath()));
      });
//      this.createDirButton.setIcon(FontAwesome.FOLDER_O);
//      this.createDirButton.addClassName(ValoTheme.BUTTON_ICON_ONLY);
      controlBar.add(this.createDirButton);

      this.removeDirButton = new Button(mc.getMessage(UI_FILEBROWSER_BUTTON_RMDIR), event -> {
         showSubwindow(new RemoveItemSubWindow(this, selectedFileItem));
      });
//      this.removeDirButton.setIcon(FontAwesome.TIMES);
//      this.removeDirButton.addClassName(ValoTheme.BUTTON_ICON_ONLY);
      this.removeDirButton.setEnabled(false);
      controlBar.add(removeDirButton);

      // Upload/Download group
      Div groupUploadDownload = new Div();
//      groupUploadDownload.addClassName(ValoTheme.LAYOUT_COMPONENT_GROUP);
      this.downloadButton = new Button(mc.getMessage(UI_FILEBROWSER_BUTTON_DOWNLOAD));
//      this.downloadButton.setIcon(FontAwesome.DOWNLOAD);
//      this.downloadButton.addClassName(ValoTheme.BUTTON_ICON_ONLY);
      this.downloadButton.setEnabled(false);
      groupUploadDownload.add(this.downloadButton);

      uploadButton = new Button(mc.getMessage(UI_FILEBROWSER_BUTTON_UPLOAD), event -> {
         showSubwindow(new FileUploadSubWindow(this, selectedFileItem, managerHome.getLocation().toPath()));
      });
//      uploadButton.setIcon(FontAwesome.UPLOAD);
//      uploadButton.addClassName(ValoTheme.BUTTON_ICON_ONLY);
      groupUploadDownload.add(uploadButton);
      controlBar.add(groupUploadDownload);

      bookmarkComboBox = new ComboBox<>();
//      bookmarkComboBox.setTextInputAllowed(false);
      bookmarkComboBox.setPlaceholder(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_BOOKMARKS));
//      bookmarkComboBox.setEmptySelectionAllowed(true);
//      bookmarkComboBox.setItemIconGenerator(FileBrowserView::resolveIcon);
      bookmarkComboBox.setItemLabelGenerator(this::resolveBookmarkLabel);
      bookmarkComboBox.setItems(metadataManager.getBookmarks());
//      bookmarkComboBox.setWidth(100, Unit.PERCENTAGE);

      bookmarkComboBox.addValueChangeListener(this::navigatoToBookmark);
      controlBar.add(bookmarkComboBox);
//      controlBar.setExpandRatio(bookmarkComboBox, 1);

//      controlBar.setWidth(100, Unit.PERCENTAGE);
      content.add(controlBar);
      createTreeGrid();
      content.add(docList);
//      content.setExpandRatio(docList, 1);

      return content;
   }

   private void navigatoToBookmark(HasValue.ValueChangeEvent<Bookmark> e) {
      if (e.isFromClient()) { // if user selects a value form ComboBox explicitly
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

   // TODO Bookmark icon
//   static Resource resolveIcon(Bookmark bookmark) {
//
//      if (!Strings.isNullOrEmpty(bookmark.getIcon())) {
//         if (bookmark.getIcon().startsWith(ICON_PREFIX_VAADIN)) {
//            final String icon = bookmark.getIcon().substring(ICON_PREFIX_VAADIN.length());
//
//            // convert to typical enum constant name
//            try {
//               return VaadinIcons.valueOf(icon.toUpperCase().replace('-', '_'));
//            } catch (IllegalArgumentException e) {
//               LOGGER.info("Non existing icon requested: " + icon, e);
//            }
//
//         }
//
//      }
//
//      return null;
//   }

   private void showSubwindow(Dialog windowToShow) {
     if (subWindow != null) {
       UI.getCurrent().remove(subWindow);
     }
      UI.getCurrent().add(subWindow = windowToShow);
     subWindow.open();
   }

   private void createTreeGrid() {

      docList = new TreeGrid<>(File.class);
//      {
//        @Override
//        public void scrollTo(int row, ScrollDestination destination) {
//          Objects.requireNonNull(destination, "ScrollDestination can not be null");
//          getRpcProxy(GridClientRpc.class).scrollToRow(row, destination);
//        }
//      };

//     docList.removeAllColumns();
     docList.setHierarchyColumn("name");
     docList.removeColumnByKey("hidden");
     docList.removeColumnByKey("parent");
     docList.removeColumnByKey("parentFile");
     docList.removeColumnByKey("freeSpace");
     docList.removeColumnByKey("usableSpace");
     docList.removeColumnByKey("totalSpace");
     docList.removeColumnByKey("directory");
     docList.removeColumnByKey("canonicalFile");
     docList.removeColumnByKey("path");
     docList.removeColumnByKey("file");
     docList.removeColumnByKey("absolute");
     docList.removeColumnByKey("absoluteFile");
     docList.removeColumnByKey("canonicalPath");
     docList.removeColumnByKey("absolutePath");


//     docList.addColumn(file -> file.getName()).setHeader("Name").setId("name");
//     docList.addColumn(file -> {
//       String iconHtml;
//       if (file.isDirectory()) {
//         iconHtml = VaadinIcons.FOLDER_O.getHtml();
//       } else {
//         iconHtml = VaadinIcons.FILE_O.getHtml();
//       }
//       return iconHtml + " " + Jsoup.clean(file.getName(), Whitelist.simpleText());
//     }).setHeader("Name").setId("name");

     docList.addColumn(file -> file.isDirectory() ? "--" : file.length() + " bytes")
         .setHeader(mc.getMessage(UI_FILEBROWSER_COLUMN_SIZE)).setId("file-size");

     docList.addColumn(file -> new Date(file.lastModified()).toString())
         .setHeader(mc.getMessage(UI_FILEBROWSER_COLUMN_MODIFIED))
         .setId("file-last-modified");

      docList.addItemClickListener(event -> onSelectedFileItemChanged(event.getItem()));
      docList.addCollapseListener(event -> visibleItems.removeAll(getCollapsedChilds(event.getItems().iterator().next())));
      docList.addExpandListener(event -> {
        File item = event.getItems().iterator().next();
        List<File> childrenExpanded = getExpandedChilds(item);
        visibleItems.addAll(visibleItems.indexOf(item) + 1, childrenExpanded);
      });

     dataProvider = new FileSystemDataProvider(managerHome.getLocation());
     visibleItems = dataProvider.fetchChildrenFromBackEnd(new HierarchicalQuery<>(null, managerHome.getLocation()))
         .collect(Collectors.toList());
     docList.setDataProvider(dataProvider);

   }

  /**
   * Find all collapsable files and folders
   * @param item the root-file to start search for childs
   * @return a list of collapsed 'hidden' files
   */
  private List<File> getCollapsedChilds(File item) {
    List<File> collapsedChilds = new ArrayList<>();
    dataProvider.fetch(new HierarchicalQuery<>(null, item)).forEach(child -> {
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
    dataProvider.fetch(new HierarchicalQuery<>(null, item)).forEach(child -> {
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
//        int indexOf = visibleItems.indexOf(expand.toFile());
//        docList.scrollTo(indexOf, ScrollDestination.START);

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
//      new ArrayList<>(downloadButton.getExtensions()).forEach(ex -> downloadButton.removeExtension(ex));
//      if (selectedFileItem != null && Files.isRegularFile(selectedFileItem)) {
//         FileDownloader fileDownloader = new FileDownloader(new FileResource(selectedFileItem.toFile()));
//         fileDownloader.setOverrideContentType(false);
//         fileDownloader.extend(downloadButton);
//      }

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


   class FileSystemDataProvider extends AbstractBackEndHierarchicalDataProvider<File, FilenameFilter> {

      private  final Comparator<File> nameComparator = (fileA, fileB) -> String.CASE_INSENSITIVE_ORDER.compare(fileA.getName(), fileB.getName());

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
                    if (sortOrder.getSorted().equals("name")) {
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
