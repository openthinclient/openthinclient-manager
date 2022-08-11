package org.openthinclient.service.store;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.openthinclient.common.model.schema.EntryNode;
import org.openthinclient.common.model.schema.Node;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/**
 * Reads (some) schema files and re-reads them if they change.
 *
 * The only exposed method is the static SchemaStore.getClientBootDefaults
 */
public enum SchemaStore {
  INSTANCE();

  /**
   * Get the default values for client boot data (i.e. default values
   * from the schemas client, hardwaretype, location and realm) merged
   * in a simple map.
   */
  public static Map<String, String> getClientBootDefaults() {
    return INSTANCE.clientBootDefaults;
  }


  private final Logger LOG = LoggerFactory.getLogger(SchemaStore.class);

  private final String[] BOOT_DATA_SCHEMAS = new String[]{
      "client", "hardwaretype", "location", "realm"};
  private final Set<Path> BOOT_DATA_FILES =
      Stream.of(BOOT_DATA_SCHEMAS)
      .map(s -> s + ".xml").map(Paths::get).collect(Collectors.toSet());


  private Path schemaPath;
  private JAXBContext jaxbContext;
  private WatchService watchService;
  private Map<String, Schema> schemas = new ConcurrentHashMap<>();
  private Map<String, String> clientBootDefaults;

  SchemaStore() {
    schemaPath = Paths.get(
        (new ManagerHomeFactory()).getManagerHomeDirectory().getAbsolutePath(),
        "nfs", "root", "schema");
    try {
      jaxbContext = JAXBContext.newInstance(Schema.class);
      initSchemas();
      startFileChangeWatcher();
    } catch (IOException | JAXBException ex) {
      throw new RuntimeException("Failed to initialize", ex);
    }
  }

  private static boolean isXMLFile(Path path) {
    return Files.isRegularFile(path)
        && path.getFileName().toString().endsWith(".xml");
  }

  private void initSchemas() throws IOException {
    Files.list(schemaPath).forEach(this::updateSchema);
    updateClientBootDefaults();
  }

  private void updateSchema(Path schemaPath) {
    String filename = schemaPath.getFileName().toString();
    if (!filename.endsWith(".xml")) {
      return;
    }
    String schemaName = filename.substring(0, filename.length()-4);

    if (Files.isRegularFile(schemaPath)) {
      try {
        schemas.put(schemaName, loadSchema(schemaPath));
      } catch (IOException | JAXBException ex) {
        LOG.error("Could not load schema {}", schemaName, ex);
      }
    } else {
      LOG.info("Removing schema {}", schemaName);
      schemas.remove(schemaName);
    };
  }

  private Schema loadSchema(Path file) throws IOException, JAXBException {
    try (InputStream in = Files.newInputStream(file)) {
      // read using an input source, so that the XML parser will use the
      // encoding specification from the file.
      InputSource source = new InputSource(in);
      return (Schema) jaxbContext.createUnmarshaller().unmarshal(source);
    }
  }

  private void startFileChangeWatcher() throws IOException {
    watchService = FileSystems.getDefault().newWatchService();
    schemaPath.register(watchService,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE,
        StandardWatchEventKinds.ENTRY_MODIFY);

    new Thread(() -> {
      WatchKey watchKey;
      try {
        while ((watchKey = watchService.take()) != null) {
          Set<Path> affectedSchemaPaths = new HashSet<>();
          boolean affectsBootDefaults = false;
          // gather affectedSchemaPaths that changed within 200ms of each other
          while (watchKey != null) {
            Path dirPath = (Path)watchKey.watchable();
            for (WatchEvent<?> event: watchKey.pollEvents()) {
              Path path = (Path) event.context();
              Path abspath = dirPath.resolve(path);
              if (isXMLFile(abspath)) {
                affectedSchemaPaths.add(abspath);
                affectsBootDefaults |= BOOT_DATA_FILES.contains(path);
              }
            }
            watchKey.reset();
            watchKey = watchService.poll(200, TimeUnit.MILLISECONDS);
          }

          if (affectedSchemaPaths.size() == 0) {
            continue;
          }

          affectedSchemaPaths.forEach(this::updateSchema);

          if (affectsBootDefaults) {
            updateClientBootDefaults();
          }
        }
      } catch (InterruptedException ex) {
      }
    }).start();
  }

  private void updateClientBootDefaults() {
    Map<String, String> map = new HashMap<>();
    for (String schemaName: BOOT_DATA_SCHEMAS) {
      if(schemas.containsKey(schemaName)) {
        updateMap(map, schemas.get(schemaName));
      } else {
        LOG.warn("No schema for {}", schemaName);
      }
    }
    clientBootDefaults = Collections.unmodifiableMap(map);
  }

  private static void updateMap(Map<String, String> map, Node node, String... names) {
    if (node.getParent() != null)  {  // skip schema name
      names = Arrays.copyOf(names, names.length + 1);
      names[names.length - 1] = node.getName();
    }
    if (node instanceof EntryNode) {
      // use computeIfAbsent (instead of putIfAbsent) to skip possible null
      // values (from node.getValue)
      map.computeIfAbsent(String.join(".", names),
                          k -> ((EntryNode)node).getValue());
    } else {
      for (Node child: node.getChildren()) {
        updateMap(map, child, names);
      }
    }
  }
}
