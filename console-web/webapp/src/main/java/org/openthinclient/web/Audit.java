package org.openthinclient.web;

import com.vaadin.ui.*;
import org.openthinclient.common.model.DirectoryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.*;

public class Audit {
  private static final Logger LOGGER = LoggerFactory.getLogger("Audit");

  private static void log(String op, String type, String... details) {
    if(details == null) {
      details = new String[]{};
    }
    String ip = UI.getCurrent().getPage().getWebBrowser().getAddress();
    LOGGER.info(Stream.concat(Stream.of(ip, op, type),
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
