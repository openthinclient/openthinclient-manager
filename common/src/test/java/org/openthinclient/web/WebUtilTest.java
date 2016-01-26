package org.openthinclient.web;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WebUtilTest {

   @Test
   public void testGetServletMappingRoot() throws Exception {

      assertEquals("/xyz/", WebUtil.getServletMappingRoot("/xyz/*"));
      assertEquals("/xyz/abc/", WebUtil.getServletMappingRoot("/xyz/abc"));
      assertEquals("/xyz/abc/", WebUtil.getServletMappingRoot("/xyz/abc/"));
      assertEquals("/", WebUtil.getServletMappingRoot("/"));
      assertEquals("/", WebUtil.getServletMappingRoot(""));

   }
}
