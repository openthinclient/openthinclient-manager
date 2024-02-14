package org.openthinclient.service.store;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

  public static Collection<String> getClientKeys() {
    return INSTANCE.clientKeys;
  }

  public static Map<String, String> getSchema(String schemaName) {
    return INSTANCE.schemas.get(schemaName);
  }


  private final Logger LOG = LoggerFactory.getLogger(SchemaStore.class);

  private final String[] BOOT_DATA_SCHEMAS = new String[]{
      "client", "hardwaretype", "location", "realm"};
  private Set<Path> BOOT_DATA_FILES;


  private Path schemaPath;
  private JAXBContext jaxbContext;
  private Map<String, Map<String, String>> schemas = new ConcurrentHashMap<>();
  private Map<String, String> clientBootDefaults;
  private Set<String> clientKeys;

  SchemaStore() {
    schemaPath = Paths.get(
        (new ManagerHomeFactory()).getManagerHomeDirectory().getAbsolutePath(),
        "nfs", "root", "schema");
    BOOT_DATA_FILES = Stream.of(BOOT_DATA_SCHEMAS)
                      .map(s -> s + ".xml")
                      .map(schemaPath::resolve)
                      .collect(Collectors.toSet());
    try {
      jaxbContext = JAXBContext.newInstance(Schema.class);
      initSchemas(schemaPath);
      updateClientBootDefaults();
      startFileChangeWatcher();
    } catch (IOException | JAXBException ex) {
      throw new RuntimeException("Failed to initialize", ex);
    }
  }

  private static boolean isXML(Path path) {
    return path.getFileName().toString().toLowerCase().endsWith(".xml");
  }

  private void initSchemas(Path dir) throws IOException {
    LOG.info("Initializing schemas in {}", dir);
    try (Stream<Path> paths = Files.list(dir)) {
      paths.forEach(path -> {
        if (Files.isRegularFile(path) && isXML(path)) {
          updateSchema(path);
        } else if (Files.isDirectory(path)) {
          try {
            initSchemas(path);
          } catch (IOException ex) {
            LOG.error("Failed to initialize schemas in {}", path, ex);
          }
        }
      });
    }
  }


  private void updateSchema(Path path) {
    LOG.info("Updating schema file {}", path);
    if (!isXML(path)) {
      return;
    }
    // Normalize path (relative to schema path)
    Path relPath = schemaPath.relativize(path);
    String normPath = StreamSupport.stream(relPath.spliterator(), false)
                      .map(Path::toString)
                      .map(String::toLowerCase)
                      .collect(Collectors.joining("/"));
    // schema name is the relative path without the .xml extension
    String schemaName = normPath.substring(0, normPath.length()-4);

    if (Files.isRegularFile(path)) {
      LOG.info("Loading schema {}", schemaName);
      try {
        Map<String, String> map = new HashMap<>();
        updateMap(map, loadSchema(path), null);
        schemas.put(schemaName, map);
      } catch (Exception ex) {
        LOG.error("Could not load schema from {}", path, ex);
        return;
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
    WatchService watchService = FileSystems.getDefault().newWatchService();
    schemaPath.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);

    new Thread(() -> {
      WatchKey watchKey;
      try {
        while ((watchKey = watchService.take()) != null) {
          Set<Path> affectedSchemaPaths = new HashSet<>();
          Set<Path> affectedSubPaths = new HashSet<>();
          boolean affectsBootDefaults = false;
          // gather affected paths that changed within 200ms of each other
          while (watchKey != null) {
            Path dirPath = (Path)watchKey.watchable();
            for (WatchEvent<?> event: watchKey.pollEvents()) {
              Path path = dirPath.resolve((Path) event.context());
              if (isXML(path)) {
                affectedSchemaPaths.add(path);
                affectsBootDefaults |= BOOT_DATA_FILES.contains(path);
              } else if(Files.isDirectory(path)) {
                affectedSubPaths.add(path);
              }
            }
            watchKey.reset();
            watchKey = watchService.poll(200, TimeUnit.MILLISECONDS);
          }

          // update schemas
          if (affectedSchemaPaths.size() > 0) {
            affectedSchemaPaths.forEach(this::updateSchema);
            if (affectsBootDefaults) {
              updateClientBootDefaults();
            }
          }

          // register new subdirs with this watcher and add schemas
          for (Path path: affectedSubPaths) {
            try {
              path.register(watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY);
            } catch (IOException ex) {
              LOG.error("Failed to register schema watcher for {}", path, ex);
            }
            try {
              initSchemas(path);
            } catch (IOException ex) {
              LOG.error("Failed to initialize schemas in {}", path, ex);
            }
          }
        }
      } catch (InterruptedException ex) {
        return;
      } catch(Exception ex) {
        LOG.error("Error in schema watcher. Ignoring", ex);
      }
    }).start();
  }

  private void updateClientBootDefaults() {
    Set<String> keys = new HashSet<>();
    LOG.info("Updating client boot defaults");
    for (String schemaName: BOOT_DATA_SCHEMAS) {
      if(schemas.containsKey(schemaName)) {
        keys.addAll(schemas.get(schemaName).keySet());
      } else {
        LOG.warn("No schema for {}", schemaName);
      }
    }
    clientKeys = Collections.unmodifiableSet(keys);

    Map<String, String> map = new HashMap<>();
    for (String key: clientKeys) {
      for (String schemaName: BOOT_DATA_SCHEMAS) {
        if(!schemas.containsKey(schemaName)) continue;
        String value = schemas.get(schemaName).get(key);
        if (value != null) {
          map.put(key, value);
          break;
        }
      }
    };
    clientBootDefaults = Collections.unmodifiableMap(map);
  }

  private void updateMap(Map<String, String> map, Node node, String key) {
    // key for this node. Skip the <schema> node and nameless groups.
    if (node.getParent() != null && node.getName() != null)  {
      key = (key == null)? node.getName() : key + "." + node.getName();
    }
    if (node instanceof EntryNode) {
      if (node.getName() == null) {
        LOG.warn("Ignoring <{}> without name in {}.", node.getClass(), key);
        return;
      }
      // use computeIfAbsent (instead of putIfAbsent) to skip possible null
      // values (from node.getValue)
      map.computeIfAbsent(key, k -> ((EntryNode)node).getValue());
    } else {
      for (Node child: node.getChildren()) {
        updateMap(map, child, key);
      }
    }
  }
}
