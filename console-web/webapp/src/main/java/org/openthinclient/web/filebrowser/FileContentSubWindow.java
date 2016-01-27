package org.openthinclient.web.filebrowser;

import com.vaadin.data.util.TextFileProperty;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import java.io.File;

public class FileContentSubWindow extends Window {

   /**
    * serialVersionUID
    */
   private static final long serialVersionUID = 3887697184057926390L;

   public static boolean isMimeTypeSupported(String mimeType) {
      switch (mimeType) {
      case "text/plain":
      case "text/xml":
      case "text/html":
         return true;
      }
      return false;
   }

   public FileContentSubWindow(File doc) {

      super("View file " + doc.getName());
      setHeight("50%");
      setWidth("50%");
      center();

      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setHeight("100%");

      TextArea text = new TextArea(new TextFileProperty(doc));
      text.setHeight("100%");
      text.setWidth("100%");
      subContent.addComponent(text);

      setContent(subContent);

   }

}
