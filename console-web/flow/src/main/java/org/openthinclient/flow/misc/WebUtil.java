package org.openthinclient.flow.misc;

public class WebUtil {

   /**
    * Returns value of 'vaadin.servlet.urlMapping' without characters behind last '/' 
    *  <pre>
    * {@code
    * /xyz/*      => /xyz/
    * /xyz/abc    => /xyz/abc/
    * /xyz/abc/   => /xyz/abc/
    * /           => /
    * <leer>      => / 
    * }
    * </pre>
    * @param mapping the mapping-String
    * @return String value of property or defaults to '/'
    */
   public static String getServletMappingRoot(String mapping) {
      if (mapping.endsWith("/*")) {
         mapping = mapping.substring(0, mapping.length() - 1);
      } else if (!mapping.endsWith("/")) {
         return mapping + "/";
      }
      return mapping;

   }
}