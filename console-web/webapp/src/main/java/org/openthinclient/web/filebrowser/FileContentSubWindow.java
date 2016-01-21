package org.openthinclient.web.filebrowser;

import java.io.File;

import com.vaadin.data.util.TextFileProperty;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;


public class FileContentSubWindow extends Window {

   /** serialVersionUID */
   private static final long serialVersionUID = 3887697184057926390L;

   public FileContentSubWindow(File doc) {
      
      super(doc.getName()); 
      setHeight("50%");
      setWidth("50%");
      center(); 
      
      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      
      TextArea text = new TextArea(new TextFileProperty(doc));
//      text.setHeight("100%");
      text.setWidth("100%");
      subContent.addComponent(text);
      
      setContent(subContent);
      
   }
}
