package org.openthinclient.web.filebrowser;

import com.vaadin.data.util.TextFileProperty;
import com.vaadin.ui.RichTextArea;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class DocEditor extends VerticalLayout {

   private TextArea html;
   private RichTextArea wysiwyg;

   public DocEditor() {
      buildMainLayout();
   }

   public void setPropertyDataSource(TextFileProperty doc) {
      html.setPropertyDataSource(doc);
      wysiwyg.setPropertyDataSource(doc);
   }

   private void buildMainLayout() {
      // common part: create layout
      setImmediate(false);
      setHeight("100%");
      // top-level component properties
      setWidth("100.0%");
      setHeight("100.0%");
      // tabSheet_1
      TabSheet tabSheet_1 = buildTabSheet_1();
      addComponent(tabSheet_1);
   }

   private TabSheet buildTabSheet_1() {
      // common part: create layout
      TabSheet tabSheet_1 = new TabSheet();
      tabSheet_1.setImmediate(true);
      tabSheet_1.setWidth("100.0%");
      tabSheet_1.setHeight("100.0%");
      // wysiwyg
      wysiwyg = new RichTextArea();
      wysiwyg.setImmediate(false);
      wysiwyg.setWidth("100.0%");
      wysiwyg.setHeight("100.0%");
      tabSheet_1.addTab(wysiwyg, "WYSIWYG", null);
      // html
      html = new TextArea();
      html.setImmediate(false);
      html.setWidth("100.0%");
      html.setHeight("100.0%");
      tabSheet_1.addTab(html, "HTML", null);
      return tabSheet_1;
   }

}
