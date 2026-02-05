package org.openthinclient.web;

import com.vaadin.server.VaadinSession;
import com.vaadin.ui.*;
import org.openthinclient.common.model.DirectoryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.*;

public class Audit {
  private static final Logger LOGGER = LoggerFactory.getLogger("Audit");

  private static void log(String op, String type, String... details) {
    String name="<unknown>";
    try {
      SecurityContext securityContext = (SecurityContext) VaadinSession
          .getCurrent().getSession()
          .getAttribute("SPRING_SECURITY_CONTEXT");
      UserDetails userDetails = (UserDetails) securityContext
          .getAuthentication().getPrincipal();
      name = userDetails.getUsername();
    } catch (Exception ex) {
      LOGGER.warn("Failed to get user name for audit log", ex);
    }
    if(details == null) {
      details = new String[]{};
    }
    String ip = UI.getCurrent().getPage().getWebBrowser().getAddress();
    LOGGER.info(Stream.concat(Stream.of(ip, name, op, type),
                              Arrays.stream(details))
                      .map(str -> Objects.isNull(str)? "": str)
                      .collect(Collectors.joining("\t")));
  }

  private static void log(String op, DirectoryObject directoryObject) {
    log(op,
        directoryObject.getClass().getSimpleName(),
        directoryObject.getName(),
        directoryObject.getDn());
  }

  public static void logSave(String type, String... details) {
    log("SAVE", type, details);
  }

  public static void logSave(DirectoryObject directoryObject) {
    log("SAVE", directoryObject);
  }

  public static void logDelete(String type, String... details) {
    log("DELETE", type, details);
  }

  public static void logDelete(DirectoryObject directoryObject) {
    log("DELETE", directoryObject);
  }

  public static void logDeleteUninstall(DirectoryObject directoryObject) {
    log("DELETE (UNINSTALL)", directoryObject);
  }
}
