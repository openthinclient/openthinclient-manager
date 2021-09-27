package org.openthinclient.web.support;

import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.openthinclient.common.model.ClientMetaData;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import javax.annotation.PostConstruct;


@SpringView(name = "logfiles")
@SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode = "UI_SUPPORT_LOGS_HEADER", order = 80)
public class LogFilesView extends Panel implements View {

  private static final Logger LOG = LoggerFactory.getLogger(LogFilesView.class);

  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private ClientService clientService;

  private final MessageConveyor mc;
  private final CssLayout root;
  private Path logDir;

  public LogFilesView() {
    mc = new MessageConveyor(UI.getCurrent().getLocale());
    setSizeFull();

    root = new CssLayout();
    root.setStyleName("logfilesview");
    setContent(root);
  }

  @Override
  public String getCaption() {
     return mc.getMessage(ConsoleWebMessages.UI_SUPPORT_LOGS_HEADER);
  }

  @PostConstruct
  private void init() {
    logDir = managerHome.getLocation().toPath().resolve("logs");

    root.addComponents(
      new Label(
        mc.getMessage(ConsoleWebMessages.UI_SUPPORT_LOGS_DESCRIPTION,
                      logDir.toString()),
        ContentMode.HTML
      ),
      buildLogBox(
        ConsoleWebMessages.UI_SUPPORT_LOGS_SERVER_HEADER,
        ConsoleWebMessages.UI_SUPPORT_LOGS_SERVER_DESCRIPTION,
        buildFileListing("openthinclient-manager.")
      ),
      buildLogBox(
        ConsoleWebMessages.UI_SUPPORT_LOGS_AUDIT_HEADER,
        ConsoleWebMessages.UI_SUPPORT_LOGS_AUDIT_DESCRIPTION,
        buildFileListing("audit.")
      ),
      buildLogBox(
        ConsoleWebMessages.UI_SUPPORT_LOGS_CLIENT_HEADER,
        ConsoleWebMessages.UI_SUPPORT_LOGS_CLIENT_DESCRIPTION,
        buildClientSelectorAndFileListing()
      )
    );
  }

  private Component buildLogBox(ConsoleWebMessages header, ConsoleWebMessages description, Component component) {
    CssLayout content = new CssLayout();
    Component title = new Label(mc.getMessage(header));
    title.addStyleName("title");

    content.addComponents(
      title,
      new Label(mc.getMessage(description)),
      component
    );

    return content;
  }

  private Component buildClientSelectorAndFileListing() {
    CssLayout content = new CssLayout();
    content.addStyleName("client-logs");
    Label unselectedLabel = new Label(mc.getMessage(
        ConsoleWebMessages.UI_SUPPORT_LOGS_NO_CLIENT_SELECTED));

    ComboBox<ClientMetaData> clientCombo = new ComboBox<>();
    clientCombo.setItemCaptionGenerator(client ->
      String.format("%s (%s / %s)", client.getName(),
                                    client.getMacAddress(),
                                    client.getIpHostNumber()
      )
    );
    clientCombo.setItems(clientService.findAllClientMetaData()
                          .stream()
                          .sorted(Comparator.comparing(
                            ClientMetaData::getName,
                            String::compareToIgnoreCase
                          ))
    );
    clientCombo.setPlaceholder(mc.getMessage(
        ConsoleWebMessages.UI_SUPPORT_LOGS_CLIENT_SELECT_PLACEHOLDER));
    clientCombo.setSizeUndefined();

    clientCombo.addValueChangeListener(event -> {
      Component component;
      ClientMetaData client = event.getValue();
      if(client == null) {
        component = unselectedLabel;
      } else {
        String glob = String.format("%s.", client.getMacAddress());
        component = buildFileListing("syslog", glob);
      }
      content.removeComponent(content.getComponent(content.getComponentCount() - 1));
      content.addComponent(component);
    });

    content.addComponents(
      clientCombo,
      unselectedLabel
    );

    return content;
  }

  private Component buildFileListing(String... location) {
    String[] pathParts = Arrays.copyOf(location, location.length-1);
    String glob = location[location.length-1];

    Layout content = new CssLayout();

    Path dir = managerHome.getLocation().toPath().resolve("logs");
    StringBuilder urlBuilder = new StringBuilder("/openthinclient/logs/");
    for(String pathPart : pathParts) {
      dir = dir.resolve(pathPart);
      urlBuilder.append(pathPart).append("/");
    }

    File[] files = dir.toFile().listFiles((FilenameFilter)new PrefixFileFilter(glob));

    if(files == null) {
      LOG.error("Error while listing log files.");
      Component result = new Label(mc.getMessage(ConsoleWebMessages.UI_SUPPORT_LOGS_ACCESS_ERROR));
      result.addStyleName("error");
      content.addComponent(result);
      return content;
    }

    if(files.length == 0) {
      content.addComponent(new Label(mc.getMessage(ConsoleWebMessages.UI_SUPPORT_LOGS_NO_FILES_FOUND)));
      return content;
    }

    Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
    for(File file : files) {
      String fileName = file.getName();
      String url = new StringBuilder(urlBuilder).append(fileName).toString();
      Component link = new Label(
        String.format("<a href=\"%s\" download>%s</a>", url, fileName),
        ContentMode.HTML);
      content.addComponent(link);

      String size = FileUtils.byteCountToDisplaySize(file.length());
      content.addComponent(new Label(size));
    }
    content.addStyleName("file-listing");

    return content;
  }

}
