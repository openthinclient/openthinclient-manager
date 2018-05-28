package org.openthinclient.web.novnc;

import com.vaadin.annotations.JavaScript;
import com.vaadin.server.Resource;
import com.vaadin.ui.AbstractJavaScriptComponent;

@JavaScript({"novnc-component-connector.js"})
public class NoVNCComponent extends AbstractJavaScriptComponent {

  public void setNoVNCPageResource(Resource resource) {
    setResource("novnc", resource);
  }

  public Resource getNoVNCPageResource() {
    return getResource("novnc");
  }

}
