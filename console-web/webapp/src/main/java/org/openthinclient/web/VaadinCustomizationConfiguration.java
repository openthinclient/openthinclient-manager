package org.openthinclient.web;

import com.vaadin.server.FontAwesome;
import com.vaadin.util.FileTypeResolver;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * A specialized {@link Configuration} that will configure some vaadin defaults on startup.
 */
@Configuration
public class VaadinCustomizationConfiguration {

   private static final String MIME_TYPE_SFS = "application/x-otc-sfs-image";

   @PostConstruct
   public void configureVaadinFileTypeResolver() {

      FileTypeResolver.addIcon("inode/drive", FontAwesome.HDD_O);
      FileTypeResolver.addIcon("inode/directory", FontAwesome.FOLDER_O);


      // most openthinclient data is provided as sfs images. Register those here
      FileTypeResolver.addExtension("sfs", MIME_TYPE_SFS);
      FileTypeResolver.addIcon(MIME_TYPE_SFS, FontAwesome.HDD_O);

      // specify the default fallback icon.
      FileTypeResolver.DEFAULT_ICON = FontAwesome.FILE_O;

      // setting some commonly used mime types
      FileTypeResolver.addIcon("application/pdf", FontAwesome.FILE_PDF_O);
      FileTypeResolver.addIcon("application/x-debian-package", FontAwesome.ARCHIVE);
      FileTypeResolver.addIcon("application/x-java-archive", FontAwesome.FILE_ARCHIVE_O);
      FileTypeResolver.addIcon("application/x-tar", FontAwesome.FILE_ARCHIVE_O);
      FileTypeResolver.addIcon("image/bitmap", FontAwesome.FILE_IMAGE_O);
      FileTypeResolver.addIcon("image/gif", FontAwesome.FILE_IMAGE_O);
      FileTypeResolver.addIcon("image/jpeg", FontAwesome.FILE_IMAGE_O);
      FileTypeResolver.addIcon("image/png", FontAwesome.FILE_IMAGE_O);
      FileTypeResolver.addIcon("image/svg+xml", FontAwesome.FILE_IMAGE_O);
      FileTypeResolver.addIcon("text/html", FontAwesome.CODE);
      FileTypeResolver.addIcon("text/plain", FontAwesome.FILE_TEXT_O);
      FileTypeResolver.addIcon("text/xml", FontAwesome.CODE);



   }

}
