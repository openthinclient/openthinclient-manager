package org.openthinclient.web;

import com.vaadin.icons.VaadinIcons;
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

      // most openthinclient data is provided as sfs images. Register those here
      FileTypeResolver.addExtension("sfs", MIME_TYPE_SFS);

      // specify the default fallback icon.
      FileTypeResolver.DEFAULT_ICON = VaadinIcons.FILE_O;
   }

}
